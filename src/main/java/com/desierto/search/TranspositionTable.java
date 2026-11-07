package com.desierto.search;

final class TranspositionTable {

  static final int DEFAULT_CAPACITY = 1 << 16;

  enum Bound {
    EXACT, LOWER, UPPER
  }

  record Entry(long key, int depth, int ply, int score, Bound bound) {

    boolean canCutOff(int alpha, int beta) {
      return bound == Bound.EXACT
          || bound == Bound.LOWER && score >= beta
          || bound == Bound.UPPER && score <= alpha;
    }
  }

  private final Entry[] entries;

  TranspositionTable(int capacity) {
    if (capacity < 1 || (capacity & (capacity - 1)) != 0) {
      throw new IllegalArgumentException("capacity must be a positive power of two");
    }
    entries = new Entry[capacity];
  }

  Entry find(long key, int depth, int ply) {
    Entry entry = entries[index(key)];
    if (entry == null || entry.key() != key || entry.depth() != depth || entry.ply() != ply) {
      return null;
    }
    return entry;
  }

  void store(long key, int depth, int ply, int score, int alpha, int beta) {
    Bound bound = score <= alpha ? Bound.UPPER : score >= beta ? Bound.LOWER : Bound.EXACT;
    entries[index(key)] = new Entry(key, depth, ply, score, bound);
  }

  private int index(long key) {
    return (int) (key ^ (key >>> 32)) & (entries.length - 1);
  }
}
