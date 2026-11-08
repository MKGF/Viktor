package com.desierto.search;

import com.desierto.domain.Board;
import com.desierto.domain.Color;
import com.desierto.domain.LegalMove;
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
        + " score=" + (result.fromBook() || result.tablebaseOutcome().isPresent()
            ? "unavailable" : score(result.score()).replace(' ', ':'));
  }

  public static String decision(String source, String selectedMove, SearchResult result, String details) {
    return formatDecision(source, selectedMove, result, details, null);
  }

  public static String decision(String source, String selectedMove, SearchResult result, String details,
      Board board) {
    return formatDecision(source, selectedMove, result, details,
        Objects.requireNonNull(board, "board must not be null"));
  }

  private static String formatDecision(String source, String selectedMove, SearchResult result,
      String details, Board board) {
    Objects.requireNonNull(source, "source must not be null");
    Objects.requireNonNull(selectedMove, "selectedMove must not be null");
    Objects.requireNonNull(details, "details must not be null");
    StringBuilder text = new StringBuilder("decision:\n  source=").append(source)
        .append("\n  selected_move=").append(selectedMove)
        .append("\n  score=").append(result == null || result.fromBook() || result.tablebaseOutcome().isPresent()
            ? "unavailable" : score(result.score()).replace(' ', ':'));
    if (result != null && result.bestMove().isPresent()) {
      if (result.fromBook()) {
        text.append("\n  reason=opening_book");
      } else if (result.tablebaseOutcome().isPresent()) {
        var outcome = result.tablebaseOutcome().orElseThrow();
        text.append("\n  reason=tablebase")
            .append("\n  wdl=").append(outcome.wdl().name().toLowerCase(Locale.ROOT))
            .append("\n  dtz=").append(outcome.dtz());
      } else {
        text.append("\n  reason=best_searched_score");
        if (board != null) {
          Board after = new LegalMove(board, result.bestMove().orElseThrow()).resultingPosition();
          Color perspective = board.getSideToMove();
          MaterialEvaluator material = new MaterialEvaluator();
          PositionalEvaluator positional = new PositionalEvaluator();
          int materialDelta = material.evaluate(after, perspective) - material.evaluate(board, perspective);
          int positionalDelta = positional.evaluate(after, perspective) - positional.evaluate(board, perspective);
          text.append("\n  static_perspective=").append(perspective.name().toLowerCase(Locale.ROOT))
              .append("\n  static_material_delta=cp:").append(materialDelta)
              .append("\n  static_positional_delta=cp:").append(positionalDelta);
        }
      }
    } else {
      text.append("\n  reason=").append(source);
    }
    if (result != null && !result.fromBook() && result.tablebaseOutcome().isEmpty()) {
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
