package com.desierto;

import java.nio.file.Path;

/** Locations for resources next to a packaged executable or in the working directory. */
public record DefaultResources(Path book, Path tablebases) {
  public static DefaultResources locate() {
    return locate(System.getProperty("jpackage.app-path"), Path.of("").toAbsolutePath());
  }

  static DefaultResources locate(String appPath, Path workingDirectory) {
    Path root = appPath == null || appPath.isBlank()
        ? workingDirectory
        : Path.of(appPath).toAbsolutePath().getParent();
    return inDirectory(root);
  }

  public static DefaultResources inDirectory(Path directory) {
    Path root = directory.toAbsolutePath().normalize();
    return new DefaultResources(root.resolve("books").resolve("openings.bin"),
        root.resolve("tablebases"));
  }
}
