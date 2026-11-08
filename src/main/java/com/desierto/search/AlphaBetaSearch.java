package com.desierto.search;

import com.desierto.domain.Board;
import com.desierto.domain.Color;
import com.desierto.domain.DrawDetector;
import com.desierto.domain.LegalMove;
import com.desierto.domain.Move;
import com.desierto.domain.MoveGenerator;
import com.desierto.domain.MoveNotation;
import com.desierto.domain.SearchPosition;
import com.desierto.search.evaluation.PositionEvaluator;
import com.desierto.search.evaluation.StandardPositionEvaluator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BooleanSupplier;

public final class AlphaBetaSearch implements SearchAlgorithm {

  private final PositionEvaluator evaluator;
  private final MoveGenerator moveGenerator;
  private final MoveOrderingStrategy moveOrderingStrategy;
  private final QuiescenceSearch quiescenceSearch;
  private final boolean transpositionsEnabled;

  public AlphaBetaSearch() {
    this(true);
  }

  public AlphaBetaSearch(boolean quiescenceEnabled) {
    this(quiescenceEnabled, true);
  }

  public AlphaBetaSearch(boolean quiescenceEnabled, boolean transpositionsEnabled) {
    this(new StandardPositionEvaluator(), new MoveGenerator(), new TacticalMoveOrderingStrategy(),
        quiescenceEnabled, transpositionsEnabled);
  }

  AlphaBetaSearch(PositionEvaluator evaluator, MoveGenerator moveGenerator) {
    this(evaluator, moveGenerator, new TacticalMoveOrderingStrategy());
  }

  AlphaBetaSearch(PositionEvaluator evaluator, MoveGenerator moveGenerator,
      MoveOrderingStrategy moveOrderingStrategy) {
    this(evaluator, moveGenerator, moveOrderingStrategy, true);
  }

  AlphaBetaSearch(PositionEvaluator evaluator, MoveGenerator moveGenerator,
      MoveOrderingStrategy moveOrderingStrategy, boolean quiescenceEnabled) {
    this(evaluator, moveGenerator, moveOrderingStrategy, quiescenceEnabled, false);
  }

  AlphaBetaSearch(PositionEvaluator evaluator, MoveGenerator moveGenerator,
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
    if (isAutomaticDraw(workingBoard)) {
      context.checkTime();
      return new SearchResult(Optional.empty(), perspective, 0, 0, context.nodes());
    }

    return searchRootMoves(position, legalMoves, limits.maxDepth(), perspective, preferredMove, context);
  }

  private SearchResult searchRootMoves(SearchPosition position, List<LegalMove> legalMoves, int depth,
      Color perspective, Optional<MoveKey> preferredMove, SearchContext context) {
    Board workingBoard = position.board();
    Optional<DrawClaim> claim = SearchDrawRules.claim(workingBoard, legalMoves);
    Move bestMove = null;
    int bestScore = claim.isPresent() ? 0 : Integer.MIN_VALUE;
    int alpha = bestScore;
    int beta = Integer.MAX_VALUE;
    List<SearchTrace.Alternative> alternatives = new ArrayList<>();
    List<String> bestLine = List.of();
    List<LegalMove> orderedMoves = moveOrderingStrategy.order(workingBoard, legalMoves, preferredMove);
    context.checkTime();
    for (LegalMove legalMove : orderedMoves) {
      Move move = legalMove.move();
      int score;
      int initialAlpha = alpha;
      try (SearchPosition.MoveScope ignored = position.exploreMove(legalMove)) {
        score = alphaBeta(position, depth - 1, perspective, 1, alpha, beta, context);
      }
      String notation = MoveNotation.format(move);
      alternatives.add(new SearchTrace.Alternative(notation, score,
          score <= initialAlpha ? SearchTrace.Bound.UPPER : SearchTrace.Bound.EXACT));
      if (score > bestScore) {
        bestScore = score;
        bestMove = move;
        bestLine = new ArrayList<>();
        bestLine.add(notation);
        bestLine.addAll(context.pvLine(1));
      }
      alpha = Math.max(alpha, bestScore);
    }
    context.checkTime();
    alternatives.sort(Comparator.comparingInt(SearchTrace.Alternative::score).reversed());
    List<SearchTrace.Alternative> topAlternatives = List.copyOf(
        alternatives.subList(0, Math.min(3, alternatives.size())));
    if (bestMove == null && claim.isPresent()) {
      Optional<Move> intended = claim.orElseThrow().intendedMove().map(position::originalMove);
      return new SearchResult(Optional.empty(), perspective, 0, depth, context.nodes(),
          Optional.of(new DrawClaim(intended)), new SearchTrace(List.of(), topAlternatives));
    }
    return new SearchResult(Optional.of(position.originalMove(bestMove)), perspective, bestScore, depth,
        context.nodes(), Optional.empty(), new SearchTrace(bestLine, topAlternatives));
  }

