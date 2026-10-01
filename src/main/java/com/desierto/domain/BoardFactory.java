package com.desierto.domain;

import java.util.ArrayList;
import java.util.List;

public final class BoardFactory {

  private BoardFactory() {
  }

  public record Placement(Piece piece, Cell cell) {
  }

  public static Placement placement(Piece piece, Cell cell) {
    return new Placement(piece, cell);
  }

  public static Board empty() {
    return new Board();
  }

  public static Board standard() {
    return Board.standard();
  }

  public static Board withPieces(Placement... placements) {
    Board board = new Board();
    for (Placement placement : placements) {
      board.placePiece(placement.piece(), placement.cell());
    }
    return board;
  }

  public static PositionBuilder position() {
    return new PositionBuilder();
  }

  public static final class PositionBuilder {

    private final List<Placement> placements = new ArrayList<>();
    private Color sideToMove = Color.WHITE;
    private boolean whiteKingsideCastleRight = true;
    private boolean whiteQueensideCastleRight = true;
    private boolean blackKingsideCastleRight = true;
    private boolean blackQueensideCastleRight = true;
    private Cell enPassantTarget;
    private int halfmoveClock;
    private int fullmoveNumber = 1;

    public PositionBuilder place(Piece piece, Cell cell) {
      placements.add(placement(piece, cell));
      return this;
    }

    public PositionBuilder sideToMove(Color sideToMove) {
      this.sideToMove = sideToMove;
      return this;
    }

    public PositionBuilder castlingRights(boolean whiteKingside, boolean whiteQueenside,
        boolean blackKingside, boolean blackQueenside) {
      whiteKingsideCastleRight = whiteKingside;
      whiteQueensideCastleRight = whiteQueenside;
      blackKingsideCastleRight = blackKingside;
      blackQueensideCastleRight = blackQueenside;
      return this;
    }

    public PositionBuilder enPassantTarget(Cell enPassantTarget) {
      this.enPassantTarget = enPassantTarget;
      return this;
    }

    public PositionBuilder halfmoveClock(int halfmoveClock) {
      this.halfmoveClock = halfmoveClock;
      return this;
    }

    public PositionBuilder fullmoveNumber(int fullmoveNumber) {
      this.fullmoveNumber = fullmoveNumber;
      return this;
    }

    public Board build() {
      Board board = new Board();
      board.configurePosition(sideToMove, whiteKingsideCastleRight, whiteQueensideCastleRight,
          blackKingsideCastleRight, blackQueensideCastleRight, enPassantTarget, halfmoveClock,
          fullmoveNumber);
      for (Placement placement : placements) {
        board.placePiece(placement.piece(), placement.cell());
      }
      return board;
    }
  }
}
