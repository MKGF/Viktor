package com.desierto.domain.pieces;

import com.desierto.domain.Cell;
import com.desierto.domain.Piece;
import java.util.List;

public class Queen implements Piece {

  private Cell cell;

  public Queen(Cell cell) {
    this.cell = cell;
  }
}
