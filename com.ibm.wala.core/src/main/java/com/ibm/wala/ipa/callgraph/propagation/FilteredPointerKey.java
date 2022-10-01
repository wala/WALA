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
package com.ibm.wala.ipa.callgraph.propagation;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetAction;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;
import java.util.Arrays;

/** A {@link PointerKey} which carries a type filter, used during pointer analysis */
public interface FilteredPointerKey extends PointerKey {

  public interface TypeFilter extends ContextItem {

    boolean addFiltered(PropagationSystem system, PointsToSetVariable L, PointsToSetVariable R);

    boolean addInverseFiltered(
        PropagationSystem system, PointsToSetVariable L, PointsToSetVariable R);

    boolean isRootFilter();
  }

  public class SingleClassFilter implements TypeFilter {
    private final IClass concreteType;

    public SingleClassFilter(IClass concreteType) {
      this.concreteType = concreteType;
    }

    @Override
    public String toString() {
      return "SingleClassFilter: " + concreteType;
    }

    public IClass getConcreteType() {
      return concreteType;
    }

    @Override
    public int hashCode() {
      return concreteType.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      return (o instanceof SingleClassFilter)
          && ((SingleClassFilter) o).getConcreteType().equals(concreteType);
    }

    @Override
    public boolean addFiltered(
        PropagationSystem system, PointsToSetVariable L, PointsToSetVariable R) {
      IntSet f = system.getInstanceKeysForClass(concreteType);
      return (f == null) ? false : L.addAllInIntersection(R, f);
    }

    @Override
    public boolean addInverseFiltered(
        PropagationSystem system, PointsToSetVariable L, PointsToSetVariable R) {
      IntSet f = system.getInstanceKeysForClass(concreteType);

      // SJF: this is horribly inefficient. we really don't want to do
      // diffs in here. TODO: fix it. probably keep not(f) cached and
      // use addAllInIntersection
      return (f == null) ? L.addAll(R) : L.addAll(IntSetUtil.diff(R.getValue(), f));
    }

    @Override
    public boolean isRootFilter() {
      return concreteType.equals(concreteType.getClassHierarchy().getRootClass());
    }
  }

  public class MultipleClassesFilter implements TypeFilter {
    private final IClass[] concreteType;

    public MultipleClassesFilter(IClass[] concreteType) {
      this.concreteType = concreteType;
    }

    @Override
    public String toString() {
      return "MultipleClassesFilter: " + Arrays.toString(concreteType);
    }

    public IClass[] getConcreteTypes() {
      return concreteType;
    }

    @Override
    public int hashCode() {
      return concreteType[0].hashCode();
    }

    @Override
    public boolean equals(Object o) {
      if (!(o instanceof MultipleClassesFilter)) {
        return false;
      }

      MultipleClassesFilter f = (MultipleClassesFilter) o;

      if (concreteType.length != f.concreteType.length) {
        return false;
      }

      for (int i = 0; i < concreteType.length; i++) {
        if (!concreteType[i].equals(f.concreteType[i])) {
          return false;
        }
      }

      return true;
    }

    private IntSet bits(PropagationSystem system) {
      IntSet f = null;
      for (IClass cls : concreteType) {
        if (f == null) {
          f = system.getInstanceKeysForClass(cls);
        } else {
          f = f.union(system.getInstanceKeysForClass(cls));
        }
      }
      return f;
    }

    @Override
    public boolean addFiltered(
        PropagationSystem system, PointsToSetVariable L, PointsToSetVariable R) {
      IntSet f = bits(system);
      return (f == null) ? false : L.addAllInIntersection(R, f);
    }

    @Override
    public boolean addInverseFiltered(
        PropagationSystem system, PointsToSetVariable L, PointsToSetVariable R) {
      IntSet f = bits(system);

      // SJF: this is horribly inefficient. we really don't want to do
      // diffs in here. TODO: fix it. probably keep not(f) cached and
      // use addAllInIntersection
      return (f == null) ? L.addAll(R) : L.addAll(IntSetUtil.diff(R.getValue(), f));
    }

