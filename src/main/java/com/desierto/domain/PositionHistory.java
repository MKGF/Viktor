package com.desierto.domain;

import java.util.ArrayDeque;
import java.util.Deque;

final class PositionHistory {

  private final Deque<Board> snapshots = new ArrayDeque<>();

  void save(Board board) {
    snapshots.push(board);
  }

  Board restore() {
    if (snapshots.isEmpty()) {
      throw new IllegalStateException("No move to undo");
    }
    return snapshots.pop();
  }
}
