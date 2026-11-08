package com.desierto.search;

import com.desierto.domain.Board;
import com.desierto.domain.LegalMove;
import com.desierto.domain.MoveGenerator;
import com.desierto.domain.MoveNotation;
import java.util.List;
import java.util.Optional;

/** Soft limits applied only between completed iterations of a timed search. */
final class ManagedSearchPolicy {
  private final long budgetNanos;
  private final boolean forced;
  private final boolean quiet;
  private final boolean claimPossible;
  private Optional<String> previousMove = Optional.empty();
  private int previousScore;
  private int stableIterations;
  private int completedDepth;

  ManagedSearchPolicy(Board board, long budgetNanos) {
    MoveGenerator generator = new MoveGenerator();
    List<LegalMove> moves = generator.generateLegalMoveApplications(board, board.getSideToMove());
    this.budgetNanos = budgetNanos;
    forced = moves.size() == 1;
    claimPossible = SearchDrawRules.claim(board, moves).isPresent();
    quiet = moves.size() > 1 && moves.size() <= 12
        && !generator.isKingInCheck(board, board.getSideToMove())
        && moves.stream().noneMatch(move -> move.move().isCapture() || move.move().isPromotion());
  }

  boolean shouldStop(SearchResult result, long elapsedNanos) {
    completedDepth = result.depth();
    if (claimPossible || result.bestMove().isEmpty() || result.drawClaim().isPresent()) {
      stableIterations = 0;
      return false;
    }
    if (forced) {
      return true;
    }
    String move = MoveNotation.format(result.bestMove().orElseThrow());
    if (previousMove.isPresent() && previousMove.orElseThrow().equals(move)
        && Math.abs((long) result.score() - previousScore) <= 25) {
      stableIterations++;
    } else {
      stableIterations = 1;
    }
    previousMove = Optional.of(move);
    previousScore = result.score();
    List<SearchTrace.Alternative> alternatives = result.trace().alternatives();
    boolean clearLead = alternatives.size() >= 2
        && alternatives.get(0).move().equals(move)
        && alternatives.get(0).bound() == SearchTrace.Bound.EXACT
        && (long) alternatives.get(0).score() - alternatives.get(1).score() >= 100;
    if (quiet && result.depth() >= 4 && stableIterations >= 3 && clearLead
        && elapsedNanos >= budgetNanos / 2) {
      return true;
    }
    return shouldCancel(elapsedNanos);
  }

  boolean shouldCancel(long elapsedNanos) {
    long softLimit = budgetNanos - budgetNanos / 4;
    return !claimPossible && quiet && completedDepth >= 2 && elapsedNanos >= softLimit;
  }
}
