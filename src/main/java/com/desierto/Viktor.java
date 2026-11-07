package com.desierto;

import com.desierto.console.ConsoleGame;
import com.desierto.domain.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

public final class Viktor {

  private Viktor() {
  }

  public static void main(String[] args) throws IOException {
    Color humanColor = Color.WHITE;
    int depth = 3;
    Duration timeLimit = ConsoleGame.DEFAULT_TIME_LIMIT;
    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--help", "--ayuda" -> {
          System.out.println("Usage: java -cp target\\classes com.desierto.Viktor [--black] [--depth N] [--time SECONDS]");
          return;
        }
        case "--black", "--negras" -> humanColor = Color.BLACK;
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
        default -> {
          fail("Unknown option: " + args[i]);
          return;
        }
      }
    }
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    new ConsoleGame(humanColor, depth, timeLimit).run(input, System.out);
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
