package com.desierto.uci;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.desierto.domain.Color;
import org.junit.jupiter.api.Test;

class GoOptionsTest {
  @Test
  void supportsDepthMovetimeClocksAndInfiniteSearch() {
    GoOptions depth = GoOptions.parse("go depth 4".split(" "), Color.WHITE);
    assertEquals(4, depth.depth());
    assertEquals(-1, depth.timeMillis());
    assertFalse(depth.infinite());
    assertEquals(250, GoOptions.parse("go movetime 250".split(" "), Color.WHITE).timeMillis());
    assertFalse(GoOptions.parse("go movetime 250 wtime 30000".split(" "), Color.WHITE).managedTime());
    assertFalse(depth.managedTime());
    GoOptions clock = GoOptions.parse("go wtime 30000 btime 10000 winc 1000 binc 200 movestogo 10"
        .split(" "), Color.WHITE);
    assertEquals(3750, clock.timeMillis());
    assertTrue(clock.managedTime());
    assertEquals(1150, GoOptions.parse("go wtime 30000 btime 10000 winc 1000 binc 200 movestogo 10"
        .split(" "), Color.BLACK).timeMillis());
    assertTrue(GoOptions.parse("go infinite".split(" "), Color.WHITE).infinite());
    assertFalse(GoOptions.parse("go infinite wtime 30000".split(" "), Color.WHITE).managedTime());
    assertTrue(GoOptions.parse("go".split(" "), Color.WHITE).infinite());
    assertEquals(0, GoOptions.parse("go wtime 0 btime 0".split(" "), Color.WHITE).timeMillis());
  }

  @Test
  void rejectsUnsupportedOrInvalidLimits() {
    for (String command : new String[] {"go depth 0", "go depth 65", "go nodes 50",
        "go movetime -1", "go movestogo 0", "go depth", "go depth 1 depth 2"}) {
      assertThrows(IllegalArgumentException.class,
          () -> GoOptions.parse(command.split(" "), Color.WHITE), command);
    }
  }
}
