package com.desierto.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MoveGenerator {

  private final AttackDetector attackDetector;
  private final PseudoLegalMoveGenerator pseudoLegalMoveGenerator;

  public MoveGenerator() {
    attackDetector = new AttackDetector();
    pseudoLegalMoveGenerator = new PseudoLegalMoveGenerator(attackDetector);
  }

  public List<Move> generateMoves(Board board, Cell from) {
    return generateLegalMoves(board, from);
  }

  public List<Move> generateLegalMoves(Board board, Cell from) {
    Objects.requireNonNull(board, "board must not be null");
    Objects.requireNonNull(from, "from must not be null");

    Piece piece = board.getPieceAt(from)
        .orElseThrow(() -> new IllegalStateException("No piece on square " + from.toAlgebraic()));
    return filterMovesThatLeaveKingInCheck(board, pseudoLegalMoveGenerator.generate(board, piece),
        piece.getColor());
  }

  public List<Move> generateMoves(Board board, Color color) {
    return generateLegalMoves(board, color);
  }

  public List<Move> generateLegalMoves(Board board, Color color) {
    Objects.requireNonNull(board, "board must not be null");
    Objects.requireNonNull(color, "color must not be null");

    List<Move> moves = new ArrayList<>();
    for (Piece piece : board.getPieces(color)) {
      moves.addAll(filterMovesThatLeaveKingInCheck(board, pseudoLegalMoveGenerator.generate(board, piece),
          color));
    }
    return List.copyOf(moves);
  }

  public boolean isKingInCheck(Board board, Color color) {
    return attackDetector.isKingInCheck(board, color);
  }

  public boolean isCheckmate(Board board, Color color) {
    Objects.requireNonNull(board, "board must not be null");
    Objects.requireNonNull(color, "color must not be null");
    return isKingInCheck(board, color) && generateLegalMoves(board, color).isEmpty();
  }

  public boolean isStalemate(Board board, Color color) {
    Objects.requireNonNull(board, "board must not be null");
    Objects.requireNonNull(color, "color must not be null");
    return !isKingInCheck(board, color) && generateLegalMoves(board, color).isEmpty();
  }

  private List<Move> filterMovesThatLeaveKingInCheck(Board board, List<Move> candidateMoves,
      Color moverColor) {
    List<Move> legalMoves = new ArrayList<>();
    for (Move move : candidateMoves) {
      Board resultingBoard = board.copy();
      resultingBoard.makeMove(move);
      if (!attackDetector.isKingInCheck(resultingBoard, moverColor)) {
        legalMoves.add(move);
      }
    }
    return List.copyOf(legalMoves);
  }
}
