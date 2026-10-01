package com.desierto.search.evaluation;

import com.desierto.domain.Board;
import com.desierto.domain.Color;
import com.desierto.domain.MoveGenerator;
import java.util.Objects;

public final class StandardPositionEvaluator implements PositionEvaluator {

  public static final int CHECKMATE_SCORE = 100_000;

  private final PositionEvaluator materialEvaluator;
  private final MoveGenerator moveGenerator;

  public StandardPositionEvaluator() {
    this(new MaterialEvaluator(), new MoveGenerator());
  }

  StandardPositionEvaluator(PositionEvaluator materialEvaluator, MoveGenerator moveGenerator) {
    this.materialEvaluator = Objects.requireNonNull(materialEvaluator, "materialEvaluator must not be null");
    this.moveGenerator = Objects.requireNonNull(moveGenerator, "moveGenerator must not be null");
  }

  @Override
  public int evaluate(Board board, Color perspective) {
    return evaluate(board, perspective, 0);
  }

  @Override
  public int evaluate(Board board, Color perspective, int plyFromRoot) {
    Objects.requireNonNull(board, "board must not be null");
    Objects.requireNonNull(perspective, "perspective must not be null");
    if (plyFromRoot < 0 || plyFromRoot >= CHECKMATE_SCORE) {
      throw new IllegalArgumentException("plyFromRoot must be between 0 and " + (CHECKMATE_SCORE - 1));
    }

    Color sideToMove = board.getSideToMove();
    if (moveGenerator.isCheckmate(board, sideToMove)) {
      int score = CHECKMATE_SCORE - plyFromRoot;
      return sideToMove == perspective ? -score : score;
    }
    if (moveGenerator.isStalemate(board, sideToMove)) {
      return 0;
    }
    return materialEvaluator.evaluate(board, perspective);
  }
}
