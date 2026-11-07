package com.desierto.search;

import com.desierto.domain.Board;
import com.desierto.domain.Color;
import com.desierto.search.evaluation.MaterialEvaluator;
import com.desierto.search.evaluation.PositionalEvaluator;
import com.desierto.search.evaluation.StandardPositionEvaluator;
import java.util.Locale;
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

  public static String decision(String source, String selectedMove, SearchResult result, String details) {
    Objects.requireNonNull(source, "source must not be null");
    Objects.requireNonNull(selectedMove, "selectedMove must not be null");
    Objects.requireNonNull(details, "details must not be null");
    StringBuilder text = new StringBuilder("decision:\n  source=").append(source)
        .append("\n  selected_move=").append(selectedMove)
        .append("\n  score=").append(result == null ? "unavailable" : score(result.score()).replace(' ', ':'));
    if (result != null) {
      text.append("\n  score_perspective=").append(result.perspective().name().toLowerCase(Locale.ROOT));
    }
    text.append("\n  pv=");
    if (result == null || result.trace().principalVariation().isEmpty()) {
      text.append("unavailable");
    } else {
      text.append(String.join(" ", result.trace().principalVariation()));
    }
    if (result == null || result.trace().alternatives().isEmpty()) {
      text.append("\n  alternatives=[]");
    } else {
      text.append("\n  alternatives:");
      for (SearchTrace.Alternative alternative : result.trace().alternatives()) {
        text.append("\n    - move=").append(alternative.move())
            .append(" score=").append(score(alternative.score()).replace(' ', ':'))
            .append(" bound=").append(alternative.bound().name().toLowerCase(Locale.ROOT));
      }
    }
    text.append("\n  ").append(details);
    if (result == null) {
      text.append(" depth=unavailable nodes=unavailable");
    } else {
      text.append(" depth=").append(result.depth()).append(" nodes=").append(result.nodes());
    }
    return text.toString();
  }

  public static String position(Board board, String stage) {
    Objects.requireNonNull(board, "board must not be null");
    Objects.requireNonNull(stage, "stage must not be null");
    int evaluation = new StandardPositionEvaluator().evaluate(board, Color.WHITE);
    int material = new MaterialEvaluator().evaluate(board, Color.WHITE);
    int positional = new PositionalEvaluator().evaluate(board, Color.WHITE);
    return "position:\n  stage=" + stage
        + "\n  perspective=white\n  side_to_move=" + board.getSideToMove().name().toLowerCase(Locale.ROOT)
        + "\n  evaluation=" + score(evaluation).replace(' ', ':')
        + "\n  material=cp:" + material + "\n  positional=cp:" + positional;
  }
}
