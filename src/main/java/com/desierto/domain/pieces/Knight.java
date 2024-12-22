package com.desierto.domain.pieces;

import com.desierto.domain.Cell;
import com.desierto.domain.Piece;
import java.util.List;

public class Knight implements Piece {

  private Cell cell;

  public Knight(Cell cell) {
    this.cell = cell;
  }
}
