package com.desierto.search;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SearchLimitsTest {

  @Test
  void shouldAcceptAPositiveMaximumDepth() {
    SearchLimits limits = new SearchLimits(4);

    assertEquals(4, limits.maxDepth());
  }

  @Test
  void shouldRejectANonPositiveMaximumDepth() {
    assertThrows(IllegalArgumentException.class, () -> new SearchLimits(0));
    assertThrows(IllegalArgumentException.class, () -> new SearchLimits(-1));
  }
}
