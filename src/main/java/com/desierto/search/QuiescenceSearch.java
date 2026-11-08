package com.desierto.search;

import com.desierto.domain.Board;
import com.desierto.domain.Color;
import com.desierto.domain.DrawDetector;
import com.desierto.domain.LegalMove;
import com.desierto.domain.MoveGenerator;
import com.desierto.domain.SearchPosition;
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
      AlphaBetaSearch.SearchContext context) {
    return search(new SearchPosition(board), perspective, plyFromRoot, alpha, beta, context);
  }

  int search(SearchPosition position, Color perspective, int plyFromRoot, int alpha, int beta,
      AlphaBetaSearch.SearchContext context) {
    return search(position, perspective, plyFromRoot, 0, alpha, beta, context);
  }

  private int search(SearchPosition position, Color perspective, int plyFromRoot, int quiescencePly,
      int alpha, int beta, AlphaBetaSearch.SearchContext context) {
    Board board = position.board();
    context.visitPosition();
    if (DrawDetector.isDeadByInsufficientMaterial(board)) {
      context.checkTime();
      return 0;
    }
    if (DrawDetector.isSeventyFiveMoveDraw(board, moveGenerator)
        || DrawDetector.isThreefoldRepetitionDraw(board, moveGenerator)) {
      context.checkTime();
      return 0;
    }
    if (quiescencePly == maxPly) {
      int score = evaluate(board, perspective, plyFromRoot, context);
      if (SearchDrawRules.canClaim(board, moveGenerator)) {
        score = SearchDrawRules.withClaimOption(score, board, perspective);
      }
      context.checkTime();
      return score;
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
    boolean canClaim = SearchDrawRules.claim(board, legalMoves).isPresent();
    int bestScore;
    if (inCheck) {
      bestScore = canClaim ? 0 : maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
    } else {
      bestScore = evaluate(board, perspective, plyFromRoot, context);
      if (canClaim) {
        bestScore = SearchDrawRules.withClaimOption(bestScore, board, perspective);
      }
      legalMoves = legalMoves.stream()
          .filter(move -> move.move().isCapture() || move.move().isPromotion())
          .toList();
    }

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

    if (legalMoves.isEmpty()) {
      return bestScore;
    }
    List<LegalMove> orderedMoves = moveOrderingStrategy.order(board, legalMoves, Optional.empty());
    context.checkTime();
    for (LegalMove move : orderedMoves) {
      int score;
      try (SearchPosition.MoveScope ignored = position.apply(move)) {
        score = search(position, perspective, plyFromRoot + 1,
            quiescencePly + 1, alpha, beta, context);
      }
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
      AlphaBetaSearch.SearchContext context) {
    int score = evaluator.evaluate(board, perspective, plyFromRoot);
    context.checkTime();
    return score;
  }
}
