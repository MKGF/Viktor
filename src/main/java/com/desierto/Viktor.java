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
        case "--ayuda" -> {
          System.out.println("Uso: java -cp target\\classes com.desierto.Viktor [--negras] [--profundidad N]");
          return;
        }
        case "--negras" -> humanColor = Color.BLACK;
        case "--profundidad" -> {
          if (++i == args.length) {
            fail("Falta el valor de --profundidad.");
            return;
          }
          try {
            depth = Integer.parseInt(args[i]);
          } catch (NumberFormatException ex) {
            fail("La profundidad debe ser un numero entero positivo.");
            return;
          }
          if (depth < 1) {
            fail("La profundidad debe ser un numero entero positivo.");
            return;
          }
        }
        default -> {
          fail("Opcion desconocida: " + args[i]);
          return;
        }
      }
    }
    BufferedReader input = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
    new ConsoleGame(humanColor, depth).run(input, System.out);
  }

  private static void fail(String message) {
    System.err.println(message + " Usa --ayuda para consultar las opciones.");
    System.exit(2);
  }
}
