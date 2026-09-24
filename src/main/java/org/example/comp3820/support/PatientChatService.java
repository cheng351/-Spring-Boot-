package org.example.comp3820.support;

import org.example.comp3820.ai.GeminiClient;
import org.example.comp3820.domain.model.ClinicalFinding;
import org.example.comp3820.domain.model.RiskProfile;
import org.example.comp3820.model.ChatTurnDto;
import org.springframework.stereotype.Service;

import java.util.List;
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

    /** How many recent turns to carry. Enough for a follow-up to make sense, bounded on purpose. */
    private static final int MAX_TURNS = 12;

    /** Per-turn cap, so one very long message cannot crowd out the rest of the prompt. */
    private static final int MAX_TURN_CHARS = 1000;

    private static final String RULES = """
            You are a warm, plain-spoken assistant talking with someone living with Parkinson's \
            disease. Below are facts already established from their health record, including \
            what their scores mean against published screening thresholds.

            How to talk:
            - Speak the way a kind person would, not a form letter. Vary how you open. It is \
              fine to acknowledge how something might feel before explaining it.
            - Two to five sentences. Everyday words. No lists, no medical jargon.

            What you may say:
            - Explain what the scores and thresholds below mean in plain language. That \
              interpretation has already been worked out for you - use it.
            - Point out what has changed over time when the facts show a trend.
            - Suggest practical, everyday things within what the record supports.

            What you must not do:
            - Never invent or estimate a number, score, date or measurement. If a value is not \
              listed below, you do not know it, and you should say so.
            - Never tell them they have or do not have a condition. The thresholds below are \
              screening thresholds: crossing one is a reason to talk to a clinician, not a \
              diagnosis. Say it that way.
            - Do not give emergency advice.

            Mention contacting their healthcare professional when it is actually warranted - a \
            new or worsening symptom, a score that has crossed a threshold, a decision about \
            treatment. Ending every answer that way makes the advice easy to ignore.
            """;

    private final RiskFactorService riskFactors;
    private final GeminiClient gemini;

    public PatientChatService(RiskFactorService riskFactors, GeminiClient gemini) {
        this.riskFactors = riskFactors;
        this.gemini = gemini;
    }

    /**
     * Answers a free-text question about the patient's own record.
     *
     * @param history earlier turns, oldest first. Without it a follow-up like "tell me more"
     *                has nothing to refer to, because each request would otherwise arrive as
     *                the first thing the patient had ever said.
     */
    public String answer(String patientId, String question, List<ChatTurnDto> history) {
        RiskProfile profile = riskFactors.profileFor(patientId);
        return gemini.complete(RULES
                + "\n" + facts(profile)
                + transcript(history)
                + "\nTheir question: " + question.strip());
    }

    /** Convenience for a first question, with nothing said before it. */
    public String answer(String patientId, String question) {
        return answer(patientId, question, List.of());
    }

    /**
     * Renders recent turns as a transcript.
     *
     * <p>Only the tail is kept. An unbounded conversation would grow the prompt without limit,
     * and the older it gets the less it bears on what is being asked now.
     */
    private String transcript(List<ChatTurnDto> history) {
        if (history == null || history.isEmpty()) {
            return "";
        }
        List<ChatTurnDto> recent = history.size() > MAX_TURNS
                ? history.subList(history.size() - MAX_TURNS, history.size())
                : history;

        StringBuilder out = new StringBuilder("\nThe conversation so far:\n");
        for (ChatTurnDto turn : recent) {
            String speaker = "user".equalsIgnoreCase(turn.role()) ? "Them" : "You";
            String text = turn.text() == null ? "" : turn.text().strip();
            if (text.isEmpty()) {
                continue;
            }
            if (text.length() > MAX_TURN_CHARS) {
                text = text.substring(0, MAX_TURN_CHARS) + "…";
            }
            out.append(speaker).append(": ").append(text).append('\n');
        }
        return out.toString();
    }

    /** A short opening message for when the patient first arrives. */
    public String opening(String patientId) {
        RiskProfile profile = riskFactors.profileFor(patientId);
        return gemini.complete(RULES
                + "\n" + facts(profile)
                + "\nThey have just opened the app. Greet them briefly and offer one everyday "
                + "suggestion that fits what the record shows. Do not list their diagnoses back "
                + "at them.");
    }

    /**
     * Renders the profile as the prompt's fact sheet.
     *
     * <p>Every number and every threshold here was computed by a rule before this method ran.
     * The model is being told what is true, not asked to work it out — which is the whole
     * reason the rules exist. Instruments the record does not carry produce no line at all.
     */
    private String facts(RiskProfile profile) {
        StringBuilder out = new StringBuilder("Facts established from their health record:\n");

        if (!profile.conditions().isEmpty()) {
            out.append("- Recorded diagnoses: ")
                    .append(String.join(", ", profile.conditions())).append('\n');
        }

        for (ClinicalFinding finding : profile.findings()) {
            out.append("- ").append(finding.label()).append(": ").append(finding.measurement());
            if (finding.interpretation() != null) {
                out.append(". This is ").append(finding.interpretation());
            }
            if (finding.trend() != null) {
                out.append(". Over time it is ").append(finding.trend());
            }
            if (finding.basis() != null) {
                out.append(". Threshold source: ").append(finding.basis());
            }
            out.append(".\n");
        }
        return out.toString();
    }
}
