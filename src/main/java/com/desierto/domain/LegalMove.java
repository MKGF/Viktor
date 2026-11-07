package com.desierto.domain;

import java.util.Objects;

public final class LegalMove {

  private final Board sourceBoard;
  private final long sourceVersion;
  private final Move move;

  LegalMove(Board sourceBoard, Move move) {
    this.sourceBoard = Objects.requireNonNull(sourceBoard, "sourceBoard must not be null");
    this.sourceVersion = sourceBoard.positionVersion();
    this.move = Objects.requireNonNull(move, "move must not be null");
  }

  public Move move() {
    return move;
  }

  public Board resultingPosition() {
    validateSource(sourceBoard);
    return sourceBoard.copyAfterGeneratedMove(move);
  }

  public boolean givesCheck() {
    validateSource(sourceBoard);
    Board.UndoState state = sourceBoard.makeTemporaryMove(move);
    try {
      return new AttackDetector().isKingInCheck(sourceBoard, sourceBoard.getSideToMove());
    } finally {
      sourceBoard.restoreTemporaryMove(state);
    }
  }

  void validateSource(Board board) {
    if (sourceBoard != board || !sourceBoard.isAtVersion(sourceVersion)) {
      throw new IllegalStateException("The source position changed after generating the legal move");
    }
  }
}
