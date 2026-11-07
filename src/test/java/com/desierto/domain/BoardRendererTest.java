package com.desierto.domain;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class BoardRendererTest {

  @Test
  void shouldDistinguishColorsAndKnightsFromKings() {
    String expected = String.join(System.lineSeparator(),
        "8 r n b q k b n r",
        "7 p p p p p p p p",
        "6 . . . . . . . .",
        "5 . . . . . . . .",
        "4 . . . . . . . .",
        "3 . . . . . . . .",
        "2 P P P P P P P P",
        "1 R N B Q K B N R",
        "  A B C D E F G H");
    assertEquals(expected, Board.standard().toString());
  }
}
