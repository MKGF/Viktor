package com.desierto.domain.pieces;

import com.desierto.domain.Cell;
import com.desierto.domain.Piece;
import java.util.List;

public class Pawn implements Piece {

  private Cell cell;

  public Pawn(Cell cell) {
    this.cell = cell;
  }
}
