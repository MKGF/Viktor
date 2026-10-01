package com.desierto.search;

public record SearchLimits(int maxDepth) {

  public SearchLimits {
    if (maxDepth < 1) {
      throw new IllegalArgumentException("maxDepth must be at least 1");
    }
  }
}
