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

import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.fixpoint.IntSetVariable;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetAction;
import com.ibm.wala.util.intset.MutableMapping;
import com.ibm.wala.util.intset.MutableSparseIntSet;

/**
 * @author sfink
 * 
 */
public class PointsToSetVariable extends IntSetVariable<PointsToSetVariable> {

  /**
   * if set, check that all instance keys in a points-to set are consistent with the type of the corresponding pointer
   * key
   */
  public static final boolean PARANOID = false;

  /**
   * used only for paranoid checking. a bit ugly, but avoids adding an instance field just for debugging
   */
  public static MutableMapping<InstanceKey> instanceKeys = null;

  private PointerKey pointerKey;

  public PointsToSetVariable(PointerKey key) {
    super();
    if (Assertions.verifyAssertions) {
      Assertions._assert(key != null);
    }
    this.pointerKey = key;
  }

  /**
   * @return Returns the pointerKey.
   */
  public PointerKey getPointerKey() {
    return pointerKey;
  }

  @Override
  public boolean equals(Object obj) {
    if (obj instanceof PointsToSetVariable) {
      return pointerKey.equals(((PointsToSetVariable) obj).pointerKey);
    } else {
      return false;
    }
  }

  @Override
  public void add(int b) {
    if (PARANOID) {
      MutableSparseIntSet m = new MutableSparseIntSet(1);
      m.add(b);
      checkTypes(m);
    }
    super.add(b);
  }

  @Override
  public boolean addAll(IntSet B) {
    if (PARANOID) {
      checkTypes(B);
    }
    return super.addAll(B);
  }

  /**
   * check that the types of all instance keys are assignable to declared type of pointer key
   */
  private void checkTypes(IntSet b) {
    assert PARANOID;
    if (b == null)
      return;
    if (!(pointerKey instanceof LocalPointerKey)) {
      return;
    }
    final LocalPointerKey lpk = (LocalPointerKey) pointerKey;
    CGNode node = lpk.getNode();
    final IClassHierarchy cha = node.getClassHierarchy();
    final IR ir = node.getIR();
    if (ir == null)
      return;
    TypeInference ti = TypeInference.make(ir, false);
    final IClass type = ti.getType(lpk.getValueNumber()).getType();
    if (type == null)
      return;
    // don't perform checking for exception variables
    if (cha.isAssignableFrom(cha.lookupClass(TypeReference.JavaLangThrowable), type)) {
      return;
    }
    b.foreach(new IntSetAction() {

      public void act(int x) {
        InstanceKey ik = instanceKeys.getMappedObject(x);
        IClass concreteType = ik.getConcreteType();
        if (!cha.isAssignableFrom(type, concreteType)) {
          System.err.println("BOOM");
          System.err.println(ir);
          System.err.println(lpk + " type " + type);
          System.err.println(ik + " type " + concreteType);
          Assertions.UNREACHABLE();
        }
      }

    });
  }

  @Override
  public boolean addAll(PointsToSetVariable other) {
    if (PARANOID) {
      checkTypes(other.getValue());
    }
    // TODO Auto-generated method stub
    return super.addAll(other);
  }

  /**
   * Use this with extreme care, to add filters to this variable..
   * 
   * @param pointerKey The pointerKey to set.
   */
  void setPointerKey(PointerKey pointerKey) {
    // check that we haven't modified the hash code!!! this is crucial
    if (Assertions.verifyAssertions) {
      Assertions._assert(this.pointerKey.hashCode() == pointerKey.hashCode());
    }
    this.pointerKey = pointerKey;
  }

  @Override
  public String toString() {
    return pointerKey.toString() + ":" + super.toString();
  }
}
