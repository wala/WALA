/******************************************************************************
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

/**
 * 
 */
package com.ibm.wala.cast.ir.translator;

import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstNodeTypeMap;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;

abstract class AbstractDataEntity extends AbstractEntity {
  @Override
  public CAstNode getAST() {
    return null;
  }

  @Override
  public CAstControlFlowMap getControlFlow() {
    return null;
  }

  @Override
  public CAstSourcePositionMap getSourceMap() {
    return null;
  }

  @Override
  public CAstNodeTypeMap getNodeTypeMap() {
    return null;
  }

  @Override
  public String[] getArgumentNames() {
    return new String[0];
  }

  @Override
  public CAstNode[] getArgumentDefaults() {
    return new CAstNode[0];
  }

  @Override
  public int getArgumentCount() {
    return 0;
  }
}
