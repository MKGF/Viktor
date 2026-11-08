package com.desierto.domain;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.desierto.domain.pieces.Bishop;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Knight;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;
import org.junit.jupiter.api.Test;

class AttackGeometryTest {
  private final MoveGenerator generator = new MoveGenerator();

  @Test
  void slidingAttacksStopAtTheFirstPieceAndIncludeFriendlyPieces() {
    Bishop bishop = new Bishop(Color.WHITE, Cell.C1);
    Pawn blocker = new Pawn(Color.WHITE, Cell.E3);
    Rook rook = new Rook(Color.BLACK, Cell.A4);
    Queen queen = new Queen(Color.BLACK, Cell.H8);
    Board board = BoardFactory.position().castlingRights(false, false, false, false)
        .place(bishop, Cell.C1).place(blocker, Cell.E3)
        .place(rook, Cell.A4).place(queen, Cell.H8).build();
    assertTrue(generator.attacksSquare(board, bishop, Cell.E3));
    assertFalse(generator.attacksSquare(board, bishop, Cell.F4));
    assertFalse(generator.attacksSquare(board, bishop, Cell.C1));
    assertFalse(generator.attacksSquare(board, bishop, Cell.C4));
    assertTrue(generator.attacksSquare(board, rook, Cell.D4));
    assertFalse(generator.attacksSquare(board, rook, Cell.B5));
    assertTrue(generator.attacksSquare(board, queen, Cell.H5));
    assertTrue(generator.attacksSquare(board, queen, Cell.F6));
    assertFalse(generator.attacksSquare(board, queen, Cell.F5));
  }

  @Test
  void pinnedPiecesStillHaveGeometricAttacks() {
    Knight knight = new Knight(Color.WHITE, Cell.E2);
    Board board = BoardFactory.position().castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(knight, Cell.E2)
        .place(new Rook(Color.BLACK, Cell.E8), Cell.E8)
        .place(new King(Color.BLACK, Cell.A8), Cell.A8).build();
    assertTrue(generator.attacksSquare(board, knight, Cell.F4));
    assertTrue(generator.generateLegalMoves(board, Cell.E2).isEmpty());
    assertThrows(IllegalArgumentException.class,
        () -> generator.attacksSquare(board, new Knight(Color.WHITE, Cell.E2), Cell.F4));
  }
}
