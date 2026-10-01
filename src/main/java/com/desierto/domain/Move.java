package com.desierto.domain;

public class Move {

  public Color color;
  public Piece piece;
  public Cell cell;

  public Move(Color color, Piece piece, Cell cell) {
    this.color = color;
    this.piece = piece;
    this.cell = cell;
  }
}
