package com.desierto.domain.pieces;

import com.desierto.domain.Cell;
import com.desierto.domain.Piece;
import java.util.List;

public class Bishop implements Piece {

  private Cell cell;

  public Bishop(Cell cell) {
    this.cell = cell;
  }
}
