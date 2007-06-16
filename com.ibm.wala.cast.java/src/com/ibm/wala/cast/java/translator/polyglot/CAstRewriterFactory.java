package com.ibm.wala.cast.java.translator.polyglot;

import com.ibm.wala.cast.tree.*;
import com.ibm.wala.cast.tree.impl.*;

public interface CAstRewriterFactory<Context> {
  public CAstRewriter<Context> createCAstRewriter(CAst ast);
}
