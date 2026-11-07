package com.desierto.uci;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

public final class UciMain {
  private UciMain() {
  }

  public static void main(String[] args) throws IOException {
    try (BufferedReader input = new BufferedReader(
        new InputStreamReader(System.in, StandardCharsets.UTF_8))) {
      new UciProtocol(System.out, System.err).run(input);
    }
  }
}
