package com.desierto.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.desierto.domain.pieces.Bishop;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Knight;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;

class MoveGeneratorTest {

  private final MoveGenerator moveGenerator = new MoveGenerator();

  @Test
  void shouldGeneratePawnAdvancesFromInitialPosition() {
    Pawn pawn = new Pawn(Color.WHITE, Cell.E2);
    Board board = BoardFactory.withPieces(BoardFactory.placement(pawn, Cell.E2));

    List<Move> moves = moveGenerator.generateMoves(board, Cell.E2);
    Set<Cell> destinations = moves.stream().map(Move::to).collect(Collectors.toSet());

    assertEquals(Set.of(Cell.E3, Cell.E4), destinations);
    assertTrue(moves.stream().allMatch(move -> move.type() == MoveType.NORMAL));
  }

  @Test
  void shouldGenerateKnightMovesAndCaptureEnemyPieces() {
    Knight knight = new Knight(Color.WHITE, Cell.G1);
    Pawn friendlyPawn = new Pawn(Color.WHITE, Cell.E2);
    Pawn enemyPawn = new Pawn(Color.BLACK, Cell.H3);
    Board board = BoardFactory.withPieces(
        BoardFactory.placement(knight, Cell.G1),
        BoardFactory.placement(friendlyPawn, Cell.E2),
        BoardFactory.placement(enemyPawn, Cell.H3));

    List<Move> moves = moveGenerator.generateMoves(board, Cell.G1);

    assertEquals(2, moves.size());
    assertTrue(moves.stream().anyMatch(move -> move.to().equals(Cell.F3) && move.type() == MoveType.NORMAL));
    assertTrue(moves.stream().anyMatch(move -> move.to().equals(Cell.H3) && move.type() == MoveType.CAPTURE));
  }

  @Test
  void shouldGenerateSlidingMovesUntilBlocked() {
    Rook rook = new Rook(Color.WHITE, Cell.D4);
    Pawn enemyPawn = new Pawn(Color.BLACK, Cell.D6);
    Pawn friendlyPawn = new Pawn(Color.WHITE, Cell.F4);
    Board board = BoardFactory.withPieces(
        BoardFactory.placement(rook, Cell.D4),
        BoardFactory.placement(enemyPawn, Cell.D6),
        BoardFactory.placement(friendlyPawn, Cell.F4));

    List<Move> moves = moveGenerator.generateMoves(board, Cell.D4);

    assertTrue(moves.stream().anyMatch(move -> move.to().equals(Cell.D5) && move.type() == MoveType.NORMAL));
    assertTrue(moves.stream().anyMatch(move -> move.to().equals(Cell.D6) && move.type() == MoveType.CAPTURE));
    assertTrue(moves.stream().anyMatch(move -> move.to().equals(Cell.A4) && move.type() == MoveType.NORMAL));
    assertTrue(moves.stream().anyMatch(move -> move.to().equals(Cell.E4) && move.type() == MoveType.NORMAL));
    assertTrue(moves.stream().noneMatch(move -> move.to().equals(Cell.F4)));
    assertTrue(moves.stream().noneMatch(move -> move.to().equals(Cell.G4)));
  }

  @Test
  void shouldGenerateCastlingMovesWhenPathIsClear() {
    King king = new King(Color.WHITE, Cell.E1);
    Rook kingsideRook = new Rook(Color.WHITE, Cell.H1);
    Rook queensideRook = new Rook(Color.WHITE, Cell.A1);
    Board board = BoardFactory.withPieces(
        BoardFactory.placement(king, Cell.E1),
        BoardFactory.placement(kingsideRook, Cell.H1),
        BoardFactory.placement(queensideRook, Cell.A1));

    List<Move> moves = moveGenerator.generateMoves(board, Cell.E1);

    assertTrue(moves.stream().anyMatch(move -> move.type() == MoveType.CASTLE_KINGSIDE && move.to().equals(Cell.G1)));
    assertTrue(moves.stream().anyMatch(move -> move.type() == MoveType.CASTLE_QUEENSIDE && move.to().equals(Cell.C1)));
  }

  @Test
  void shouldNotGenerateCastlingWhenKingHasMoved() {
    King king = new King(Color.WHITE, Cell.E1);
    Rook rook = new Rook(Color.WHITE, Cell.H1);
    Board board = BoardFactory.withPieces(
        BoardFactory.placement(king, Cell.E1),
        BoardFactory.placement(rook, Cell.H1));
    king.setHasMoved(true);

    List<Move> moves = moveGenerator.generateMoves(board, Cell.E1);

    assertTrue(moves.stream().noneMatch(move -> move.type() == MoveType.CASTLE_KINGSIDE));
    assertTrue(moves.stream().noneMatch(move -> move.type() == MoveType.CASTLE_QUEENSIDE));
  }

