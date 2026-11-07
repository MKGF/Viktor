package com.desierto.domain;

import java.util.Objects;

/**
 * An isolated working copy for search. Moves must be closed in reverse order, and callers must not
 * mutate {@link #board()} directly while a move is active.
 */
public final class SearchPosition {

  private final Board board;
  private final Board source;
  private final long sourceVersion;
  private final long rootVersion;
  private MoveScope activeScope;

  public SearchPosition(Board source) {
    this.source = Objects.requireNonNull(source, "source must not be null");
    sourceVersion = source.positionVersion();
    board = source.copy();
    rootVersion = board.positionVersion();
  }

  public Board board() {
    return board;
  }

  public MoveScope apply(LegalMove move) {
    Objects.requireNonNull(move, "move must not be null").validateSource(board);
    RepetitionPosition previousPosition = board.repetitionPosition();
    RepetitionHistory.Node checkpoint = board.repetitionCheckpoint();
    Board.UndoState state = board.makeTemporaryMove(move.move());
    board.addRepetitionPosition(previousPosition, move.move());
    MoveScope scope = new MoveScope(state, checkpoint, activeScope, board.positionVersion());
    activeScope = scope;
    return scope;
  }

  public Move originalMove(Move move) {
    Objects.requireNonNull(move, "move must not be null");
    if (activeScope != null || !source.isAtVersion(sourceVersion) || !board.isAtVersion(rootVersion)) {
      throw new IllegalStateException("The source position changed during the search");
    }
    Piece piece = source.getPieceAt(move.from()).orElseThrow();
    Piece captured = null;
    if (move.isCapture()) {
      Cell captureCell = move.type() == MoveType.EN_PASSANT
          ? Cell.of(move.from().row(), move.to().column()) : move.to();
      captured = source.getPieceAt(captureCell).orElseThrow();
    }
    return new Move(move.color(), piece, move.from(), move.to(), move.promotionPiece(), move.type(),
        captured);
  }

  public final class MoveScope implements AutoCloseable {

    private final Board.UndoState state;
    private final RepetitionHistory.Node checkpoint;
    private final MoveScope previous;
    private final long appliedVersion;

    private MoveScope(Board.UndoState state, RepetitionHistory.Node checkpoint, MoveScope previous,
        long appliedVersion) {
      this.state = state;
      this.checkpoint = checkpoint;
      this.previous = previous;
      this.appliedVersion = appliedVersion;
    }

    @Override
    public void close() {
      if (activeScope != this || !board.isAtVersion(appliedVersion)) {
        throw new IllegalStateException("Search moves must be undone in order without changing the board");
      }
      board.restoreTemporaryMove(state);
      board.restoreRepetitionCheckpoint(checkpoint);
      activeScope = previous;
    }
  }
}
