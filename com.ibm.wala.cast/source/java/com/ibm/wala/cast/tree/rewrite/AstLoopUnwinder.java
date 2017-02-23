/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cast.tree.rewrite;

import java.util.Map;

import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.impl.CAstOperator;
import com.ibm.wala.util.collections.Pair;

public class AstLoopUnwinder
  extends CAstRewriter<CAstRewriter.RewriteContext<AstLoopUnwinder.UnwindKey>,AstLoopUnwinder.UnwindKey>
{

  public static class UnwindKey implements CAstRewriter.CopyKey<UnwindKey> {
    private int iteration;
    private UnwindKey rest;
			  
    private UnwindKey(int iteration, UnwindKey rest) {
      this.rest = rest;
      this.iteration = iteration;
    }
			  
    @Override
    public int hashCode() {
      return iteration * (rest == null? 1: rest.hashCode());
    }
		
    @Override
    public UnwindKey parent() {
      return rest;
    }

    @Override
    public boolean equals(Object o) {
      return (o instanceof UnwindKey) &&
	((UnwindKey)o).iteration == iteration &&
	( rest==null? ((UnwindKey)o).rest == null: rest.equals(((UnwindKey)o).rest) );
    }

    @Override
    public String toString() {
      return "#"+iteration+ ((rest==null)?"":rest.toString());
    }
  }

  // private static final boolean DEBUG = false;

  private final int unwindFactor;
	
  public AstLoopUnwinder(CAst Ast, boolean recursive) {
    this(Ast, recursive, 3);
  }
	  
  public AstLoopUnwinder(CAst Ast, boolean recursive, int unwindFactor) {
    super(Ast, recursive, new RootContext());
    this.unwindFactor = unwindFactor;
  }
	  
  public CAstEntity translate(CAstEntity original) {
    return rewrite(original);
  }

  private static class RootContext implements RewriteContext<UnwindKey> {
		  
    @Override
    public UnwindKey key() {
      return null;
    }
  }
	  	  
  private class LoopContext implements RewriteContext<UnwindKey> {
    private final CAstRewriter.RewriteContext<UnwindKey> parent;
    private final int iteration;
        
    private LoopContext(int iteration, RewriteContext<UnwindKey> parent) {
      this.iteration = iteration;
      this.parent = parent;
    }
        
    @Override
    public UnwindKey key() {
      return new UnwindKey(iteration, parent.key());
    }
      
  }
	  
  @Override
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

  @Override
  protected CAstNode copyNodes(CAstNode n, final CAstControlFlowMap cfg, RewriteContext<UnwindKey> c, Map<Pair<CAstNode,UnwindKey>,CAstNode> nodeMap) {
    if (n instanceof CAstOperator) {
      return n;
    } else if (n.getValue() != null) {
      return Ast.makeConstant( n.getValue() );
    } else if (n.getKind() == CAstNode.LOOP) {
      CAstNode test = n.getChild(0);
      CAstNode body = n.getChild(1);
      
      int count = unwindFactor;
      RewriteContext<UnwindKey> lc = new LoopContext(count, c);
      CAstNode code = 
        Ast.makeNode(CAstNode.ASSERT,
	  Ast.makeNode(CAstNode.UNARY_EXPR,
	    CAstOperator.OP_NOT,
            copyNodes(test, cfg, lc, nodeMap)),
          Ast.makeConstant(false));
      while (count-- > 0) {
	lc = new LoopContext(count, c);
	code = Ast.makeNode(CAstNode.IF_STMT, 
			    copyNodes(test, cfg, lc, nodeMap),
			    Ast.makeNode(CAstNode.BLOCK_STMT,
					 copyNodes(body, cfg, lc, nodeMap),
					 code));
      }
			  
      return code;
    } else {
	CAstNode[] newChildren = new CAstNode[ n.getChildCount() ];
	for(int i = 0; i < newChildren.length; i++) 
	  newChildren[i] = copyNodes(n.getChild(i), cfg, c, nodeMap);
			  
	CAstNode newN = Ast.makeNode(n.getKind(), newChildren);
			  			
	nodeMap.put(Pair.make(n, c.key()), newN);
	
	return newN; 
    }
  }	  
}
