package com.desierto.uci;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;
import java.util.UUID;

final class UciSessionLog implements AutoCloseable {
  private final Path path;
  private final BufferedWriter writer;

  private UciSessionLog(Path path, BufferedWriter writer) {
    this.path = path;
    this.writer = writer;
  }

  static UciSessionLog open(Path directory, String engineName) throws IOException {
    Objects.requireNonNull(directory, "directory must not be null");
    Objects.requireNonNull(engineName, "engineName must not be null");
    Files.createDirectories(directory);
    String timestamp = OffsetDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
    Path path = directory.resolve("viktor-uci-" + timestamp + "-" + UUID.randomUUID() + ".log");
    BufferedWriter writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
        StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
    UciSessionLog log = new UciSessionLog(path, writer);
    try {
      log.record("session:\n  engine=" + engineName + "\n  protocol=uci");
      return log;
    } catch (IOException ex) {
      try {
        writer.close();
      } catch (IOException closeFailure) {
        ex.addSuppressed(closeFailure);
      }
      throw ex;
    }
  }

  Path path() {
    return path;
  }

  synchronized void record(String event) throws IOException {
    Objects.requireNonNull(event, "event must not be null");
    writer.write("[" + OffsetDateTime.now() + "]");
    writer.newLine();
    writer.write(event.replace("\n", System.lineSeparator()));
    writer.newLine();
    writer.newLine();
    writer.flush();
  }

  @Override
  public synchronized void close() throws IOException {
    writer.close();
  }
}
