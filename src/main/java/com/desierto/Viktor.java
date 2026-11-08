package com.desierto;

import com.desierto.console.ConsoleGame;
import com.desierto.book.OpeningBook;
import com.desierto.domain.Color;
import com.desierto.tablebase.SyzygyTablebase;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;
import java.time.Duration;

public final class Viktor {

  private Viktor() {
  }

  public static void main(String[] args) throws IOException {
    Color humanColor = Color.WHITE;
    int depth = 3;
    Duration timeLimit = ConsoleGame.DEFAULT_TIME_LIMIT;
    DefaultResources defaults = DefaultResources.locate();
    Path bookPath = defaults.book();
    Path tablebasePath = defaults.tablebases();
    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--help", "--ayuda" -> {
          System.out.println("Usage: java -cp <classpath> com.desierto.Viktor [--black] [--depth N] [--time SECONDS] [--book PATH|--no-book] [--tablebases PATH|--no-tablebases]");
          return;
        }
        case "--black", "--negras" -> humanColor = Color.BLACK;
        case "--no-book" -> bookPath = null;
        case "--no-tablebases" -> tablebasePath = null;
        case "--depth", "--profundidad" -> {
          if (++i == args.length) {
            fail("Missing value for --depth.");
            return;
          }
          try {
            depth = Integer.parseInt(args[i]);
          } catch (NumberFormatException ex) {
            fail("Depth must be a positive integer.");
            return;
          }
          if (depth < 1) {
            fail("Depth must be a positive integer.");
            return;
          }
        }
        case "--time" -> {
          if (++i == args.length) {
            fail("Missing value for --time.");
            return;
          }
          try {
            timeLimit = parseTimeLimit(args[i]);
          } catch (IllegalArgumentException ex) {
            fail(ex.getMessage());
            return;
          }
        }
        case "--book" -> {
          if (++i == args.length || args[i].isBlank()) {
            fail("Missing value for --book.");
            return;
          }
          try {
            bookPath = Path.of(args[i]);
          } catch (InvalidPathException ex) {
            fail("Invalid path for --book: " + ex.getMessage());
            return;
          }
        }
        case "--tablebases" -> {
          if (++i == args.length || args[i].isBlank()) {
            fail("Missing value for --tablebases.");
            return;
          }
          try {
            tablebasePath = Path.of(args[i]);
          } catch (InvalidPathException ex) {
            fail("Invalid path for --tablebases: " + ex.getMessage());
            return;
          }
        }
        default -> {
          fail("Unknown option: " + args[i]);
          return;
        }
      }
    }
    OpeningBook book = null;
    if (bookPath != null) {
      try {
        book = new OpeningBook(bookPath, message -> System.err.println("Warning: " + message));
      } catch (IOException | SecurityException ex) {
        System.err.println("Warning: cannot load opening book " + bookPath + ": " + ex.getMessage());
      }
    }
    SyzygyTablebase tablebase = null;
    if (tablebasePath != null) {
      try {
        tablebase = new SyzygyTablebase(tablebasePath, message -> System.err.println("Warning: " + message));
      } catch (IOException | SecurityException ex) {
        System.err.println("Warning: cannot load tablebases " + tablebasePath + ": " + ex.getMessage());
      }
    }
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    new ConsoleGame(humanColor, depth, timeLimit, book, tablebase).run(input, System.out);
  }

  static Duration parseTimeLimit(String value) {
    long seconds;
    try {
      seconds = Long.parseLong(value);
    } catch (NumberFormatException ex) {
      throw new IllegalArgumentException("Time must be a positive whole number of seconds.", ex);
    }
    if (seconds < 1 || seconds > Long.MAX_VALUE / 1_000_000_000L) {
      throw new IllegalArgumentException("Time must be between 1 and 9223372036 seconds.");
    }
    return Duration.ofSeconds(seconds);
  }

  private static void fail(String message) {
    System.err.println(message + " Use --help to see the available options.");
    System.exit(2);
  }
}
