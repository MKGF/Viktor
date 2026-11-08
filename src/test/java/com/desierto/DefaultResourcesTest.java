package com.desierto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.nio.file.Path;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DefaultResourcesTest {
  @Test
  void usesWorkingDirectoryForJavaAndExecutableDirectoryForPackagedApplication(@TempDir Path directory) {
    Path working = directory.resolve("working");
    Path executable = directory.resolve("app").resolve("ViktorUCI.exe");
    assertEquals(DefaultResources.inDirectory(working), DefaultResources.locate(null, working));
    assertEquals(DefaultResources.inDirectory(working), DefaultResources.locate("", working));
    DefaultResources packaged = DefaultResources.locate(executable.toString(), working);
    assertEquals(executable.getParent().resolve("books").resolve("openings.bin"), packaged.book());
    assertEquals(executable.getParent().resolve("tablebases"), packaged.tablebases());
  }
}