  private int alphaBeta(SearchPosition position, int remainingDepth, Color perspective, int plyFromRoot, int alpha,
      int beta, SearchContext context) {
    List<String> principalVariation = context.pvLine(plyFromRoot);
    principalVariation.clear();
    Board board = position.board();
    if (remainingDepth == 0 && quiescenceSearch != null) {
      return quiescenceSearch.search(position, perspective, plyFromRoot, alpha, beta, context);
    }
    context.visitPosition();
    if (isAutomaticDraw(board)) {
      context.checkTime();
      return 0;
    }
    if (remainingDepth == 0) {
      return evaluateLeaf(board, perspective, plyFromRoot, context);
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

    int bestScore;
    if (board.getSideToMove() == perspective) {
      bestScore = maximizeScore(position, legalMoves, remainingDepth, perspective, plyFromRoot,
          alpha, beta, context);
    } else {
      bestScore = minimizeScore(position, legalMoves, remainingDepth, perspective, plyFromRoot,
          alpha, beta, context);
    }
    if (context.table != null) {
      context.table.store(key, remainingDepth, plyFromRoot, bestScore, originalAlpha, originalBeta);
    }
    return bestScore;
  }

  private int maximizeScore(SearchPosition position, List<LegalMove> legalMoves, int remainingDepth,
      Color perspective, int plyFromRoot, int alpha, int beta, SearchContext context) {
    Board board = position.board();
    int bestScore = SearchDrawRules.claim(board, legalMoves).isPresent() ? 0 : Integer.MIN_VALUE;
    alpha = Math.max(alpha, bestScore);
    for (LegalMove legalMove : orderMoves(board, legalMoves, context)) {
      int score;
      try (SearchPosition.MoveScope ignored = position.exploreMove(legalMove)) {
        score = alphaBeta(position, remainingDepth - 1, perspective, plyFromRoot + 1, alpha, beta,
            context);
      }
      if (score > bestScore) {
        updatePrincipalVariation(context, plyFromRoot, legalMove.move());
      }
      bestScore = Math.max(bestScore, score);
      alpha = Math.max(alpha, bestScore);
      if (alpha >= beta) {
        break;
      }
    }
    return bestScore;
  }

  private int minimizeScore(SearchPosition position, List<LegalMove> legalMoves, int remainingDepth,
      Color perspective, int plyFromRoot, int alpha, int beta, SearchContext context) {
    Board board = position.board();
    int bestScore = SearchDrawRules.claim(board, legalMoves).isPresent() ? 0 : Integer.MAX_VALUE;
    beta = Math.min(beta, bestScore);
    for (LegalMove legalMove : orderMoves(board, legalMoves, context)) {
      int score;
      try (SearchPosition.MoveScope ignored = position.exploreMove(legalMove)) {
        score = alphaBeta(position, remainingDepth - 1, perspective, plyFromRoot + 1, alpha, beta,
            context);
      }
      if (score < bestScore) {
        updatePrincipalVariation(context, plyFromRoot, legalMove.move());
      }
      bestScore = Math.min(bestScore, score);
      beta = Math.min(beta, bestScore);
      if (alpha >= beta) {
        break;
      }
    }
    return bestScore;
  }

  private boolean isAutomaticDraw(Board board) {
    return DrawDetector.isDeadByInsufficientMaterial(board)
        || DrawDetector.isSeventyFiveMoveDraw(board, moveGenerator)
        || DrawDetector.isThreefoldRepetitionDraw(board, moveGenerator);
  }

  private int evaluateLeaf(Board board, Color perspective, int plyFromRoot, SearchContext context) {
    int score = evaluator.evaluate(board, perspective, plyFromRoot);
    if (SearchDrawRules.canClaim(board, moveGenerator)) {
      score = SearchDrawRules.withClaimOption(score, board, perspective);
    }
    context.checkTime();
    return score;
  }

  private static void updatePrincipalVariation(SearchContext context, int plyFromRoot, Move move) {
    List<String> principalVariation = context.pvLine(plyFromRoot);
    principalVariation.clear();
    principalVariation.add(MoveNotation.format(move));
    principalVariation.addAll(context.pvLine(plyFromRoot + 1));
  }

  private List<LegalMove> orderMoves(Board board, List<LegalMove> legalMoves, SearchContext context) {
    List<LegalMove> orderedMoves = moveOrderingStrategy.order(board, legalMoves, Optional.empty());
    context.checkTime();
    return orderedMoves;
  }

  static final class SearchContext {

    private long nodes;
    private final BooleanSupplier timeExpired;
    private final List<List<String>> pvLines = new ArrayList<>();
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

    List<String> pvLine(int ply) {
      while (pvLines.size() <= ply) {
        pvLines.add(new ArrayList<>());
      }
      return pvLines.get(ply);
    }
  }

  static final class SearchDeadlineExceeded extends RuntimeException {

    SearchDeadlineExceeded() {
      super(null, null, false, false);
    }
  }
}
