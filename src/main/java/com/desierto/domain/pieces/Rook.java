package com.desierto.domain.pieces;

import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.GenericPiece;

public final class Rook extends GenericPiece {

  public Rook(Color color, Cell cell) {
    super(color, cell);
  }

  private Rook(Color color, Cell cell, boolean hasMoved) {
    super(color, cell, hasMoved);
  }

  @Override
  protected Rook recreate(Cell cell, boolean hasMoved) {
    return new Rook(getColor(), cell, hasMoved);
  }
}
