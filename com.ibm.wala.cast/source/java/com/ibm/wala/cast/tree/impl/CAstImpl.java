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

import java.util.NoSuchElementException;

import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.util.CAstPrinter;
import com.ibm.wala.util.debug.Assertions;

/**
 *  An implementation of CAst, i.e. a simple factory for creating capa
 * ast nodes.  This class simply creates generic nodes with a kind
 * field, and either an array of children or a constant values.  Note
 * that there is no easy way to mutate these trees; do not change
 * this (see CAstNode for the rationale for this rule).
 *
 * @author Julian Dolby (dolby@us.ibm.com)
 *
 */
public class CAstImpl implements CAst {
  private int nextID= 0;

  public String makeUnique() {
    return "id" + (nextID++);
  }

  protected static class CAstNodeImpl implements CAstNode {
    protected final CAstNode[] cs;
    protected final int kind;

    protected CAstNodeImpl(int kind, CAstNode[] cs) {
      this.kind = kind;
      this.cs = cs;

      if (Assertions.verifyAssertions)
	for(int i = 0; i < cs.length; i++)
	  Assertions._assert(cs[i] != null, 
	    "argument " + i + " is null for node kind " + kind + " [" + CAstPrinter.entityKindAsString(kind) + "]");
    }

    public int getKind() {
      return kind;
    }

    public Object getValue() {
      return null;
    }

    public CAstNode getChild(int n) {
      try {
	return cs[n];
      } catch (ArrayIndexOutOfBoundsException e) {
	throw new NoSuchElementException(n + " of " + CAstPrinter.print(this));
      }
    }
      
    public int getChildCount() {
      return cs.length;
    }

    public String toString() {
	return super.toString() + ":" + CAstPrinter.print(this);
    }

    public int hashCode() {
      int code = getKind() * (getChildCount()+13);
      for(int i = 0; i < getChildCount(); i++) {
	code *= getChild(i).getKind();
      }

      return code;
    }
  }

  public CAstNode makeNode(final int kind, final CAstNode[] cs) {
    return new CAstNodeImpl(kind, cs);
  }

  public CAstNode makeNode(int kind, CAstNode c1, CAstNode[] cs) {
    CAstNode[] children = new CAstNode[ cs.length + 1 ];
    children[0] = c1;
    System.arraycopy(cs, 0, children, 1, cs.length);
    return makeNode(kind, children);
  }

  public CAstNode makeNode(int kind) {
    return makeNode(kind, new CAstNode[0]);
  }

  public CAstNode makeNode(int kind, CAstNode c1) {
    return makeNode(kind, new CAstNode[]{c1});
  }

  public CAstNode makeNode(int kind, CAstNode c1, CAstNode c2) {
    return makeNode(kind, new CAstNode[]{c1, c2});
  }

  public CAstNode makeNode(int kind, CAstNode c1, CAstNode c2, CAstNode c3) {
    return makeNode(kind, new CAstNode[]{c1, c2, c3});
  }

  public CAstNode makeNode(int kind, CAstNode c1, CAstNode c2, CAstNode c3, CAstNode c4) {
    return makeNode(kind, new CAstNode[]{c1, c2, c3, c4});
  }

  public CAstNode makeNode(int kind, CAstNode c1, CAstNode c2, CAstNode c3, CAstNode c4, CAstNode c5) {
    return makeNode(kind, new CAstNode[]{c1, c2, c3, c4, c5});
  }
    
  public CAstNode makeNode(int kind, CAstNode c1, CAstNode c2, CAstNode c3, CAstNode c4, CAstNode c5, CAstNode c6) {
    return makeNode(kind, new CAstNode[]{c1, c2, c3, c4, c5, c6});
  }

  protected static class CAstValueImpl implements CAstNode {
    protected final Object value;

    protected CAstValueImpl(Object value) {
      this.value = value;
    }

    public int getKind() {
      return CAstNode.CONSTANT;
    }

    public Object getValue() {
      return value;
    }

    public CAstNode getChild(int n) {
      throw new NoSuchElementException();
    }

    public int getChildCount() {
      return 0;
    }
    
    public String toString() {
      return "CAstValue: " + value;
    }

    public int hashCode() {
      return getKind() * toString().hashCode();
    } 
  }

  public CAstNode makeConstant(final Object value) {
    return new CAstValueImpl(value);
  }

  public CAstNode makeConstant(boolean value) {
    return makeConstant(value? Boolean.TRUE: Boolean.FALSE);
  }

  public CAstNode makeConstant(char value) {
      return makeConstant( new Character(value) );
  }

  public CAstNode makeConstant(short value) {
      return makeConstant( new Short(value) );
  }

  public CAstNode makeConstant(int value) {
    return makeConstant( new Integer(value) );
  }

  public CAstNode makeConstant(long value) {
      return makeConstant( new Long(value) );
  }

  public CAstNode makeConstant(float value) {
      return makeConstant( new Float(value) );
  }    

  public CAstNode makeConstant(double value) {
    return makeConstant( new Double(value) );
  }

}
