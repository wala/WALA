package com.ibm.wala.cast.tree;

import java.util.Map;

public interface CAstAnnotation {

  CAstType getType();
  
  Map<String,Object> getArguments();
  
}
