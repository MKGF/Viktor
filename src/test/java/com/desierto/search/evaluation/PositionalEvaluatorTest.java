package com.desierto.search.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.desierto.domain.Board;
import com.desierto.domain.BoardFactory;
import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.Piece;
import com.desierto.domain.pieces.Bishop;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Knight;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;

class PositionalEvaluatorTest {
  private final PositionalEvaluator evaluator = new PositionalEvaluator();

  @Test
  void initialPositionIsSymmetricAndPerspectivesAreOpposite() {
    Board board = Board.standard();
    assertEquals(0, evaluator.evaluate(board, Color.WHITE));
    assertEquals(0, evaluator.evaluate(board, Color.BLACK));

    Board asymmetric = singlePiece(Color.WHITE, Knight::new, Cell.C3);
    assertEquals(-evaluator.evaluate(asymmetric, Color.WHITE),
        evaluator.evaluate(asymmetric, Color.BLACK));
  }

  @Test
  void reversingColorsAndRanksReversesTheScore() {
    Board original = BoardFactory.position().castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new King(Color.BLACK, Cell.E8), Cell.E8)
        .place(new Rook(Color.WHITE, Cell.C2), Cell.C2)
        .place(new Pawn(Color.BLACK, Cell.D6), Cell.D6)
        .build();
    Board reversed = BoardFactory.position().sideToMove(Color.BLACK)
        .castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.E1), Cell.E1)
        .place(new King(Color.BLACK, Cell.E8), Cell.E8)
        .place(new Rook(Color.BLACK, Cell.C7), Cell.C7)
        .place(new Pawn(Color.WHITE, Cell.D3), Cell.D3)
        .build();
    assertEquals(-evaluator.evaluate(original, Color.WHITE),
        evaluator.evaluate(reversed, Color.WHITE));
    StandardPositionEvaluator standard = new StandardPositionEvaluator();
    assertEquals(-standard.evaluate(original, Color.WHITE),
        standard.evaluate(reversed, Color.WHITE));
  }

  @Test
  void pieceSquareTablesRewardActiveSquaresForBothColors() {
    assertImprovesForBothColors(Pawn::new, Cell.A4, Cell.D4);
    assertImprovesForBothColors(Knight::new, Cell.A3, Cell.C3);
    assertImprovesForBothColors(Bishop::new, Cell.A4, Cell.D4);
    assertImprovesForBothColors(Rook::new, Cell.D2, Cell.D7);
    assertImprovesForBothColors(Queen::new, Cell.A4, Cell.D4);
  }

  @Test
  void undevelopedMinorPenaltyFadesAsMaterialDisappears() {
    for (Color color : Color.values()) {
      int openingGain = developmentGain(color, true);
      int endgameGain = developmentGain(color, false);
      assertTrue(openingGain > endgameGain, color + ": " + openingGain + " vs " + endgameGain);
    }
  }

  @Test
  void pawnShieldProtectsKingsWhenMajorPiecesRemain() {
    for (Color color : Color.values()) {
      Board sheltered = shieldPosition(color, true);
      Board exposed = shieldPosition(color, false);
      assertTrue(evaluator.evaluate(sheltered, color) > evaluator.evaluate(exposed, color), color.toString());
    }
  }

  @Test
  void centralKingIsPreferredInEndgameButNotWithMajorPieces() {
    for (Color color : Color.values()) {
      int opening = kingPositionGain(color, true);
      int endgame = kingPositionGain(color, false);
      assertTrue(opening < 0, color + ": " + opening);
      assertTrue(endgame > 0, color + ": " + endgame);
    }
  }

  private void assertImprovesForBothColors(BiFunction<Color, Cell, Piece> piece,
      Cell passive, Cell active) {
    for (Color color : Color.values()) {
      Board before = singlePiece(color, piece, mirror(passive, color));
      Board after = singlePiece(color, piece, mirror(active, color));
      int gain = evaluator.evaluate(after, color) - evaluator.evaluate(before, color);
      assertTrue(gain > 0, color + ": " + passive + " to " + active + " = " + gain);
    }
  }

  private Board singlePiece(Color color, BiFunction<Color, Cell, Piece> piece, Cell cell) {
    return BoardFactory.position().castlingRights(false, false, false, false)
        .place(piece.apply(color, cell), cell).build();
  }

  private int developmentGain(Color color, boolean majorPieces) {
    return evaluator.evaluate(developmentPosition(color, Cell.C3, majorPieces), color)
        - evaluator.evaluate(developmentPosition(color, Cell.B1, majorPieces), color);
  }

  private Board developmentPosition(Color color, Cell square, boolean majorPieces) {
    Cell knight = mirror(square, color);
    var builder = BoardFactory.position().castlingRights(false, false, false, false)
        .place(new Knight(color, knight), knight);
    if (majorPieces) {
      addMajorPieces(builder);
    }
    return builder.build();
  }

  private Board shieldPosition(Color color, boolean sheltered) {
    Cell king = mirror(Cell.G1, color);
    var builder = BoardFactory.position().castlingRights(false, false, false, false)
        .place(new King(color, king), king);
    for (int file = 5; file <= 7; file++) {
      Cell pawn = mirror(Cell.of(sheltered ? 1 : 2, file), color);
      builder.place(new Pawn(color, pawn), pawn);
    }
    addMajorPieces(builder);
    return builder.build();
  }

  private int kingPositionGain(Color color, boolean majorPieces) {
    return evaluator.evaluate(kingPosition(color, Cell.D4, majorPieces), color)
        - evaluator.evaluate(kingPosition(color, Cell.G1, majorPieces), color);
  }

  private Board kingPosition(Color color, Cell square, boolean majorPieces) {
    Cell king = mirror(square, color);
    var builder = BoardFactory.position().castlingRights(false, false, false, false)
        .place(new King(color, king), king);
    if (majorPieces) {
      addMajorPieces(builder);
    }
    return builder.build();
  }

  private void addMajorPieces(BoardFactory.PositionBuilder builder) {
    builder.place(new Queen(Color.WHITE, Cell.D1), Cell.D1)
        .place(new Queen(Color.BLACK, Cell.D8), Cell.D8)
        .place(new Rook(Color.WHITE, Cell.A1), Cell.A1)
        .place(new Rook(Color.WHITE, Cell.H1), Cell.H1)
        .place(new Rook(Color.BLACK, Cell.A8), Cell.A8)
        .place(new Rook(Color.BLACK, Cell.H8), Cell.H8);
  }

  private Cell mirror(Cell cell, Color color) {
    return color == Color.WHITE ? cell : Cell.of(7 - cell.row(), cell.column());
  }
}
