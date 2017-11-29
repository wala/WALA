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

import com.ibm.wala.cast.tree.CAstNode;

/**
 * An implementation of CAst, i.e. a simple factory for creating capa
 * ast nodes.  This class simply creates generic nodes with a kind
 * field, and either an array of children.  Note that there is no easy
 * way to mutate these trees; do not changes this (see CAstNode for
 * the rationale for this rule).
 *
 * @author Julian Dolby (dolby@us.ibm.com)
 *
 */
public class CAstValueImpl extends CAstImpl {

  protected static class CAstNodeValueImpl extends CAstNodeImpl {
      
    protected CAstNodeValueImpl(int kind, CAstNode cs[]) {
      super(kind, cs);
    }

    @Override
    public int hashCode() {
      int value = 1237 * kind;
      for (CAstNode element : cs)
        value *= element.hashCode();

      return value;
    }

    @Override
    public boolean equals(Object o) {
      if (! (o instanceof CAstNode)) return false;
      if (kind != ((CAstNode)o).getKind()) return false;
      if (((CAstNode)o).getChildCount() != cs.length) return false;
      for(int i = 0; i < cs.length; i++)
	if (! cs[i].equals(((CAstNode)o).getChild(i)))
	  return false;

      return true;
    }
  }
  
  @Override
  public CAstNode makeNode(final int kind, final CAstNode[] cs) {
    return new CAstNodeValueImpl(kind, cs);
  }

  protected static class CAstValueValueImpl extends CAstValueImpl {

    protected CAstValueValueImpl(Object value) {
      super(value);
    }

    @Override
    public int hashCode() { 
      return value.hashCode(); 
    }

    @Override
    public boolean equals(Object o) {
      if (o instanceof CAstNode) {
	return value==null? 
	    ((CAstNode)o).getValue()==null:
	    value.equals(((CAstNode)o).getValue());
      } else {
	return false;
      }
    }
  }

  @Override
  public CAstNode makeConstant(final Object value) {
    return new CAstValueValueImpl(value);
  }

}
