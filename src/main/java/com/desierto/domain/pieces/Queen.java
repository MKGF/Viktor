package com.desierto.domain.pieces;

import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.GenericPiece;

public final class Queen extends GenericPiece {

  public Queen(Color color, Cell cell) {
    super(color, cell);
  }

  private Queen(Color color, Cell cell, boolean hasMoved) {
    super(color, cell, hasMoved);
  }

  @Override
  protected Queen recreate(Cell cell, boolean hasMoved) {
    return new Queen(getColor(), cell, hasMoved);
  }
}
