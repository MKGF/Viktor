package com.desierto.search;

import com.desierto.domain.Board;

public interface SearchEngine {

  SearchResult findBestMove(Board board, SearchLimits limits);
}
