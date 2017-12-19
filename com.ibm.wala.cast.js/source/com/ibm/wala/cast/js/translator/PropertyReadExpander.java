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
package com.ibm.wala.cast.js.translator;

import java.util.Map;

import com.ibm.wala.cast.ir.translator.AstTranslator.InternalCAstSymbol;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.impl.CAstOperator;
import com.ibm.wala.cast.tree.rewrite.CAstRewriter;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;

/**
 * Transforms property reads to make prototype chain operations explicit. Each
 * read is converted to a do loop that walks up the prototype chain until the
 * property is found or the chain has ended.
 */
public class PropertyReadExpander extends CAstRewriter<PropertyReadExpander.RewriteContext, PropertyReadExpander.ExpanderKey> {

  static enum ExpanderKey implements com.ibm.wala.cast.tree.rewrite.CAstRewriter.CopyKey<ExpanderKey> {
    EVERYWHERE, EXTRA {
      @Override
      public ExpanderKey parent() { return EVERYWHERE; }
    };
    @Override
    public ExpanderKey parent() { return null; }
  }
  
  private int readTempCounter = 0;

  private static final String TEMP_NAME = "readTemp";

  abstract static class RewriteContext implements CAstRewriter.RewriteContext<ExpanderKey> {

    @Override
    public ExpanderKey key() {
      return ExpanderKey.EVERYWHERE;
    }

    /**
     * are we in a context where a property access should be treated as a read?
     * e.g., should return false if we are handling the LHS of an assignment
     */
    abstract boolean inRead();

    /**
     * are we handling a sub-node of an assignment?
     */
    abstract boolean inAssignment();

    /**
     * @see AssignPreOrPostOpContext
     */
    abstract void setAssign(CAstNode receiverTemp, CAstNode elementTemp);
  }

  /**
   * for handling property reads within assignments with pre or post-ops, e.g.,
   * x.f++
   */
  private final class AssignPreOrPostOpContext extends RewriteContext {
    private CAstNode receiverTemp;

    private CAstNode elementTemp;

    @Override
    public boolean inAssignment() {
      return true;
    }

    @Override
    public boolean inRead() {
      return true;
    }

    /**
     * store the CAstNodes used to represent the loop variable for the
     * prototype-chain traversal (receiverTemp) and the desired property
     * (elementTemp)
     */
    @Override
    public void setAssign(CAstNode receiverTemp, CAstNode elementTemp) {
      this.receiverTemp = receiverTemp;
      this.elementTemp = elementTemp;
    }

  }

  private final static RewriteContext READ = new RewriteContext() {
    @Override
    public boolean inAssignment() {
      return false;
    }

    @Override
    public boolean inRead() {
      return true;
    }

    @Override
    public void setAssign(CAstNode receiverTemp, CAstNode elementTemp) {
      Assertions.UNREACHABLE();
    }
  };

  private final static RewriteContext ASSIGN = new RewriteContext() {
    @Override
    public boolean inAssignment() {
      return true;
    }

    @Override
    public boolean inRead() {
      return false;
    }

    @Override
    public void setAssign(CAstNode receiverTemp, CAstNode elementTemp) {
      Assertions.UNREACHABLE();
    }
  };

  public PropertyReadExpander(CAst Ast) {
    super(Ast, true, READ);
  }

