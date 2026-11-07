package com.desierto;

import com.desierto.console.ConsoleGame;
import com.desierto.domain.Color;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class Viktor {

  private Viktor() {
  }

  public static void main(String[] args) throws IOException {
    Color humanColor = Color.WHITE;
    int depth = 3;
    for (int i = 0; i < args.length; i++) {
      switch (args[i]) {
        case "--help", "--ayuda" -> {
          System.out.println("Usage: java -cp target\\classes com.desierto.Viktor [--black] [--depth N]");
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
        default -> {
          fail("Unknown option: " + args[i]);
          return;
        }
      }
    }
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    new ConsoleGame(humanColor, depth).run(input, System.out);
  }

  private static void fail(String message) {
    System.err.println(message + " Use --help to see the available options.");
    System.exit(2);
  }
}
