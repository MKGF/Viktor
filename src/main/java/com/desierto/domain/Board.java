package com.desierto.domain;

import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Rook;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Mutable position; not thread-safe. Legal move generation and check detection may temporarily
 * modify a board before restoring it, so callers must synchronize concurrent access.
 */
public class Board {

  private final Piece[][] squares;
  private final PositionHistory history;
  private final RepetitionHistory repetitionHistory;
  private Move lastMove;
  private Color sideToMove;
  private CastlingRights castlingRights;
  private Cell enPassantTarget;
  private int halfmoveClock;
  private int fullmoveNumber;
  private long positionVersion;
  private long positionSequence;

  public Board() {
    this.squares = new Piece[8][8];
    this.history = new PositionHistory();
    this.repetitionHistory = new RepetitionHistory();
    this.sideToMove = Color.WHITE;
    this.castlingRights = CastlingRights.INITIAL;
    this.enPassantTarget = null;
    this.halfmoveClock = 0;
    this.fullmoveNumber = 1;
    this.positionVersion = 0;
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
    copy.castlingRights = castlingRights;
    copy.enPassantTarget = enPassantTarget;
    copy.halfmoveClock = halfmoveClock;
    copy.fullmoveNumber = fullmoveNumber;
    copy.positionVersion = positionVersion;
    copy.positionSequence = positionSequence;
    copy.repetitionHistory.replaceWith(repetitionHistory);
    for (int row = 0; row < 8; row++) {
      for (int column = 0; column < 8; column++) {
        Piece piece = squares[row][column];
        if (piece != null) {
          copy.squares[row][column] = piece.copy();
        }
      }
    }
    copy.lastMove = lastMove == null ? null : copyMove(lastMove, copy);
    return copy;
  }

  Board copyAfterGeneratedMove(Move move) {
    Objects.requireNonNull(move, "move must not be null");
    Board copy = copy();
    RepetitionPosition previous = RepetitionPosition.of(copy);
    copy.applyGeneratedMove(move);
    copy.repetitionHistory.add(previous, isIrreversible(move));
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
    if (!piece.getCell().equals(cell)) {
      throw new IllegalArgumentException("Piece position must match the target square");
    }
    squares[cell.row()][cell.column()] = piece;
    repetitionHistory.clear();
    advancePositionVersion();
  }

  void configurePosition(Color sideToMove, CastlingRights castlingRights, Cell enPassantTarget,
      int halfmoveClock, int fullmoveNumber) {
    this.sideToMove = Objects.requireNonNull(sideToMove, "sideToMove must not be null");
    this.castlingRights = Objects.requireNonNull(castlingRights, "castlingRights must not be null");
    this.enPassantTarget = enPassantTarget;
    if (halfmoveClock < 0) {
      throw new IllegalArgumentException("halfmoveClock must not be negative");
    }
    if (fullmoveNumber < 1) {
      throw new IllegalArgumentException("fullmoveNumber must be at least 1");
    }
    this.halfmoveClock = halfmoveClock;
    this.fullmoveNumber = fullmoveNumber;
    repetitionHistory.clear();
    advancePositionVersion();
  }

  public Optional<Piece> removePiece(Cell cell) {
    validateCell(cell);
    Piece piece = squares[cell.row()][cell.column()];
    squares[cell.row()][cell.column()] = null;
    repetitionHistory.clear();
    advancePositionVersion();
    return Optional.ofNullable(piece);
  }

  /** Validates a raw move for compatibility; new callers can play a generated LegalMove. */
  public void makeMove(Move move) {
    new LegalMove(this, move).play();
  }

  void applyLegalMove(LegalMove legalMove) {
    legalMove.validateSource(this);
    Move move = legalMove.move();
    RepetitionPosition previous = RepetitionPosition.of(this);
    Board snapshot = copy();
    try (MoveTransaction transaction = beginMove(move)) {
      addRepetitionPosition(previous, move);
      history.save(snapshot);
      transaction.commit();
    }
  }

  MoveTransaction beginMove(Move move) {
    UndoState state = makeTemporaryMove(move);
    try {
      return new MoveTransaction(state);
    } catch (RuntimeException | Error ex) {
      restoreTemporaryMove(state);
      throw ex;
    }
  }

