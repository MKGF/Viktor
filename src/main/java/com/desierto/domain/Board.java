package com.desierto.domain;

import com.desierto.domain.pieces.Bishop;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Knight;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public class Board {

  private final Piece[][] squares;
  private final List<Board> history;
  private Move lastMove;
  private Color sideToMove;
  private boolean whiteKingsideCastleRight;
  private boolean whiteQueensideCastleRight;
  private boolean blackKingsideCastleRight;
  private boolean blackQueensideCastleRight;
  private Cell enPassantTarget;
  private int halfmoveClock;
  private int fullmoveNumber;

  public Board() {
    this.squares = new Piece[8][8];
    this.history = new ArrayList<>();
    this.sideToMove = Color.WHITE;
    this.whiteKingsideCastleRight = true;
    this.whiteQueensideCastleRight = true;
    this.blackKingsideCastleRight = true;
    this.blackQueensideCastleRight = true;
    this.enPassantTarget = null;
    this.halfmoveClock = 0;
    this.fullmoveNumber = 1;
  }

  public static Board standard() {
    Board board = new Board();
    board.placeArmy(new Army(Color.WHITE));
    board.placeArmy(new Army(Color.BLACK));
    return board;
  }

  public Board copy() {
    Board copy = new Board();
    copy.sideToMove = sideToMove;
    copy.whiteKingsideCastleRight = whiteKingsideCastleRight;
    copy.whiteQueensideCastleRight = whiteQueensideCastleRight;
    copy.blackKingsideCastleRight = blackKingsideCastleRight;
    copy.blackQueensideCastleRight = blackQueensideCastleRight;
    copy.enPassantTarget = enPassantTarget;
    copy.halfmoveClock = halfmoveClock;
    copy.fullmoveNumber = fullmoveNumber;
    for (int row = 0; row < 8; row++) {
      for (int column = 0; column < 8; column++) {
        Piece piece = squares[row][column];
        if (piece != null) {
          Piece cloned = clonePiece(piece);
          copy.squares[row][column] = cloned;
        }
      }
    }
    copy.lastMove = lastMove == null ? null : copyMove(lastMove, copy);
    return copy;
  }

  public Optional<Piece> getPieceAt(Cell cell) {
    validateCell(cell);
    return Optional.ofNullable(squares[cell.row()][cell.column()]);
  }

  public boolean isOccupied(Cell cell) {
    return getPieceAt(cell).isPresent();
  }

  void placePiece(Piece piece, Cell cell) {
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

  public void makeMove(Move move) {
    Objects.requireNonNull(move, "move must not be null");
    history.add(copy());
    try {
      applyMoveInternal(move);
    } catch (RuntimeException ex) {
      history.remove(history.size() - 1);
      throw ex;
    }
  }

  public void undoMove() {
    if (history.isEmpty()) {
      throw new IllegalStateException("No move to undo");
    }
    Board snapshot = history.remove(history.size() - 1);
    restoreFrom(snapshot);
  }

  private Optional<Piece> movePieceInternal(Cell from, Cell to) {
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
    piece.setHasMoved(true);
    lastMove = new Move(piece.getColor(), piece, from, to, null,
        captured == null ? MoveType.NORMAL : MoveType.CAPTURE, captured);
    updateStateAfterMove(lastMove);
    return Optional.ofNullable(captured);
  }

  public Optional<Move> getLastMove() {
    return Optional.ofNullable(lastMove);
  }

  public Color getSideToMove() {
    return sideToMove;
  }

  public boolean canWhiteCastleKingside() {
    return whiteKingsideCastleRight;
  }

  public boolean canWhiteCastleQueenside() {
    return whiteQueensideCastleRight;
  }

  public boolean canBlackCastleKingside() {
    return blackKingsideCastleRight;
  }

  public boolean canBlackCastleQueenside() {
    return blackQueensideCastleRight;
  }

  public Optional<Cell> getEnPassantTarget() {
    return Optional.ofNullable(enPassantTarget);
  }

  public int getHalfmoveClock() {
    return halfmoveClock;
  }

  public int getFullmoveNumber() {
    return fullmoveNumber;
  }

  private void applyMoveInternal(Move move) {
    validateCell(move.from());
    validateCell(move.to());

    Piece piece = squares[move.from().row()][move.from().column()];
    if (piece == null) {
      throw new IllegalStateException("No piece on square " + move.from().toAlgebraic());
    }
    if (piece.getColor() != move.color()) {
      throw new IllegalArgumentException("Move color must match piece on board");
    }

    switch (move.type()) {
      case NORMAL, CAPTURE -> movePieceInternal(move.from(), move.to());
      case PROMOTION, CAPTURE_PROMOTION -> applyPromotionMove(move, piece);
      case EN_PASSANT -> applyEnPassantMove(move, piece);
      case CASTLE_KINGSIDE -> applyCastleMove(move, piece, true);
      case CASTLE_QUEENSIDE -> applyCastleMove(move, piece, false);
      default -> throw new IllegalStateException("Unexpected move type: " + move.type());
    }
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

  private void restoreFrom(Board snapshot) {
    for (int row = 0; row < 8; row++) {
      for (int column = 0; column < 8; column++) {
        squares[row][column] = null;
      }
    }
    for (int row = 0; row < 8; row++) {
      for (int column = 0; column < 8; column++) {
        Piece piece = snapshot.squares[row][column];
        if (piece != null) {
          Piece cloned = clonePiece(piece);
          squares[row][column] = cloned;
        }
      }
    }
    lastMove = snapshot.lastMove;
    sideToMove = snapshot.sideToMove;
    whiteKingsideCastleRight = snapshot.whiteKingsideCastleRight;
    whiteQueensideCastleRight = snapshot.whiteQueensideCastleRight;
    blackKingsideCastleRight = snapshot.blackKingsideCastleRight;
    blackQueensideCastleRight = snapshot.blackQueensideCastleRight;
    enPassantTarget = snapshot.enPassantTarget;
    halfmoveClock = snapshot.halfmoveClock;
    fullmoveNumber = snapshot.fullmoveNumber;
  }

  private void applyPromotionMove(Move move, Piece pawn) {
    squares[move.from().row()][move.from().column()] = null;
    squares[move.to().row()][move.to().column()] = clonePromotionPiece(move);
    squares[move.to().row()][move.to().column()].setHasMoved(true);
    lastMove = move;
    updateStateAfterMove(move);
  }

  private void applyEnPassantMove(Move move, Piece pawn) {
    int capturedRow = move.from().row();
    int capturedColumn = move.to().column();
    squares[capturedRow][capturedColumn] = null;
    squares[move.from().row()][move.from().column()] = null;
    squares[move.to().row()][move.to().column()] = pawn;
    pawn.setCell(move.to());
    pawn.setHasMoved(true);
    lastMove = move;
    updateStateAfterMove(move);
  }

  private void applyCastleMove(Move move, Piece king, boolean kingside) {
    int rookFromColumn = kingside ? 7 : 0;
    int rookToColumn = kingside ? 5 : 3;
    Piece rook = squares[move.from().row()][rookFromColumn];
    if (rook == null) {
      throw new IllegalStateException("No rook available for castling");
    }
    squares[move.from().row()][move.from().column()] = null;
    squares[move.to().row()][move.to().column()] = king;
    king.setCell(move.to());
    king.setHasMoved(true);
    squares[move.from().row()][rookFromColumn] = null;
    squares[move.from().row()][rookToColumn] = rook;
    rook.setCell(Cell.of(move.from().row(), rookToColumn));
    rook.setHasMoved(true);
    lastMove = move;
    updateStateAfterMove(move);
  }

  private void updateStateAfterMove(Move move) {
    updateCastlingRights(move);
    updateEnPassantTarget(move);
    updateClocks(move);
    sideToMove = sideToMove == Color.WHITE ? Color.BLACK : Color.WHITE;
  }

  private void updateCastlingRights(Move move) {
    if (move.piece() instanceof King) {
      if (move.color() == Color.WHITE) {
        whiteKingsideCastleRight = false;
        whiteQueensideCastleRight = false;
      } else {
        blackKingsideCastleRight = false;
        blackQueensideCastleRight = false;
      }
    }
    if (move.piece() instanceof Rook) {
      if (move.from().equals(Cell.A1)) {
        whiteQueensideCastleRight = false;
      } else if (move.from().equals(Cell.H1)) {
        whiteKingsideCastleRight = false;
      } else if (move.from().equals(Cell.A8)) {
        blackQueensideCastleRight = false;
      } else if (move.from().equals(Cell.H8)) {
        blackKingsideCastleRight = false;
      }
    }
    if (move.capturedPiece() instanceof Rook) {
      if (move.to().equals(Cell.A1)) {
        whiteQueensideCastleRight = false;
      } else if (move.to().equals(Cell.H1)) {
        whiteKingsideCastleRight = false;
      } else if (move.to().equals(Cell.A8)) {
        blackQueensideCastleRight = false;
      } else if (move.to().equals(Cell.H8)) {
        blackKingsideCastleRight = false;
      }
    }
  }

  private void updateEnPassantTarget(Move move) {
    enPassantTarget = null;
    if (move.piece() instanceof Pawn && Math.abs(move.from().row() - move.to().row()) == 2) {
      int row = (move.from().row() + move.to().row()) / 2;
      enPassantTarget = Cell.of(row, move.from().column());
    }
  }

  private void updateClocks(Move move) {
    if (move.piece() instanceof Pawn || move.isCapture()) {
      halfmoveClock = 0;
    } else {
      halfmoveClock++;
    }
    if (move.color() == Color.BLACK) {
      fullmoveNumber++;
    }
  }

  private Piece clonePiece(Piece piece) {
    Piece cloned;
    if (piece instanceof Pawn pawn) {
      cloned = new Pawn(pawn.getColor(), pawn.getCell());
    } else if (piece instanceof Knight knight) {
      cloned = new Knight(knight.getColor(), knight.getCell());
    } else if (piece instanceof Bishop bishop) {
      cloned = new Bishop(bishop.getColor(), bishop.getCell());
    } else if (piece instanceof Rook rook) {
      cloned = new Rook(rook.getColor(), rook.getCell());
    } else if (piece instanceof Queen queen) {
      cloned = new Queen(queen.getColor(), queen.getCell());
    } else if (piece instanceof King king) {
      cloned = new King(king.getColor(), king.getCell());
    } else {
      throw new IllegalArgumentException("Unknown piece type");
    }
    cloned.setHasMoved(piece.hasMoved());
    return cloned;
  }

  private Move copyMove(Move move, Board targetBoard) {
    Piece copiedPiece = targetBoard.getPieceAt(move.to()).orElse(null);
    Piece copiedCaptured = move.capturedPiece() == null ? null : clonePiece(move.capturedPiece());
    Piece copiedPromotion = move.promotionPiece() == null ? null : clonePromotionPiece(move);
    return new Move(move.color(), copiedPiece == null ? clonePiece(move.piece()) : copiedPiece,
        move.from(), move.to(), copiedPromotion, move.type(), copiedCaptured);
  }

  private Piece clonePromotionPiece(Move move) {
    Piece promotionPiece = move.promotionPiece();
    if (promotionPiece == null) {
      return null;
    }
    if (promotionPiece instanceof Queen) {
      return new Queen(promotionPiece.getColor(), move.to());
    }
    if (promotionPiece instanceof Rook) {
      return new Rook(promotionPiece.getColor(), move.to());
    }
    if (promotionPiece instanceof Bishop) {
      return new Bishop(promotionPiece.getColor(), move.to());
    }
    if (promotionPiece instanceof Knight) {
      return new Knight(promotionPiece.getColor(), move.to());
    }
    throw new IllegalArgumentException("Unknown promotion piece type");
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
