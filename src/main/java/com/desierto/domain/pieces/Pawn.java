package com.desierto.domain.pieces;

import com.desierto.domain.Cell;
import com.desierto.domain.Color;
import com.desierto.domain.GenericPiece;

public final class Pawn extends GenericPiece {

  public Pawn(Color color, Cell cell) {
    super(color, cell);
  }

  private Pawn(Color color, Cell cell, boolean hasMoved) {
    super(color, cell, hasMoved);
  }

  @Override
  protected Pawn recreate(Cell cell, boolean hasMoved) {
    return new Pawn(getColor(), cell, hasMoved);
  }
}
