package com.desierto.benchmark;

import com.desierto.domain.Board;
import com.desierto.domain.BoardFactory;
import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.Move;
import com.desierto.domain.MoveGenerator;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;
import com.desierto.search.AlphaBetaSearch;
import com.desierto.search.IterativeDeepeningSearch;
import com.desierto.search.SearchLimits;
import com.desierto.search.SearchResult;
import com.desierto.search.SearchTimeoutException;
import java.io.PrintStream;
import java.lang.management.ManagementFactory;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.function.Supplier;

public final class SearchBenchmark {

  private SearchBenchmark() {
  }

  public static void main(String[] args) {
    run(Config.fromArgs(args), System.out);
  }

  static void run(Config config, PrintStream out) {
    out.printf("timestamp_utc=%s java=%s java_vendor=%s vm=%s os=%s os_version=%s arch=%s cpus=%d%n",
        Instant.now(), System.getProperty("java.version"), System.getProperty("java.vendor"),
        System.getProperty("java.vm.name"), System.getProperty("os.name"),
        System.getProperty("os.version"), System.getProperty("os.arch"),
        Runtime.getRuntime().availableProcessors());
    String processor = System.getenv("PROCESSOR_IDENTIFIER");
    if (processor != null && !processor.isBlank()) {
      out.printf("processor=%s%n", processor);
    }
    out.printf("jvm_args=%s%n", ManagementFactory.getRuntimeMXBean().getInputArguments());
    out.printf("warmups=%d samples=%d fixed_depth=%d iterative_max_depth=%d time_limit_ms=%d%n",
        config.warmups(), config.samples(), config.fixedDepth(), config.iterativeDepth(),
        config.timeLimitMillis());

    for (Position position : positions()) {
      measureGeneration(position, config, out);
      for (boolean quiescence : new boolean[] {false, true}) {
        for (boolean transpositions : new boolean[] {false, true}) {
          AlphaBetaSearch fixed = new AlphaBetaSearch(quiescence, transpositions);
          measureSearch(position, config, out, "fixed", quiescence, transpositions,
              board -> fixed.findBestMove(board, new SearchLimits(config.fixedDepth())));
          IterativeDeepeningSearch iterative = new IterativeDeepeningSearch(quiescence, transpositions);
          measureSearch(position, config, out, "timed", quiescence, transpositions,
              board -> iterative.findBestMove(board, new SearchLimits(config.iterativeDepth()),
                  Duration.ofMillis(config.timeLimitMillis())));
        }
      }
    }
  }

  static List<Position> positions() {
    return List.of(
        new Position("initial", Board::standard),
        new Position("tactical", () -> BoardFactory.position()
            .castlingRights(false, false, false, false)
            .place(new King(Color.WHITE, Cell.E1), Cell.E1)
            .place(new Queen(Color.WHITE, Cell.D1), Cell.D1)
            .place(new Rook(Color.BLACK, Cell.D5), Cell.D5)
            .place(new Pawn(Color.BLACK, Cell.E6), Cell.E6)
            .place(new King(Color.BLACK, Cell.E8), Cell.E8)
            .build()),
        new Position("endgame", () -> BoardFactory.position()
            .castlingRights(false, false, false, false)
            .place(new King(Color.WHITE, Cell.E4), Cell.E4)
            .place(new Pawn(Color.WHITE, Cell.A5), Cell.A5)
            .place(new King(Color.BLACK, Cell.E6), Cell.E6)
            .place(new Pawn(Color.BLACK, Cell.H4), Cell.H4)
            .build()));
  }

  private static void measureGeneration(Position position, Config config, PrintStream out) {
    Board board = position.board().get();
    MoveGenerator generator = new MoveGenerator();
    for (int i = 0; i < config.warmups(); i++) {
      generator.generateLegalMoveApplications(board, board.getSideToMove());
    }
    double[] elapsed = new double[config.samples()];
    for (int i = 0; i < config.samples(); i++) {
      long start = System.nanoTime();
      int count = generator.generateLegalMoveApplications(board, board.getSideToMove()).size();
      long nanos = System.nanoTime() - start;
      elapsed[i] = nanos / 1_000_000.0;
      out.printf(Locale.ROOT, "position=%s mode=generation sample=%d elapsed_ms=%.3f legal_moves=%d%n",
          position.name(), i + 1, elapsed[i], count);
    }
    out.printf(Locale.ROOT, "position=%s mode=generation median_ms=%.3f%n",
        position.name(), median(elapsed));
  }

