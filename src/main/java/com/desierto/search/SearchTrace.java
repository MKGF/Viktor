package com.desierto.search;

import java.util.List;
import java.util.Objects;

public record SearchTrace(List<String> principalVariation, List<Alternative> alternatives) {
  public static final SearchTrace EMPTY = new SearchTrace(List.of(), List.of());

  public SearchTrace {
    principalVariation = List.copyOf(principalVariation);
    alternatives = List.copyOf(alternatives);
  }

  public enum Bound {
    EXACT, UPPER
  }

  public record Alternative(String move, int score, Bound bound) {
    public Alternative {
      Objects.requireNonNull(move, "move must not be null");
      Objects.requireNonNull(bound, "bound must not be null");
    }
  }
}
