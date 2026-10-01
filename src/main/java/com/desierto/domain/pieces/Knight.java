package com.desierto.domain.pieces;

import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.GenericPiece;

public final class Knight extends GenericPiece {

  public Knight(Color color, Cell cell) {
    super(color, cell);
  }

  private Knight(Color color, Cell cell, boolean hasMoved) {
    super(color, cell, hasMoved);
  }

  @Override
  protected Knight recreate(Cell cell, boolean hasMoved) {
    return new Knight(getColor(), cell, hasMoved);
  }
}