  /**
   * create a CAstNode l representing a loop that traverses the prototype chain
   * from receiver searching for the constant property element. update nodeMap
   * to map root to an expression that reads the property from the right node.
   * 
   * @param root
   * @param receiver
   * @param element
   * @param context
   * @param nodeMap
   */
  private CAstNode makeConstRead(CAstNode root, CAstNode receiver, CAstNode element, RewriteContext context,
      Map<Pair<CAstNode, ExpanderKey>, CAstNode> nodeMap) {
    CAstNode get, result;
    String receiverTemp = TEMP_NAME + (readTempCounter++);
    String elt = (String) element.getValue();
    if (elt.equals("prototype") || elt.equals("__proto__")) {
      result = Ast.makeNode(CAstNode.BLOCK_EXPR, get = Ast.makeNode(CAstNode.OBJECT_REF, receiver, Ast.makeConstant(elt)));
    } else {

      if (context.inAssignment()) {
        context.setAssign(Ast.makeNode(CAstNode.VAR, Ast.makeConstant(receiverTemp)), Ast.makeConstant(elt));
      }

      result = Ast.makeNode(
         CAstNode.BLOCK_EXPR,
              // declare loop variable and initialize to the receiver
              Ast.makeNode(CAstNode.DECL_STMT, Ast.makeConstant(new InternalCAstSymbol(receiverTemp, JSAstTranslator.Any, false, false)), receiver),
              Ast.makeNode(CAstNode.LOOP,
              // while the desired property of the loop variable is not
              // defined...
                  Ast.makeNode(
                      CAstNode.UNARY_EXPR,
                      CAstOperator.OP_NOT,
                      Ast.makeNode(CAstNode.IS_DEFINED_EXPR, Ast.makeNode(CAstNode.VAR, Ast.makeConstant(receiverTemp)),
                          Ast.makeConstant(elt))),
                  // set the loop variable to be its prototype
                  Ast.makeNode(
                      CAstNode.ASSIGN,
                      Ast.makeNode(CAstNode.VAR, Ast.makeConstant(receiverTemp)),
                      Ast.makeNode(CAstNode.OBJECT_REF, Ast.makeNode(CAstNode.VAR, Ast.makeConstant(receiverTemp)),
                          Ast.makeConstant("__proto__")))),
              get = Ast.makeNode(CAstNode.OBJECT_REF, Ast.makeNode(CAstNode.VAR, Ast.makeConstant(receiverTemp)),
                  Ast.makeConstant(elt)));
    }

    nodeMap.put(Pair.make(root, context.key()), result);
    nodeMap.put(Pair.make(root, ExpanderKey.EXTRA), get);

    return result;
  }

  /**
   * similar to makeConstRead(), but desired property is some expression instead
   * of a constant
   * 
   * @see #makeConstRead(CAstNode, CAstNode, CAstNode, RewriteContext, Map)
   */
  private CAstNode makeVarRead(CAstNode root, CAstNode receiver, CAstNode element, RewriteContext context,
      Map<Pair<CAstNode, ExpanderKey>, CAstNode> nodeMap) {
    String receiverTemp = TEMP_NAME + (readTempCounter++);
    String elementTemp = TEMP_NAME + (readTempCounter++);

    if (context.inAssignment()) {
      context.setAssign(Ast.makeNode(CAstNode.VAR, Ast.makeConstant(receiverTemp)),
          Ast.makeNode(CAstNode.VAR, Ast.makeConstant(elementTemp)));
    }

    CAstNode get;
    CAstNode result = Ast.makeNode(
        CAstNode.BLOCK_EXPR,
        Ast.makeNode(CAstNode.DECL_STMT, Ast.makeConstant(new InternalCAstSymbol(receiverTemp, JSAstTranslator.Any, false, false)), receiver),
        Ast.makeNode(CAstNode.DECL_STMT, Ast.makeConstant(new InternalCAstSymbol(elementTemp, JSAstTranslator.Any, false, false)), element),
        Ast.makeNode(
            CAstNode.LOOP,
            Ast.makeNode(
                CAstNode.UNARY_EXPR,
                CAstOperator.OP_NOT,
                Ast.makeNode(CAstNode.IS_DEFINED_EXPR, Ast.makeNode(CAstNode.VAR, Ast.makeConstant(receiverTemp)),
                    Ast.makeNode(CAstNode.VAR, Ast.makeConstant(elementTemp)))),
            Ast.makeNode(
                CAstNode.ASSIGN,
                Ast.makeNode(CAstNode.VAR, Ast.makeConstant(receiverTemp)),
                Ast.makeNode(CAstNode.OBJECT_REF, Ast.makeNode(CAstNode.VAR, Ast.makeConstant(receiverTemp)),
                    Ast.makeConstant("__proto__")))),
        get = Ast.makeNode(CAstNode.OBJECT_REF, Ast.makeNode(CAstNode.VAR, Ast.makeConstant(receiverTemp)),
            Ast.makeNode(CAstNode.VAR, Ast.makeConstant(elementTemp))));

    nodeMap.put(Pair.make(root, context.key()), get);
 
    return result;
  }

