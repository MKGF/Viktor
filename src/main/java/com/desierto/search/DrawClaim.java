package com.desierto.search;

import com.desierto.domain.Move;
import java.util.Objects;
import java.util.Optional;

/**
 * A fifty-move draw claim, either in the current position or with an intended legal move.
 */
public record DrawClaim(Optional<Move> intendedMove) {
  public DrawClaim {
    Objects.requireNonNull(intendedMove, "intendedMove must not be null");
  }
}
