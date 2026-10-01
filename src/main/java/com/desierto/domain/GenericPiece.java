package com.desierto.domain;

import java.util.Objects;

public abstract class GenericPiece implements Piece {

  private final Color color;
  private final Cell cell;
  private final boolean hasMoved;

  protected GenericPiece(Color color, Cell cell) {
    this(color, cell, false);
  }

  protected GenericPiece(Color color, Cell cell, boolean hasMoved) {
    this.color = Objects.requireNonNull(color, "color must not be null");
    this.cell = Objects.requireNonNull(cell, "cell must not be null");
    this.hasMoved = hasMoved;
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
  public boolean hasMoved() {
    return hasMoved;
  }

  @Override
  public final Piece movedTo(Cell cell) {
    return recreate(Objects.requireNonNull(cell, "cell must not be null"), true);
  }

  @Override
  public final Piece copy() {
    return recreate(cell, hasMoved);
  }

  protected abstract Piece recreate(Cell cell, boolean hasMoved);
}
