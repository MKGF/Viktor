package com.desierto.tablebase;

import java.util.Objects;

/** Syzygy's outcome for the side to move and distance to a zeroing move, in plies. */
public record TablebaseOutcome(Wdl wdl, int dtz) {
  public enum Wdl {
    LOSS, BLESSED_LOSS, DRAW, CURSED_WIN, WIN
  }

  public TablebaseOutcome {
    Objects.requireNonNull(wdl, "wdl must not be null");
    if (dtz < 0) {
      throw new IllegalArgumentException("dtz must not be negative");
    }
  }
}
