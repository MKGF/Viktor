package com.desierto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ViktorTest {

  @Test
  void shouldParsePositiveWholeSecondsWithinTheSupportedRange() {
    assertEquals(Duration.ofSeconds(1), Viktor.parseTimeLimit("1"));
    assertEquals(Duration.ofSeconds(10), Viktor.parseTimeLimit("10"));
    assertEquals(Duration.ofSeconds(9223372036L), Viktor.parseTimeLimit("9223372036"));
  }

  @Test
  void shouldRejectInvalidTimeValues() {
    for (String value : new String[] {"0", "-1", "1.5", "abc", "9223372037",
        "9999999999999999999999999999"}) {
      assertThrows(IllegalArgumentException.class, () -> Viktor.parseTimeLimit(value), value);
    }
  }
}
