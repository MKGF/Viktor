package com.desierto.uci;

import com.desierto.EngineInfo;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;

public final class UciMain {
  private UciMain() {
  }

  public static void main(String[] args) throws IOException {
    try (BufferedReader input = new BufferedReader(
        new InputStreamReader(System.in, StandardCharsets.UTF_8));
        UciSessionLog log = openSessionLog()) {
      new UciProtocol(System.out, System.err, log).run(input);
    }
  }

  private static UciSessionLog openSessionLog() {
    try {
      String home = System.getProperty("user.home");
      if (home == null || home.isBlank()) {
        throw new IOException("user.home is not configured");
      }
      Path directory = Path.of(home, "Viktor", "logs");
      UciSessionLog log = UciSessionLog.open(directory, EngineInfo.name());
      System.err.println("Viktor log: " + log.path());
      return log;
    } catch (IOException | SecurityException ex) {
      String warning = "Warning: cannot create Viktor log: " + ex.getMessage();
      System.err.println(warning);
      System.out.println("info string " + warning);
      return null;
    }
  }
}
