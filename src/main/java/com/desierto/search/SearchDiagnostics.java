package com.desierto.search;

import com.desierto.search.evaluation.StandardPositionEvaluator;
import java.util.Objects;

/** Formatting shared by the console and UCI search reports. */
public final class SearchDiagnostics {
  private SearchDiagnostics() {
  }

  public static String score(int score) {
    if (Math.abs((long) score) >= StandardPositionEvaluator.CHECKMATE_SCORE - 128
        && Math.abs((long) score) <= StandardPositionEvaluator.CHECKMATE_SCORE) {
      int plies = StandardPositionEvaluator.CHECKMATE_SCORE - Math.abs(score);
      int moves = (plies + 1) / 2;
      return "mate " + (score < 0 ? -moves : moves);
    }
    return "cp " + score;
  }

  public static String result(SearchResult result) {
    Objects.requireNonNull(result, "result must not be null");
    return "depth=" + result.depth() + " nodes=" + result.nodes()
        + " score=" + score(result.score()).replace(' ', ':');
  }
}
