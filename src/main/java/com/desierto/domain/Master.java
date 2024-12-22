package com.desierto.domain;

import com.desierto.domain.pieces.Bishop;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Knight;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class Master {

  private Army remainingWhitePieces;

  private Army remainingBlackPieces;

  private Move previousMove;

  public Master() {

  }

  private void initPieces() {
    remainingWhitePieces = new Army(Color.WHITE);
    remainingBlackPieces = new Army(Color.BLACK);
  }

  public List<Move> getPossibleMoves(Piece piece, Color color) {
    List<Move> moves;
    switch (piece) {
      case Pawn pawn -> moves = getPossiblePawnMoves(pawn, color);
      case Bishop bishop -> moves = getPossibleBishopMoves(bishop, color);
      case Knight knight -> moves = getPossibleKnightMoves(knight, color);
      case Rook rook -> moves = getPossibleRookMoves(rook, color);
      case Queen queen -> moves = getPossibleQueenMoves(queen, color);
      case King king -> moves = getPossibleKingMoves(king, color);
      default -> throw new IllegalArgumentException("Unknown piece type");
    }
    return moves;
  }

  private List<Move> getPossiblePawnMoves(Pawn pawn, Color color) {
    return List.of();
  }

  private List<Move> getPossibleBishopMoves(Bishop bishop, Color color) {
    return List.of();
  }

  private List<Move> getPossibleKnightMoves(Knight knight, Color color) {
    return List.of();
  }

  private List<Move> getPossibleRookMoves(Rook rook, Color color) {
    return List.of();
  }

  private List<Move> getPossibleQueenMoves(Queen queen, Color color) {
    return List.of();
  }

  private List<Move> getPossibleKingMoves(King king, Color color) {
    return List.of();
  }
}
