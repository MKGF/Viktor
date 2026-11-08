package com.desierto.uci;

import com.desierto.domain.Color;
import java.util.HashSet;
import java.util.Set;

record GoOptions(int depth, long timeMillis, boolean infinite, boolean managedTime) {
  private static final int MAX_DEPTH = 64;

  static GoOptions parse(String[] tokens, Color side) {
    int depth = MAX_DEPTH;
    Long moveTime = null;
    Long whiteTime = null;
    Long blackTime = null;
    long whiteIncrement = 0;
    long blackIncrement = 0;
    long movesToGo = 30;
    boolean infinite = false;
    boolean depthSpecified = false;
    Set<String> seen = new HashSet<>();
    for (int i = 1; i < tokens.length; i++) {
      String name = tokens[i];
      if (!seen.add(name)) {
        throw new IllegalArgumentException("Duplicate go parameter: " + name);
      }
      if (name.equals("infinite")) {
        infinite = true;
        continue;
      }
      if (!Set.of("depth", "movetime", "wtime", "btime", "winc", "binc", "movestogo")
          .contains(name)) {
        throw new IllegalArgumentException("Unsupported go parameter: " + name);
      }
      if (++i == tokens.length || !tokens[i].matches("[0-9]+")) {
        throw new IllegalArgumentException("Missing or invalid value for " + name);
      }
      long value;
      try {
        value = Long.parseLong(tokens[i]);
      } catch (NumberFormatException ex) {
        throw new IllegalArgumentException("Value too large for " + name, ex);
      }
      switch (name) {
        case "depth" -> {
          if (value < 1 || value > MAX_DEPTH) {
            throw new IllegalArgumentException("Depth must be between 1 and " + MAX_DEPTH);
          }
          depth = (int) value;
          depthSpecified = true;
        }
        case "movetime" -> moveTime = value;
        case "wtime" -> whiteTime = value;
        case "btime" -> blackTime = value;
        case "winc" -> whiteIncrement = value;
        case "binc" -> blackIncrement = value;
        case "movestogo" -> {
          if (value == 0) {
            throw new IllegalArgumentException("movestogo must be positive");
          }
          movesToGo = value;
        }
        default -> throw new IllegalStateException("Unexpected go parameter");
      }
    }
    Long remaining = side == Color.WHITE ? whiteTime : blackTime;
    long increment = side == Color.WHITE ? whiteIncrement : blackIncrement;
    long time = -1;
    if (!infinite && moveTime != null) {
      time = moveTime;
    } else if (!infinite && remaining != null) {
      long reserve = Math.min(100, remaining / 20);
      long available = remaining - reserve;
      long incrementBudget = increment - increment / 4;
      long base = remaining / movesToGo;
      time = base >= available || incrementBudget >= available - base
          ? available : base + incrementBudget;
    }
    return new GoOptions(depth, time, infinite || (time < 0 && !depthSpecified),
        !infinite && moveTime == null && remaining != null && time > 0);
  }
}
