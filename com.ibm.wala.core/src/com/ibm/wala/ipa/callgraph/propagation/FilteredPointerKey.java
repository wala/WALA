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

import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.util.intset.*;

/**
 *
 * A PointerKey which carries a type filter, used during pointer analysis
 * 
 * @author sfink
 */
public interface FilteredPointerKey extends PointerKey {
    
    public interface TypeFilter extends ContextItem {

      boolean addFiltered(PropagationSystem system, PointsToSetVariable L, PointsToSetVariable R);

      boolean addInverseFiltered(PropagationSystem system, PointsToSetVariable L, PointsToSetVariable R);

    }

    public class SingleClassFilter implements TypeFilter {
      private final IClass concreteType;

      public SingleClassFilter(IClass concreteType) {
	this.concreteType = concreteType;
      }

      public String toString() {
	return "SingleClassFilter: " + concreteType;
      }

      public IClass getConcreteType() {
	return concreteType;
      }

      public int hashCode() {
	return concreteType.hashCode();
      }

      public boolean equals(Object o) {
	return
	  (o instanceof SingleClassFilter) 
	                  &&
	  ((SingleClassFilter)o).getConcreteType().equals(concreteType);
      }

      public boolean addFiltered(PropagationSystem system, PointsToSetVariable L, PointsToSetVariable R) {
	IntSet f = system.getInstanceKeysForClass(concreteType);
        return (f == null) ? false : L.addAllInIntersection(R, f);
      }

      public boolean addInverseFiltered(PropagationSystem system, PointsToSetVariable L, PointsToSetVariable R) {
	IntSet f = system.getInstanceKeysForClass(concreteType);

	// SJF: this is horribly inefficient. we really don't want to do 
	// diffs in here. TODO: fix it. probably keep not(f) cached and 
	// use addAllInIntersection
	return (f == null) ? L.addAll(R) : L.addAll(IntSetUtil.diff(R.getValue(), f));
      }
    }

    public class SingleInstanceFilter implements TypeFilter {
      private final InstanceKey concreteType;

      public SingleInstanceFilter(InstanceKey concreteType) {
	this.concreteType = concreteType;
      }

      public String toString() {
	return "SingleInstanceFilter: " + concreteType + 
	       " (" + concreteType.getClass() + ")";
      }

      public InstanceKey getInstance() {
	return concreteType;
      }

      public int hashCode() {
	return concreteType.hashCode();
      }

      public boolean equals(Object o) {
	return
	  (o instanceof SingleInstanceFilter) 
	                  &&
	  ((SingleInstanceFilter)o).getInstance().equals(concreteType);
      }

      public boolean addFiltered(PropagationSystem system, PointsToSetVariable L, PointsToSetVariable R) {
	int idx = system.findOrCreateIndexForInstanceKey(concreteType);
        if (R.contains(idx)) {
          if (!L.contains(idx)) {
            L.add(idx);
	    return true;
          }
        }

	return false;
      }

      public boolean addInverseFiltered(PropagationSystem system, PointsToSetVariable L, PointsToSetVariable R) {
	int idx = system.findOrCreateIndexForInstanceKey(concreteType);
        if (!R.contains(idx) || L.contains(idx)) {
	  return L.addAll(R);
	} else {
	  MutableIntSet copy = IntSetUtil. makeMutableCopy(R.getValue());
	  copy.remove(idx);
	  return L.addAll(copy);
	}
      }
    }

    public class TargetMethodFilter implements TypeFilter {
      private final IMethod targetMethod;

      public TargetMethodFilter(IMethod targetMethod) {
	this.targetMethod = targetMethod;
      }

      public String toString() {
	return "TargetMethodFilter: " + targetMethod;
      }

      public IMethod getMethod() {
	return targetMethod;
      }

      public int hashCode() {
	return targetMethod.hashCode();
      }

      public boolean equals(Object o) {
	return
	  (o instanceof TargetMethodFilter) 
	                  &&
	  ((TargetMethodFilter)o).getMethod().equals(targetMethod);
      }

      private class UpdateAction implements IntSetAction {
	private boolean result = false;
	  
	private final PointsToSetVariable L;
	private final PropagationSystem system;
	private final boolean sense;

	private UpdateAction(PropagationSystem system,
			     PointsToSetVariable L, 
			     boolean sense) 
	{
	  this.L = L;
	  this.sense = sense;
	  this.system = system;
	}

	public void act(int i) {
	  InstanceKey I = system.getInstanceKey(i);
	  IClass C = I.getConcreteType();
	  if ((C.getMethod(targetMethod.getSelector())==targetMethod)==sense) {
	    if (! L.contains(i)) {
	      result = true;
	      L.add(i);
	    }
	  }
	}
      }

      public boolean addFiltered(PropagationSystem system, PointsToSetVariable L, PointsToSetVariable R) {
	if (R.getValue() == null) {
	  return false;
	} else {  
	  UpdateAction act = new UpdateAction(system, L, true);
	  R.getValue().foreach(act);
	  return act.result;
	}
      }

      public boolean addInverseFiltered(PropagationSystem system, PointsToSetVariable L, PointsToSetVariable R) {
	if (R.getValue() == null) {
	  return false;
	} else {  
	  UpdateAction act = new UpdateAction(system, L, false);
	  R.getValue().foreach(act);
	  return act.result;
	}
      }
    }

    /**
     * @return the class which should govern filtering of instances to
     * which this pointer points, or null if no filtering needed
     */
    public TypeFilter getTypeFilter();
}
