package com.desierto.search;

import com.desierto.domain.Board;
import com.desierto.domain.Color;
import com.desierto.domain.LegalMove;
import com.desierto.domain.Move;
import com.desierto.domain.MoveGenerator;
import com.desierto.domain.SearchPosition;
import com.desierto.search.evaluation.PositionEvaluator;
import com.desierto.search.evaluation.StandardPositionEvaluator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;

public final class FixedDepthAlphaBetaEngine implements SearchEngine {

  private final PositionEvaluator evaluator;
  private final MoveGenerator moveGenerator;
  private final MoveOrderingStrategy moveOrderingStrategy;
  private final QuiescenceSearch quiescenceSearch;
  private final boolean transpositionsEnabled;

  public FixedDepthAlphaBetaEngine() {
    this(true);
  }

  public FixedDepthAlphaBetaEngine(boolean quiescenceEnabled) {
    this(quiescenceEnabled, true);
  }

  public FixedDepthAlphaBetaEngine(boolean quiescenceEnabled, boolean transpositionsEnabled) {
    this(new StandardPositionEvaluator(), new MoveGenerator(), new TacticalMoveOrderingStrategy(),
        quiescenceEnabled, transpositionsEnabled);
  }

  FixedDepthAlphaBetaEngine(PositionEvaluator evaluator, MoveGenerator moveGenerator) {
    this(evaluator, moveGenerator, new TacticalMoveOrderingStrategy());
  }

  FixedDepthAlphaBetaEngine(PositionEvaluator evaluator, MoveGenerator moveGenerator,
      MoveOrderingStrategy moveOrderingStrategy) {
    this(evaluator, moveGenerator, moveOrderingStrategy, true);
  }

  FixedDepthAlphaBetaEngine(PositionEvaluator evaluator, MoveGenerator moveGenerator,
      MoveOrderingStrategy moveOrderingStrategy, boolean quiescenceEnabled) {
    this(evaluator, moveGenerator, moveOrderingStrategy, quiescenceEnabled, false);
  }

  FixedDepthAlphaBetaEngine(PositionEvaluator evaluator, MoveGenerator moveGenerator,
      MoveOrderingStrategy moveOrderingStrategy, boolean quiescenceEnabled,
      boolean transpositionsEnabled) {
    this.evaluator = Objects.requireNonNull(evaluator, "evaluator must not be null");
    this.moveGenerator = Objects.requireNonNull(moveGenerator, "moveGenerator must not be null");
    this.moveOrderingStrategy = Objects.requireNonNull(moveOrderingStrategy,
        "moveOrderingStrategy must not be null");
    this.quiescenceSearch = quiescenceEnabled
        ? new QuiescenceSearch(evaluator, moveGenerator, moveOrderingStrategy) : null;
    this.transpositionsEnabled = transpositionsEnabled;
  }

  @Override
  public SearchResult findBestMove(Board board, SearchLimits limits) {
    return findBestMove(board, limits, Optional.empty());
  }

  public SearchResult findBestMove(Board board, SearchLimits limits,
      Optional<MoveKey> preferredMove) {
    return findBestMove(board, limits, preferredMove, new SearchContext());
  }

  SearchResult findBestMove(Board board, SearchLimits limits, Optional<MoveKey> preferredMove,
      SearchContext context) {
    Objects.requireNonNull(board, "board must not be null");
    Objects.requireNonNull(limits, "limits must not be null");
    Objects.requireNonNull(preferredMove, "preferredMove must not be null");
    Objects.requireNonNull(context, "context must not be null");
    if (transpositionsEnabled) {
      context.enableTranspositions();
    }

    SearchPosition position = new SearchPosition(board);
    Board workingBoard = position.board();
    Color perspective = workingBoard.getSideToMove();
    context.visitPosition();
    List<LegalMove> legalMoves = moveGenerator.generateLegalMoveApplications(workingBoard, perspective);
    context.checkTime();
    if (legalMoves.isEmpty()) {
      int score = evaluator.evaluate(workingBoard, perspective, 0);
      context.checkTime();
      return new SearchResult(Optional.empty(), perspective, score, 0, context.nodes());
    }

    Move bestMove = null;
    int bestScore = Integer.MIN_VALUE;
    int alpha = Integer.MIN_VALUE;
    int beta = Integer.MAX_VALUE;
    List<LegalMove> orderedMoves = moveOrderingStrategy.order(workingBoard, legalMoves, preferredMove);
    context.checkTime();
    for (LegalMove legalMove : orderedMoves) {
      Move move = legalMove.move();
      int score;
      try (SearchPosition.MoveScope ignored = position.apply(legalMove)) {
        score = alphaBeta(position, limits.maxDepth() - 1, perspective, 1, alpha, beta, context);
      }
      if (score > bestScore) {
        bestScore = score;
        bestMove = move;
      }
      alpha = Math.max(alpha, bestScore);
    }
    context.checkTime();
    return new SearchResult(Optional.of(position.originalMove(bestMove)), perspective, bestScore, limits.maxDepth(),
        context.nodes());
  }

