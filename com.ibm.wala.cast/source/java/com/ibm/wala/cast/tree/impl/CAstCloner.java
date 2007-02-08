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
package com.ibm.wala.cast.tree.impl;

import com.ibm.wala.cast.tree.*;
import com.ibm.wala.classLoader.IField;

import java.util.*;

public class CAstCloner {

  private final CAst Ast;

  public CAstCloner(CAst Ast) {
    this.Ast = Ast;
  }

  public interface Clone {

    CAstNode newRoot();

    CAstControlFlowMap newCfg();

    CAstSourcePositionMap newPos();

  }

  private CAstNode copyNodes(CAstNode root, Map<CAstNode, CAstNode> nodeMap) {
    if (root instanceof CAstOperator) {
      nodeMap.put(root, root);
      return root;
    } else if (root.getValue() != null) {
      CAstNode copy = Ast.makeConstant( root.getValue() );
      nodeMap.put(root, copy);
      return copy;
    } else {
      CAstNode newChildren[] = new CAstNode[ root.getChildCount() ];

      for(int i = 0; i < root.getChildCount(); i++) {
	newChildren[i] = copyNodes(root.getChild(i), nodeMap);
      }

      CAstNode copy = Ast.makeNode(root.getKind(), newChildren);
      nodeMap.put(root, copy);
      return copy;
    }
  }

  private CAstControlFlowMap copyFlow(Map<CAstNode, CAstNode> nodeMap, CAstControlFlowMap orig) {
    Collection<CAstNode> oldSources = orig.getMappedNodes();
    CAstControlFlowRecorder newMap = new CAstControlFlowRecorder();
    for(Iterator<CAstNode> NS = nodeMap.keySet().iterator(); NS.hasNext(); ) {
      CAstNode old = NS.next();
      CAstNode newNode = nodeMap.get(old);
      newMap.map(newNode, newNode);
      if (oldSources.contains(old)) {
	if (orig.getTarget(old, null) != null) {
	  CAstNode oldTarget = orig.getTarget(old, null);
	  if (nodeMap.containsKey(oldTarget)) {
	    newMap.add(newNode, nodeMap.get(oldTarget), null);
	  } else {
	    newMap.add(newNode, oldTarget, null);
	  }
	}
	
	for(Iterator<IField> LS = orig.getTargetLabels(old).iterator(); LS.hasNext(); ) {
	  Object label = LS.next();
	  CAstNode oldTarget = orig.getTarget(old, label);
	  if (nodeMap.containsKey(oldTarget)) {
	    newMap.add(newNode, nodeMap.get(oldTarget), label);
	  } else {
	    newMap.add(newNode, oldTarget, label);
	  }
	}
      }
    }

    return newMap;
  }

  private CAstSourcePositionMap 
    copySource(Map<CAstNode, CAstNode> nodeMap, CAstSourcePositionMap orig) 
  {
    if (orig == null) {
      return null;
    } else {
      CAstSourcePositionRecorder newMap = new CAstSourcePositionRecorder();
      for(Iterator<CAstNode> NS = nodeMap.keySet().iterator(); NS.hasNext(); ) {
	CAstNode old = NS.next();
	CAstNode newNode = nodeMap.get(old);
	
	if (orig.getPosition(old) != null) {
	  newMap.setPosition(newNode, orig.getPosition(old));
	}
      }

      return newMap;
    }
  }

  public Clone copy(CAstNode root, 
		    final CAstControlFlowMap cfg,
		    final CAstSourcePositionMap pos) 
  {
    final Map<CAstNode, CAstNode> nodes = new HashMap<CAstNode, CAstNode>();
    final CAstNode newRoot = copyNodes(root, nodes);
    return new Clone() {
      private CAstControlFlowMap theCfg = null;
      private CAstSourcePositionMap theSource = null;
      public CAstNode newRoot() { return newRoot; }
      public CAstControlFlowMap newCfg() { 
	if (theCfg == null) theCfg = copyFlow(nodes, cfg); 
	return theCfg;
      }
      public CAstSourcePositionMap newPos() { 
	if (theSource == null) theSource = copySource(nodes, pos); 
	return theSource;
      }
    };
  }
}
