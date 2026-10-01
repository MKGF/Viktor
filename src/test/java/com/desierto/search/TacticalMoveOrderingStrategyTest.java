package com.desierto.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.desierto.domain.Board;
import com.desierto.domain.BoardFactory;
import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.LegalMove;
import com.desierto.domain.Move;
import com.desierto.domain.MoveGenerator;
import com.desierto.domain.MoveType;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TacticalMoveOrderingStrategyTest {

  private final MoveGenerator moveGenerator = new MoveGenerator();
  private final TacticalMoveOrderingStrategy strategy = new TacticalMoveOrderingStrategy();

  @Test
  void shouldOrderPromotionsCapturesChecksAndQuietMoves() {
    Board board = orderingPosition();
    List<LegalMove> legalMoves = moveGenerator.generateLegalMoveApplications(board, Color.WHITE);
    Move promotion = findMove(legalMoves, Cell.E7, Cell.E8, MoveType.PROMOTION);
    Move capture = findMove(legalMoves, Cell.A1, Cell.A4, MoveType.CAPTURE);
    Move check = findMove(legalMoves, Cell.D1, Cell.D8, MoveType.NORMAL);
    Move quietMove = findMove(legalMoves, Cell.D1, Cell.D2, MoveType.NORMAL);

    List<LegalMove> orderedMoves = strategy.order(board, legalMoves, Optional.empty());

    assertTrue(indexOf(orderedMoves, promotion) < indexOf(orderedMoves, capture));
    assertTrue(indexOf(orderedMoves, capture) < indexOf(orderedMoves, check));
    assertTrue(indexOf(orderedMoves, check) < indexOf(orderedMoves, quietMove));
  }

  @Test
  void shouldPutThePreferredMoveBeforeTacticalMoves() {
    Board board = orderingPosition();
    List<LegalMove> legalMoves = moveGenerator.generateLegalMoveApplications(board, Color.WHITE);
    Move quietMove = findMove(legalMoves, Cell.D1, Cell.D2, MoveType.NORMAL);

    List<LegalMove> orderedMoves = strategy.order(board, legalMoves, Optional.of(MoveKey.from(quietMove)));

    assertEquals(MoveKey.from(quietMove), MoveKey.from(orderedMoves.getFirst().move()));
  }

  private Board orderingPosition() {
    return BoardFactory.position()
        .castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new Rook(Color.WHITE, Cell.A1), Cell.A1)
        .place(new Queen(Color.WHITE, Cell.D1), Cell.D1)
        .place(new Pawn(Color.WHITE, Cell.E7), Cell.E7)
        .place(new Pawn(Color.BLACK, Cell.A4), Cell.A4)
        .place(new King(Color.BLACK, Cell.H8), Cell.H8)
        .build();
  }

  private Move findMove(List<LegalMove> moves, Cell from, Cell to, MoveType type) {
    return moves.stream()
        .map(LegalMove::move)
        .filter(move -> move.from().equals(from) && move.to().equals(to) && move.type() == type)
        .findFirst()
        .orElseThrow();
  }

  private int indexOf(List<LegalMove> moves, Move move) {
    return moves.stream().map(LegalMove::move).toList().indexOf(move);
  }
}
