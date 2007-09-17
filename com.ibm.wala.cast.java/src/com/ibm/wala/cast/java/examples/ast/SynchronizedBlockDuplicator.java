package com.ibm.wala.cast.java.examples.ast;

import com.ibm.wala.cast.java.translator.TranslatorToCAst;
import com.ibm.wala.cast.tree.*;
import com.ibm.wala.cast.tree.impl.*;
import com.ibm.wala.cast.util.CAstPrinter;
import com.ibm.wala.classLoader.*;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.collections.*;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.*;

import java.util.*;

public class SynchronizedBlockDuplicator
  extends CAstRewriter<CAstRewriter.RewriteContext<SynchronizedBlockDuplicator.UnwindKey>,SynchronizedBlockDuplicator.UnwindKey>
{

  class UnwindKey implements CAstRewriter.CopyKey<UnwindKey> {
    private boolean testDirection;
    private CAstNode syncNode;
    private UnwindKey rest;
			  
    private UnwindKey(boolean testDirection, CAstNode syncNode, UnwindKey rest)
    {
      this.rest = rest;
      this.syncNode = syncNode;
      this.testDirection = testDirection;
    }
			  
    public int hashCode() {
      return (testDirection? 1: -1) *
	     System.identityHashCode(syncNode) *
	     (rest == null? 1: rest.hashCode());
    }
		
    public UnwindKey parent() {
      return rest;
    }

    public boolean equals(Object o) {
      return (o instanceof UnwindKey) &&
	((UnwindKey)o).testDirection == testDirection &&
	((UnwindKey)o).syncNode == syncNode &&
	( rest==null? ((UnwindKey)o).rest == null: rest.equals(((UnwindKey)o).rest) );
    }

    public String toString() {
      return "#"+testDirection+ ((rest==null)?"":rest.toString());
    }
  }

  private static final boolean DEBUG = false;

  private final CallSiteReference f;

  public SynchronizedBlockDuplicator(CAst Ast,
				     boolean recursive, 
				     CallSiteReference f)
  {
    super(Ast, recursive, new RootContext());
    this.f = f;
  }
	  
  public CAstEntity translate(CAstEntity original) {
    return rewrite(original);
  }

  private static class RootContext implements RewriteContext<UnwindKey> {
		  
    public UnwindKey key() {
      return null;
    }
  }
	  	  
  class SyncContext implements RewriteContext<UnwindKey> {
    private final CAstRewriter.RewriteContext<UnwindKey> parent;
    private final boolean testDirection;
    private final CAstNode syncNode;

    private SyncContext(boolean testDirection,
			CAstNode syncNode,
			RewriteContext<UnwindKey> parent)
    {
      this.testDirection = testDirection;
      this.syncNode = syncNode;
      this.parent = parent;
    }
        
    public UnwindKey key() {
      return new UnwindKey(testDirection, syncNode, parent.key());
    }

    private boolean containsNode(CAstNode n) {
      if (n == syncNode) {
	return true;
      } else if (parent != null) {
	return contains(parent, n);
      } else {
	return false;
      }
    }
  }
	  
  protected CAstNode flowOutTo(Map nodeMap, 
			       CAstNode oldSource,
			       Object label,
			       CAstNode oldTarget,
			       CAstControlFlowMap orig, 
			       CAstSourcePositionMap src) 
  {
    assert oldTarget == CAstControlFlowMap.EXCEPTION_TO_EXIT;
    return oldTarget;
  }

  private boolean contains(RewriteContext<UnwindKey> c, CAstNode n) {
    if (c instanceof SyncContext) {
      return ((SyncContext)c).containsNode(n);
    } else {
      return false;
    }
  }

  private String isSynchronizedOnVar(CAstNode root) {
    if (root.getKind() == CAstNode.UNWIND) {
      CAstNode unwindBody = root.getChild(0);
      if (unwindBody.getKind() == CAstNode.BLOCK_STMT) {
	CAstNode firstStmt = unwindBody.getChild(0);
	if (firstStmt.getKind() == CAstNode.MONITOR_ENTER) {
	  CAstNode expr = firstStmt.getChild(0);
	  if (expr.getKind() == CAstNode.VAR) {
	    String varName = (String) expr.getChild(0).getValue();
	    
	    CAstNode protectBody = root.getChild(1);
	    if (protectBody.getKind() == CAstNode.MONITOR_EXIT) {
	      CAstNode expr2 = protectBody.getChild(0);
	      if (expr2.getKind() == CAstNode.VAR) {
		String varName2 = (String) expr2.getChild(0).getValue();
		if (varName.equals(varName2)) {
		  return varName;
		}
	      }
	    }
	  }
	}
      }
    }

    return null;
  }

  protected CAstNode copyNodes(CAstNode n, RewriteContext<UnwindKey> c, Map nodeMap) {
    String varName;
    if (n instanceof CAstOperator) {
      return n;

    } else if (n.getValue() != null) {
      return Ast.makeConstant( n.getValue() );

    } else if (!contains(c, n) && (varName = isSynchronizedOnVar(n)) != null) {
      CAstNode test = 
	Ast.makeNode(CAstNode.CALL, 
	  Ast.makeNode(CAstNode.VOID),
	  Ast.makeConstant(f),
	  Ast.makeNode(CAstNode.VAR, Ast.makeConstant(varName)));

      return
	Ast.makeNode(CAstNode.IF_STMT,
	  test,
	  copyNodes(n, new SyncContext(true, n, c), nodeMap),
	  copyNodes(n, new SyncContext(false, n, c), nodeMap));

    } else {
	CAstNode[] newChildren = new CAstNode[ n.getChildCount() ];
	for(int i = 0; i < newChildren.length; i++) 
	  newChildren[i] = copyNodes(n.getChild(i), c, nodeMap);
			  
	CAstNode newN = Ast.makeNode(n.getKind(), newChildren);
			  			
	nodeMap.put(Pair.make(n, c.key()), newN);
	
	return newN; 
    }
  }	  
}
