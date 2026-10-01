package com.desierto.domain;

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
}
