package org.example.comp3820.support;

import org.example.comp3820.ai.GeminiClient;
import org.example.comp3820.domain.model.CognitiveEngagement;
import org.example.comp3820.domain.model.PatientSnapshot;
import org.example.comp3820.domain.model.SleepRecord;
import org.example.comp3820.domain.model.StepRecord;
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
        new PatientChatService(gateway, gemini)
                .answer(PATIENT_ID, "How am I doing?", history);
        return gemini.prompt;
    }

    private PatientSnapshot plainSnapshot() {
        return snapshot(List.of(), List.of(), new CognitiveEngagement(0, 0));
    }

    private PatientSnapshot snapshot(List<SleepRecord> sleep, List<StepRecord> steps,
                                     CognitiveEngagement cognitive) {
        return new PatientSnapshot(72, "Female",
                List.of("Parkinson's disease", "Depressive disorder"), sleep, steps, cognitive);
    }

    @Test
    void carriesTheClinicalFactsTheRecordHolds() {
        String prompt = promptFor(snapshot(List.of(), List.of(), new CognitiveEngagement(0, 0)));

        assertThat(prompt)
                .contains("Age: 72")
                .contains("Gender: Female")
                .contains("Parkinson's disease")
                .contains("Depressive disorder")
                .contains("How am I doing?");
    }

    @Test
    void omitsSectionsTheRecordDoesNotHave() {
        String prompt = promptFor(snapshot(List.of(), List.of(), new CognitiveEngagement(0, 0)));

        // Absent is not the same as empty: telling the model a list is empty invites it to
        // fill the gap, whereas an absent heading is simply not a fact it was given.
        assertThat(prompt)
                .doesNotContain("Most recent sleep")
                .doesNotContain("Most recent step count")
                .doesNotContain("Care plan activities")
                .doesNotContain("[]");
    }

    @Test
    void includesMeasurementsWhenTheyArePresent() {
        String prompt = promptFor(snapshot(
                List.of(new SleepRecord(LocalDate.of(2026, 9, 16), 6.5)),
                List.of(new StepRecord(LocalDate.of(2026, 9, 16), 4280)),
                new CognitiveEngagement(2, 3)));

        assertThat(prompt)
                .contains("Most recent sleep: 6.5 hours on 2026-09-16")
                .contains("Most recent step count: 4,280 on 2026-09-16")
                .contains("Care plan activities completed this week: 2 of 3");
    }

    @Test
    void formatsNumbersInEnglishRegardlessOfTheHostLocale() {
        // The machine this was written on runs a zh_CN locale, where an unqualified
        // String.format produces different grouping and decimal separators. A prompt that
        // reads "4.280" to a model trained on English is a different number.
        java.util.Locale original = java.util.Locale.getDefault();
        try {
            java.util.Locale.setDefault(java.util.Locale.GERMANY);
            String prompt = promptFor(snapshot(
                    List.of(new SleepRecord(LocalDate.of(2026, 9, 16), 6.5)),
                    List.of(new StepRecord(LocalDate.of(2026, 9, 16), 4280)),
                    new CognitiveEngagement(0, 0)));

            assertThat(prompt).contains("6.5 hours").contains("4,280");
        } finally {
            java.util.Locale.setDefault(original);
        }
    }

    @Test
    void tellsTheModelNotToInventValues() {
        String prompt = promptFor(snapshot(List.of(), List.of(), new CognitiveEngagement(0, 0)));

        assertThat(prompt).contains("Never invent or estimate a number");
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