  private int alphaBeta(SearchPosition position, int remainingDepth, Color perspective, int plyFromRoot, int alpha,
      int beta, SearchContext context) {
    Board board = position.board();
    if (remainingDepth == 0 && quiescenceSearch != null) {
      return quiescenceSearch.search(position, perspective, plyFromRoot, alpha, beta, context);
    }
    context.visitPosition();
    if (remainingDepth == 0) {
      int score = evaluator.evaluate(board, perspective, plyFromRoot);
      context.checkTime();
      return score;
    }

    int originalAlpha = alpha;
    int originalBeta = beta;
    long key = 0;
    if (context.table != null) {
      key = PositionHash.of(board);
      TranspositionTable.Entry entry = context.table.find(key, remainingDepth, plyFromRoot);
      if (entry != null && entry.canCutOff(alpha, beta)) {
        return entry.score();
      }
    }

    List<LegalMove> legalMoves = moveGenerator.generateLegalMoveApplications(board,
        board.getSideToMove());
    context.checkTime();
    if (legalMoves.isEmpty()) {
      int score = evaluator.evaluate(board, perspective, plyFromRoot);
      context.checkTime();
      if (context.table != null) {
        context.table.store(key, remainingDepth, plyFromRoot, score, originalAlpha, originalBeta);
      }
      return score;
    }

    if (board.getSideToMove() == perspective) {
      int bestScore = Integer.MIN_VALUE;
      for (LegalMove legalMove : orderMoves(board, legalMoves, context)) {
        int score;
        try (SearchPosition.MoveScope ignored = position.apply(legalMove)) {
          score = alphaBeta(position, remainingDepth - 1, perspective, plyFromRoot + 1, alpha, beta,
              context);
        }
        bestScore = Math.max(bestScore, score);
        alpha = Math.max(alpha, bestScore);
        if (alpha >= beta) {
          break;
        }
      }
      if (context.table != null) {
        context.table.store(key, remainingDepth, plyFromRoot, bestScore, originalAlpha, originalBeta);
      }
      return bestScore;
    }

    int bestScore = Integer.MAX_VALUE;
    for (LegalMove legalMove : orderMoves(board, legalMoves, context)) {
      int score;
      try (SearchPosition.MoveScope ignored = position.apply(legalMove)) {
        score = alphaBeta(position, remainingDepth - 1, perspective, plyFromRoot + 1, alpha, beta,
            context);
      }
      bestScore = Math.min(bestScore, score);
      beta = Math.min(beta, bestScore);
      if (alpha >= beta) {
        break;
      }
    }
    if (context.table != null) {
      context.table.store(key, remainingDepth, plyFromRoot, bestScore, originalAlpha, originalBeta);
    }
    return bestScore;
  }

  private List<LegalMove> orderMoves(Board board, List<LegalMove> legalMoves, SearchContext context) {
    List<LegalMove> orderedMoves = moveOrderingStrategy.order(board, legalMoves, Optional.empty());
    context.checkTime();
    return orderedMoves;
  }

  static final class SearchContext {

    private long nodes;
    private final BooleanSupplier timeExpired;
    private TranspositionTable table;

    SearchContext() {
      this(() -> false);
    }

    SearchContext(BooleanSupplier timeExpired) {
      this.timeExpired = Objects.requireNonNull(timeExpired, "timeExpired must not be null");
    }

    void checkTime() {
      if (timeExpired.getAsBoolean()) {
        throw new SearchDeadlineExceeded();
      }
    }

    void enableTranspositions() {
      if (table == null) {
        table = new TranspositionTable(TranspositionTable.DEFAULT_CAPACITY);
      }
    }

    void visitPosition() {
      checkTime();
      nodes++;
    }

    long nodes() {
      return nodes;
    }
  }

  static final class SearchDeadlineExceeded extends RuntimeException {

    SearchDeadlineExceeded() {
      super(null, null, false, false);
    }
  }
}
