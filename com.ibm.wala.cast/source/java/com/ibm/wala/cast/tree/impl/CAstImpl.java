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

/**
 * An implementation of CAst, i.e. a simple factory for creating capa ast nodes. This class simply creates generic nodes with a kind
 * field, and either an array of children or a constant values. Note that there is no easy way to mutate these trees; do not change
 * this (see CAstNode for the rationale for this rule).
 * 
 * @author Julian Dolby (dolby@us.ibm.com)
 * 
 */
public class CAstImpl implements CAst {
  private int nextID = 0;

  @Override
  public String makeUnique() {
    return "id" + (nextID++);
  }

  protected static class CAstNodeImpl implements CAstNode {
    protected final CAstNode[] cs;

    protected final int kind;

    protected CAstNodeImpl(int kind, CAstNode[] cs) {
      this.kind = kind;
      this.cs = cs;

      for (int i = 0; i < cs.length; i++)
        assert cs[i] != null : "argument " + i + " is null for node kind " + kind + " [" + CAstPrinter.entityKindAsString(kind)
            + "]";
    }

    @Override
    public int getKind() {
      return kind;
    }

    @Override
    public Object getValue() {
      return null;
    }

    @Override
    public CAstNode getChild(int n) {
      try {
        return cs[n];
      } catch (ArrayIndexOutOfBoundsException e) {
        throw new NoSuchElementException(n + " of " + CAstPrinter.print(this));
      }
    }

    @Override
    public int getChildCount() {
      return cs.length;
    }

    @Override
    public String toString() {
      return System.identityHashCode(this) + ":" + CAstPrinter.print(this);
    }

    @Override
    public int hashCode() {
      int code = getKind() * (getChildCount() + 13);
      for (int i = 0; i < getChildCount() && i < 15; i++) {
        if (getChild(i) != null) {
          code *= getChild(i).getKind();
        }
      }

      return code;
    }
  }

  @Override
  public CAstNode makeNode(final int kind, final CAstNode[] cs) {
    return new CAstNodeImpl(kind, cs);
  }

  @Override
  public CAstNode makeNode(int kind, CAstNode c1, CAstNode[] cs) {
    CAstNode[] children = new CAstNode[cs.length + 1];
    children[0] = c1;
    System.arraycopy(cs, 0, children, 1, cs.length);
    return makeNode(kind, children);
  }

  @Override
  public CAstNode makeNode(int kind) {
    return makeNode(kind, new CAstNode[0]);
  }

  @Override
  public CAstNode makeNode(int kind, CAstNode c1) {
    return makeNode(kind, new CAstNode[] { c1 });
  }

  @Override
  public CAstNode makeNode(int kind, CAstNode c1, CAstNode c2) {
    return makeNode(kind, new CAstNode[] { c1, c2 });
  }

  @Override
  public CAstNode makeNode(int kind, CAstNode c1, CAstNode c2, CAstNode c3) {
    return makeNode(kind, new CAstNode[] { c1, c2, c3 });
  }

  @Override
  public CAstNode makeNode(int kind, CAstNode c1, CAstNode c2, CAstNode c3, CAstNode c4) {
    return makeNode(kind, new CAstNode[] { c1, c2, c3, c4 });
  }

  @Override
  public CAstNode makeNode(int kind, CAstNode c1, CAstNode c2, CAstNode c3, CAstNode c4, CAstNode c5) {
    return makeNode(kind, new CAstNode[] { c1, c2, c3, c4, c5 });
  }

  @Override
  public CAstNode makeNode(int kind, CAstNode c1, CAstNode c2, CAstNode c3, CAstNode c4, CAstNode c5, CAstNode c6) {
    return makeNode(kind, new CAstNode[] { c1, c2, c3, c4, c5, c6 });
  }

  protected static class CAstValueImpl implements CAstNode {
    protected final Object value;

    protected CAstValueImpl(Object value) {
      this.value = value;
    }

    @Override
    public int getKind() {
      return CAstNode.CONSTANT;
    }

    @Override
    public Object getValue() {
      return value;
    }

    @Override
    public CAstNode getChild(int n) {
      throw new NoSuchElementException();
    }

    @Override
    public int getChildCount() {
      return 0;
    }

    @Override
    public String toString() {
      return "CAstValue: " + value;
    }

    @Override
    public int hashCode() {
      return getKind() * toString().hashCode();
    }
  }

  @Override
  public CAstNode makeConstant(final Object value) {
    return new CAstValueImpl(value);
  }

  @Override
  public CAstNode makeConstant(boolean value) {
    return makeConstant(value ? Boolean.TRUE : Boolean.FALSE);
  }

  @Override
  public CAstNode makeConstant(char value) {
    return makeConstant(new Character(value));
  }

  @Override
  public CAstNode makeConstant(short value) {
    return makeConstant(new Short(value));
  }

  @Override
  public CAstNode makeConstant(int value) {
    return makeConstant(new Integer(value));
  }

  @Override
  public CAstNode makeConstant(long value) {
    return makeConstant(new Long(value));
  }

  @Override
  public CAstNode makeConstant(float value) {
    return makeConstant(new Float(value));
  }

  @Override
  public CAstNode makeConstant(double value) {
    return makeConstant(new Double(value));
  }

}
