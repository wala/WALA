package com.ibm.wala.cast.js.html;

import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;

public interface IncludedPosition extends Position {

  Position getIncludePosition();
  
}
