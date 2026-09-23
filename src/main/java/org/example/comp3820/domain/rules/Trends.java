package org.example.comp3820.domain.rules;

import org.example.comp3820.domain.model.ScoredAssessment;

import java.util.List;
import java.util.Locale;

/**
 * Describes how a series of scores has moved.
 *
 * <p>Direction matters and is not guessable from the numbers: a falling MoCA is worse, a
 * falling UPDRS is better. Callers say which, rather than this class assuming.
 *
 * <p>The wording stays factual — "down 16 points since 2021-04-01" — because the assistant is
 * given this as a fact to explain. Saying "deteriorating" here would put a clinical judgement
 * in the middle of what is supposed to be arithmetic.
 */
final class Trends {

    enum Direction { LOWER_IS_WORSE, HIGHER_IS_WORSE }

    private Trends() {
    }

    /** Null when there is only one recording, in which case there is no trend to report. */
    static String describe(List<ScoredAssessment> series, String unit, Direction direction) {
        if (series.size() < 2) {
            return null;
        }
        ScoredAssessment first = series.get(0);
        ScoredAssessment latest = series.get(series.size() - 1);
        double change = latest.score() - first.score();

        if (change == 0) {
            return String.format(Locale.ENGLISH,
                    "unchanged since %s, across %d recordings", first.date(), series.size());
        }

        boolean worse = direction == Direction.LOWER_IS_WORSE ? change < 0 : change > 0;
        return String.format(Locale.ENGLISH,
                "%s %,.0f %s since %s (%d recordings), which is a move in the %s direction",
                change < 0 ? "down" : "up",
                Math.abs(change), unit, first.date(), series.size(),
                worse ? "unfavourable" : "favourable");
    }
}
