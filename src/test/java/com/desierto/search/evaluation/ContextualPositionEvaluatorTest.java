package com.desierto.search.evaluation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.desierto.domain.Board;
import com.desierto.domain.BoardFactory;
import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.Piece;
import com.desierto.domain.pieces.Bishop;
import com.desierto.domain.pieces.Knight;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Rook;
import org.junit.jupiter.api.Test;

class ContextualPositionEvaluatorTest {
  private static final int SCALE = PositionalEvaluator.SCORE_SCALE;

  @Test
  void rookPrefersOpenToSemiOpenAndClosedFilesForBothColors() {
    for (Color color : Color.values()) {
      Rook rook = new Rook(color, mirror(Cell.D1, color));
      Pawn friendly = new Pawn(color, mirror(Cell.D4, color));
      Pawn enemy = new Pawn(color.opposite(), mirror(Cell.D6, color));
      RookPositionEvaluator evaluator = new RookPositionEvaluator();
      assertEquals(8 * SCALE, adjustment(evaluator, rook, position(rook)));
      assertEquals(4 * SCALE, adjustment(evaluator, rook, position(rook, enemy)));
      assertEquals(0, adjustment(evaluator, rook, position(rook, friendly)));
      assertEquals(0, adjustment(evaluator, rook, position(rook, friendly, enemy)));
    }
  }

  @Test
  void bishopValuesEnemyPawnsOnItsColorAndPenalizesFriendlyBlockers() {
    for (Color color : Color.values()) {
      Bishop bishop = new Bishop(color, mirror(Cell.C1, color));
      Pawn target = new Pawn(color.opposite(), mirror(Cell.F4, color));
      Pawn otherColor = new Pawn(color.opposite(), mirror(Cell.F5, color));
      Pawn blocker = new Pawn(color, mirror(Cell.D2, color));
      BishopPositionEvaluator evaluator = new BishopPositionEvaluator();
      assertEquals(2 * SCALE, adjustment(evaluator, bishop, position(bishop, target)));
      assertEquals(0, adjustment(evaluator, bishop, position(bishop, otherColor)));
      assertEquals(-3 * SCALE, adjustment(evaluator, bishop, position(bishop, blocker)));
      assertEquals(-SCALE, adjustment(evaluator, bishop, position(bishop, blocker, target)));
    }
  }

  @Test
  void knightOutpostDependsOnEnemyPawnChallengesAndFriendlyPawnSupport() {
    for (Color color : Color.values()) {
      Knight knight = new Knight(color, mirror(Cell.D5, color));
      Pawn supported = new Pawn(color, mirror(Cell.C4, color));
      Pawn challenger = new Pawn(color.opposite(), mirror(Cell.C7, color));
      Pawn passed = new Pawn(color.opposite(), mirror(Cell.C3, color));
      Pawn nearby = new Pawn(color.opposite(), mirror(Cell.F5, color));
      Pawn distant = new Pawn(color.opposite(), mirror(Cell.G5, color));
      KnightPositionEvaluator evaluator = new KnightPositionEvaluator();
      assertEquals(8 * SCALE, adjustment(evaluator, knight, position(knight)));
      assertEquals(12 * SCALE, adjustment(evaluator, knight, position(knight, supported)));
      assertEquals(0, adjustment(evaluator, knight, position(knight, supported, challenger)));
      assertEquals(8 * SCALE, adjustment(evaluator, knight, position(knight, passed)));
      assertEquals(9 * SCALE, adjustment(evaluator, knight, position(knight, nearby)));
      assertEquals(8 * SCALE, adjustment(evaluator, knight, position(knight, distant)));

      Knight backRank = new Knight(color, mirror(Cell.D2, color));
      assertEquals(0, adjustment(evaluator, backRank, position(backRank)));
    }
  }

  @Test
  void tableWeightFadesWithDevelopmentButContextBonusDoesNot() {
    Rook rook = new Rook(Color.WHITE, Cell.D1);
    PositionContext opening = new PositionContext(Board.standard());
    PositionContext middlegame = new PositionContext(position(rook,
        new Knight(Color.WHITE, Cell.B1), new Knight(Color.WHITE, Cell.G1),
        new Knight(Color.BLACK, Cell.B8), new Knight(Color.BLACK, Cell.G8)));
    PositionContext endgame = new PositionContext(position(rook));
    assertEquals(4 * 32 * PositionalEvaluator.MAX_PHASE, opening.base(rook));
    assertEquals(4 * 20 * PositionalEvaluator.MAX_PHASE, middlegame.base(rook));
    assertEquals(4 * 8 * PositionalEvaluator.MAX_PHASE, endgame.base(rook));
    RookPositionEvaluator evaluator = new RookPositionEvaluator();
    assertEquals(8 * SCALE, evaluator.evaluate(rook, middlegame) - middlegame.base(rook));
    assertEquals(8 * SCALE, evaluator.evaluate(rook, endgame) - endgame.base(rook));
  }

  @Test
  void pressureDistinguishesUndefendedDefendedAndBlockedAttacks() {
    for (Color color : Color.values()) {
      Rook target = new Rook(color, mirror(Cell.D4, color));
      Knight attacker = new Knight(color.opposite(), mirror(Cell.F5, color));
      Pawn defender = new Pawn(color, mirror(Cell.C3, color));
      PositionContext attacked = new PositionContext(position(target, attacker));
      PositionContext defended = new PositionContext(position(target, attacker, defender));
      assertEquals(-6, attacked.pressurePenalty(target));
      assertEquals(-2, defended.pressurePenalty(target));

      Bishop bishop = new Bishop(color.opposite(), mirror(Cell.B2, color));
      Pawn blocker = new Pawn(color.opposite(), mirror(Cell.C3, color));
      assertEquals(0, new PositionContext(position(target, bishop, blocker)).pressurePenalty(target));
    }
  }

  @Test
  void sameTableSquareFavorsOpenFileInCompleteEvaluation() {
    PositionalEvaluator evaluator = new PositionalEvaluator();
    for (Color color : Color.values()) {
      Pawn pawn = new Pawn(color, mirror(Cell.E4, color));
      Rook open = new Rook(color, mirror(Cell.D1, color));
      Rook closed = new Rook(color, mirror(Cell.E1, color));
      int openScore = evaluator.evaluate(position(open, pawn), color);
      int closedScore = evaluator.evaluate(position(closed, pawn), color);
      assertTrue(openScore > closedScore, color + ": " + openScore + " vs " + closedScore);
    }
  }

  private int adjustment(RookPositionEvaluator evaluator, Rook piece, Board board) {
    PositionContext context = new PositionContext(board);
    return evaluator.evaluate(piece, context) - context.base(piece);
  }

  private int adjustment(BishopPositionEvaluator evaluator, Bishop piece, Board board) {
    PositionContext context = new PositionContext(board);
    return evaluator.evaluate(piece, context) - context.base(piece) - context.undevelopedPenalty(piece);
  }

  private int adjustment(KnightPositionEvaluator evaluator, Knight piece, Board board) {
    PositionContext context = new PositionContext(board);
    return evaluator.evaluate(piece, context) - context.base(piece) - context.undevelopedPenalty(piece);
  }

  private Board position(Piece... pieces) {
    var builder = BoardFactory.position().castlingRights(false, false, false, false);
    for (Piece piece : pieces) builder.place(piece, piece.getCell());
    return builder.build();
  }

  private Cell mirror(Cell square, Color color) {
    return color == Color.WHITE ? square : Cell.of(7 - square.row(), square.column());
  }
}
