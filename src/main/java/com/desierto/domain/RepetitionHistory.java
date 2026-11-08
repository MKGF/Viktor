package com.desierto.domain;

final class RepetitionHistory {
  // Immutable nodes let search scopes restore even histories discarded by irreversible moves.
  record Node(RepetitionPosition position, Node previous, int size, long fingerprint) {
  }

  private Node head;

  void add(RepetitionPosition position, boolean irreversible) {
    if (irreversible) {
      head = null;
    } else {
      // Repetition depends on occurrence counts, not on the order of earlier positions.
      head = new Node(position, head, head == null ? 1 : head.size() + 1,
          fingerprint() + position.fingerprint());
    }
  }

  Node checkpoint() {
    return head;
  }

  void restore(Node checkpoint) {
    head = checkpoint;
  }

  boolean containsTwice(RepetitionPosition position) {
    int count = 0;
    for (Node node = head; node != null; node = node.previous()) {
      if (node.position().equals(position) && ++count == 2) {
        return true;
      }
    }
    return false;
  }

  boolean mayRepeat() {
    return head != null && head.size() >= 2;
  }

  boolean isEmpty() {
    return head == null;
  }

  long fingerprint() {
    return head == null ? 0 : head.fingerprint();
  }

  void replaceWith(RepetitionHistory other) {
    head = other.head;
  }

  void clear() {
    head = null;
  }
}
