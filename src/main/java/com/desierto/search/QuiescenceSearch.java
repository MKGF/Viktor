package com.desierto.search;

import com.desierto.domain.Board;
import com.desierto.domain.Color;
import com.desierto.domain.LegalMove;
import com.desierto.domain.MoveGenerator;
import com.desierto.search.evaluation.PositionEvaluator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

final class QuiescenceSearch {

  static final int MAX_PLY = 8;

  private final PositionEvaluator evaluator;
  private final MoveGenerator moveGenerator;
  private final MoveOrderingStrategy moveOrderingStrategy;
  private final int maxPly;

  QuiescenceSearch(PositionEvaluator evaluator, MoveGenerator moveGenerator,
      MoveOrderingStrategy moveOrderingStrategy) {
    this(evaluator, moveGenerator, moveOrderingStrategy, MAX_PLY);
  }

  QuiescenceSearch(PositionEvaluator evaluator, MoveGenerator moveGenerator,
      MoveOrderingStrategy moveOrderingStrategy, int maxPly) {
    this.evaluator = Objects.requireNonNull(evaluator, "evaluator must not be null");
    this.moveGenerator = Objects.requireNonNull(moveGenerator, "moveGenerator must not be null");
    this.moveOrderingStrategy = Objects.requireNonNull(moveOrderingStrategy,
        "moveOrderingStrategy must not be null");
    if (maxPly < 0 || maxPly > MAX_PLY) {
      throw new IllegalArgumentException("maxPly must be between 0 and " + MAX_PLY);
    }
    this.maxPly = maxPly;
  }

  int search(Board board, Color perspective, int plyFromRoot, int alpha, int beta,
      FixedDepthAlphaBetaEngine.SearchContext context) {
    return search(board, perspective, plyFromRoot, 0, alpha, beta, context);
  }

  private int search(Board board, Color perspective, int plyFromRoot, int quiescencePly,
      int alpha, int beta, FixedDepthAlphaBetaEngine.SearchContext context) {
    context.visitPosition();
    if (quiescencePly == maxPly) {
      return evaluate(board, perspective, plyFromRoot, context);
    }

    List<LegalMove> legalMoves = moveGenerator.generateLegalMoveApplications(board,
        board.getSideToMove());
    context.checkTime();
    if (legalMoves.isEmpty()) {
      return evaluate(board, perspective, plyFromRoot, context);
    }

    boolean inCheck = moveGenerator.isKingInCheck(board, board.getSideToMove());
    context.checkTime();
    boolean maximizing = board.getSideToMove() == perspective;
    int bestScore;
    if (inCheck) {
      bestScore = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
    } else {
      bestScore = evaluate(board, perspective, plyFromRoot, context);
      if (maximizing) {
        if (bestScore >= beta) {
          return bestScore;
        }
        alpha = Math.max(alpha, bestScore);
      } else {
        if (bestScore <= alpha) {
          return bestScore;
        }
        beta = Math.min(beta, bestScore);
      }
      legalMoves = legalMoves.stream()
          .filter(move -> move.move().isCapture() || move.move().isPromotion())
          .toList();
    }

    if (legalMoves.isEmpty()) {
      return bestScore;
    }
    List<LegalMove> orderedMoves = moveOrderingStrategy.order(board, legalMoves, Optional.empty());
    context.checkTime();
    for (LegalMove move : orderedMoves) {
      int score = search(move.resultingPosition(), perspective, plyFromRoot + 1,
          quiescencePly + 1, alpha, beta, context);
      if (maximizing) {
        bestScore = Math.max(bestScore, score);
        alpha = Math.max(alpha, bestScore);
      } else {
        bestScore = Math.min(bestScore, score);
        beta = Math.min(beta, bestScore);
      }
      if (alpha >= beta) {
        break;
      }
    }
    return bestScore;
  }

  private int evaluate(Board board, Color perspective, int plyFromRoot,
      FixedDepthAlphaBetaEngine.SearchContext context) {
    int score = evaluator.evaluate(board, perspective, plyFromRoot);
    context.checkTime();
    return score;
  }
}
