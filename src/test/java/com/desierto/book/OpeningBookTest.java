package com.desierto.book;

import static org.junit.jupiter.api.Assertions.*;

import com.desierto.domain.Board;
import com.desierto.domain.BoardFactory;
import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.MoveGenerator;
import com.desierto.domain.MoveNotation;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Pawn;
import com.desierto.search.Engine;
import com.desierto.search.SearchLimits;
import com.desierto.search.SearchTimeoutException;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class OpeningBookTest {
  @TempDir Path directory;

  @Test
  void matchesPublishedPolyglotPositionKeys() {
    Board board = Board.standard();
    assertEquals(0x463b96181691fc9cL, PolyglotKey.of(board));
    play(board, "e2e4");
    assertEquals(0x823c9b50fd114196L, PolyglotKey.of(board));
    play(board, "d7d5");
    assertEquals(0x0756b94461c50fb0L, PolyglotKey.of(board));
    play(board, "e4e5");
    assertEquals(0x662fafb965db29d4L, PolyglotKey.of(board));
    play(board, "f7f5");
    assertEquals(0x22a48b5a8e47ff78L, PolyglotKey.of(board));
    play(board, "e1e2");
    assertEquals(0x652a607ca3f242c1L, PolyglotKey.of(board));
    play(board, "e8f7");
    assertEquals(0x00fdd303c946bdd9L, PolyglotKey.of(board));

    Board blackEnPassant = Board.standard();
    for (String move : List.of("a2a4", "b7b5", "h2h4", "b5b4", "c2c4")) {
      play(blackEnPassant, move);
    }
    assertEquals(0x3c8123ea7b067637L, PolyglotKey.of(blackEnPassant));
  }

  @Test
  void selectsWeightedLegalMovesWithoutChangingBoard() throws Exception {
    Board board = Board.standard();
    long key = PolyglotKey.of(board);
    Path file = write(new Entry(key, encode("e2e4"), 2),
        new Entry(key, encode("d2d4"), 3), new Entry(key, encode("e2e5"), 20),
        new Entry(key, encode("g1f3"), 0));
    List<String> warnings = new ArrayList<>();
    OpeningBook first = new OpeningBook(file, warnings::add, bound -> 1);
    OpeningBook last = new OpeningBook(file, warnings::add, bound -> bound - 1);
    assertEquals("e2e4", MoveNotation.format(first.findMove(board).orElseThrow()));
    assertEquals("d2d4", MoveNotation.format(last.findMove(board).orElseThrow()));
    assertTrue(warnings.stream().anyMatch(w -> w.contains("Illegal move")));
    assertEquals(0x463b96181691fc9cL, PolyglotKey.of(board));
    assertEquals(Color.WHITE, board.getSideToMove());
  }

  @Test
  void handlesCastlingPromotionAndTranspositions() throws Exception {
    Board castle = Board.standard();
    for (String move : List.of("e2e4", "e7e5", "g1f3", "b8c6", "f1c4", "g8f6")) play(castle, move);
    Path file = write(new Entry(PolyglotKey.of(castle), encode("e1h1"), 1));
    assertEquals("e1g1", MoveNotation.format(new OpeningBook(file, failWarnings()).findMove(castle).orElseThrow()));

    Board promotion = BoardFactory.position()
        .castlingRights(false, false, false, false)
        .place(new King(Color.WHITE, Cell.A1), Cell.A1)
        .place(new King(Color.BLACK, Cell.H8), Cell.H8)
        .place(new Pawn(Color.WHITE, Cell.E7), Cell.E7).build();
    file = write(new Entry(PolyglotKey.of(promotion), encode("e7e8q"), 1));
    assertEquals("e7e8q", MoveNotation.format(new OpeningBook(file, failWarnings()).findMove(promotion).orElseThrow()));

    Board a = Board.standard();
    Board b = Board.standard();
    for (String move : List.of("g1f3", "g8f6", "b1c3", "b8c6")) play(a, move);
    for (String move : List.of("b1c3", "b8c6", "g1f3", "g8f6")) play(b, move);
    assertEquals(PolyglotKey.of(a), PolyglotKey.of(b));
    file = write(new Entry(PolyglotKey.of(a), encode("e2e4"), 1));
    assertEquals("e2e4", MoveNotation.format(new OpeningBook(file, failWarnings()).findMove(b).orElseThrow()));
  }

  @Test
  void validatesFileAndReportsRuntimeErrors() throws Exception {
    Path file = directory.resolve("book.bin");
    Files.write(file, new byte[15]);
    assertThrows(IOException.class, () -> new OpeningBook(file, failWarnings()));
    write(new Entry(2, encode("e2e4"), 1), new Entry(1, encode("d2d4"), 1));
    assertThrows(IOException.class, () -> new OpeningBook(file, failWarnings()));
    write(new Entry(PolyglotKey.of(Board.standard()), 0x8000, 1));
    assertThrows(IOException.class, () -> new OpeningBook(file, failWarnings()));
    write(new Entry(PolyglotKey.of(Board.standard()), encode("e2e4"), 1));
    List<String> warnings = new ArrayList<>();
    OpeningBook book = new OpeningBook(file, warnings::add);
    Files.delete(file);
    assertTrue(book.findMove(Board.standard()).isEmpty());
    assertTrue(warnings.getFirst().contains("Cannot read Polyglot book"));
  }

  @Test
  void engineUsesBookButFallsBackForDrawsAndMissingEntries() throws Exception {
    Board board = Board.standard();
    OpeningBook book = new OpeningBook(write(new Entry(PolyglotKey.of(board), encode("e2e4"), 1)),
        failWarnings());
    Engine engine = new Engine(book);
    var result = engine.think(board, new SearchLimits(1), Duration.ofSeconds(5));
    assertTrue(result.fromBook());
    assertEquals("e2e4", MoveNotation.format(result.bestMove().orElseThrow()));
    assertEquals(0, result.nodes());
    assertThrows(SearchTimeoutException.class,
        () -> engine.think(board, new SearchLimits(1), () -> true, ignored -> {}));
    play(board, "e2e4");
    assertFalse(engine.think(board, new SearchLimits(1)).fromBook());

    Board repeated = Board.standard();
    for (String move : List.of("g1f3", "g8f6", "f3g1", "f6g8", "g1f3", "g8f6", "f3g1", "f6g8")) {
      play(repeated, move);
    }
    assertTrue(engine.think(repeated, new SearchLimits(1)).bestMove().isEmpty());

    BoardFactory.PositionBuilder builder = BoardFactory.position().halfmoveClock(99);
    for (var piece : Board.standard().getPieces()) builder.place(piece, piece.getCell());
    Board claimable = builder.build();
    assertEquals(PolyglotKey.of(Board.standard()), PolyglotKey.of(claimable));
    assertFalse(engine.think(claimable, new SearchLimits(1)).fromBook());
  }

  @Test
  void binarySearchUsesUnsignedKeyOrder() throws Exception {
    Board board = Board.standard();
    long key = PolyglotKey.of(board);
    Path file = write(new Entry(0, encode("a2a3"), 1),
        new Entry(key, encode("e2e4"), 1), new Entry(-1, encode("a2a3"), 1));
    assertEquals("e2e4", MoveNotation.format(new OpeningBook(file, failWarnings())
        .findMove(board).orElseThrow()));
    play(board, "e2e4");
    assertTrue(new OpeningBook(file, failWarnings()).findMove(board).isEmpty());
  }

  private java.util.function.Consumer<String> failWarnings() {
    return warning -> fail(warning);
  }

  private Path write(Entry... entries) throws IOException {
    Path file = directory.resolve("book.bin");
    try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(file))) {
      for (Entry entry : entries) {
        out.writeLong(entry.key());
        out.writeShort(entry.move());
        out.writeShort(entry.weight());
        out.writeInt(0);
      }
    }
    return file;
  }

  private static int encode(String notation) {
    Cell from = Cell.fromAlgebraic(notation.substring(0, 2));
    Cell to = Cell.fromAlgebraic(notation.substring(2, 4));
    int promotion = notation.length() == 5 ? " nbrq".indexOf(notation.charAt(4)) : 0;
    return (promotion << 12) | ((from.row() * 8 + from.column()) << 6)
        | (to.row() * 8 + to.column());
  }

  private static void play(Board board, String notation) {
    var move = MoveNotation.find(notation,
        new MoveGenerator().generateLegalMoves(board, board.getSideToMove())).orElseThrow();
    board.makeMove(move);
  }

  private record Entry(long key, int move, int weight) {}
}
