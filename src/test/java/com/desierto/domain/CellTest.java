package com.desierto.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class CellTest {

  @Test
  void shouldConvertToAndFromAlgebraicNotation() {
    Cell cell = Cell.fromAlgebraic("E4");

    assertEquals(3, cell.row());
    assertEquals(4, cell.column());
    assertEquals("E4", cell.toAlgebraic());
  }

  @Test
  void shouldRejectInvalidNotation() {
    assertThrows(IllegalArgumentException.class, () -> Cell.fromAlgebraic("I9"));
    assertThrows(IllegalArgumentException.class, () -> Cell.fromAlgebraic("A0"));
    assertThrows(IllegalArgumentException.class, () -> new Cell(-1, 0));
    assertThrows(IllegalArgumentException.class, () -> new Cell(0, 8));
  }
}