  @Test
  void shouldGenerateEnPassantAfterDoublePawnAdvance() {
    Pawn whitePawn = new Pawn(Color.WHITE, Cell.E5);
    Pawn blackPawn = new Pawn(Color.BLACK, Cell.D7);
    Board board = BoardFactory.withPieces(
        BoardFactory.placement(whitePawn, Cell.E5),
        BoardFactory.placement(blackPawn, Cell.D7));
    board.makeMove(new Move(Color.BLACK, blackPawn, Cell.D7, Cell.D5, null, MoveType.NORMAL, null));

    List<Move> moves = moveGenerator.generateMoves(board, Cell.E5);

    assertTrue(moves.stream().anyMatch(move ->
        move.type() == MoveType.EN_PASSANT
            && move.to().equals(Cell.D6)
            && move.capturedPiece().equals(blackPawn)));
  }

  @Test
  void shouldGenerateAllPromotionChoicesOnAdvance() {
    Pawn pawn = new Pawn(Color.WHITE, Cell.E7);
    Board board = BoardFactory.withPieces(BoardFactory.placement(pawn, Cell.E7));

    List<Move> moves = moveGenerator.generateMoves(board, Cell.E7);

    assertEquals(4, moves.size());
    assertTrue(moves.stream().allMatch(move -> move.type() == MoveType.PROMOTION));
    assertTrue(moves.stream().anyMatch(move -> move.promotionPiece() instanceof Queen));
    assertTrue(moves.stream().anyMatch(move -> move.promotionPiece() instanceof Rook));
    assertTrue(moves.stream().anyMatch(move -> move.promotionPiece() instanceof Bishop));
    assertTrue(moves.stream().anyMatch(move -> move.promotionPiece() instanceof Knight));
  }

  @Test
  void shouldGenerateAllPromotionChoicesOnCapture() {
    Pawn pawn = new Pawn(Color.WHITE, Cell.E7);
    Pawn enemy = new Pawn(Color.BLACK, Cell.F8);
    Pawn blocker = new Pawn(Color.WHITE, Cell.E8);
    Board board = BoardFactory.withPieces(
        BoardFactory.placement(pawn, Cell.E7),
        BoardFactory.placement(enemy, Cell.F8),
        BoardFactory.placement(blocker, Cell.E8));

    List<Move> moves = moveGenerator.generateMoves(board, Cell.E7);

    assertEquals(4, moves.size());
    assertTrue(moves.stream().allMatch(move -> move.type() == MoveType.CAPTURE_PROMOTION));
    assertTrue(moves.stream().allMatch(move -> move.capturedPiece().equals(enemy)));
    assertTrue(moves.stream().anyMatch(move -> move.promotionPiece() instanceof Queen));
    assertTrue(moves.stream().anyMatch(move -> move.promotionPiece() instanceof Rook));
    assertTrue(moves.stream().anyMatch(move -> move.promotionPiece() instanceof Bishop));
    assertTrue(moves.stream().anyMatch(move -> move.promotionPiece() instanceof Knight));
  }

  @Test
  void shouldDetectCheckOnKing() {
    Board board = BoardFactory.withPieces(
        BoardFactory.placement(new King(Color.WHITE, Cell.E1), Cell.E1),
        BoardFactory.placement(new Rook(Color.BLACK, Cell.E8), Cell.E8));

    assertTrue(moveGenerator.isKingInCheck(board, Color.WHITE));
  }

  @Test
  void shouldFilterMovesThatExposeKingToCheck() {
    King king = new King(Color.WHITE, Cell.E1);
    Bishop bishop = new Bishop(Color.WHITE, Cell.E2);
    Rook rook = new Rook(Color.BLACK, Cell.E8);
    Board board = BoardFactory.withPieces(
        BoardFactory.placement(king, Cell.E1),
        BoardFactory.placement(bishop, Cell.E2),
        BoardFactory.placement(rook, Cell.E8));

    List<Move> moves = moveGenerator.generateMoves(board, Cell.E2);

    assertTrue(moves.isEmpty());
  }

  @Test
  void shouldDetectCheckmate() {
    Board board = BoardFactory.withPieces(
        BoardFactory.placement(new King(Color.BLACK, Cell.H8), Cell.H8),
        BoardFactory.placement(new Queen(Color.WHITE, Cell.G7), Cell.G7),
        BoardFactory.placement(new King(Color.WHITE, Cell.F6), Cell.F6));

    assertTrue(moveGenerator.isCheckmate(board, Color.BLACK));
  }

  @Test
  void shouldDetectStalemate() {
    Board board = BoardFactory.withPieces(
        BoardFactory.placement(new King(Color.BLACK, Cell.A8), Cell.A8),
        BoardFactory.placement(new Queen(Color.WHITE, Cell.C7), Cell.C7),
        BoardFactory.placement(new King(Color.WHITE, Cell.C6), Cell.C6));

    assertTrue(moveGenerator.isStalemate(board, Color.BLACK));
  }
}
