package com.desierto.domain;

import com.desierto.domain.pieces.Bishop;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Knight;
import com.desierto.domain.pieces.Pawn;
import java.util.Objects;

public final class DrawDetector {

  private DrawDetector() {
  }

  public static boolean isThreefoldRepetitionDraw(Board board, MoveGenerator generator) {
    Objects.requireNonNull(board, "board must not be null");
    Objects.requireNonNull(generator, "generator must not be null");
    return board.isThreefoldRepetition()
        && !generator.isCheckmate(board, board.getSideToMove());
  }

  public static boolean isSeventyFiveMoveDraw(Board board, MoveGenerator generator) {
    Objects.requireNonNull(board, "board must not be null");
    Objects.requireNonNull(generator, "generator must not be null");
    return board.getHalfmoveClock() >= 150
        && !generator.isCheckmate(board, board.getSideToMove());
  }

  public static boolean canClaimFiftyMoves(Board board) {
    return Objects.requireNonNull(board, "board must not be null").getHalfmoveClock() >= 100;
  }

  /**
   * The move must be legal in the current position.
   */
  public static boolean canClaimFiftyMovesAfter(Board board, Move legalMove) {
    Objects.requireNonNull(board, "board must not be null");
    Objects.requireNonNull(legalMove, "legalMove must not be null");
    return board.getHalfmoveClock() >= 99
        && !(legalMove.piece() instanceof Pawn) && !legalMove.isCapture();
  }

  /**
   * Recognizes material configurations that cannot lead to checkmate by any legal sequence.
   * This is not a complete detector for all dead positions.
   */
  public static boolean isDeadByInsufficientMaterial(Board board) {
    Objects.requireNonNull(board, "board must not be null");
    int whiteKings = 0;
    int blackKings = 0;
    int knights = 0;
    int bishops = 0;
    int bishopSquareColor = -1;
    for (Piece piece : board.getPieces()) {
      if (piece instanceof King) {
        if (piece.getColor() == Color.WHITE) {
          whiteKings++;
        } else {
          blackKings++;
        }
      } else if (piece instanceof Knight) {
        knights++;
      } else if (piece instanceof Bishop) {
        bishops++;
        int squareColor = (piece.getCell().row() + piece.getCell().column()) % 2;
        if (bishopSquareColor != -1 && bishopSquareColor != squareColor) {
          return false;
        }
        bishopSquareColor = squareColor;
      } else {
        return false;
      }
    }
    return whiteKings == 1 && blackKings == 1
        && (knights == 0 || (knights == 1 && bishops == 0));
  }
}
