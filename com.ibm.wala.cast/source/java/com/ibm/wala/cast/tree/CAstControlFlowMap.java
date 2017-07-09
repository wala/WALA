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
package com.ibm.wala.cast.tree;

import java.util.Collection;

import com.ibm.wala.util.debug.Assertions;

/**
 * The control flow information for the CAPA AST of a particular entity. An ast
 * may contain various nodes that pertain to control flow---such as gotos,
 * branches, exceptions and so on---and this map denotes the target ast nodes of
 * ast nodes that are control flow instructions. The label is fairly
 * arbitrary---it will depend on the language, producers and consumers of the
 * tree---but is generally expected to be things like case labels, exception
 * types, conditional outcomes and so on.
 * 
 * @author Julian Dolby (dolby@us.ibm.com)
 * 
 */
public interface CAstControlFlowMap {

  /**
   * A distinguished label that means this control flow is the default target of
   * a switch (or case) statement as found in many procedural languages.
   */
  public static final Object SWITCH_DEFAULT = new Object();

  /**
   * A distinguished target that means this control flow is the target of an
   * uncaught exception.
   */
  public static final CAstNode EXCEPTION_TO_EXIT = new CAstNode() {
    @Override
    public int getKind() {
      return CAstNode.CONSTANT;
    }

    @Override
    public Object getValue() {
      return this;
    }

    @Override
    public CAstNode getChild(int n) {
      Assertions.UNREACHABLE();
      return null;
    }

    @Override
    public int getChildCount() {
      return 0;
    }

    @Override
    public String toString() {
      return "EXCEPTION_TO_EXIT";
    }

    @Override
    public int hashCode() {
      return getKind() * toString().hashCode();
    }

    @Override
    public boolean equals(Object o) {
      return o == this;
    }
  };

  /**
   * Return the target ast node of the control-flow instruction denoted by from
   * with respect to the given label.
   */
  CAstNode getTarget(CAstNode from, Object label);

  /**
   * Return a collection of all labels for which the control-flow ast node
   * <code>from</code> has a target.
   */
  Collection<Object> getTargetLabels(CAstNode from);

  /**
   * Return a collection of control-flow ast nodes that have this one as a
   * possible target.
   */
  Collection<Object> getSourceNodes(CAstNode to);

  /**
   * Returns an iterator of all CAstNodes for which this map contains control
   * flow mapping information.
   */
  Collection<CAstNode> getMappedNodes();
}
