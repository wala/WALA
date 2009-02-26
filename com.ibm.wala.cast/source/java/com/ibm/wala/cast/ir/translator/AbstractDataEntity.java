/**
 * 
 */
package com.ibm.wala.cast.ir.translator;

import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstNodeTypeMap;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;

abstract class AbstractDataEntity extends AbstractEntity {
  public CAstNode getAST() {
    return null;
  }

  public CAstControlFlowMap getControlFlow() {
    return null;
  }

  public CAstSourcePositionMap getSourceMap() {
    return null;
  }

  public CAstNodeTypeMap getNodeTypeMap() {
    return null;
  }

  public String[] getArgumentNames() {
    return new String[0];
  }

  public CAstNode[] getArgumentDefaults() {
    return new CAstNode[0];
  }

  public int getArgumentCount() {
    return 0;
  }
}