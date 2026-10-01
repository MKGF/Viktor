package com.desierto.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;
import org.junit.jupiter.api.Test;

class BoardTest {

  @Test
  void shouldCreateStandardPosition() {
    Board board = Board.standard();

    assertEquals(32, board.getPieces().size());
    assertEquals(16, board.getPieces(Color.WHITE).size());
    assertEquals(16, board.getPieces(Color.BLACK).size());

    Piece whiteRook = board.getPieceAt(Cell.A1).orElseThrow();
    Piece whiteKing = board.getPieceAt(Cell.E1).orElseThrow();
    Piece blackQueen = board.getPieceAt(Cell.D8).orElseThrow();
    Piece blackPawn = board.getPieceAt(Cell.A7).orElseThrow();

    assertTrue(whiteRook instanceof Rook);
    assertTrue(whiteKing instanceof King);
    assertTrue(blackQueen instanceof Queen);
    assertTrue(blackPawn instanceof Pawn);

    assertEquals(Color.WHITE, whiteRook.getColor());
    assertEquals(Color.WHITE, whiteKing.getColor());
    assertEquals(Color.BLACK, blackQueen.getColor());
    assertEquals(Color.BLACK, blackPawn.getColor());
  }

  @Test
  void shouldPlaceMoveAndRemovePieces() {
    Board board = new Board();
    Pawn pawn = new Pawn(Color.WHITE, Cell.E2);

    board.placePiece(pawn, Cell.E2);

    assertTrue(board.isOccupied(Cell.E2));
    assertEquals(Cell.E2, pawn.getCell());

    assertTrue(board.movePiece(Cell.E2, Cell.E4).isEmpty());
    assertFalse(board.isOccupied(Cell.E2));
    assertTrue(board.isOccupied(Cell.E4));
    assertEquals(Cell.E4, pawn.getCell());

    assertTrue(board.removePiece(Cell.E4).isPresent());
    assertFalse(board.isOccupied(Cell.E4));
  }

  @Test
  void shouldReturnCapturedPieceWhenMovingOntoOccupiedSquare() {
    Board board = new Board();
    Rook rook = new Rook(Color.WHITE, Cell.A1);
    Pawn pawn = new Pawn(Color.BLACK, Cell.A8);

    board.placePiece(rook, Cell.A1);
    board.placePiece(pawn, Cell.A8);

    Piece captured = board.movePiece(Cell.A1, Cell.A8).orElseThrow();

    assertEquals(pawn, captured);
    assertEquals(Cell.A8, rook.getCell());
    assertFalse(board.isOccupied(Cell.A1));
    assertTrue(board.isOccupied(Cell.A8));
  }
}
