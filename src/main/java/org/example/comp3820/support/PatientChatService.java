package org.example.comp3820.support;

import org.example.comp3820.ai.GeminiClient;
import org.example.comp3820.domain.model.PatientSnapshot;
import org.example.comp3820.domain.model.SleepRecord;
import org.example.comp3820.domain.model.StepRecord;
import org.example.comp3820.fhir.ClinicalDataGateway;
import org.springframework.stereotype.Service;

import java.util.Locale;

/**
 * Answers a patient's questions about their own record, in plain language.
 *
 * <p>The record is read as a {@link PatientSnapshot}, which carries no name, no identifier and
 * no date of birth — so nothing the patient could be identified by reaches Google. The display
 * name the front end greets them with comes from the separate profile path and never passes
 * through here.
 *
 * <p>Every clinical value in the prompt is copied from FHIR as a given fact. The model is asked
 * to explain and reassure, never to compute: a language model that invents a score or a date in
 * a clinical setting is worse than one that says nothing.
 */
@Service
public class PatientChatService {

    private static final String RULES = """
            You are a warm, plain-spoken assistant talking directly to a person living with \
            Parkinson's disease. You are given facts taken from their health record.

            Rules you must follow:
            1. Use only the facts given below. If the record does not answer the question, say \
               so plainly and suggest they ask their care team.
            2. Never invent or estimate a number, score, date or measurement. If a value is not \
               listed, it is not known to you.
            3. Keep it under four short sentences. Everyday words, no medical jargon, no lists.
            4. You do not diagnose and you do not give emergency advice. For new or worsening \
               symptoms, tell them to contact their healthcare professional.
            """;

    private final ClinicalDataGateway gateway;
    private final GeminiClient gemini;

    public PatientChatService(ClinicalDataGateway gateway, GeminiClient gemini) {
        this.gateway = gateway;
        this.gemini = gemini;
    }

    /** Answers a free-text question about the patient's own record. */
    public String answer(String patientId, String question) {
        PatientSnapshot snapshot = gateway.fetchSnapshot(patientId);
        return gemini.complete(RULES
                + "\n" + facts(snapshot)
                + "\nTheir question: " + question.strip());
    }

    /** A short opening message for when the patient first arrives. */
    public String opening(String patientId) {
        PatientSnapshot snapshot = gateway.fetchSnapshot(patientId);
        return gemini.complete(RULES
                + "\n" + facts(snapshot)
                + "\nThey have just opened the app. Greet them briefly and offer one everyday "
                + "suggestion that fits what the record shows. Do not list their diagnoses back "
                + "at them.");
    }

    /**
     * Renders the snapshot as the prompt's fact sheet.
     *
     * <p>Empty sections are left out rather than sent as "[]": telling the model a list is empty
     * invites it to fill the gap, whereas an absent heading simply is not a fact it has.
     */
    private String facts(PatientSnapshot snapshot) {
        StringBuilder out = new StringBuilder("Facts from their health record:\n");
        out.append("- Age: ").append(snapshot.age()).append('\n');
        out.append("- Gender: ").append(snapshot.gender()).append('\n');

        if (!snapshot.conditions().isEmpty()) {
            out.append("- Recorded conditions: ")
                    .append(String.join(", ", snapshot.conditions())).append('\n');
        }
        if (!snapshot.sleep().isEmpty()) {
            SleepRecord latest = snapshot.sleep().get(0);
            out.append(String.format(Locale.ENGLISH,
                    "- Most recent sleep: %.1f hours on %s%n", latest.hours(), latest.date()));
        }
        if (!snapshot.steps().isEmpty()) {
            StepRecord latest = snapshot.steps().get(0);
            out.append(String.format(Locale.ENGLISH,
                    "- Most recent step count: %,d on %s%n", latest.steps(), latest.date()));
        }
        if (snapshot.cognitive() != null && snapshot.cognitive().weeklyGoal() > 0) {
            out.append(String.format(Locale.ENGLISH,
                    "- Care plan activities completed this week: %d of %d%n",
                    snapshot.cognitive().completedThisWeek(),
                    snapshot.cognitive().weeklyGoal()));
        }
        return out.toString();
    }
}
