/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
/*
 * Created on Oct 10, 2005
 */
package com.ibm.wala.cast.tree.impl;

import com.ibm.wala.cast.tree.*;

import java.util.*;

public class CAstNodeTypeMapRecorder 
    extends HashMap <CAstNode,CAstType>
    implements CAstNodeTypeMap
{
  private static final long serialVersionUID= 7812144102027916961L;

  public CAstType getNodeType(CAstNode node) {
    return (CAstType) get(node);
  }

  public void add(CAstNode node, CAstType type) {
    put(node, type);
  }

}
