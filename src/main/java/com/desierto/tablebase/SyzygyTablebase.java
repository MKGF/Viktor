package com.desierto.tablebase;

import com.desierto.domain.Board;
import com.desierto.domain.Color;
import com.desierto.domain.DrawDetector;
import com.desierto.domain.Move;
import com.desierto.domain.MoveGenerator;
import com.desierto.domain.Piece;
import com.desierto.domain.pieces.Bishop;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Knight;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.FileChannel;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Consumer;
import net.chesstango.piazzolla.syzygy.Syzygy;
import net.chesstango.piazzolla.syzygy.SyzygyPosition;

/** Optional local Syzygy probe, restricted to fully installed three- and four-piece tables. */
public final class SyzygyTablebase {
  public record Decision(Move move, TablebaseOutcome outcome) {
    public Decision {
      Objects.requireNonNull(move, "move must not be null");
      Objects.requireNonNull(outcome, "outcome must not be null");
    }
  }

  private final Syzygy syzygy;
  private final Consumer<String> warnings;
  private boolean disabled;

  public SyzygyTablebase(Path directory, Consumer<String> warnings) throws IOException {
    Objects.requireNonNull(directory, "directory must not be null");
    this.warnings = Objects.requireNonNull(warnings, "warnings must not be null");
    for (String name : requiredTables()) {
      checkHeader(directory.resolve(name));
    }
    try {
      syzygy = Syzygy.open(directory);
    } catch (IllegalArgumentException | IllegalStateException | IndexOutOfBoundsException
        | ArithmeticException | NullPointerException | UnsupportedOperationException ex) {
      throw new IOException("Cannot initialize Syzygy tables in " + directory, ex);
    }
  }

  public synchronized Optional<Decision> probe(Board board) {
    Objects.requireNonNull(board, "board must not be null");
    if (disabled || board.getPieces().size() > 4 || board.getHalfmoveClock() != 0
        || board.hasRepetitionHistory() || board.getEnPassantTarget().isPresent()
        || board.canWhiteCastleKingside() || board.canWhiteCastleQueenside()
        || board.canBlackCastleKingside() || board.canBlackCastleQueenside()
        || DrawDetector.isDeadByInsufficientMaterial(board)) {
      return Optional.empty();
    }
    List<Move> legal = new MoveGenerator().generateLegalMoves(board, board.getSideToMove());
    if (legal.isEmpty()) {
      return Optional.empty();
    }
    try {
      int result = syzygy.tb_probe_root(toPosition(board), null);
      if (result == Syzygy.TB_RESULT_FAILED) {
        return failed("Syzygy could not probe the position");
      }
      int from = Syzygy.TB_GET_FROM(result);
      int to = Syzygy.TB_GET_TO(result);
      int promotion = Syzygy.TB_GET_PROMOTES(result);
      Optional<Move> move = legal.stream().filter(candidate ->
          square(candidate.from().row(), candidate.from().column()) == from
              && square(candidate.to().row(), candidate.to().column()) == to
              && promotion(candidate) == promotion).findFirst();
      if (move.isEmpty()) {
        return failed("Syzygy returned a move that is not legal");
      }
      TablebaseOutcome.Wdl wdl = switch (Syzygy.TB_GET_WDL(result)) {
        case Syzygy.TB_LOSS -> TablebaseOutcome.Wdl.LOSS;
        case Syzygy.TB_BLESSED_LOSS -> TablebaseOutcome.Wdl.BLESSED_LOSS;
        case Syzygy.TB_DRAW -> TablebaseOutcome.Wdl.DRAW;
        case Syzygy.TB_CURSED_WIN -> TablebaseOutcome.Wdl.CURSED_WIN;
        case Syzygy.TB_WIN -> TablebaseOutcome.Wdl.WIN;
        default -> throw new IllegalArgumentException("Invalid Syzygy outcome");
      };
      return Optional.of(new Decision(move.orElseThrow(),
          new TablebaseOutcome(wdl, Syzygy.TB_GET_DTZ(result))));
    } catch (IllegalArgumentException | IllegalStateException | IndexOutOfBoundsException
        | ArithmeticException | NullPointerException | UnsupportedOperationException ex) {
      return failed("Syzygy probe failed: " + ex);
    }
  }

  private Optional<Decision> failed(String message) {
    disabled = true;
    warnings.accept(message + "; continuing with normal search");
    return Optional.empty();
  }

  private static SyzygyPosition toPosition(Board board) {
    long white = 0, black = 0, kings = 0, queens = 0, rooks = 0, bishops = 0, knights = 0, pawns = 0;
    for (Piece piece : board.getPieces()) {
      long bit = 1L << square(piece.getCell().row(), piece.getCell().column());
      if (piece.getColor() == Color.WHITE) white |= bit; else black |= bit;
      if (piece instanceof King) kings |= bit;
      else if (piece instanceof Queen) queens |= bit;
      else if (piece instanceof Rook) rooks |= bit;
      else if (piece instanceof Bishop) bishops |= bit;
      else if (piece instanceof Knight) knights |= bit;
      else if (piece instanceof Pawn) pawns |= bit;
      else throw new IllegalArgumentException("Unknown piece type: " + piece.getClass().getName());
    }
    SyzygyPosition position = new SyzygyPosition();
    position.setWhite(white);
    position.setBlack(black);
    position.setKings(kings);
    position.setQueens(queens);
    position.setRooks(rooks);
    position.setBishops(bishops);
    position.setKnights(knights);
    position.setPawns(pawns);
    position.setTurn(board.getSideToMove() == Color.WHITE);
    return position;
  }

  private static int square(int row, int column) {
    return row * 8 + column;
  }

  private static int promotion(Move move) {
    Piece piece = move.promotionPiece();
    if (piece == null) return Syzygy.TB_PROMOTES_NONE;
    if (piece instanceof Queen) return Syzygy.TB_PROMOTES_QUEEN;
    if (piece instanceof Rook) return Syzygy.TB_PROMOTES_ROOK;
    if (piece instanceof Bishop) return Syzygy.TB_PROMOTES_BISHOP;
    if (piece instanceof Knight) return Syzygy.TB_PROMOTES_KNIGHT;
    throw new IllegalArgumentException("Unknown promotion piece");
  }

  private static List<String> requiredTables() {
    String pieces = "QRBNP";
    List<String> names = new ArrayList<>();
    for (int i = 0; i < pieces.length(); i++) {
      addPair(names, "K" + pieces.charAt(i) + "vK");
      for (int j = i; j < pieces.length(); j++) {
        addPair(names, "K" + pieces.charAt(i) + "vK" + pieces.charAt(j));
        addPair(names, "K" + pieces.charAt(i) + pieces.charAt(j) + "vK");
      }
    }
    return names;
  }

  private static void addPair(List<String> names, String material) {
    names.add(material + ".rtbw");
    names.add(material + ".rtbz");
  }

  private static void checkHeader(Path file) throws IOException {
    if (!Files.isRegularFile(file)) {
      throw new IOException("Missing tablebase file: " + file);
    }
    try (FileChannel channel = FileChannel.open(file, StandardOpenOption.READ)) {
      ByteBuffer header = ByteBuffer.allocate(4).order(ByteOrder.LITTLE_ENDIAN);
      if (channel.size() < 16 || channel.read(header) != 4) {
        throw new IOException("Invalid tablebase file: " + file);
      }
      header.flip();
      int expected = file.toString().endsWith(".rtbw") ? 0x5d23e871 : 0xa50c66d7;
      if (header.getInt() != expected) {
        throw new IOException("Invalid tablebase header: " + file);
      }
    }
  }
}
