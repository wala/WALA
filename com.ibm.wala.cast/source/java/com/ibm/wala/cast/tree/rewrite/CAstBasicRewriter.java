package com.ibm.wala.cast.tree.rewrite;

import java.util.HashMap;
import java.util.Map;

import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;

/**
 * abstract base class for {@link CAstRewriter}s that do no cloning of nodes
 *
 */
public abstract class CAstBasicRewriter
  extends CAstRewriter<CAstBasicRewriter.NonCopyingContext, 
	               CAstBasicRewriter.NoKey> 
{

  /**
   * context indicating that no cloning is being performed
   */
  public static class NonCopyingContext implements CAstRewriter.RewriteContext<NoKey> {
    private final Map nodeMap = new HashMap();

    public Map nodeMap() {
      return nodeMap;
    }

    public NoKey key() {
      return null;
    }

  }

  /**
   * key indicating that no duplication is being performed
   */
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

  protected abstract CAstNode copyNodes(CAstNode root, final CAstControlFlowMap cfg, NonCopyingContext context, Map<Pair<CAstNode,NoKey>, CAstNode> nodeMap);
  
}