  @Override
  protected CAstNode copyNodes(CAstNode root, final CAstControlFlowMap cfg, RewriteContext context, Map<Pair<CAstNode, ExpanderKey>, CAstNode> nodeMap) {
    int kind = root.getKind();

    if (kind == CAstNode.OBJECT_REF && context.inRead()) {
      // if we see a property access (OBJECT_REF) in a read context, transform
      // to a loop traversing the prototype chain
      CAstNode readLoop;
      CAstNode receiver = copyNodes(root.getChild(0), cfg, READ, nodeMap);
      CAstNode element = copyNodes(root.getChild(1), cfg, READ, nodeMap);
      if (element.getKind() == CAstNode.CONSTANT && element.getValue() instanceof String) {
        readLoop = makeConstRead(root, receiver, element, context, nodeMap);
      } else {
        readLoop = makeVarRead(root, receiver, element, context, nodeMap);
      }
      return readLoop;

    } else if (kind == CAstNode.ASSIGN_PRE_OP || kind == CAstNode.ASSIGN_POST_OP) {
      // handle cases like x.f++, represented as ASSIGN_POST_OP(x.f,1,+)
      AssignPreOrPostOpContext ctxt = new AssignPreOrPostOpContext();
      // generate loop for the first child (x.f for example), keeping the loop var and element var in ctxt
      CAstNode lval = copyNodes(root.getChild(0), cfg, ctxt, nodeMap);
      CAstNode rval = copyNodes(root.getChild(1), cfg, READ, nodeMap);
      CAstNode op = copyNodes(root.getChild(2), cfg, READ, nodeMap);
      if (ctxt.receiverTemp != null) {  // if we found a nested property access
        String temp1 = TEMP_NAME + (readTempCounter++);
        String temp2 = TEMP_NAME + (readTempCounter++);
        CAstNode copy = Ast.makeNode(
            CAstNode.BLOCK_EXPR,
            // assign lval to temp1 (where lval is a block that includes the prototype chain loop)
            Ast.makeNode(CAstNode.DECL_STMT, Ast.makeConstant(new InternalCAstSymbol(temp1, JSAstTranslator.Any, true, false)), lval),
            // ? --MS
            //rval,
            // assign temp2 the new value to be assigned
            Ast.makeNode(CAstNode.DECL_STMT, Ast.makeConstant(new InternalCAstSymbol(temp2, JSAstTranslator.Any, true, false)),
                Ast.makeNode(CAstNode.BINARY_EXPR, op, Ast.makeNode(CAstNode.VAR, Ast.makeConstant(temp1)), rval)),
            // write temp2 into the property
            Ast.makeNode(CAstNode.ASSIGN, Ast.makeNode(CAstNode.OBJECT_REF, ctxt.receiverTemp, ctxt.elementTemp),
                Ast.makeNode(CAstNode.VAR, Ast.makeConstant(temp2))),
            // final value depends on whether we had a pre op or post op    
            Ast.makeNode(CAstNode.VAR, Ast.makeConstant((kind == CAstNode.ASSIGN_PRE_OP) ? temp2 : temp1)));
        nodeMap.put(Pair.make(root, context.key()), copy);
        return copy;
      } else {
        CAstNode copy = Ast.makeNode(kind, lval, rval, op);
        nodeMap.put(Pair.make(root, context.key()), copy);
        return copy;
      }

    } else if (kind == CAstNode.ASSIGN) {
      // use ASSIGN context for LHS so we don't translate property accesses there
      CAstNode copy = Ast.makeNode(CAstNode.ASSIGN, copyNodes(root.getChild(0), cfg, ASSIGN, nodeMap),
          copyNodes(root.getChild(1), cfg, READ, nodeMap));
      nodeMap.put(Pair.make(root, context.key()), copy);
      return copy;

    } else if (kind == CAstNode.BLOCK_EXPR) {
      CAstNode children[] = new CAstNode[root.getChildCount()];
      int last = (children.length - 1);
      for (int i = 0; i < last; i++) {
        children[i] = copyNodes(root.getChild(i), cfg, READ, nodeMap);
      }
      children[last] = copyNodes(root.getChild(last), cfg, context, nodeMap);

      CAstNode copy = Ast.makeNode(CAstNode.BLOCK_EXPR, children);
      nodeMap.put(Pair.make(root, context.key()), copy);
      return copy;

    } else if (root.getKind() == CAstNode.CONSTANT) {
      CAstNode copy = Ast.makeConstant(root.getValue());
      nodeMap.put(Pair.make(root, context.key()), copy);
      return copy;

    } else if (root.getKind() == CAstNode.OPERATOR) {
      nodeMap.put(Pair.make(root, context.key()), root);
      return root;

    } else {
      CAstNode children[] = new CAstNode[root.getChildCount()];
      for (int i = 0; i < children.length; i++) {
        children[i] = copyNodes(root.getChild(i), cfg, READ, nodeMap);
      }
      for(Object label: cfg.getTargetLabels(root)) {
        if (label instanceof CAstNode) {
          copyNodes((CAstNode)label, cfg, READ, nodeMap);
        }
      }
      CAstNode copy = Ast.makeNode(kind, children);
      nodeMap.put(Pair.make(root, context.key()), copy);
      return copy;
    }
  }
}
