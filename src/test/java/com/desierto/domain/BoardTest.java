package com.desierto.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    assertEquals(Color.WHITE, board.getSideToMove());
    assertTrue(board.canWhiteCastleKingside());
    assertTrue(board.canWhiteCastleQueenside());
    assertTrue(board.canBlackCastleKingside());
    assertTrue(board.canBlackCastleQueenside());
    assertEquals(0, board.getHalfmoveClock());
    assertEquals(1, board.getFullmoveNumber());
    assertTrue(board.getEnPassantTarget().isEmpty());

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
  void shouldBuildACustomPositionWithSearchRelevantState() {
    Board board = BoardFactory.position()
        .sideToMove(Color.BLACK)
        .castlingRights(true, false, false, true)
        .enPassantTarget(Cell.D3)
        .halfmoveClock(17)
        .fullmoveNumber(28)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new King(Color.BLACK, Cell.E8), Cell.E8)
        .build();

    assertEquals(Color.BLACK, board.getSideToMove());
    assertTrue(board.canWhiteCastleKingside());
    assertFalse(board.canWhiteCastleQueenside());
    assertFalse(board.canBlackCastleKingside());
    assertTrue(board.canBlackCastleQueenside());
    assertEquals(Cell.D3, board.getEnPassantTarget().orElseThrow());
    assertEquals(17, board.getHalfmoveClock());
    assertEquals(28, board.getFullmoveNumber());
  }

  @Test
  void shouldPlaceMoveAndRemovePieces() {
    Pawn pawn = new Pawn(Color.WHITE, Cell.E2);
    Board board = BoardFactory.withPieces(BoardFactory.placement(pawn, Cell.E2));

    assertTrue(board.isOccupied(Cell.E2));
    assertEquals(Cell.E2, pawn.getCell());

    board.makeMove(new Move(Color.WHITE, pawn, Cell.E2, Cell.E4, null, MoveType.NORMAL, null));
    assertFalse(board.isOccupied(Cell.E2));
    assertTrue(board.isOccupied(Cell.E4));
    assertEquals(Cell.E2, pawn.getCell());
    assertEquals(Cell.E4, board.getPieceAt(Cell.E4).orElseThrow().getCell());

    assertTrue(board.removePiece(Cell.E4).isPresent());
    assertFalse(board.isOccupied(Cell.E4));
  }

  @Test
  void shouldCreateAnIndependentPositionAfterALegalMove() {
    Pawn pawn = new Pawn(Color.WHITE, Cell.E2);
    Board board = BoardFactory.withPieces(BoardFactory.placement(pawn, Cell.E2));
    Move move = new Move(Color.WHITE, pawn, Cell.E2, Cell.E4, null, MoveType.NORMAL, null);

    LegalMove legalMove = new MoveGenerator().generateLegalMoveApplications(board, Color.WHITE).stream()
        .filter(candidate -> candidate.move().to().equals(Cell.E4))
        .findFirst()
        .orElseThrow();

    Board resultingBoard = legalMove.resultingPosition();

    assertTrue(board.isOccupied(Cell.E2));
    assertTrue(board.getPieceAt(Cell.E4).isEmpty());
    assertTrue(resultingBoard.getPieceAt(Cell.E2).isEmpty());
    assertTrue(resultingBoard.isOccupied(Cell.E4));
    assertEquals(Color.BLACK, resultingBoard.getSideToMove());
  }

  @Test
  void shouldRejectALegalMoveCreatedForAnOlderPosition() {
    Pawn pawn = new Pawn(Color.WHITE, Cell.E2);
    Board board = BoardFactory.withPieces(BoardFactory.placement(pawn, Cell.E2));
    LegalMove legalMove = new MoveGenerator().generateLegalMoveApplications(board, Color.WHITE).stream()
        .filter(candidate -> candidate.move().to().equals(Cell.E4))
        .findFirst()
        .orElseThrow();

    board.makeMove(new Move(Color.WHITE, pawn, Cell.E2, Cell.E3, null, MoveType.NORMAL, null));

    assertThrows(IllegalStateException.class, legalMove::resultingPosition);
  }

  @Test
  void shouldInvalidateLegalMovesWhenUndoingTheirSourcePosition() {
    Pawn whitePawn = new Pawn(Color.WHITE, Cell.E2);
    Pawn blackPawn = new Pawn(Color.BLACK, Cell.A7);
    Board board = BoardFactory.withPieces(
        BoardFactory.placement(whitePawn, Cell.E2),
        BoardFactory.placement(blackPawn, Cell.A7));
    board.makeMove(new Move(Color.WHITE, whitePawn, Cell.E2, Cell.E3, null, MoveType.NORMAL, null));
    LegalMove legalMove = new MoveGenerator().generateLegalMoveApplications(board, Color.BLACK).stream()
        .filter(candidate -> candidate.move().to().equals(Cell.A6))
        .findFirst()
        .orElseThrow();

    board.undoMove();

    assertThrows(IllegalStateException.class, legalMove::resultingPosition);
  }

  @Test
  void shouldReturnCapturedPieceWhenMovingOntoOccupiedSquare() {
    Rook rook = new Rook(Color.WHITE, Cell.A1);
    Pawn pawn = new Pawn(Color.BLACK, Cell.A8);
    Board board = BoardFactory.withPieces(
        BoardFactory.placement(rook, Cell.A1),
        BoardFactory.placement(pawn, Cell.A8));

    board.makeMove(new Move(Color.WHITE, rook, Cell.A1, Cell.A8, null, MoveType.CAPTURE, pawn));

    assertEquals(Cell.A1, rook.getCell());
    assertEquals(Cell.A8, board.getPieceAt(Cell.A8).orElseThrow().getCell());
    assertFalse(board.isOccupied(Cell.A1));
    assertTrue(board.isOccupied(Cell.A8));
  }

  @Test
  void shouldUpdateCastlingRightsAfterACapture() {
    Rook whiteRook = new Rook(Color.WHITE, Cell.H1);
    Rook blackRook = new Rook(Color.BLACK, Cell.H8);
    Board board = BoardFactory.position()
        .sideToMove(Color.BLACK)
        .place(whiteRook, Cell.H1)
        .place(blackRook, Cell.H8)
        .build();
    Move capture = new Move(Color.BLACK, blackRook, Cell.H8, Cell.H1, null, MoveType.CAPTURE,
        whiteRook);

    board.makeMove(capture);

    assertFalse(board.canWhiteCastleKingside());
    assertEquals(Cell.H1, board.getLastMove().orElseThrow().piece().getCell());
    assertTrue(board.getLastMove().orElseThrow().capturedPiece() instanceof Rook);
  }

  @Test
  void shouldRejectACaptureWithIncorrectMetadata() {
    Rook whiteRook = new Rook(Color.WHITE, Cell.H1);
    Rook blackRook = new Rook(Color.BLACK, Cell.H8);
    Board board = BoardFactory.position()
        .sideToMove(Color.BLACK)
        .place(whiteRook, Cell.H1)
        .place(blackRook, Cell.H8)
        .build();
    Move forgedCapture = new Move(Color.BLACK, blackRook, Cell.H8, Cell.H1, null,
        MoveType.CAPTURE, new Pawn(Color.WHITE, Cell.H1));

    assertThrows(IllegalArgumentException.class, () -> board.makeMove(forgedCapture));

    assertTrue(board.canWhiteCastleKingside());
    assertTrue(board.isOccupied(Cell.H1));
    assertTrue(board.isOccupied(Cell.H8));
  }

  @Test
  void shouldRejectAMoveForTheWrongSideToMove() {
    Board board = Board.standard();
    Pawn blackPawn = (Pawn) board.getPieceAt(Cell.E7).orElseThrow();

    assertThrows(IllegalArgumentException.class,
        () -> board.makeMove(new Move(Color.BLACK, blackPawn, Cell.E7, Cell.E5, null,
            MoveType.NORMAL, null)));

    assertEquals(Color.WHITE, board.getSideToMove());
    assertTrue(board.isOccupied(Cell.E7));
  }

  @Test
  void shouldRejectAMoveThatIsNotLegal() {
    Pawn pawn = new Pawn(Color.WHITE, Cell.E2);
    Board board = BoardFactory.withPieces(BoardFactory.placement(pawn, Cell.E2));

    assertThrows(IllegalArgumentException.class,
        () -> board.makeMove(new Move(Color.WHITE, pawn, Cell.E2, Cell.E5, null,
            MoveType.NORMAL, null)));

    assertTrue(board.isOccupied(Cell.E2));
    assertTrue(board.getPieceAt(Cell.E5).isEmpty());
  }

  @Test
  void shouldAcceptALegalMoveWithCachedPieceMetadata() {
    Pawn whitePawn = new Pawn(Color.WHITE, Cell.E2);
    Pawn blackPawn = new Pawn(Color.BLACK, Cell.A7);
    Board board = BoardFactory.withPieces(
        BoardFactory.placement(whitePawn, Cell.E2),
        BoardFactory.placement(blackPawn, Cell.A7));
    board.makeMove(new Move(Color.WHITE, whitePawn, Cell.E2, Cell.E4, null, MoveType.NORMAL, null));
    board.makeMove(new Move(Color.BLACK, blackPawn, Cell.A7, Cell.A6, null, MoveType.NORMAL, null));

    board.makeMove(new Move(Color.WHITE, whitePawn, Cell.E4, Cell.E5, null, MoveType.NORMAL, null));

    assertTrue(board.isOccupied(Cell.E5));
    assertTrue(board.getPieceAt(Cell.E4).isEmpty());
  }

  @Test
  void shouldRejectCastlingThroughOccupiedSquares() {
    Board board = Board.standard();
    King king = (King) board.getPieceAt(Cell.E1).orElseThrow();

    assertThrows(IllegalArgumentException.class,
        () -> board.makeMove(new Move(Color.WHITE, king, Cell.E1, Cell.G1, null,
            MoveType.CASTLE_KINGSIDE, null)));
  }

  @Test
  void shouldUpdateStateAfterPawnDoubleMove() {
    Pawn pawn = new Pawn(Color.WHITE, Cell.E2);
    Board board = BoardFactory.withPieces(BoardFactory.placement(pawn, Cell.E2));

    board.makeMove(new Move(Color.WHITE, pawn, Cell.E2, Cell.E4, null, MoveType.NORMAL, null));

    assertEquals(Color.BLACK, board.getSideToMove());
    assertEquals(Cell.E3, board.getEnPassantTarget().orElseThrow());
    assertEquals(0, board.getHalfmoveClock());
    assertEquals(1, board.getFullmoveNumber());
  }

  @Test
  void shouldUpdateFullmoveAfterBlackMove() {
    Pawn whitePawn = new Pawn(Color.WHITE, Cell.E2);
    Pawn blackPawn = new Pawn(Color.BLACK, Cell.E7);
    Board board = BoardFactory.withPieces(
        BoardFactory.placement(whitePawn, Cell.E2),
        BoardFactory.placement(blackPawn, Cell.E7));

    board.makeMove(new Move(Color.WHITE, whitePawn, Cell.E2, Cell.E4, null, MoveType.NORMAL, null));
    board.makeMove(new Move(Color.BLACK, blackPawn, Cell.E7, Cell.E5, null, MoveType.NORMAL, null));

    assertEquals(Color.WHITE, board.getSideToMove());
    assertEquals(2, board.getFullmoveNumber());
  }

  @Test
  void shouldUndoSimpleMove() {
    Pawn pawn = new Pawn(Color.WHITE, Cell.E2);
    Board board = BoardFactory.withPieces(BoardFactory.placement(pawn, Cell.E2));

    board.makeMove(new Move(Color.WHITE, pawn, Cell.E2, Cell.E4, null, MoveType.NORMAL, null));
    board.undoMove();

    assertTrue(board.isOccupied(Cell.E2));
    assertFalse(board.isOccupied(Cell.E4));
    assertEquals(Color.WHITE, board.getSideToMove());
    assertEquals(1, board.getFullmoveNumber());
    assertTrue(board.getEnPassantTarget().isEmpty());
  }

  @Test
  void shouldUndoCastlingMove() {
    King king = new King(Color.WHITE, Cell.E1);
    Rook rook = new Rook(Color.WHITE, Cell.H1);
    Board board = BoardFactory.withPieces(
        BoardFactory.placement(king, Cell.E1),
        BoardFactory.placement(rook, Cell.H1));

    Move castle = new Move(Color.WHITE, king, Cell.E1, Cell.G1, null, MoveType.CASTLE_KINGSIDE, null);
    board.makeMove(castle);
    board.undoMove();

    assertTrue(board.getPieceAt(Cell.E1).orElseThrow() instanceof King);
    assertTrue(board.getPieceAt(Cell.H1).orElseThrow() instanceof Rook);
    assertTrue(board.getPieceAt(Cell.F1).isEmpty());
    assertTrue(board.getPieceAt(Cell.G1).isEmpty());
    assertEquals(Color.WHITE, board.getSideToMove());
    assertTrue(board.canWhiteCastleKingside());
    assertTrue(board.canWhiteCastleQueenside());
  }

  @Test
  void shouldApplyAndUndoPromotion() {
    Pawn pawn = new Pawn(Color.WHITE, Cell.E7);
    Board board = BoardFactory.withPieces(BoardFactory.placement(pawn, Cell.E7));
    Move promotion = new Move(Color.WHITE, pawn, Cell.E7, Cell.E8,
        new Queen(Color.WHITE, Cell.E8), MoveType.PROMOTION, null);

    board.makeMove(promotion);

    assertTrue(board.getPieceAt(Cell.E8).orElseThrow() instanceof Queen);
    assertTrue(board.getPieceAt(Cell.E7).isEmpty());
    assertEquals(Color.BLACK, board.getSideToMove());

    board.undoMove();

    assertTrue(board.getPieceAt(Cell.E7).orElseThrow() instanceof Pawn);
    assertTrue(board.getPieceAt(Cell.E8).isEmpty());
    assertEquals(Color.WHITE, board.getSideToMove());
  }

  @Test
  void shouldApplyAndUndoEnPassant() {
    Pawn whitePawn = new Pawn(Color.WHITE, Cell.E5);
    Pawn blackPawn = new Pawn(Color.BLACK, Cell.D7);
    Board board = BoardFactory.position()
        .sideToMove(Color.BLACK)
        .place(whitePawn, Cell.E5)
        .place(blackPawn, Cell.D7)
        .build();
    board.makeMove(new Move(Color.BLACK, blackPawn, Cell.D7, Cell.D5, null, MoveType.NORMAL, null));
    Move enPassant = new MoveGenerator().generateMoves(board, Cell.E5).stream()
        .filter(move -> move.type() == MoveType.EN_PASSANT)
        .findFirst()
        .orElseThrow();

    board.makeMove(enPassant);

    assertTrue(board.getPieceAt(Cell.D5).isEmpty());
    assertTrue(board.getPieceAt(Cell.D6).orElseThrow() instanceof Pawn);
    assertEquals(Color.BLACK, board.getSideToMove());

    board.undoMove();

    assertTrue(board.getPieceAt(Cell.E5).orElseThrow() instanceof Pawn);
    assertTrue(board.getPieceAt(Cell.D5).orElseThrow() instanceof Pawn);
    assertEquals(Cell.D6, board.getEnPassantTarget().orElseThrow());
    assertEquals(Color.WHITE, board.getSideToMove());
  }
}
