package org.example.comp3820.support;

import org.example.comp3820.ai.GeminiClient;
import org.example.comp3820.domain.model.PatientSnapshot;
import org.example.comp3820.domain.model.ScoredAssessment;
import org.example.comp3820.fhir.ClinicalDataGateway;
import org.example.comp3820.fhir.InMemoryClinicalDataGateway;
import org.example.comp3820.model.ChatTurnDto;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Checks what actually goes into the prompt.
 *
 * <p>The prompt is the point at which this application discloses data to a third party, so what
 * it contains is worth asserting rather than assuming. These tests capture the prompt instead
 * of calling Gemini, which also keeps them fast and offline.
 */
class PatientChatServiceTest {

    private static final String PATIENT_ID = "patient-1";

    /** Captures the prompt instead of sending it anywhere. */
    private static final class CapturingGemini extends GeminiClient {
        private String prompt;

        private CapturingGemini() {
            super(null, "http://unused", "unused");
        }

        @Override
        public String complete(String prompt) {
            this.prompt = prompt;
            return "reply";
        }
    }

    private String promptFor(PatientSnapshot snapshot) {
        return promptFor(snapshot, List.of());
    }

    private String promptFor(PatientSnapshot snapshot, List<ChatTurnDto> history) {
        ClinicalDataGateway gateway = new InMemoryClinicalDataGateway()
                .withPatient(PATIENT_ID, snapshot);
        CapturingGemini gemini = new CapturingGemini();
        new PatientChatService(new RiskFactorService(gateway), gemini)
                .answer(PATIENT_ID, "How am I doing?", history);
        return gemini.prompt;
    }

    private PatientSnapshot plainSnapshot() {
        return snapshot(List.of(), List.of(), List.of());
    }

    private PatientSnapshot snapshot(List<ScoredAssessment> moca, List<ScoredAssessment> updrs,
                                     List<ScoredAssessment> gds) {
        return new PatientSnapshot(72, "Female",
                List.of("Parkinson's disease", "Depressive disorder"), moca, updrs, gds);
    }

    @Test
    void carriesTheDiagnosesTheRecordHolds() {
        String prompt = promptFor(plainSnapshot());

        assertThat(prompt)
                .contains("Parkinson's disease")
                .contains("Depressive disorder")
                .contains("How am I doing?");
    }

    @Test
    void omitsInstrumentsTheRecordDoesNotHave() {
        String prompt = promptFor(plainSnapshot());

        // Absent is not the same as empty: an explicit "no data" line invites the model to
        // speculate about why, whereas an absent heading is simply not a fact it was given.
        assertThat(prompt)
                .doesNotContain("MoCA")
                .doesNotContain("UPDRS")
                .doesNotContain("GDS")
                .doesNotContain("[]");
    }

    @Test
    void handsTheModelTheInterpretationRatherThanTheRawNumberAlone() {
        String prompt = promptFor(snapshot(
                List.of(new ScoredAssessment(LocalDate.of(2025, 3, 31), 5)),
                List.of(), List.of()));

        // The banding is computed before the prompt exists. The model is told what the score
        // means; it is never asked to work that out, because it would guess.
        assertThat(prompt)
                .contains("5 out of 30")
                .contains("screening threshold of 21")
                .contains("Threshold source: Dalrymple-Alford");
    }

    @Test
    void formatsNumbersInEnglishRegardlessOfTheHostLocale() {
        // The machine this was written on runs a zh_CN locale, where an unqualified
        // String.format produces different grouping and decimal separators.
        java.util.Locale original = java.util.Locale.getDefault();
        try {
            java.util.Locale.setDefault(java.util.Locale.GERMANY);
            String prompt = promptFor(snapshot(List.of(), List.of(
                    new ScoredAssessment(LocalDate.of(2025, 3, 31), 1234)), List.of()));

            assertThat(prompt).contains("1,234");
        } finally {
            java.util.Locale.setDefault(original);
        }
    }

    @Test
    void tellsTheModelNotToInventValues() {
        assertThat(promptFor(plainSnapshot()))
                .contains("Never invent or estimate a number");
    }

    @Test
    void tellsTheModelNotToDiagnose() {
        assertThat(promptFor(plainSnapshot()))
                .contains("Never tell them they have or do not have a condition");
    }

    // --- conversation memory ---------------------------------------------------------------

    @Test
    void carriesEarlierTurnsSoAFollowUpHasSomethingToReferTo() {
        String prompt = promptFor(plainSnapshot(), List.of(
                new ChatTurnDto("user", "Why does my balance feel worse?"),
                new ChatTurnDto("assistant", "Your record lists orthostatic hypotension.")));

        assertThat(prompt)
                .contains("The conversation so far:")
                .contains("Them: Why does my balance feel worse?")
                .contains("You: Your record lists orthostatic hypotension.");
    }

    @Test
    void leavesOutTheTranscriptHeadingOnTheFirstQuestion() {
        assertThat(promptFor(plainSnapshot())).doesNotContain("The conversation so far");
    }

    @Test
    void keepsOnlyTheMostRecentTurns() {
        List<ChatTurnDto> history = new java.util.ArrayList<>();
        for (int i = 0; i < 20; i++) {
            history.add(new ChatTurnDto("user", "question number " + i));
        }

        String prompt = promptFor(plainSnapshot(), history);

        // An unbounded conversation would grow the prompt without limit.
        assertThat(prompt).doesNotContain("question number 0")
                .contains("question number 19");
    }

    @Test
    void truncatesAnOverlongTurnRatherThanLettingItCrowdOutTheRest() {
        String huge = "x".repeat(5000);
        String prompt = promptFor(plainSnapshot(), List.of(new ChatTurnDto("user", huge)));

        assertThat(prompt).doesNotContain(huge).contains("…");
    }

    @Test
    void skipsBlankTurns() {
        String prompt = promptFor(plainSnapshot(), List.of(
                new ChatTurnDto("user", "   "),
                new ChatTurnDto("assistant", "A real answer.")));

        assertThat(prompt).contains("You: A real answer.").doesNotContain("Them: \n");
    }
}
