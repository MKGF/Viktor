package com.desierto.search;

public final class SearchTimeoutException extends RuntimeException {

  public SearchTimeoutException() {
    super("Time limit expired before a search iteration completed");
  }
}
