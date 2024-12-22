package com.desierto.domain.pieces;

import com.desierto.domain.Cell;
import com.desierto.domain.Piece;
import java.util.List;

public class King implements Piece {

  private Cell cell;

  public King(Cell cell) {
    this.cell = cell;
  }
}
