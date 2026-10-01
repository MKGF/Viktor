package com.desierto.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Board {

  private final Piece[][] squares;

  public Board() {
    this.squares = new Piece[8][8];
  }

  public static Board standard() {
    Board board = new Board();
    board.placeArmy(new Army(Color.WHITE));
    board.placeArmy(new Army(Color.BLACK));
    return board;
  }

  public Optional<Piece> getPieceAt(Cell cell) {
    validateCell(cell);
    return Optional.ofNullable(squares[cell.row()][cell.column()]);
  }

  public boolean isOccupied(Cell cell) {
    return getPieceAt(cell).isPresent();
  }

  public void placePiece(Piece piece, Cell cell) {
    Objects.requireNonNull(piece, "piece must not be null");
    validateCell(cell);
    if (squares[cell.row()][cell.column()] != null) {
      throw new IllegalStateException("Square " + cell.toAlgebraic() + " is already occupied");
    }
    squares[cell.row()][cell.column()] = piece;
    piece.setCell(cell);
  }

  public Optional<Piece> removePiece(Cell cell) {
    validateCell(cell);
    Piece piece = squares[cell.row()][cell.column()];
    squares[cell.row()][cell.column()] = null;
    return Optional.ofNullable(piece);
  }

  public Optional<Piece> movePiece(Cell from, Cell to) {
    validateCell(from);
    validateCell(to);

    Piece piece = squares[from.row()][from.column()];
    if (piece == null) {
      throw new IllegalStateException("No piece on square " + from.toAlgebraic());
    }

    Piece captured = squares[to.row()][to.column()];
    squares[from.row()][from.column()] = null;
    squares[to.row()][to.column()] = piece;
    piece.setCell(to);
    return Optional.ofNullable(captured);
  }

  public List<Piece> getPieces() {
    List<Piece> pieces = new ArrayList<>();
    for (int row = 0; row < 8; row++) {
      for (int column = 0; column < 8; column++) {
        Piece piece = squares[row][column];
        if (piece != null) {
          pieces.add(piece);
        }
      }
    }
    return List.copyOf(pieces);
  }

  public List<Piece> getPieces(Color color) {
    List<Piece> pieces = new ArrayList<>();
    for (Piece piece : getPieces()) {
      if (piece.getColor() == color) {
        pieces.add(piece);
      }
    }
    return List.copyOf(pieces);
  }

  private void placeArmy(Army army) {
    for (Piece piece : army.getPieces()) {
      placePiece(piece, piece.getCell());
    }
  }

  private void validateCell(Cell cell) {
    Objects.requireNonNull(cell, "cell must not be null");
    if (cell.row() < 0 || cell.row() > 7 || cell.column() < 0 || cell.column() > 7) {
      throw new IllegalArgumentException("Cell is outside the board");
    }
  }

  @Override
  public String toString() {
    StringBuilder builder = new StringBuilder();
    for (int row = 7; row >= 0; row--) {
      builder.append(row + 1).append(' ');
      for (int column = 0; column < 8; column++) {
        Piece piece = squares[row][column];
        builder.append(piece == null ? "." : piece.getClass().getSimpleName().charAt(0));
        if (column < 7) {
          builder.append(' ');
        }
      }
      builder.append(System.lineSeparator());
    }
    builder.append("  A B C D E F G H");
    return builder.toString();
  }
}