    @Override
    public boolean isRootFilter() {
      return concreteType.length == 1
          && concreteType[0].getClassHierarchy().getRootClass().equals(concreteType[0]);
    }
  }

  public class SingleInstanceFilter implements TypeFilter {
    private final InstanceKey concreteType;

    public SingleInstanceFilter(InstanceKey concreteType) {
      this.concreteType = concreteType;
    }

    @Override
    public String toString() {
      return "SingleInstanceFilter: " + concreteType + " (" + concreteType.getClass() + ')';
    }

    public InstanceKey getInstance() {
      return concreteType;
    }

    @Override
    public int hashCode() {
      return concreteType.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      return (o instanceof SingleInstanceFilter)
          && ((SingleInstanceFilter) o).getInstance().equals(concreteType);
    }

    @Override
    public boolean addFiltered(
        PropagationSystem system, PointsToSetVariable L, PointsToSetVariable R) {
      int idx = system.findOrCreateIndexForInstanceKey(concreteType);
      if (R.contains(idx)) {
        return L.add(idx);
      }

      return false;
    }

    @Override
    public boolean addInverseFiltered(
        PropagationSystem system, PointsToSetVariable L, PointsToSetVariable R) {
      int idx = system.findOrCreateIndexForInstanceKey(concreteType);
      if (!R.contains(idx) || L.contains(idx)) {
        return L.addAll(R);
      } else {
        MutableIntSet copy = IntSetUtil.makeMutableCopy(R.getValue());
        copy.remove(idx);
        return L.addAll(copy);
      }
    }

    @Override
    public boolean isRootFilter() {
      return false;
    }
  }

  public class TargetMethodFilter implements TypeFilter {
    private final IMethod targetMethod;

    public TargetMethodFilter(IMethod targetMethod) {
      this.targetMethod = targetMethod;
    }

    @Override
    public String toString() {
      return "TargetMethodFilter: " + targetMethod;
    }

    public IMethod getMethod() {
      return targetMethod;
    }

    @Override
    public int hashCode() {
      return targetMethod.hashCode();
    }

    @Override
    public boolean equals(Object o) {
      return (o instanceof TargetMethodFilter)
          && ((TargetMethodFilter) o).getMethod().equals(targetMethod);
    }

    private class UpdateAction implements IntSetAction {
      private boolean result = false;

      private final PointsToSetVariable L;

      private final PropagationSystem system;

      private final boolean sense;

      private UpdateAction(PropagationSystem system, PointsToSetVariable L, boolean sense) {
        this.L = L;
        this.sense = sense;
        this.system = system;
      }

      @Override
      public void act(int i) {
        InstanceKey I = system.getInstanceKey(i);
        IClass C = I.getConcreteType();
        if ((C.getMethod(targetMethod.getSelector()) == targetMethod) == sense) {
          if (L.add(i)) {
            result = true;
          }
        }
      }
    }

    @Override
    public boolean addFiltered(
        PropagationSystem system, PointsToSetVariable L, PointsToSetVariable R) {
      if (R.getValue() == null) {
        return false;
      } else {
        UpdateAction act = new UpdateAction(system, L, true);
        R.getValue().foreach(act);
        return act.result;
      }
    }

    @Override
    public boolean addInverseFiltered(
        PropagationSystem system, PointsToSetVariable L, PointsToSetVariable R) {
      if (R.getValue() == null) {
        return false;
      } else {
        UpdateAction act = new UpdateAction(system, L, false);
        R.getValue().foreach(act);
        return act.result;
      }
    }

    @Override
    public boolean isRootFilter() {
      return false;
    }
  }

  /**
   * @return the class which should govern filtering of instances to which this pointer points, or
   *     null if no filtering needed
   */
  public TypeFilter getTypeFilter();
}
