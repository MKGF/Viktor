package com.desierto.domain;

public class Cell {

  public static final Cell A1 = new Cell(0, 0);
  public static final Cell A2 = new Cell(1, 0);
  public static final Cell A3 = new Cell(2, 0);
  public static final Cell A4 = new Cell(3, 0);
  public static final Cell A5 = new Cell(4, 0);
  public static final Cell A6 = new Cell(5, 0);
  public static final Cell A7 = new Cell(6, 0);
  public static final Cell A8 = new Cell(7, 0);

  public static final Cell B1 = new Cell(0, 1);
  public static final Cell B2 = new Cell(1, 1);
  public static final Cell B3 = new Cell(2, 1);
  public static final Cell B4 = new Cell(3, 1);
  public static final Cell B5 = new Cell(4, 1);
  public static final Cell B6 = new Cell(5, 1);
  public static final Cell B7 = new Cell(6, 1);
  public static final Cell B8 = new Cell(7, 1);

  public static final Cell C1 = new Cell(0, 2);
  public static final Cell C2 = new Cell(1, 2);
  public static final Cell C3 = new Cell(2, 2);
  public static final Cell C4 = new Cell(3, 2);
  public static final Cell C5 = new Cell(4, 2);
  public static final Cell C6 = new Cell(5, 2);
  public static final Cell C7 = new Cell(6, 2);
  public static final Cell C8 = new Cell(7, 2);

  public static final Cell D1 = new Cell(0, 3);
  public static final Cell D2 = new Cell(1, 3);
  public static final Cell D3 = new Cell(2, 3);
  public static final Cell D4 = new Cell(3, 3);
  public static final Cell D5 = new Cell(4, 3);
  public static final Cell D6 = new Cell(5, 3);
  public static final Cell D7 = new Cell(6, 3);
  public static final Cell D8 = new Cell(7, 3);

  public static final Cell E1 = new Cell(0, 4);
  public static final Cell E2 = new Cell(1, 4);
  public static final Cell E3 = new Cell(2, 4);
  public static final Cell E4 = new Cell(3, 4);
  public static final Cell E5 = new Cell(4, 4);
  public static final Cell E6 = new Cell(5, 4);
  public static final Cell E7 = new Cell(6, 4);
  public static final Cell E8 = new Cell(7, 4);

  public static final Cell F1 = new Cell(0, 5);
  public static final Cell F2 = new Cell(1, 5);
  public static final Cell F3 = new Cell(2, 5);
  public static final Cell F4 = new Cell(3, 5);
  public static final Cell F5 = new Cell(4, 5);
  public static final Cell F6 = new Cell(5, 5);
  public static final Cell F7 = new Cell(6, 5);
  public static final Cell F8 = new Cell(7, 5);

  public static final Cell G1 = new Cell(0, 6);
  public static final Cell G2 = new Cell(1, 6);
  public static final Cell G3 = new Cell(2, 6);
  public static final Cell G4 = new Cell(3, 6);
  public static final Cell G5 = new Cell(4, 6);
  public static final Cell G6 = new Cell(5, 6);
  public static final Cell G7 = new Cell(6, 6);
  public static final Cell G8 = new Cell(7, 6);

  public static final Cell H1 = new Cell(0, 7);
  public static final Cell H2 = new Cell(1, 7);
  public static final Cell H3 = new Cell(2, 7);
  public static final Cell H4 = new Cell(3, 7);
  public static final Cell H5 = new Cell(4, 7);
  public static final Cell H6 = new Cell(5, 7);
  public static final Cell H7 = new Cell(6, 7);
  public static final Cell H8 = new Cell(7, 7);

  public Integer row;
  public Integer column;

  public Cell(Integer row, Integer column) {
    this.row = row;
    this.column = column;
  }


}
