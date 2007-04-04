package com.ibm.wala.cast.tree;

public interface CAstSymbol {

  public String name();

  public boolean isFinal();

  public boolean isCaseInsensitive();

  public Object defaultInitValue();

}

