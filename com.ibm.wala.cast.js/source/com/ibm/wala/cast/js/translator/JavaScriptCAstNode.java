package com.ibm.wala.cast.js.translator;

import com.ibm.wala.cast.tree.CAstNode;

public interface JavaScriptCAstNode extends CAstNode {

  public static final int ENTER_WITH = SUB_LANGUAGE_BASE + 1;

  public static final int EXIT_WITH = SUB_LANGUAGE_BASE + 2;

}