  void applyGeneratedMove(Move move) {
    Objects.requireNonNull(move, "move must not be null");
    applyMoveInternal(move);
  }

  UndoState makeTemporaryMove(Move move) {
    Objects.requireNonNull(move, "move must not be null");
    Cell extraFrom = null;
    Cell extraTo = null;
    if (move.type() == MoveType.EN_PASSANT) {
      extraFrom = Cell.of(move.from().row(), move.to().column());
    } else if (move.type() == MoveType.CASTLE_KINGSIDE
        || move.type() == MoveType.CASTLE_QUEENSIDE) {
      boolean kingside = move.type() == MoveType.CASTLE_KINGSIDE;
      extraFrom = Cell.of(move.from().row(), kingside ? 7 : 0);
      extraTo = Cell.of(move.from().row(), kingside ? 5 : 3);
    }
    UndoState state = new UndoState(move.from(), move.to(), pieceAt(move.from()), pieceAt(move.to()),
        extraFrom, pieceAt(extraFrom), extraTo, pieceAt(extraTo), lastMove, sideToMove,
        castlingRights, enPassantTarget, halfmoveClock, fullmoveNumber, positionVersion,
        repetitionHistory.checkpoint());
    try {
      applyGeneratedMove(move);
    } catch (RuntimeException | Error ex) {
      restoreTemporaryMove(state);
      throw ex;
    }
    return state;
  }

  void restoreTemporaryMove(UndoState state) {
    setPiece(state.from(), state.fromPiece());
    setPiece(state.to(), state.toPiece());
    setPiece(state.extraFrom(), state.extraFromPiece());
    setPiece(state.extraTo(), state.extraToPiece());
    lastMove = state.lastMove();
    sideToMove = state.sideToMove();
    castlingRights = state.castlingRights();
    enPassantTarget = state.enPassantTarget();
    halfmoveClock = state.halfmoveClock();
    fullmoveNumber = state.fullmoveNumber();
    positionVersion = state.positionVersion();
    repetitionHistory.restore(state.repetitionCheckpoint());
  }

  private Piece pieceAt(Cell cell) {
    return cell == null ? null : squares[cell.row()][cell.column()];
  }

  private void setPiece(Cell cell, Piece piece) {
    if (cell != null) {
      squares[cell.row()][cell.column()] = piece;
    }
  }

  private void advancePositionVersion() {
    positionVersion = ++positionSequence;
  }

  record UndoState(Cell from, Cell to, Piece fromPiece, Piece toPiece, Cell extraFrom,
                   Piece extraFromPiece, Cell extraTo, Piece extraToPiece, Move lastMove,
                   Color sideToMove, CastlingRights castlingRights, Cell enPassantTarget,
                   int halfmoveClock, int fullmoveNumber, long positionVersion,
                   RepetitionHistory.Node repetitionCheckpoint) {
  }

  final class MoveTransaction implements AutoCloseable {
    private final UndoState state;
    private boolean committed;
    private boolean closed;

    private MoveTransaction(UndoState state) {
      this.state = state;
    }

    void commit() {
      if (closed) {
        throw new IllegalStateException("Move transaction is already closed");
      }
      committed = true;
    }

    @Override
    public void close() {
      if (closed) {
        return;
      }
      closed = true;
      if (!committed) {
        restoreTemporaryMove(state);
      }
    }
  }

  boolean isAtVersion(long version) {
    return positionVersion == version;
  }

  long positionVersion() {
    return positionVersion;
  }

  /** Counts the current position together with relevant positions from the game or search path. */
  public boolean isThreefoldRepetition() {
    return repetitionHistory.mayRepeat()
        && repetitionHistory.containsTwice(RepetitionPosition.of(this));
  }

  /** Returns a fingerprint of the previous-position counts for transposition caching. */
  public long repetitionHistoryFingerprint() {
    return repetitionHistory.fingerprint();
  }

  RepetitionPosition repetitionPosition() {
    return RepetitionPosition.of(this);
  }

  void addRepetitionPosition(RepetitionPosition position, Move move) {
    repetitionHistory.add(position, isIrreversible(move));
  }

