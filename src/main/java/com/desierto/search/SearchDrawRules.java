package com.desierto.search;

import com.desierto.domain.Board;
import com.desierto.domain.Color;
import com.desierto.domain.DrawDetector;
import com.desierto.domain.LegalMove;
import com.desierto.domain.MoveGenerator;
import java.util.List;
import java.util.Optional;

final class SearchDrawRules {
  private SearchDrawRules() {
  }

  static Optional<DrawClaim> claim(Board board, List<LegalMove> legalMoves) {
    if (legalMoves.isEmpty()) {
      return Optional.empty();
    }
    if (DrawDetector.canClaimFiftyMoves(board)) {
      return Optional.of(new DrawClaim(Optional.empty()));
    }
    if (board.getHalfmoveClock() < 99) {
      return Optional.empty();
    }
    return legalMoves.stream().map(LegalMove::move)
        .filter(move -> DrawDetector.canClaimFiftyMovesAfter(board, move))
        .findFirst().map(move -> new DrawClaim(Optional.of(move)));
  }

  static boolean canClaim(Board board, MoveGenerator generator) {
    return board.getHalfmoveClock() >= 99
        && claim(board, generator.generateLegalMoveApplications(board, board.getSideToMove())).isPresent();
  }

  static int withClaimOption(int score, Board board, Color perspective) {
    return board.getSideToMove() == perspective ? Math.max(score, 0) : Math.min(score, 0);
  }
}
