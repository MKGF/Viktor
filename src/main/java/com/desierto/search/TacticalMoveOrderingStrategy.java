package com.desierto.search;

import com.desierto.domain.Board;
import com.desierto.domain.LegalMove;
import com.desierto.domain.Move;
import com.desierto.domain.MoveGenerator;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class TacticalMoveOrderingStrategy implements MoveOrderingStrategy {

  private final MoveGenerator moveGenerator;

  public TacticalMoveOrderingStrategy() {
    this(new MoveGenerator());
  }

  TacticalMoveOrderingStrategy(MoveGenerator moveGenerator) {
    this.moveGenerator = Objects.requireNonNull(moveGenerator, "moveGenerator must not be null");
  }

  @Override
  public List<LegalMove> order(Board board, List<LegalMove> legalMoves,
      Optional<MoveKey> preferredMove) {
    Objects.requireNonNull(board, "board must not be null");
    Objects.requireNonNull(legalMoves, "legalMoves must not be null");
    Objects.requireNonNull(preferredMove, "preferredMove must not be null");

    List<LegalMove> principalVariationMoves = new ArrayList<>();
    List<LegalMove> promotions = new ArrayList<>();
    List<LegalMove> captures = new ArrayList<>();
    List<LegalMove> checks = new ArrayList<>();
    List<LegalMove> quietMoves = new ArrayList<>();
    for (LegalMove legalMove : legalMoves) {
      Move move = legalMove.move();
      if (preferredMove.filter(key -> key.equals(MoveKey.from(move))).isPresent()) {
        principalVariationMoves.add(legalMove);
      } else if (move.isPromotion()) {
        promotions.add(legalMove);
      } else if (move.isCapture()) {
        captures.add(legalMove);
      } else if (givesCheck(legalMove)) {
        checks.add(legalMove);
      } else {
        quietMoves.add(legalMove);
      }
    }

    List<LegalMove> orderedMoves = new ArrayList<>(legalMoves.size());
    orderedMoves.addAll(principalVariationMoves);
    orderedMoves.addAll(promotions);
    orderedMoves.addAll(captures);
    orderedMoves.addAll(checks);
    orderedMoves.addAll(quietMoves);
    return List.copyOf(orderedMoves);
  }

  private boolean givesCheck(LegalMove legalMove) {
    Board resultingBoard = legalMove.resultingPosition();
    return moveGenerator.isKingInCheck(resultingBoard, resultingBoard.getSideToMove());
  }
}
