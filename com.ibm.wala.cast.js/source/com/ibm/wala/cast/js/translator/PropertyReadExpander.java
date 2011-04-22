package com.ibm.wala.cast.js.translator;

import java.util.Map;

import com.ibm.wala.cast.ir.translator.AstTranslator.InternalCAstSymbol;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.impl.CAstBasicRewriter;
import com.ibm.wala.cast.tree.impl.CAstOperator;
import com.ibm.wala.cast.tree.impl.CAstRewriter;
import com.ibm.wala.cast.tree.impl.CAstBasicRewriter.NoKey;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;

/**
 * Transforms property reads to make prototype chain operations explicit. Each
 * read is converted to a do loop that walks up the prototype chain until the
 * property is found or the chain has ended.
 */
public class PropertyReadExpander extends CAstRewriter<PropertyReadExpander.RewriteContext, CAstBasicRewriter.NoKey> {

  private int readTempCounter = 0;

  private static final String TEMP_NAME = "readTemp";

  abstract static class RewriteContext extends CAstBasicRewriter.NonCopyingContext {

    abstract boolean inRead();

    abstract boolean inAssignment();

    abstract void setAssign(CAstNode receiverTemp, CAstNode elementTemp);
  }

  private final class AssignOpContext extends RewriteContext {
    private CAstNode receiverTemp;

    private CAstNode elementTemp;

    public boolean inAssignment() {
      return true;
    }

    public boolean inRead() {
      return true;
    }

    public void setAssign(CAstNode receiverTemp, CAstNode elementTemp) {
      this.receiverTemp = receiverTemp;
      this.elementTemp = elementTemp;
    }

  };

  private final static RewriteContext READ = new RewriteContext() {
    public boolean inAssignment() {
      return false;
    }

    public boolean inRead() {
      return true;
    }

    public void setAssign(CAstNode receiverTemp, CAstNode elementTemp) {
      Assertions.UNREACHABLE();
    }
  };

  private final static RewriteContext ASSIGN = new RewriteContext() {
    public boolean inAssignment() {
      return true;
    }

    public boolean inRead() {
      return false;
    }

    public void setAssign(CAstNode receiverTemp, CAstNode elementTemp) {
      Assertions.UNREACHABLE();
    }
  };

  public PropertyReadExpander(CAst Ast) {
    super(Ast, true, READ);
  }

  private CAstNode makeConstRead(CAstNode root, CAstNode receiver, CAstNode element, RewriteContext context,
      Map<Pair<CAstNode, NoKey>, CAstNode> nodeMap) {
    CAstNode get, result;
    String receiverTemp = TEMP_NAME + (readTempCounter++);
    String elt = (String) element.getValue();
    if (elt.equals("prototype")) {
      result = Ast.makeNode(CAstNode.BLOCK_EXPR, get = Ast.makeNode(CAstNode.OBJECT_REF, receiver, Ast.makeConstant("prototype")));
    } else {

      if (context.inAssignment()) {
        context.setAssign(Ast.makeNode(CAstNode.VAR, Ast.makeConstant(receiverTemp)), Ast.makeConstant(elt));
      }

      result = Ast
          .makeNode(
              CAstNode.BLOCK_EXPR,
              Ast.makeNode(CAstNode.DECL_STMT, Ast.makeConstant(new InternalCAstSymbol(receiverTemp, false, false)), receiver),
              Ast.makeNode(
                  CAstNode.LOOP,
                  Ast.makeNode(
                      CAstNode.UNARY_EXPR,
                      CAstOperator.OP_NOT,
                      Ast.makeNode(CAstNode.IS_DEFINED_EXPR, Ast.makeNode(CAstNode.VAR, Ast.makeConstant(receiverTemp)),
                          Ast.makeConstant(elt))),
                  Ast.makeNode(
                      CAstNode.ASSIGN,
                      Ast.makeNode(CAstNode.VAR, Ast.makeConstant(receiverTemp)),
                      Ast.makeNode(CAstNode.OBJECT_REF, Ast.makeNode(CAstNode.VAR, Ast.makeConstant(receiverTemp)),
                          Ast.makeConstant("prototype")))),
              get = Ast.makeNode(CAstNode.OBJECT_REF, Ast.makeNode(CAstNode.VAR, Ast.makeConstant(receiverTemp)),
                  Ast.makeConstant(elt)));
    }

    nodeMap.put(Pair.make(root, context.key()), get);

    return result;
  }

