package com.desierto.domain;

public class Move {

  private Color color;
  private Piece piece;
  private Cell cell;

  public Move(Color color, Piece piece, Cell cell) {
    this.color = color;
    this.piece = piece;
    this.cell = cell;
  }
}
