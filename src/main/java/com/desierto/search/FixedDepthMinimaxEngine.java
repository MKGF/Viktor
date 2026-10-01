package com.desierto.search;

import com.desierto.domain.Board;
import com.desierto.domain.Color;
import com.desierto.domain.LegalMove;
import com.desierto.domain.Move;
import com.desierto.domain.MoveGenerator;
import com.desierto.search.evaluation.PositionEvaluator;
import com.desierto.search.evaluation.StandardPositionEvaluator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class FixedDepthMinimaxEngine implements SearchEngine {

  private final PositionEvaluator evaluator;
  private final MoveGenerator moveGenerator;

  public FixedDepthMinimaxEngine() {
    this(new StandardPositionEvaluator(), new MoveGenerator());
  }

  FixedDepthMinimaxEngine(PositionEvaluator evaluator, MoveGenerator moveGenerator) {
    this.evaluator = Objects.requireNonNull(evaluator, "evaluator must not be null");
    this.moveGenerator = Objects.requireNonNull(moveGenerator, "moveGenerator must not be null");
  }

  @Override
  public SearchResult findBestMove(Board board, SearchLimits limits) {
    Objects.requireNonNull(board, "board must not be null");
    Objects.requireNonNull(limits, "limits must not be null");

    Color perspective = board.getSideToMove();
    SearchContext context = new SearchContext();
    context.visitPosition();
    List<LegalMove> legalMoves = moveGenerator.generateLegalMoveApplications(board, perspective);
    if (legalMoves.isEmpty()) {
      return new SearchResult(Optional.empty(), perspective, evaluator.evaluate(board, perspective, 0), 0,
          context.nodes());
    }

    Move bestMove = null;
    int bestScore = Integer.MIN_VALUE;
    for (LegalMove legalMove : legalMoves) {
      Move move = legalMove.move();
      Board child = legalMove.resultingPosition();
      int score = minimax(child, limits.maxDepth() - 1, perspective, 1, context);
      if (score > bestScore) {
        bestScore = score;
        bestMove = move;
      }
    }
    return new SearchResult(Optional.of(bestMove), perspective, bestScore, limits.maxDepth(),
        context.nodes());
  }

  private int minimax(Board board, int remainingDepth, Color perspective, int plyFromRoot,
      SearchContext context) {
    context.visitPosition();
    if (remainingDepth == 0) {
      return evaluator.evaluate(board, perspective, plyFromRoot);
    }

    List<LegalMove> legalMoves = moveGenerator.generateLegalMoveApplications(board,
        board.getSideToMove());
    if (legalMoves.isEmpty()) {
      return evaluator.evaluate(board, perspective, plyFromRoot);
    }

    boolean maximizesScore = board.getSideToMove() == perspective;
    int bestScore = maximizesScore ? Integer.MIN_VALUE : Integer.MAX_VALUE;
    for (LegalMove legalMove : legalMoves) {
      Board child = legalMove.resultingPosition();
      int score = minimax(child, remainingDepth - 1, perspective, plyFromRoot + 1, context);
      bestScore = maximizesScore ? Math.max(bestScore, score) : Math.min(bestScore, score);
    }
    return bestScore;
  }

  private static final class SearchContext {

    private long nodes;

    void visitPosition() {
      nodes++;
    }

    long nodes() {
      return nodes;
    }
  }
}