  static void measureSearch(Position position, Config config, PrintStream out, String mode,
      boolean quiescence, boolean transpositions, SearchOperation operation) {
    for (int i = 0; i < config.warmups(); i++) {
      execute(position.board().get(), operation);
    }
    List<Double> elapsed = new ArrayList<>();
    List<Double> nodesPerSecond = new ArrayList<>();
    int timeouts = 0;
    for (int i = 0; i < config.samples(); i++) {
      Measurement measurement = execute(position.board().get(), operation);
      if (measurement.result().isEmpty()) {
        timeouts++;
        out.printf(Locale.ROOT, "position=%s mode=%s quiescence=%s transpositions=%s sample=%d"
                + " status=timeout elapsed_ms=%.3f%n", position.name(), mode, quiescence,
            transpositions, i + 1,
            measurement.nanos() / 1_000_000.0);
        continue;
      }
      SearchResult result = measurement.result().orElseThrow();
      double millis = measurement.nanos() / 1_000_000.0;
      double nps = result.nodes() * 1_000_000_000.0 / Math.max(1, measurement.nanos());
      elapsed.add(millis);
      nodesPerSecond.add(nps);
      out.printf(Locale.ROOT, "position=%s mode=%s quiescence=%s transpositions=%s sample=%d status=result"
              + " elapsed_ms=%.3f depth=%d nodes=%d nps=%.0f score_cp=%d move=%s%n",
          position.name(), mode, quiescence, transpositions, i + 1, millis, result.depth(), result.nodes(), nps,
          result.score(), result.bestMove().map(SearchBenchmark::formatMove).orElse("none"));
    }
    if (elapsed.isEmpty()) {
      out.printf("position=%s mode=%s quiescence=%s transpositions=%s results=0 timeouts=%d%n",
          position.name(), mode, quiescence, transpositions, timeouts);
    } else {
      out.printf(Locale.ROOT, "position=%s mode=%s quiescence=%s transpositions=%s results=%d timeouts=%d"
              + " median_ms=%.3f median_nps=%.0f%n",
          position.name(), mode, quiescence, transpositions, elapsed.size(), timeouts,
          median(elapsed.stream().mapToDouble(Double::doubleValue).toArray()),
          median(nodesPerSecond.stream().mapToDouble(Double::doubleValue).toArray()));
    }
  }

  private static Measurement execute(Board board, SearchOperation operation) {
    long start = System.nanoTime();
    try {
      SearchResult result = operation.search(board);
      return new Measurement(Optional.of(result), System.nanoTime() - start);
    } catch (SearchTimeoutException ex) {
      return new Measurement(Optional.empty(), System.nanoTime() - start);
    }
  }

  private static String formatMove(Move move) {
    String promotion = move.promotionPiece() == null
        ? "" : "=" + move.promotionPiece().getClass().getSimpleName();
    return move.from().toAlgebraic() + move.to().toAlgebraic() + promotion;
  }

  static double median(double[] values) {
    if (values.length == 0) {
      throw new IllegalArgumentException("values must not be empty");
    }
    double[] sorted = values.clone();
    Arrays.sort(sorted);
    int middle = sorted.length / 2;
    return sorted.length % 2 == 0
        ? sorted[middle - 1] / 2 + sorted[middle] / 2 : sorted[middle];
  }

  record Position(String name, Supplier<Board> board) {
  }

  record Config(int warmups, int samples, int fixedDepth, int iterativeDepth, long timeLimitMillis) {

    Config {
      if (warmups < 0 || samples < 1 || fixedDepth < 1 || iterativeDepth < 1
          || timeLimitMillis < 1 || timeLimitMillis > Long.MAX_VALUE / 1_000_000) {
        throw new IllegalArgumentException("Invalid benchmark configuration");
      }
    }

    static Config fromArgs(String[] args) {
      if (args.length == 0) {
        return new Config(2, 5, 2, 8, 250);
      }
      if (args.length != 5) {
        throw new IllegalArgumentException(
            "Expected: warmups samples fixedDepth iterativeMaxDepth timeLimitMillis");
      }
      return new Config(Integer.parseInt(args[0]), Integer.parseInt(args[1]),
          Integer.parseInt(args[2]), Integer.parseInt(args[3]), Long.parseLong(args[4]));
    }
  }

  private record Measurement(Optional<SearchResult> result, long nanos) {
  }

  @FunctionalInterface
  interface SearchOperation {
    SearchResult search(Board board);
  }
}
