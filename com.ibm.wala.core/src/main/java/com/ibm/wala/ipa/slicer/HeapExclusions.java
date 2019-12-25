/*
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.ipa.slicer;

import com.ibm.wala.ipa.callgraph.propagation.AbstractFieldPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.StaticFieldKey;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.config.SetOfClasses;
import com.ibm.wala.util.debug.Assertions;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/** heap locations that should be excluded from data dependence during slicing */
public class HeapExclusions {

  private static final boolean VERBOSE = false;

  /** used only for verbose processing. */
  private static final Collection<TypeReference> considered = HashSetFactory.make();

  private final SetOfClasses set;

  public HeapExclusions(SetOfClasses set) {
    this.set = set;
  }

  /**
   * @return the PointerKeys that are not excluded
   * @throws IllegalArgumentException if s is null
   */
  public Set<PointerKey> filter(Collection<PointerKey> s) {
    if (s == null) {
      throw new IllegalArgumentException("s is null");
    }
    HashSet<PointerKey> result = HashSetFactory.make();
    for (PointerKey p : s) {
      if (p instanceof AbstractFieldPointerKey) {
        AbstractFieldPointerKey f = (AbstractFieldPointerKey) p;
        if (f.getInstanceKey().getConcreteType() != null) {
          if (!set.contains(
              f.getInstanceKey()
                  .getConcreteType()
                  .getReference()
                  .getName()
                  .toString()
                  .substring(1))) {
            result.add(p);
            if (VERBOSE) {
              verboseAction(p);
            }
          } else {
            // do nothing
          }
        }
      } else if (p instanceof StaticFieldKey) {
        StaticFieldKey sf = (StaticFieldKey) p;
        if (!set.contains(
            sf.getField().getDeclaringClass().getReference().getName().toString().substring(1))) {
          result.add(p);
          if (VERBOSE) {
            verboseAction(p);
          }
        } else {
          // do nothing
        }
      } else {
        Assertions.UNREACHABLE(s.getClass().toString());
      }
    }
    return result;
  }

  private static void verboseAction(PointerKey p) {
    if (p instanceof AbstractFieldPointerKey) {
      AbstractFieldPointerKey f = (AbstractFieldPointerKey) p;
      if (f.getInstanceKey().getConcreteType() != null) {
        TypeReference t = f.getInstanceKey().getConcreteType().getReference();
        if (!considered.contains(t)) {
          considered.add(t);
          System.err.println("Considered " + t);
        }
      }
    } else if (p instanceof StaticFieldKey) {
      StaticFieldKey sf = (StaticFieldKey) p;
      TypeReference t = sf.getField().getDeclaringClass().getReference();
      if (!considered.contains(t)) {
        considered.add(t);
        System.err.println("Considered " + t);
      }
    }
  }

  public boolean excludes(PointerKey pk) {
    TypeReference t = getType(pk);
    return (t == null) ? false : set.contains(t.getName().toString().substring(1));
  }

  public static TypeReference getType(PointerKey pk) {
    if (pk instanceof AbstractFieldPointerKey) {
      AbstractFieldPointerKey f = (AbstractFieldPointerKey) pk;
      if (f.getInstanceKey().getConcreteType() != null) {
        return f.getInstanceKey().getConcreteType().getReference();
      }
    } else if (pk instanceof StaticFieldKey) {
      StaticFieldKey sf = (StaticFieldKey) pk;
      return sf.getField().getDeclaringClass().getReference();
    }
    return null;
  }
}
