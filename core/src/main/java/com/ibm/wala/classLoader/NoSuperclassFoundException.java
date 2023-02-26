package com.ibm.wala.classLoader;

/**
 * Indicates the superclass for a class was not found in the {@link
 * com.ibm.wala.ipa.callgraph.AnalysisScope}
 */
public class NoSuperclassFoundException extends RuntimeException {

  static final long serialVersionUID = 333L;

  public NoSuperclassFoundException(String message) {
    super(message);
  }
}
