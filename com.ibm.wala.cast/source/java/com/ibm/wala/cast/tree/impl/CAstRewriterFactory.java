package com.ibm.wala.cast.tree.impl;

import com.ibm.wala.cast.tree.CAst;

public interface CAstRewriterFactory<Context> {
  public CAstRewriter<Context> createCAstRewriter(CAst ast);
}
