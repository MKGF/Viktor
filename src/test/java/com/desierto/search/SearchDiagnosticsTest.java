package com.desierto.search;

import static org.junit.jupiter.api.Assertions.assertTrue;

import com.desierto.domain.Board;
import com.desierto.domain.BoardFactory;
import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Pawn;
import com.desierto.search.evaluation.MaterialEvaluator;
import com.desierto.search.evaluation.PositionalEvaluator;
import org.junit.jupiter.api.Test;

class SearchDiagnosticsTest {
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
