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
package com.ibm.wala.cast.ipa.callgraph;

import com.ibm.wala.classLoader.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.propagation.*;
import com.ibm.wala.util.*;
import com.ibm.wala.util.collections.*;

import java.util.*;

public class DelegatingAstPointerKeys implements AstPointerKeyFactory {
  private final PointerKeyFactory base;

  public DelegatingAstPointerKeys(PointerKeyFactory base) {
    this.base = base;
  }

  public PointerKey getPointerKeyForLocal(CGNode node, int valueNumber) {
    return base.getPointerKeyForLocal(node, valueNumber);
  }

  public FilteredPointerKey getFilteredPointerKeyForLocal(CGNode node, int valueNumber, IClass filter) {
    return base.getFilteredPointerKeyForLocal(node, valueNumber, filter);
  }

  public InstanceFilteredPointerKey getFilteredPointerKeyForLocal(CGNode node, int valueNumber, InstanceKey filter) {
    return base.getFilteredPointerKeyForLocal(node, valueNumber, filter);
  }

  public PointerKey getPointerKeyForReturnValue(CGNode node){
    return base.getPointerKeyForReturnValue(node);
  }

  public PointerKey getPointerKeyForExceptionalReturnValue(CGNode node) {
    return base.getPointerKeyForExceptionalReturnValue(node);
  }

  public PointerKey getPointerKeyForStaticField(IField f) {
    return base.getPointerKeyForStaticField(f);
  }

  public PointerKey getPointerKeyForObjectCatalog(InstanceKey I) {
    return new ObjectPropertyCatalogKey(I);
  }

  private final Map specificStringKeys = new HashMap();
//  private final Map specificIndexKeys = new HashMap();
    
  public PointerKey getPointerKeyForInstanceField(InstanceKey I, IField f) {
    PointerKey fk = base.getPointerKeyForInstanceField(I, f);
    if (! specificStringKeys.containsKey(f)) {
      specificStringKeys.put(f, new HashSet());
    }

    ((Set)specificStringKeys.get(f)).add(fk);

    return fk;
  }

  public PointerKey getPointerKeyForArrayContents(InstanceKey I) {
    return base.getPointerKeyForArrayContents(I);
  }

  public Iterator getPointerKeysForReflectedFieldRead(InstanceKey I, InstanceKey F) {
    List result = new LinkedList();

    // FIXME: current only constant string are handled
    if (F instanceof ConstantKey) {
      Object v = ((ConstantKey)F).getValue();
      if (v instanceof String) {
	IField f = 
	  I.getConcreteType().getField(Atom.findOrCreateUnicodeAtom((String)v));
	result.add( getPointerKeyForInstanceField(I, f) );
      }
    }

    result.add(ReflectedFieldPointerKey.mapped(new ConcreteTypeKey(F.getConcreteType()), I));
    
    return result.iterator();
  }

  public Iterator getPointerKeysForReflectedFieldWrite(InstanceKey I, InstanceKey F) {
    // FIXME: current only constant string are handled
    if (F instanceof ConstantKey) {
      Object v = ((ConstantKey)F).getValue();
      if (v instanceof String) {
	IField f =
	  I.getConcreteType().getField(Atom.findOrCreateUnicodeAtom((String)v));
	return new NonNullSingletonIterator(getPointerKeyForInstanceField(I, f));
      }
    }

    return new NonNullSingletonIterator(ReflectedFieldPointerKey.mapped(new ConcreteTypeKey(F.getConcreteType()), I));
  }
}
