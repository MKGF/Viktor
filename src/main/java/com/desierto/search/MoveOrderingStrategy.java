package com.desierto.search;

import com.desierto.domain.Board;
import com.desierto.domain.LegalMove;
import java.util.List;
import java.util.Optional;

public interface MoveOrderingStrategy {

  List<LegalMove> order(Board board, List<LegalMove> legalMoves, Optional<MoveKey> preferredMove);
}
