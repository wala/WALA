package com.ibm.wala.cast.tree.impl;

import java.util.HashMap;
import java.util.Map;

import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;

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

  protected abstract CAstNode copyNodes(CAstNode root, NonCopyingContext context, Map<Pair, CAstNode> nodeMap);
  
}