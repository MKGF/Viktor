package com.desierto.domain;

import java.util.Objects;

public abstract class GenericPiece implements Piece {

  private final Color color;
  private Cell cell;

  protected GenericPiece(Color color, Cell cell) {
    this.color = Objects.requireNonNull(color, "color must not be null");
    this.cell = Objects.requireNonNull(cell, "cell must not be null");
  }

  @Override
  public Color getColor() {
    return color;
  }

  @Override
  public Cell getCell() {
    return cell;
  }

  @Override
  public void setCell(Cell cell) {
    this.cell = Objects.requireNonNull(cell, "cell must not be null");
  }
}
