package com.desierto.search;

import com.desierto.domain.Board;

public interface SearchAlgorithm {

  SearchResult findBestMove(Board board, SearchLimits limits);
}
