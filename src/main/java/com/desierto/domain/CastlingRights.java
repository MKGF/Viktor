package com.desierto.domain;

record CastlingRights(boolean whiteKingside, boolean whiteQueenside,
                      boolean blackKingside, boolean blackQueenside) {

  static final CastlingRights INITIAL = new CastlingRights(true, true, true, true);

  CastlingRights afterKingMoves(Color color) {
    if (color == Color.WHITE) {
      return whiteKingside || whiteQueenside
          ? new CastlingRights(false, false, blackKingside, blackQueenside) : this;
    }
    return blackKingside || blackQueenside
        ? new CastlingRights(whiteKingside, whiteQueenside, false, false) : this;
  }

  CastlingRights afterRookMoves(Cell square) {
    return withoutRightsForRookAt(square);
  }

  CastlingRights afterRookIsCaptured(Cell square) {
    return withoutRightsForRookAt(square);
  }

  private CastlingRights withoutRightsForRookAt(Cell square) {
    if (square.equals(Cell.A1) && whiteQueenside) {
      return new CastlingRights(whiteKingside, false, blackKingside, blackQueenside);
    }
    if (square.equals(Cell.H1) && whiteKingside) {
      return new CastlingRights(false, whiteQueenside, blackKingside, blackQueenside);
    }
    if (square.equals(Cell.A8) && blackQueenside) {
      return new CastlingRights(whiteKingside, whiteQueenside, blackKingside, false);
    }
    if (square.equals(Cell.H8) && blackKingside) {
      return new CastlingRights(whiteKingside, whiteQueenside, false, blackQueenside);
    }
    return this;
  }
}
