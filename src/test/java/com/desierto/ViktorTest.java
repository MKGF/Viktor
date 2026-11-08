package com.desierto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.assertFalse;

import java.io.DataOutputStream;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class ViktorTest {

  @Test
  void usesDefaultResourcesAndHonorsConsoleOptOuts(@TempDir Path directory) throws Exception {
    Path book = directory.resolve("books").resolve("openings.bin");
    Files.createDirectories(book.getParent());
    try (DataOutputStream out = new DataOutputStream(Files.newOutputStream(book))) {
      out.writeLong(0x463b96181691fc9cL);
      out.writeShort((12 << 6) | 28);
      out.writeShort(1);
      out.writeInt(0);
    }

    String defaultOutput = runConsole(directory, "default.log");
    assertTrue(defaultOutput.contains("source=book"));
    assertTrue(defaultOutput.contains("Viktor plays: e2e4 (opening book)"));
    assertTrue(defaultOutput.contains("Warning: cannot load tablebases"));

    String disabledOutput = runConsole(directory, "disabled.log", "--no-book", "--no-tablebases");
    assertTrue(disabledOutput.contains("source=search"));
    assertFalse(disabledOutput.contains("Warning:"));

    String overrideOutput = runConsole(directory, "override.log",
        "--no-book", "--book", book.toString(), "--no-tablebases");
    assertTrue(overrideOutput.contains("source=book"));
    assertFalse(overrideOutput.contains("Warning:"));
  }

  private static String runConsole(Path directory, String outputName, String... options) throws Exception {
    String classpath = System.getProperty("surefire.test.class.path", System.getProperty("java.class.path"));
    classpath = Arrays.stream(classpath.split(Pattern.quote(File.pathSeparator)))
        .map(entry -> Path.of(entry).toAbsolutePath().toString())
        .collect(Collectors.joining(File.pathSeparator));
    Path java = Path.of(System.getProperty("java.home"), "bin", "java");
    if (Files.exists(java.resolveSibling("java.exe"))) java = java.resolveSibling("java.exe");
    ArrayList<String> command = new ArrayList<>(Arrays.asList(
        java.toString(), "-cp", classpath, "com.desierto.Viktor", "--black", "--depth", "1"));
    command.addAll(Arrays.asList(options));
    Path output = directory.resolve(outputName);
    Process process = new ProcessBuilder(command).directory(directory.toFile())
        .redirectErrorStream(true).redirectOutput(output.toFile()).start();
    try {
      process.getOutputStream().write("quit\n".getBytes(StandardCharsets.UTF_8));
      process.getOutputStream().close();
      assertTrue(process.waitFor(30, TimeUnit.SECONDS), "Console process timed out");
      String text = Files.readString(output, StandardCharsets.UTF_8);
      assertEquals(0, process.exitValue(), text);
      return text;
    } finally {
      if (process.isAlive()) process.destroyForcibly();
    }
  }

  @Test
  void shouldParsePositiveWholeSecondsWithinTheSupportedRange() {
    assertEquals(Duration.ofSeconds(1), Viktor.parseTimeLimit("1"));
    assertEquals(Duration.ofSeconds(10), Viktor.parseTimeLimit("10"));
    assertEquals(Duration.ofSeconds(9223372036L), Viktor.parseTimeLimit("9223372036"));
  }

  @Test
  void shouldRejectInvalidTimeValues() {
    for (String value : new String[] {"0", "-1", "1.5", "abc", "9223372037",
        "9999999999999999999999999999"}) {
      assertThrows(IllegalArgumentException.class, () -> Viktor.parseTimeLimit(value), value);
    }
  }
}
