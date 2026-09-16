package org.example.comp3820.domain.rules;

import org.example.comp3820.domain.model.PatientSnapshot;
import org.example.comp3820.domain.model.RiskFactor;
import org.example.comp3820.domain.model.RiskLevel;
import org.example.comp3820.domain.model.SleepRecord;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Turns recent sleep observations into the deterministic half of the Sleep card.
 *
 * <p>Short sleep is one of the modifiable dementia risk factors named in the project brief, so
 * the card reports how often the patient fell below a target duration rather than judging any
 * single night — one short night is noise, a pattern is not.
 *
 * <p><strong>Thresholds are provisional.</strong> {@link #TARGET_HOURS} and
 * {@link #SHORT_NIGHTS_FOR_ATTENTION} are placeholders chosen to be defensible, not clinically
 * ratified. They must be confirmed with the project mentor and cited in the report before
 * submission; they are constants precisely so that changing them is a one-line edit.
 */
public final class SleepRiskRule {

    /** Nights shorter than this count as short. */
    public static final double TARGET_HOURS = 7.0;

    /** How many nights in the window may look at the whole week of data. */
    public static final int WINDOW_NIGHTS = 7;

    /** Short nights within the window before we raise the card to "Worth attention". */
    public static final int SHORT_NIGHTS_FOR_ATTENTION = 3;

    private SleepRiskRule() {
    }

    /**
     * @param snapshot the patient's recent data
     * @param today    the date to interpret "last night" against, passed in so the rule stays
     *                 deterministic under test rather than reading the system clock
     */
    public static RiskFactor evaluate(PatientSnapshot snapshot, LocalDate today) {
        List<SleepRecord> window = snapshot.sleep().stream()
                .sorted(Comparator.comparing(SleepRecord::date).reversed())
                .limit(WINDOW_NIGHTS)
                .toList();

        if (window.isEmpty()) {
            return new RiskFactor("Sleep", "Observation", "No recent readings",
                    "We will show this once sleep is recorded", RiskLevel.IN_PROGRESS);
        }

        SleepRecord latest = window.get(0);
        long shortNights = window.stream().filter(n -> n.hours() < TARGET_HOURS).count();

        RiskLevel level;
        String note;
        if (shortNights >= SHORT_NIGHTS_FOR_ATTENTION) {
            level = RiskLevel.WORTH_ATTENTION;
            note = "A small change could help";
        } else if (shortNights > 0) {
            level = RiskLevel.IN_PROGRESS;
            note = "Mostly steady this week";
        } else {
            level = RiskLevel.ON_TRACK;
            note = "Your sleep is holding steady";
        }

        return new RiskFactor("Sleep", "Observation", describe(latest, today), note, level);
    }

    private static String describe(SleepRecord record, LocalDate today) {
        int totalMinutes = (int) Math.round(record.hours() * 60);
        int hours = totalMinutes / 60;
        int minutes = totalMinutes % 60;
        String duration = minutes == 0
                ? String.format(Locale.ENGLISH, "%d h", hours)
                : String.format(Locale.ENGLISH, "%d h %d min", hours, minutes);
        String when = record.date().equals(today.minusDays(1))
                ? "last night"
                : "on " + record.date();
        return duration + " " + when;
    }
}
