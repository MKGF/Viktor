package com.desierto.domain.pieces;

import com.desierto.domain.Cell;
import com.desierto.domain.Piece;
import java.util.List;

public class Rook implements Piece {

  private Cell cell;

  public Rook(Cell cell) {
    this.cell = cell;
  }
}
