package com.desierto.domain;

import static com.desierto.domain.Cell.*;

import com.desierto.domain.pieces.Bishop;
import com.desierto.domain.pieces.King;
import com.desierto.domain.pieces.Knight;
import com.desierto.domain.pieces.Pawn;
import com.desierto.domain.pieces.Queen;
import com.desierto.domain.pieces.Rook;
import java.util.ArrayList;
import java.util.List;

public class Army {

  private final List<Piece> pieces;

  public final Color color;

  public Army(Color color) {
    this.color = color;
    pieces = new ArrayList<Piece>(16);
    if(color.equals(Color.WHITE)) {
      pieces.addAll(List.of(new Rook(Color.WHITE, A1), new Rook(Color.WHITE, H1)));
      pieces.addAll(List.of(new Knight(Color.WHITE, B1), new Knight(Color.WHITE, G1)));
      pieces.addAll(List.of(new Bishop(Color.WHITE, C1), new Bishop(Color.WHITE, F1)));
      pieces.add(new Queen(Color.WHITE, D1));
      pieces.add(new King(Color.WHITE, E1));
      pieces.addAll(List.of(new Pawn(Color.WHITE, A2),
          new Pawn(Color.WHITE, B2),
          new Pawn(Color.WHITE, C2),
          new Pawn(Color.WHITE, D2),
          new Pawn(Color.WHITE, E2),
          new Pawn(Color.WHITE, F2),
          new Pawn(Color.WHITE, G2),
          new Pawn(Color.WHITE, H2)
      ));
    } else {
      pieces.addAll(List.of(new Rook(Color.BLACK, A8), new Rook(Color.BLACK, H8)));
      pieces.addAll(List.of(new Knight(Color.BLACK, B8), new Knight(Color.BLACK, G8)));
      pieces.addAll(List.of(new Bishop(Color.BLACK, C8), new Bishop(Color.BLACK, F8)));
      pieces.add(new Queen(Color.BLACK, D8));
      pieces.add(new King(Color.BLACK, E8));
      pieces.addAll(List.of(new Pawn(Color.BLACK, A7),
          new Pawn(Color.BLACK, B7),
          new Pawn(Color.BLACK, C7),
          new Pawn(Color.BLACK, D7),
          new Pawn(Color.BLACK, E7),
          new Pawn(Color.BLACK, F7),
          new Pawn(Color.BLACK, G7),
          new Pawn(Color.BLACK, H7)
      ));
    }
  }

  public List<Piece> getPieces() {
    return List.copyOf(pieces);
  }
}