  private CAstNode makeVarRead(CAstNode root, CAstNode receiver, CAstNode element, RewriteContext context,
      Map<Pair<CAstNode, NoKey>, CAstNode> nodeMap) {
    String receiverTemp = TEMP_NAME + (readTempCounter++);
    String elementTemp = TEMP_NAME + (readTempCounter++);

    if (context.inAssignment()) {
      context.setAssign(Ast.makeNode(CAstNode.VAR, Ast.makeConstant(receiverTemp)),
          Ast.makeNode(CAstNode.VAR, Ast.makeConstant(elementTemp)));
    }

    CAstNode get;
    CAstNode result = Ast.makeNode(
        CAstNode.BLOCK_EXPR,
        Ast.makeNode(CAstNode.DECL_STMT, Ast.makeConstant(new InternalCAstSymbol(receiverTemp, false, false)), receiver),
        Ast.makeNode(CAstNode.DECL_STMT, Ast.makeConstant(new InternalCAstSymbol(elementTemp, false, false)), element),
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
                    Ast.makeConstant("prototype")))),
        get = Ast.makeNode(CAstNode.OBJECT_REF, Ast.makeNode(CAstNode.VAR, Ast.makeConstant(receiverTemp)),
            Ast.makeNode(CAstNode.VAR, Ast.makeConstant(elementTemp))));

    nodeMap.put(Pair.make(root, context.key()), get);

    return result;
  }

  protected CAstNode copyNodes(CAstNode root, RewriteContext context, Map<Pair<CAstNode,NoKey>, CAstNode> nodeMap) {
    int kind = root.getKind();

    if (kind == CAstNode.OBJECT_REF && context.inRead()) {
      CAstNode readLoop;
      CAstNode receiver = copyNodes(root.getChild(0), READ, nodeMap);
      CAstNode element = copyNodes(root.getChild(1), READ, nodeMap);
      if (element.getKind() == CAstNode.CONSTANT && element.getValue() instanceof String) {
        readLoop = makeConstRead(root, receiver, element, context, nodeMap);
      } else {
        readLoop = makeVarRead(root, receiver, element, context, nodeMap);
      }
      return readLoop;

    } else if (kind == CAstNode.ASSIGN_PRE_OP || kind == CAstNode.ASSIGN_POST_OP) {
      AssignOpContext ctxt = new AssignOpContext();
      CAstNode lval = copyNodes(root.getChild(0), ctxt, nodeMap);
      CAstNode rval = copyNodes(root.getChild(1), READ, nodeMap);
      CAstNode op = copyNodes(root.getChild(2), READ, nodeMap);
      if (ctxt.receiverTemp != null) {
        String temp1 = TEMP_NAME + (readTempCounter++);
        String temp2 = TEMP_NAME + (readTempCounter++);
        CAstNode copy = Ast.makeNode(
            CAstNode.BLOCK_EXPR,
            Ast.makeNode(CAstNode.DECL_STMT, Ast.makeConstant(new InternalCAstSymbol(temp1, true, false)), lval),
            rval,
            Ast.makeNode(CAstNode.DECL_STMT, Ast.makeConstant(new InternalCAstSymbol(temp2, true, false)),
                Ast.makeNode(CAstNode.BINARY_EXPR, op, Ast.makeNode(CAstNode.VAR, Ast.makeConstant(temp1)), rval)),
            Ast.makeNode(CAstNode.ASSIGN, Ast.makeNode(CAstNode.OBJECT_REF, ctxt.receiverTemp, ctxt.elementTemp),
                Ast.makeNode(CAstNode.VAR, Ast.makeConstant(temp2))),
            Ast.makeNode(CAstNode.VAR, Ast.makeConstant((kind == CAstNode.ASSIGN_PRE_OP) ? temp2 : temp1)));
        nodeMap.put(Pair.make(root, context.key()), copy);
        return copy;
      } else {
        CAstNode copy = Ast.makeNode(kind, lval, rval, op);
        nodeMap.put(Pair.make(root, context.key()), copy);
        return copy;
      }

    } else if (kind == CAstNode.ASSIGN) {
      CAstNode copy = Ast.makeNode(CAstNode.ASSIGN, copyNodes(root.getChild(0), ASSIGN, nodeMap),
          copyNodes(root.getChild(1), READ, nodeMap));
      nodeMap.put(Pair.make(root, context.key()), copy);
      return copy;

    } else if (kind == CAstNode.BLOCK_EXPR) {
      CAstNode children[] = new CAstNode[root.getChildCount()];
      int last = (children.length - 1);
      for (int i = 0; i < last; i++) {
        children[i] = copyNodes(root.getChild(i), READ, nodeMap);
      }
      children[last] = copyNodes(root.getChild(last), context, nodeMap);

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
        children[i] = copyNodes(root.getChild(i), READ, nodeMap);
      }
      CAstNode copy = Ast.makeNode(kind, children);
      nodeMap.put(Pair.make(root, context.key()), copy);
      return copy;
    }
  }
}
