package com.desierto.search;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.desierto.domain.Board;
import com.desierto.domain.BoardFactory;
import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.LegalMove;
import com.desierto.domain.Move;
import com.desierto.domain.MoveGenerator;
import com.desierto.domain.MoveNotation;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Rook;
import com.desierto.search.evaluation.MaterialEvaluator;
import com.desierto.search.evaluation.PositionalEvaluator;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class SearchDiagnosticsTest {
  @Test
  void explainsSearchWithImmediateChangesFromTheMovingSidesPerspective() {
    for (Color side : Color.values()) {
      boolean white = side == Color.WHITE;
      Cell rookCell = white ? Cell.B1 : Cell.G8;
      Cell pawnCell = white ? Cell.B4 : Cell.G5;
      Board board = BoardFactory.position().sideToMove(side)
          .castlingRights(false, false, false, false)
          .place(new King(Color.WHITE, Cell.A1), Cell.A1)
          .place(new King(Color.BLACK, Cell.H8), Cell.H8)
          .place(new Rook(side, rookCell), rookCell)
          .place(new Pawn(side.opposite(), pawnCell), pawnCell).build();
      String notation = white ? "b1b4" : "g8g5";
      Move move = MoveNotation.find(notation,
          new MoveGenerator().generateLegalMoves(board, side)).orElseThrow();
      Board after = new LegalMove(board, move).resultingPosition();
      int positionalDelta = new PositionalEvaluator().evaluate(after, side)
          - new PositionalEvaluator().evaluate(board, side);
      String original = board.toString();
      SearchResult result = new SearchResult(Optional.of(move), side, 200, 2, 20);
      String diagnostic = SearchDiagnostics.decision("search", notation, result, "", board);
      assertTrue(diagnostic.contains("reason=best_searched_score"));
      assertTrue(diagnostic.contains("static_perspective=" + side.name().toLowerCase(java.util.Locale.ROOT)));
      assertTrue(diagnostic.contains("static_material_delta=cp:100"));
      assertTrue(diagnostic.contains("static_positional_delta=cp:" + positionalDelta));
      assertEquals(original, board.toString());
      assertEquals(side, board.getSideToMove());
    }
  }

  @Test
  void identifiesBookAndFallbackWithoutClaimingSearchOrStaticAttribution() {
    Board board = Board.standard();
    Move move = MoveNotation.find("e2e4",
        new MoveGenerator().generateLegalMoves(board, Color.WHITE)).orElseThrow();
    SearchResult book = new SearchResult(Optional.of(move), Color.WHITE, 0, 0, 0,
        Optional.empty(), SearchTrace.EMPTY, true);
    String diagnostic = SearchDiagnostics.decision("book", "e2e4", book, "", board);
    assertTrue(diagnostic.contains("reason=opening_book"));
    assertFalse(diagnostic.contains("static_material_delta"));
    assertTrue(diagnostic.contains("score=unavailable"));
    String fallback = SearchDiagnostics.decision("no_completed_iteration", "none", null, "", board);
    assertTrue(fallback.contains("reason=no_completed_iteration"));
    assertFalse(fallback.contains("static_material_delta"));
  }

  @Test
  void positionBreakdownUsesWhitePerspectiveForBothTurns() {
    for (Color side : Color.values()) {
      Board board = BoardFactory.position().sideToMove(side)
          .castlingRights(false, false, false, false)
          .place(new King(Color.WHITE, Cell.E1), Cell.E1)
          .place(new King(Color.BLACK, Cell.E8), Cell.E8)
          .place(new Pawn(Color.BLACK, Cell.D5), Cell.D5)
          .build();
      int material = new MaterialEvaluator().evaluate(board, Color.WHITE);
      int positional = new PositionalEvaluator().evaluate(board, Color.WHITE);
      String diagnostic = SearchDiagnostics.position(board, "received");
      assertTrue(diagnostic.contains("perspective=white"));
      assertTrue(diagnostic.contains("side_to_move=" + side.name().toLowerCase(java.util.Locale.ROOT)));
      assertTrue(diagnostic.contains("evaluation=cp:" + (material + positional)));
      assertTrue(diagnostic.contains("material=cp:" + material));
      assertTrue(diagnostic.contains("positional=cp:" + positional));
    }
  }

  @Test
  void recognizedDrawTakesPrecedenceOverHeuristicBreakdown() {
    Board board = BoardFactory.position().castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.D4), Cell.D4)
        .place(new King(Color.BLACK, Cell.H8), Cell.H8)
        .build();
    String diagnostic = SearchDiagnostics.position(board, "received");
    assertTrue(diagnostic.contains("evaluation=cp:0"));
    assertTrue(diagnostic.contains("material=cp:0"));
    assertTrue(diagnostic.contains("positional=cp:"));
  }
}
