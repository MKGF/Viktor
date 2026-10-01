package com.desierto.domain;

public interface Piece {

  Color getColor();

  Cell getCell();

  boolean hasMoved();

  Piece movedTo(Cell cell);

  Piece copy();
}
