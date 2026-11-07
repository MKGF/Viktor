package com.desierto.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.desierto.domain.Board;
import com.desierto.domain.BoardFactory;
import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.Move;
import com.desierto.domain.MoveGenerator;
import com.desierto.domain.MoveType;
import com.desierto.domain.Piece;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Rook;
import java.util.List;
import org.junit.jupiter.api.Test;

class PositionHashTest {

  @Test
  void shouldBeStableAcrossCopiesAndUndo() {
    Board board = Board.standard();
    long initialHash = PositionHash.of(board);
    Move move = new MoveGenerator().generateMoves(board, Cell.E2).stream()
        .filter(candidate -> candidate.to().equals(Cell.E4)).findFirst().orElseThrow();

    assertEquals(initialHash, PositionHash.of(board.copy()));
    board.makeMove(move);
    assertNotEquals(initialHash, PositionHash.of(board));
    assertEquals(PositionHash.of(board), PositionHash.of(board.copy()));
    board.undoMove();
    assertEquals(initialHash, PositionHash.of(board));
  }

  @Test
  void shouldIncludeSideRightsAndEnPassantSquare() {
    long baseline = PositionHash.of(position(Color.WHITE, true, true, true, true, null,
        new Rook(Color.WHITE, Cell.H1)));

    assertNotEquals(baseline, PositionHash.of(position(Color.BLACK, true, true, true, true, null,
        new Rook(Color.WHITE, Cell.H1))));
    assertNotEquals(baseline, PositionHash.of(position(Color.WHITE, false, true, true, true, null,
        new Rook(Color.WHITE, Cell.H1))));
    assertNotEquals(baseline, PositionHash.of(position(Color.WHITE, true, false, true, true, null,
        new Rook(Color.WHITE, Cell.H1))));
    assertNotEquals(baseline, PositionHash.of(position(Color.WHITE, true, true, false, true, null,
        new Rook(Color.WHITE, Cell.H1))));
    assertNotEquals(baseline, PositionHash.of(position(Color.WHITE, true, true, true, false, null,
        new Rook(Color.WHITE, Cell.H1))));
    long d6 = PositionHash.of(position(Color.WHITE, true, true, true, true, Cell.D6,
        new Rook(Color.WHITE, Cell.H1)));
    long e6 = PositionHash.of(position(Color.WHITE, true, true, true, true, Cell.E6,
        new Rook(Color.WHITE, Cell.H1)));
    assertNotEquals(baseline, d6);
    assertNotEquals(d6, e6);
  }

  @Test
  void shouldDistinguishMovedPiecesEvenWhenRightsAreTheSame() {
    Board unmoved = position(Color.WHITE, true, false, false, false, null,
        new Rook(Color.WHITE, Cell.H1));
    Board moved = position(Color.WHITE, true, false, false, false, null,
        new Rook(Color.WHITE, Cell.H1).movedTo(Cell.H1));

    assertNotEquals(PositionHash.of(unmoved), PositionHash.of(moved));
    assertTrue(new MoveGenerator().generateMoves(unmoved, Cell.E1).stream()
        .anyMatch(move -> move.to().equals(Cell.G1)));
    assertTrue(new MoveGenerator().generateMoves(moved, Cell.E1).stream()
        .noneMatch(move -> move.to().equals(Cell.G1)));

    Board movedKing = BoardFactory.position().castlingRights(true, false, false, false)
        .place(new King(Color.WHITE, Cell.E1).movedTo(Cell.E1), Cell.E1)
        .place(new Rook(Color.WHITE, Cell.H1), Cell.H1)
        .place(new King(Color.BLACK, Cell.E8), Cell.E8)
        .build();
    assertNotEquals(PositionHash.of(unmoved), PositionHash.of(movedKing));
  }

  @Test
  void shouldRestoreTheHashAfterSpecialMoves() {
    Board castle = position(Color.WHITE, true, false, false, false, null,
        new Rook(Color.WHITE, Cell.H1));
    Board enPassant = BoardFactory.position().castlingRights(false, false, false, false)
        .enPassantTarget(Cell.D6)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new King(Color.BLACK, Cell.E8), Cell.E8)
        .place(new Pawn(Color.WHITE, Cell.E5), Cell.E5)
        .place(new Pawn(Color.BLACK, Cell.D5), Cell.D5)
        .build();
    Board promotion = BoardFactory.position().castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new King(Color.BLACK, Cell.H8), Cell.H8)
        .place(new Pawn(Color.WHITE, Cell.A7), Cell.A7)
        .build();
    List<Board> boards = List.of(castle, enPassant, promotion);
    List<MoveType> types = List.of(MoveType.CASTLE_KINGSIDE, MoveType.EN_PASSANT, MoveType.PROMOTION);

    for (int i = 0; i < boards.size(); i++) {
      Board board = boards.get(i);
      long initial = PositionHash.of(board);
      MoveType type = types.get(i);
      Move move = new MoveGenerator().generateLegalMoves(board, board.getSideToMove()).stream()
          .filter(candidate -> candidate.type() == type).findFirst().orElseThrow();
      board.makeMove(move);
      assertNotEquals(initial, PositionHash.of(board));
      board.undoMove();
      assertEquals(initial, PositionHash.of(board));
    }
  }

  @Test
  void shouldDistinguishPieceColorTypeAndSquare() {
    Board whitePawn = BoardFactory.withPieces(BoardFactory.placement(new Pawn(Color.WHITE, Cell.A2), Cell.A2));
    Board blackPawn = BoardFactory.withPieces(BoardFactory.placement(new Pawn(Color.BLACK, Cell.A2), Cell.A2));
    Board whiteRook = BoardFactory.withPieces(BoardFactory.placement(new Rook(Color.WHITE, Cell.A2), Cell.A2));
    Board otherSquare = BoardFactory.withPieces(BoardFactory.placement(new Pawn(Color.WHITE, Cell.B2), Cell.B2));

    assertNotEquals(PositionHash.of(whitePawn), PositionHash.of(blackPawn));
    assertNotEquals(PositionHash.of(whitePawn), PositionHash.of(whiteRook));
    assertNotEquals(PositionHash.of(whitePawn), PositionHash.of(otherSquare));
  }

  private Board position(Color side, boolean wk, boolean wq, boolean bk, boolean bq, Cell enPassant,
      Piece rook) {
    return BoardFactory.position().sideToMove(side).castlingRights(wk, wq, bk, bq)
        .enPassantTarget(enPassant)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(rook, Cell.H1)
        .place(new King(Color.BLACK, Cell.E8), Cell.E8)
        .build();
  }
}
