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
package com.ibm.wala.cast.js.ipa.callgraph;

import java.util.Iterator;

import com.ibm.wala.cast.ipa.callgraph.AstCFAPointerKeys;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.propagation.ConcreteTypeKey;
import com.ibm.wala.ipa.callgraph.propagation.ConstantKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.strings.Atom;

/**
 * Common utilities for CFA-style call graph builders.
 */
public abstract class JSCFABuilder extends JSSSAPropagationCallGraphBuilder {

  public JSCFABuilder(IClassHierarchy cha, AnalysisOptions options, AnalysisCache cache) {
    super(cha, options, cache, new AstCFAPointerKeys() {

      private boolean isBogusKey(InstanceKey K) {
        TypeReference t = K.getConcreteType().getReference();
        return t == JavaScriptTypes.Null || t == JavaScriptTypes.Undefined;
      }
      
      @Override
      public PointerKey getPointerKeyForObjectCatalog(InstanceKey I) {
        if (isBogusKey(I)) {
          return null;
        } else {
          return super.getPointerKeyForObjectCatalog(I);
        }
      }

      @Override
      public PointerKey getPointerKeyForInstanceField(InstanceKey I, IField f) {
        if (isBogusKey(I)) {
          return null;
        } else {
          return super.getPointerKeyForInstanceField(I, f);
        }
      }

      @Override
      public PointerKey getPointerKeyForArrayContents(InstanceKey I) {
        if (isBogusKey(I)) {
          return null;
        } else {
          return super.getPointerKeyForArrayContents(I);
        }
      }

      @Override
      public Iterator<PointerKey> getPointerKeysForReflectedFieldRead(InstanceKey I, InstanceKey F) {
        IClassHierarchy cha = I.getConcreteType().getClassHierarchy();
        IClass function = cha.lookupClass(JavaScriptTypes.Function);
        if (isBogusKey(I)) {
          return EmptyIterator.instance();
        } else if (cha.isSubclassOf(F.getConcreteType(), function)) {
          return super.getPointerKeysForReflectedFieldRead(I, new ConcreteTypeKey(function));
        } else {
          return super.getPointerKeysForReflectedFieldRead(I, F);
        }
      }

      @Override
      public Iterator<PointerKey> getPointerKeysForReflectedFieldWrite(InstanceKey I, InstanceKey F) {
        IClassHierarchy cha = I.getConcreteType().getClassHierarchy();
        IClass function = cha.lookupClass(JavaScriptTypes.Function);
        if (isBogusKey(I)) {
          return EmptyIterator.instance();
        } else if (cha.isSubclassOf(F.getConcreteType(), function)) {
          return super.getPointerKeysForReflectedFieldWrite(I, new ConcreteTypeKey(function));
        } else {
          return super.getPointerKeysForReflectedFieldWrite(I, F);
        }
      }

      @Override
      protected PointerKey getInstanceFieldPointerKeyForConstant(InstanceKey I, ConstantKey F) {
        Object v = F.getValue();
        if (v instanceof Double) {
          String strVal = simulateNumberToString((Double)v);
          IField f = I.getConcreteType().getField(Atom.findOrCreateUnicodeAtom((String) strVal));
          return getPointerKeyForInstanceField(I, f);
          
        } else {
          return super.getInstanceFieldPointerKeyForConstant(I, F);
        }
      }

      private String simulateNumberToString(Double v) {
        // TODO this is very incomplete  --MS
        String result = v.toString();
        if (result.endsWith(".0")) {
          result = result.substring(0, result.length() - 2);
        }
        return result;
      }
      
      
    });
  }

}
