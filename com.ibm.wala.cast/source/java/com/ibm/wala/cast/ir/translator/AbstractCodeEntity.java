/**
 * 
 */
package com.ibm.wala.cast.ir.translator;

import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstNodeTypeMap;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.CAstType;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.impl.CAstControlFlowRecorder;
import com.ibm.wala.cast.tree.impl.CAstNodeTypeMapRecorder;
import com.ibm.wala.cast.tree.impl.CAstSourcePositionRecorder;

public abstract class AbstractCodeEntity extends AbstractEntity {
  protected final CAstSourcePositionRecorder src = new CAstSourcePositionRecorder();

  protected final CAstControlFlowRecorder cfg = new CAstControlFlowRecorder(src);

  protected final CAstNodeTypeMapRecorder types = new CAstNodeTypeMapRecorder();

  protected final CAstType type;

  protected CAstNode Ast;

  protected AbstractCodeEntity(CAstType type) {
    this.type = type;
  }

  public CAstNode getAST() {
    return Ast;
  }

  public CAstType getType() {
    return type;
  }

  public CAstControlFlowMap getControlFlow() {
    return cfg;
  }

  public CAstSourcePositionMap getSourceMap() {
    return src;
  }

  public CAstNodeTypeMap getNodeTypeMap() {
    return types;
  }

  public void setGotoTarget(CAstNode from, CAstNode to) {
    setLabelledGotoTarget(from, to, null);
  }

  public void setLabelledGotoTarget(CAstNode from, CAstNode to, Object label) {
    if (!cfg.isMapped(from)) {
      cfg.map(from, from);
    }
    if (!cfg.isMapped(to)) {
      cfg.map(to, to);
    }
    cfg.add(from, to, label);
  }

  public void setNodePosition(CAstNode n, Position pos) {
    src.setPosition(n, pos);
  }

  public void setNodeType(CAstNode n, CAstType type) {
    types.add(n, type);
  }
  
  public void setAst(CAstNode Ast) {
    this.Ast = Ast;
  }
}