package com.desierto.domain;

public interface Piece {

  Color getColor();

  Cell getCell();

  boolean hasMoved();

  void setCell(Cell cell);

  void setHasMoved(boolean hasMoved);
}