  private static boolean isIrreversible(Move move) {
    return move.piece() instanceof Pawn || move.isCapture();
  }

  public void undoMove() {
    restoreFrom(history.restore());
  }

  public Optional<Move> getLastMove() {
    return Optional.ofNullable(lastMove);
  }

  public Color getSideToMove() {
    return sideToMove;
  }

  public boolean canWhiteCastleKingside() {
    return castlingRights.whiteKingside();
  }

  public boolean canWhiteCastleQueenside() {
    return castlingRights.whiteQueenside();
  }

  public boolean canBlackCastleKingside() {
    return castlingRights.blackKingside();
  }

  public boolean canBlackCastleQueenside() {
    return castlingRights.blackQueenside();
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
      case NORMAL, CAPTURE -> applyRegularMove(move, piece);
      case PROMOTION, CAPTURE_PROMOTION -> applyPromotionMove(move, piece);
      case EN_PASSANT -> applyEnPassantMove(move, piece);
      case CASTLE_KINGSIDE -> applyCastleMove(move, piece, true);
      case CASTLE_QUEENSIDE -> applyCastleMove(move, piece, false);
      default -> throw new IllegalStateException("Unexpected move type: " + move.type());
    }
    advancePositionVersion();
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
    for (int row = 0; row < 8; row++) {
      for (int column = 0; column < 8; column++) {
        Piece piece = squares[row][column];
        if (piece != null && piece.getColor() == color) {
          pieces.add(piece);
        }
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
          squares[row][column] = piece.copy();
        }
      }
    }
    lastMove = snapshot.lastMove;
    sideToMove = snapshot.sideToMove;
    castlingRights = snapshot.castlingRights;
    enPassantTarget = snapshot.enPassantTarget;
    halfmoveClock = snapshot.halfmoveClock;
    fullmoveNumber = snapshot.fullmoveNumber;
    repetitionHistory.replaceWith(snapshot.repetitionHistory);
    advancePositionVersion();
  }

  private void applyRegularMove(Move move, Piece piece) {
    Piece target = squares[move.to().row()][move.to().column()];
    if (move.type() == MoveType.NORMAL && target != null) {
      throw new IllegalArgumentException("A normal move cannot capture a piece");
    }
    if (move.type() == MoveType.CAPTURE && (target == null || target.getColor() == piece.getColor())) {
      throw new IllegalArgumentException("A capture move must capture an opposing piece");
    }
    squares[move.from().row()][move.from().column()] = null;
    Piece movedPiece = piece.movedTo(move.to());
    squares[move.to().row()][move.to().column()] = movedPiece;
    recordAppliedMove(move, movedPiece, target);
    updateStateAfterMove(piece, move.from(), move.to(), target);
  }

  private void applyPromotionMove(Move move, Piece pawn) {
    if (!(pawn instanceof Pawn)) {
      throw new IllegalArgumentException("Only pawns can promote");
    }
    Piece target = squares[move.to().row()][move.to().column()];
    if (move.type() == MoveType.PROMOTION && target != null) {
      throw new IllegalArgumentException("A promotion move cannot capture a piece");
    }
    if (move.type() == MoveType.CAPTURE_PROMOTION
        && (target == null || target.getColor() == pawn.getColor())) {
      throw new IllegalArgumentException("A capture promotion must capture an opposing piece");
    }
    squares[move.from().row()][move.from().column()] = null;
    Piece promotedPiece = move.promotionPiece().movedTo(move.to());
    squares[move.to().row()][move.to().column()] = promotedPiece;
    recordAppliedMove(move, promotedPiece, target);
    updateStateAfterMove(pawn, move.from(), move.to(), target);
  }

  private void applyEnPassantMove(Move move, Piece pawn) {
    if (!(pawn instanceof Pawn)) {
      throw new IllegalArgumentException("Only pawns can capture en passant");
    }
    if (squares[move.to().row()][move.to().column()] != null) {
      throw new IllegalArgumentException("The en passant target square must be empty");
    }
    int capturedRow = move.from().row();
    int capturedColumn = move.to().column();
    Piece captured = squares[capturedRow][capturedColumn];
    if (!(captured instanceof Pawn) || captured.getColor() == pawn.getColor()) {
      throw new IllegalArgumentException("En passant must capture an opposing pawn");
    }
    squares[capturedRow][capturedColumn] = null;
    squares[move.from().row()][move.from().column()] = null;
    Piece movedPawn = pawn.movedTo(move.to());
    squares[move.to().row()][move.to().column()] = movedPawn;
    recordAppliedMove(move, movedPawn, captured);
    updateStateAfterMove(pawn, move.from(), move.to(), captured);
  }

  private void applyCastleMove(Move move, Piece king, boolean kingside) {
    if (!(king instanceof King)) {
      throw new IllegalArgumentException("Only kings can castle");
    }
    int rookFromColumn = kingside ? 7 : 0;
    int rookToColumn = kingside ? 5 : 3;
    Piece rook = squares[move.from().row()][rookFromColumn];
    if (!(rook instanceof Rook) || rook.getColor() != king.getColor()) {
      throw new IllegalStateException("No rook available for castling");
    }
    squares[move.from().row()][move.from().column()] = null;
    Piece movedKing = king.movedTo(move.to());
    squares[move.to().row()][move.to().column()] = movedKing;
    squares[move.from().row()][rookFromColumn] = null;
    squares[move.from().row()][rookToColumn] = rook.movedTo(Cell.of(move.from().row(), rookToColumn));
    recordAppliedMove(move, movedKing, null);
    updateStateAfterMove(king, move.from(), move.to(), null);
  }

  private void recordAppliedMove(Move requestedMove, Piece movedPiece, Piece capturedPiece) {
    Piece promotionPiece = requestedMove.isPromotion() ? movedPiece : null;
    lastMove = new Move(movedPiece.getColor(), movedPiece, requestedMove.from(), requestedMove.to(),
        promotionPiece, requestedMove.type(), capturedPiece);
  }

  private void updateStateAfterMove(Piece movedPiece, Cell from, Cell to, Piece capturedPiece) {
    updateCastlingRights(movedPiece, from, to, capturedPiece);
    updateEnPassantTarget(movedPiece, from, to);
    updateClocks(movedPiece, capturedPiece);
    sideToMove = sideToMove.opposite();
  }

  private void updateCastlingRights(Piece movedPiece, Cell from, Cell to, Piece capturedPiece) {
    if (movedPiece instanceof King) {
      castlingRights = castlingRights.afterKingMoves(movedPiece.getColor());
    }
    if (movedPiece instanceof Rook) {
      castlingRights = castlingRights.afterRookMoves(from);
    }
    if (capturedPiece instanceof Rook) {
      castlingRights = castlingRights.afterRookIsCaptured(to);
    }
  }

  private void updateEnPassantTarget(Piece movedPiece, Cell from, Cell to) {
    enPassantTarget = null;
    if (movedPiece instanceof Pawn && Math.abs(from.row() - to.row()) == 2) {
      int row = (from.row() + to.row()) / 2;
      enPassantTarget = Cell.of(row, from.column());
    }
  }

  private void updateClocks(Piece movedPiece, Piece capturedPiece) {
    if (movedPiece instanceof Pawn || capturedPiece != null) {
      halfmoveClock = 0;
    } else {
      halfmoveClock++;
    }
    if (movedPiece.getColor() == Color.BLACK) {
      fullmoveNumber++;
    }
  }

  private Move copyMove(Move move, Board targetBoard) {
    Piece copiedPiece = targetBoard.getPieceAt(move.to()).orElse(null);
    Piece copiedCaptured = move.capturedPiece() == null ? null : move.capturedPiece().copy();
    Piece copiedPromotion = move.promotionPiece() == null ? null : move.promotionPiece().copy();
    return new Move(move.color(), copiedPiece == null ? move.piece().copy() : copiedPiece,
        move.from(), move.to(), copiedPromotion, move.type(), copiedCaptured);
  }

  private void validateCell(Cell cell) {
    Objects.requireNonNull(cell, "cell must not be null");
    if (cell.row() < 0 || cell.row() > 7 || cell.column() < 0 || cell.column() > 7) {
      throw new IllegalArgumentException("Cell is outside the board");
    }
  }

  @Override
  public String toString() {
    return BoardRenderer.render(this);
  }
}
