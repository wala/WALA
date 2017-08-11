/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.callgraph.propagation;

import java.util.Comparator;

import com.ibm.wala.classLoader.ArrayClass;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ExceptionReturnValueKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;

public class PointerKeyComparator implements Comparator {

  private final IClassHierarchy cha;

  public PointerKeyComparator(IClassHierarchy cha) {
    if (cha == null) {
      throw new IllegalArgumentException("null cha");
    }
    this.cha = cha;
  }

  protected int comparePrimitives(TypeReference r1, TypeReference r2) {
    int h1 = r1.hashCode();
    int h2 = r2.hashCode();
    if (h1 != h2)
      return h1-h2;
    else {
      assert r1 == r2;
      return 0;
    }
  }

  protected int compareConcreteTypes(IClass k1, IClass k2) {
    int n1 = cha.getNumber( k1 );
    int n2 = cha.getNumber( k2 );
    if (n1 != n2)
      return n1-n2;
    else {
      int s1 = k1.hashCode();
      int s2 = k2.hashCode();
      assert k1 == k2 || s1 != s2;
      return s1-s2;
    }
  }

  protected int compareInstanceKeys(InstanceKey k1, InstanceKey k2) {
    return compareConcreteTypes( k1.getConcreteType(), k2.getConcreteType() );
  }
    
  protected int compareFields(IField if1, IField if2) {
    int f1 = if1.hashCode();
    int f2 = if2.hashCode();
    if (f1 != f2)
      return f1-f2;
    else {
      assert if1 == if2;
      return 0;
    }
  }
    
  private static int compareLocalKey(LocalPointerKey key1, Object key2) {
    if (key2 instanceof LocalPointerKey) {
      int l1 = key1.getValueNumber();
      int l2 = ((LocalPointerKey)key2).getValueNumber();
      if (l1 != l2)
	return l1-l2;
      else {
	int n1 = key1.getNode().getGraphNodeId();
	int n2 = ((LocalPointerKey)key2).getNode().getGraphNodeId();
	if (n1 != n2)
	  return n1-n2;
	else {
	  assert key1.equals(key2);
	  return 0;
	}
      }
    }
    
    else return -1;
  }

  private static int compareReturnValueKey(ReturnValueKey key1, Object key2) {
    if (key2 instanceof ReturnValueKey) {
      int n1 = key1.getNode().getGraphNodeId();
      int n2 = ((ReturnValueKey)key2).getNode().getGraphNodeId();
      if (n1 != n2)
	return n1-n2;
      else {
	assert key1.equals(key2);
	return 0;
      }
    }

    else return -1;
  }

  private static int compareExceptionKey(ExceptionReturnValueKey key1, Object key2) {
    if (key2 instanceof ExceptionReturnValueKey) {
      int n1 = key1.getNode().getGraphNodeId();
      int n2 = ((ExceptionReturnValueKey)key2).getNode().getGraphNodeId();
      if (n1 != n2)
	return n1-n2;
      else {
	assert key1.equals(key2);
	return 0;
      }
    }

    else return -1;
  }

  private int compareFieldKey(InstanceFieldKey key1, Object key2) {
    if (key2 instanceof InstanceFieldKey) {
      int r1 = compareInstanceKeys(key1.getInstanceKey(), ((InstanceFieldKey)key2).getInstanceKey());
      if (r1 != 0)
	return r1;
      else {
	return compareFields(key1.getField(), ((InstanceFieldKey)key2).getField());
      }
    }
    
    else
      return -1;
  }

  private int compareStaticKey(StaticFieldKey key1, Object key2) {
    if (key2 instanceof StaticFieldKey) {
      int n1 = cha.getNumber( key1.getField().getDeclaringClass() );
      int n2 = cha.getNumber( ((StaticFieldKey)key2).getField().getDeclaringClass() );
      if (n1 != n2)
	return n1-n2;
      else {
	return compareFields(key1.getField(), ((StaticFieldKey)key2).getField());
      }
    }
    
    else
	return -1;
  }

  private int compareArrayKey(ArrayContentsKey key1, Object key2) {
    if (key2 instanceof ArrayContentsKey) {
      ArrayClass k1 = (ArrayClass)key1.getInstanceKey().getConcreteType();
      ArrayClass k2 = (ArrayClass)((ArrayContentsKey)key2).getInstanceKey().getConcreteType();
      int d1 = k1.getDimensionality();
      int d2 = k2.getDimensionality();
      if (d1 != d2) {
	return d1-d2;
      } else if (k1.getInnermostElementClass() == null) {
	if (k2.getInnermostElementClass() == null)
	  return 
	    comparePrimitives(
	      k1.getReference().getInnermostElementType(),
	      k2.getReference().getInnermostElementType());
	else
	  return -1;
      } else if (k2.getInnermostElementClass() == null) {
	return 1;
      } else {
        return
	  compareConcreteTypes(
	    k1.getInnermostElementClass(), 
	    k2.getInnermostElementClass());
      }
    }
    
    else
      return -1;
  }

  @Override
  public int compare(Object key1, Object key2) {
    if (key1 == key2) return 0;

    else if (key1 instanceof LocalPointerKey) {
      return compareLocalKey((LocalPointerKey) key1, key2);
    }

    else if (key2 instanceof LocalPointerKey) {
      return -1*compareLocalKey((LocalPointerKey) key2, key1);
    }

    // at this point, neither key is local
    else if (key1 instanceof ReturnValueKey) {
      return compareReturnValueKey((ReturnValueKey)key1, key2);
    }

    else if (key2 instanceof ReturnValueKey) {
      return -1*compareReturnValueKey((ReturnValueKey)key2, key1);
    }

    // at this point, neither key is local or retval
    else if (key1 instanceof ExceptionReturnValueKey) {
      return compareExceptionKey((ExceptionReturnValueKey)key1, key2);
    }

    else if (key2 instanceof ExceptionReturnValueKey) {
      return -1*compareExceptionKey((ExceptionReturnValueKey)key2, key1);
    }

    // at this point, neither key is local or retval, expretval
    else if (key1 instanceof InstanceFieldKey) {
      return compareFieldKey((InstanceFieldKey)key1, key2);
    }

    else if (key2 instanceof InstanceFieldKey) {
      return -1*compareFieldKey((InstanceFieldKey)key2, key1);
    }

    // at this point, neither key is local or retval, expretval, field
    else if (key1 instanceof StaticFieldKey) {
      return compareStaticKey((StaticFieldKey)key1, key2);
    }

    else if (key2 instanceof StaticFieldKey) {
      return -1*compareStaticKey((StaticFieldKey)key2, key1);
    }

    // at this point, neither key is local or retval, expretval, field, static
    else if (key1 instanceof ArrayContentsKey) {
      return compareArrayKey((ArrayContentsKey)key1, key2);
    }

    else if (key2 instanceof ArrayContentsKey) {
      return -1*compareArrayKey((ArrayContentsKey)key2, key1);
    }

    else {
      return compareOtherKeys(key1, key2);
    }
  }

  protected int compareOtherKeys(Object key1, Object key2) {
    System.err.println("Cannot compare " + key1 + " and " + key2);
    Assertions.UNREACHABLE();
    return 0;
  }

  @Override
  public boolean equals(Object o) {
    return (o instanceof PointerKeyComparator) &&
	((PointerKeyComparator)o).cha.equals(cha);
  }

  @Override
  public int hashCode() {
    return cha.hashCode();
  }

}
