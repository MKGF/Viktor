package com.desierto.domain;

import java.util.Objects;

public final class LegalMove {

  private final Board sourceBoard;
  private final long sourceVersion;
  private final Move move;

  /** Validates the requested move against the current position. */
  public LegalMove(Board board, Move move) {
    this(new MoveGenerator().verifyMove(board, move), 0);
  }

  LegalMove(MoveGenerator.VerifiedMoves verified, int index) {
    this.sourceBoard = verified.board();
    this.sourceVersion = sourceBoard.positionVersion();
    this.move = verified.moveAt(index);
  }

  public Move move() {
    return move;
  }

  /** Applies this move to its source board if that position has not changed. */
  public void play() {
    sourceBoard.applyLegalMove(this);
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
