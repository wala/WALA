package com.ibm.wala.cast.tree.impl;

import com.ibm.wala.cast.tree.*;
import com.ibm.wala.util.debug.*;

import java.util.*;

public abstract class CAstBasicRewriter
  extends CAstRewriter<CAstBasicRewriter.NonCopyingContext, 
	               CAstBasicRewriter.NoKey> 
{

  public static class NonCopyingContext implements CAstRewriter.RewriteContext<NoKey> {
    private final Map nodeMap = new HashMap();

    public Map nodeMap() {
      return nodeMap;
    }

    public NoKey key() {
      return null;
    }

  }

  public static class NoKey implements CAstRewriter.CopyKey<NoKey> {
    private NoKey() {
      Assertions.UNREACHABLE();
    }
    
    public int hashCode() {
      return System.identityHashCode(this);
    }

    public boolean equals(Object o) {
      return o == this;
    }

    public NoKey parent() {
      return null;
    }
  };

  protected CAstBasicRewriter(CAst Ast, boolean recursive) {
    super(Ast, recursive, new NonCopyingContext());
  }

}