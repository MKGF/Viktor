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
    if (!sourceBoard.isAtVersion(sourceVersion)) {
      throw new IllegalStateException("The source position changed after generating the legal move");
    }
    return sourceBoard.copyAfterGeneratedMove(move);
  }
}
