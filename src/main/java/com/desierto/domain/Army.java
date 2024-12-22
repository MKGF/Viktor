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

  public List<Piece> pieces;

  public Color color;

  public Army(Color color) {
    this.color = color;
    pieces = new ArrayList<Piece>(16);
    if(color.equals(Color.WHITE)) {
      pieces.addAll(List.of(new Rook(A1), new Rook(H1)));
      pieces.addAll(List.of(new Knight(B1), new Knight(G1)));
      pieces.addAll(List.of(new Bishop(C1), new Bishop(F1)));
      pieces.add(new Queen(D1));
      pieces.add(new King(E1));
      pieces.addAll(List.of(new Pawn(A2),
          new Pawn(B2),
          new Pawn(C2),
          new Pawn(D2),
          new Pawn(E2),
          new Pawn(F2),
          new Pawn(G2),
          new Pawn(H2)
      ));
    } else {
      pieces.addAll(List.of(new Rook(A8), new Rook(H8)));
      pieces.addAll(List.of(new Knight(B8), new Knight(G8)));
      pieces.addAll(List.of(new Bishop(C8), new Bishop(F8)));
      pieces.add(new Queen(D8));
      pieces.add(new King(E8));
      pieces.addAll(List.of(new Pawn(A7),
          new Pawn(B7),
          new Pawn(C7),
          new Pawn(D7),
          new Pawn(E7),
          new Pawn(F7),
          new Pawn(G7),
          new Pawn(H7)
      ));
    }
  }
}
