package com.desierto.domain.pieces;

import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.GenericPiece;

public final class Bishop extends GenericPiece {

  public Bishop(Color color, Cell cell) {
    super(color, cell);
  }

  private Bishop(Color color, Cell cell, boolean hasMoved) {
    super(color, cell, hasMoved);
  }

  @Override
  protected Bishop recreate(Cell cell, boolean hasMoved) {
    return new Bishop(getColor(), cell, hasMoved);
  }
}
