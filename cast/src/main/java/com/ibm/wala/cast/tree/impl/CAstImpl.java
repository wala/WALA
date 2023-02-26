/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.tree.impl;

import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstLeafNode;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.util.CAstPrinter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * An implementation of CAst, i.e. a simple factory for creating capa ast nodes. This class simply
 * creates generic nodes with a kind field, and either an array of children or a constant values.
 * Note that there is no easy way to mutate these trees; do not change this (see CAstNode for the
 * rationale for this rule).
 *
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public class CAstImpl implements CAst {
  private int nextID = 0;

  @Override
  public String makeUnique() {
    return "id" + nextID++;
  }

  protected static class CAstNodeImpl implements CAstNode {
    protected final List<CAstNode> cs;

    protected final int kind;

    protected CAstNodeImpl(int kind, List<CAstNode> cs) {
      this.kind = kind;
      this.cs = cs;

      for (int i = 0; i < cs.size(); i++)
        assert cs.get(i) != null
            : "argument "
                + i
                + " is null for node kind "
                + kind
                + " ["
                + CAstPrinter.entityKindAsString(kind)
                + ']';
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
    public List<CAstNode> getChildren() {
      return cs;
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
  public CAstNode makeNode(final int kind, final List<CAstNode> cs) {
    return new CAstNodeImpl(kind, cs);
  }

  @Override
  public CAstNode makeNode(int kind, CAstNode c1, CAstNode[] cs) {
    List<CAstNode> children = new ArrayList<>(cs.length + 1);
    children.add(c1);
    children.addAll(Arrays.asList(cs));
    return makeNode(kind, children);
  }

  @Override
  public CAstNode makeNode(int kind) {
    return makeNode(kind, Collections.emptyList());
  }

  @Override
  public CAstNode makeNode(int kind, CAstNode c1) {
    return makeNode(kind, Collections.singletonList(c1));
  }

  @Override
  public CAstNode makeNode(int kind, CAstNode c1, CAstNode c2) {
    return makeNode(kind, Arrays.asList(c1, c2));
  }

  @Override
  public CAstNode makeNode(int kind, CAstNode c1, CAstNode c2, CAstNode c3) {
    return makeNode(kind, Arrays.asList(c1, c2, c3));
  }

  @Override
  public CAstNode makeNode(int kind, CAstNode c1, CAstNode c2, CAstNode c3, CAstNode c4) {
    return makeNode(kind, Arrays.asList(c1, c2, c3, c4));
  }

  @Override
  public CAstNode makeNode(
      int kind, CAstNode c1, CAstNode c2, CAstNode c3, CAstNode c4, CAstNode c5) {
    return makeNode(kind, Arrays.asList(c1, c2, c3, c4, c5));
  }

  @Override
  public CAstNode makeNode(
      int kind, CAstNode c1, CAstNode c2, CAstNode c3, CAstNode c4, CAstNode c5, CAstNode c6) {
    return makeNode(kind, Arrays.asList(c1, c2, c3, c4, c5, c6));
  }

  @Override
  public CAstNode makeNode(int kind, CAstNode... cs) {
    return makeNode(kind, Arrays.asList(cs));
  }

  protected static class CAstValueImpl implements CAstLeafNode {
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
    public String toString() {
      return "CAstValue: " + value;
    }

    @Override
    public int hashCode() {
      return System.identityHashCode(this) * ((value == null) ? 1 : System.identityHashCode(value));
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
    return makeConstant(Character.valueOf(value));
  }

  @Override
  public CAstNode makeConstant(short value) {
    return makeConstant(Short.valueOf(value));
  }

  @Override
  public CAstNode makeConstant(int value) {
    return makeConstant(Integer.valueOf(value));
  }

  @Override
  public CAstNode makeConstant(long value) {
    return makeConstant(Long.valueOf(value));
  }

  @Override
  public CAstNode makeConstant(float value) {
    return makeConstant(Float.valueOf(value));
  }

  @Override
  public CAstNode makeConstant(double value) {
    return makeConstant(Double.valueOf(value));
  }
}
