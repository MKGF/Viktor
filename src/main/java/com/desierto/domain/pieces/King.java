package com.desierto.domain.pieces;

import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.GenericPiece;

public final class King extends GenericPiece {

  public King(Color color, Cell cell) {
    super(color, cell);
  }

  private King(Color color, Cell cell, boolean hasMoved) {
    super(color, cell, hasMoved);
  }

  @Override
  protected King recreate(Cell cell, boolean hasMoved) {
    return new King(getColor(), cell, hasMoved);
  }
}
