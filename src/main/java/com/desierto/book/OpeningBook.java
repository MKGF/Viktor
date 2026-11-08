package com.desierto.book;

import com.desierto.domain.Board;
import com.desierto.domain.Cell;
import com.desierto.domain.Move;
import com.desierto.domain.MoveGenerator;
import com.desierto.domain.MoveNotation;
import com.desierto.domain.pieces.King;
import java.io.DataInputStream;
import java.io.EOFException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Consumer;
import java.util.function.LongUnaryOperator;

/** A sorted, big-endian Polyglot book. The file is not kept open between searches. */
public final class OpeningBook {
  private static final int ENTRY_SIZE = 16;
  private final Path path;
  private final Consumer<String> warnings;
  private final LongUnaryOperator random;

  public OpeningBook(Path path, Consumer<String> warnings) throws IOException {
    this(path, warnings, bound -> ThreadLocalRandom.current().nextLong(bound));
  }

  OpeningBook(Path path, Consumer<String> warnings, LongUnaryOperator random) throws IOException {
    this.path = java.util.Objects.requireNonNull(path, "path must not be null");
    this.warnings = java.util.Objects.requireNonNull(warnings, "warnings must not be null");
    this.random = java.util.Objects.requireNonNull(random, "random must not be null");
    validate();
  }

  private void validate() throws IOException {
    long size = Files.size(path);
    if (size % ENTRY_SIZE != 0) {
      throw new IOException("Polyglot book length is not a multiple of 16 bytes: " + path);
    }
    try (DataInputStream input = new DataInputStream(
        new java.io.BufferedInputStream(Files.newInputStream(path)))) {
      boolean first = true;
      long previous = 0;
      for (long i = 0; i < size / ENTRY_SIZE; i++) {
        long key = input.readLong();
        int move = input.readUnsignedShort();
        input.readUnsignedShort();
        input.readInt();
        if (!first && Long.compareUnsigned(previous, key) > 0) {
          throw new IOException("Polyglot book is not sorted: " + path);
        }
        if ((move & 0x8000) != 0 || ((move >>> 12) & 7) > 4) {
          throw new IOException("Invalid Polyglot move in book: " + path);
        }
        previous = key;
        first = false;
      }
    }
  }

  public Optional<Move> findMove(Board board) {
    long key = PolyglotKey.of(board);
    try (FileChannel file = FileChannel.open(path, StandardOpenOption.READ)) {
      long size = file.size();
      if (size % ENTRY_SIZE != 0) throw new IOException("Book length changed or is invalid");
      long low = 0;
      long high = size / ENTRY_SIZE;
      ByteBuffer entry = ByteBuffer.allocate(ENTRY_SIZE);
      while (low < high) {
        long mid = low + (high - low) / 2;
        readEntry(file, entry, mid);
        if (Long.compareUnsigned(entry.getLong(), key) < 0) low = mid + 1;
        else high = mid;
      }
      if (low == size / ENTRY_SIZE) return Optional.empty();
      readEntry(file, entry, low);
      if (entry.getLong() != key) return Optional.empty();
      List<Move> legalMoves = new MoveGenerator().generateLegalMoves(board, board.getSideToMove());
      List<WeightedMove> candidates = new ArrayList<>();
      long total = 0;
      for (long i = low; i < size / ENTRY_SIZE; i++) {
        readEntry(file, entry, i);
        if (entry.getLong() != key) break;
        int encoded = Short.toUnsignedInt(entry.getShort());
        int weight = Short.toUnsignedInt(entry.getShort());
        if ((encoded & 0x8000) != 0 || ((encoded >>> 12) & 7) > 4) {
          throw new IOException("Invalid Polyglot move in book");
        }
        String notation = notation(encoded, board);
        Optional<Move> move = MoveNotation.find(notation, legalMoves);
        if (move.isEmpty()) {
          warnings.accept("Illegal move in Polyglot book " + path + ": " + notation);
        } else if (weight > 0) {
          total = Math.addExact(total, weight);
          candidates.add(new WeightedMove(move.orElseThrow(), weight));
        }
      }
      if (total == 0) return Optional.empty();
      long selected = random.applyAsLong(total);
      if (selected < 0 || selected >= total) throw new IllegalStateException("Invalid random selection");
      for (WeightedMove candidate : candidates) {
        if (selected < candidate.weight()) return Optional.of(candidate.move());
        selected -= candidate.weight();
      }
      throw new IllegalStateException("Weighted selection failed");
    } catch (IOException | SecurityException ex) {
      warnings.accept("Cannot read Polyglot book " + path + ": " + ex.getMessage());
      return Optional.empty();
    }
  }

  private static void readEntry(FileChannel file, ByteBuffer buffer, long index) throws IOException {
    buffer.clear();
    long position = index * ENTRY_SIZE;
    while (buffer.hasRemaining()) {
      int count = file.read(buffer, position + buffer.position());
      if (count < 0) throw new EOFException("Incomplete Polyglot entry");
      if (count == 0) throw new IOException("Unable to read Polyglot entry");
    }
    buffer.flip();
  }

  private static String notation(int encoded, Board board) {
    int from = (encoded >>> 6) & 63;
    int to = encoded & 63;
    String source = Cell.of(from / 8, from % 8).toAlgebraic().toLowerCase(java.util.Locale.ROOT);
    String target = Cell.of(to / 8, to % 8).toAlgebraic().toLowerCase(java.util.Locale.ROOT);
    if (board.getPieceAt(Cell.of(from / 8, from % 8)).orElse(null) instanceof King) {
      if (source.equals("e1") && target.equals("h1")) target = "g1";
      else if (source.equals("e1") && target.equals("a1")) target = "c1";
      else if (source.equals("e8") && target.equals("h8")) target = "g8";
      else if (source.equals("e8") && target.equals("a8")) target = "c8";
    }
    int promotion = (encoded >>> 12) & 7;
    return source + target + (promotion == 0 ? "" : " nbrq".substring(promotion, promotion + 1));
  }

  private record WeightedMove(Move move, int weight) {}
}
