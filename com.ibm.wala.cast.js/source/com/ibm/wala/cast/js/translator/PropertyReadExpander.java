package com.ibm.wala.cast.js.translator;

import java.util.Map;

import com.ibm.wala.cast.ir.translator.AstTranslator.InternalCAstSymbol;
import com.ibm.wala.cast.tree.*;
import com.ibm.wala.cast.tree.impl.*;
import com.ibm.wala.util.collections.*;
import com.ibm.wala.util.debug.Assertions;

public class PropertyReadExpander extends CAstRewriter<PropertyReadExpander.RewriteContext, CAstBasicRewriter.NoKey> {

  private int readTempCounter = 0;

  private static final String TEMP_NAME = "readTemp";

  abstract static class RewriteContext
    extends CAstBasicRewriter.NonCopyingContext 
  {

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

  private CAstNode makeConstRead(CAstNode receiver, 
				 CAstNode element,
				 RewriteContext context)
  {
    String receiverTemp = TEMP_NAME + (readTempCounter++);
    String elt = (String) element.getValue();

    if (context.inAssignment()) {
      context.setAssign(
        Ast.makeNode(CAstNode.VAR, Ast.makeConstant(receiverTemp)), 
	Ast.makeConstant(elt));
    }

    return
      Ast.makeNode(CAstNode.BLOCK_EXPR,
        Ast.makeNode(CAstNode.DECL_STMT,
          Ast.makeConstant(new InternalCAstSymbol(receiverTemp, false, false)),
	  receiver),
        Ast.makeNode(CAstNode.LOOP,
          Ast.makeNode(CAstNode.UNARY_EXPR,
            CAstOperator.OP_NOT,
            Ast.makeNode(CAstNode.IS_DEFINED_EXPR, 
              Ast.makeNode(CAstNode.VAR, Ast.makeConstant(receiverTemp)),
	      Ast.makeConstant(elt))),
	  Ast.makeNode(CAstNode.ASSIGN,
            Ast.makeNode(CAstNode.VAR, Ast.makeConstant(receiverTemp)),
	    Ast.makeNode(CAstNode.OBJECT_REF,
	      Ast.makeNode(CAstNode.VAR, Ast.makeConstant(receiverTemp)),
	      Ast.makeConstant("prototype")))),
	Ast.makeNode(CAstNode.OBJECT_REF,
	  Ast.makeNode(CAstNode.VAR, Ast.makeConstant(receiverTemp)),
	  Ast.makeConstant(elt)));
  }

  private CAstNode makeVarRead(CAstNode receiver, 
			       CAstNode element,
			       RewriteContext context) 
  {
    String receiverTemp = TEMP_NAME + (readTempCounter++);
    String elementTemp = TEMP_NAME + (readTempCounter++);

    if (context.inAssignment()) {
      context.setAssign(
        Ast.makeNode(CAstNode.VAR, Ast.makeConstant(receiverTemp)), 
        Ast.makeNode(CAstNode.VAR, Ast.makeConstant(elementTemp)));
    }

    return
      Ast.makeNode(CAstNode.BLOCK_EXPR,
        Ast.makeNode(CAstNode.DECL_STMT,
	  Ast.makeConstant(new InternalCAstSymbol(receiverTemp, false, false)),
	  receiver),
	Ast.makeNode(CAstNode.DECL_STMT,
	  Ast.makeConstant(new InternalCAstSymbol(elementTemp, false, false)),
	  element),
	Ast.makeNode(CAstNode.LOOP,
	  Ast.makeNode(CAstNode.UNARY_EXPR,
	    CAstOperator.OP_NOT,
	    Ast.makeNode(CAstNode.IS_DEFINED_EXPR, 
	      Ast.makeNode(CAstNode.VAR, Ast.makeConstant(receiverTemp)),
	      Ast.makeNode(CAstNode.VAR, Ast.makeConstant(elementTemp)))),
	  Ast.makeNode(CAstNode.ASSIGN,
	    Ast.makeNode(CAstNode.VAR, Ast.makeConstant(receiverTemp)),
	    Ast.makeNode(CAstNode.OBJECT_REF,
	      Ast.makeNode(CAstNode.VAR, Ast.makeConstant(receiverTemp)),
	      Ast.makeConstant("prototype")))),
        Ast.makeNode(CAstNode.OBJECT_REF,
	  Ast.makeNode(CAstNode.VAR, Ast.makeConstant(receiverTemp)),
	  Ast.makeNode(CAstNode.VAR, Ast.makeConstant(elementTemp))));
  }

  protected CAstNode copyNodes(CAstNode root, 
			       RewriteContext context,
			       Map nodeMap) 
  {
    int kind = root.getKind();

    if (kind == CAstNode.OBJECT_REF && context.inRead()) {
      CAstNode readLoop;
      CAstNode receiver = copyNodes(root.getChild(0), READ, nodeMap);
      CAstNode element = copyNodes(root.getChild(1), READ, nodeMap);
      if (element.getKind()==CAstNode.CONSTANT && element.getValue() instanceof String) {
	readLoop= makeConstRead(receiver, element, context);
      } else {
	readLoop= makeVarRead(receiver, element, context);
      }
      nodeMap.put(new Pair(root, context.key()), readLoop);
      return readLoop;

    } else if (kind==CAstNode.ASSIGN_PRE_OP || kind==CAstNode.ASSIGN_POST_OP) {
      AssignOpContext ctxt = new AssignOpContext();
      CAstNode lval = copyNodes(root.getChild(0), ctxt, nodeMap);
      CAstNode rval = copyNodes(root.getChild(1), READ, nodeMap);
      CAstNode op = copyNodes(root.getChild(2), READ, nodeMap);
      if (ctxt.receiverTemp != null) {
	String temp1 = TEMP_NAME + (readTempCounter++);
	String temp2 = TEMP_NAME + (readTempCounter++);
	CAstNode copy = Ast.makeNode(CAstNode.BLOCK_EXPR,
	  Ast.makeNode(CAstNode.DECL_STMT,
	    Ast.makeConstant(new InternalCAstSymbol(temp1, true, false)),
	    lval),
	  rval,
	  Ast.makeNode(CAstNode.DECL_STMT,
	    Ast.makeConstant(new InternalCAstSymbol(temp2, true, false)),
	    Ast.makeNode(CAstNode.BINARY_EXPR, op, 
	      Ast.makeNode(CAstNode.VAR, Ast.makeConstant(temp1)),
	      rval)),
	  Ast.makeNode(CAstNode.ASSIGN,
	    Ast.makeNode(CAstNode.OBJECT_REF,
	      ctxt.receiverTemp,
	      ctxt.elementTemp),
	    Ast.makeNode(CAstNode.VAR, Ast.makeConstant(temp2))),
	  Ast.makeNode(CAstNode.VAR, 
	    Ast.makeConstant((kind==CAstNode.ASSIGN_PRE_OP)? temp2: temp1)));
	nodeMap.put(new Pair(root, context.key()), copy);
	return copy;
      } else {
	CAstNode copy = Ast.makeNode(kind, lval, rval, op);
	nodeMap.put(new Pair(root, context.key()), copy);
	return copy;
      }

    } else if (kind == CAstNode.ASSIGN) {
	CAstNode copy = Ast.makeNode(CAstNode.ASSIGN, 
	  copyNodes(root.getChild(0), ASSIGN, nodeMap),
	  copyNodes(root.getChild(1), READ, nodeMap));
	nodeMap.put(new Pair(root, context.key()), copy);
	return copy;

    } else if (kind == CAstNode.BLOCK_EXPR) {
      CAstNode children[] = new CAstNode[ root.getChildCount() ];
      int last = (children.length - 1);
      for(int i = 0; i < last; i++) {
	children[i] = copyNodes(root.getChild(i), READ, nodeMap);
      }
      children[last] = copyNodes(root.getChild(last), context, nodeMap);

      CAstNode copy = Ast.makeNode(CAstNode.BLOCK_EXPR, children);  
      nodeMap.put(new Pair(root, context.key()), copy);
      return copy;

    } else if (root.getKind() == CAstNode.CONSTANT) {
      CAstNode copy = Ast.makeConstant( root.getValue() );
      nodeMap.put(new Pair(root, context.key()), copy);
      return copy;

    } else if (root.getKind() == CAstNode.OPERATOR) { 
      nodeMap.put(new Pair(root, context.key()), root);
      return root;

    } else { 
      CAstNode children[] = new CAstNode[ root.getChildCount() ];
      for(int i = 0; i < children.length; i++) {
	children[i] = copyNodes(root.getChild(i), READ, nodeMap);
      }
      CAstNode copy = Ast.makeNode(kind, children);  
      nodeMap.put(new Pair(root, context.key()), copy);
      return copy;
    }
  }
}

