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
import com.ibm.wala.cast.ipa.callgraph.ReflectedFieldPointerKey;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
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

  public JSCFABuilder(IClassHierarchy cha, AnalysisOptions options, IAnalysisCacheView cache) {
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
        if (isBogusKey(I)) {
          return EmptyIterator.instance();
        } else {
          return super.getPointerKeysForReflectedFieldRead(I, F);
        }
      }

      @Override
      public Iterator<PointerKey> getPointerKeysForReflectedFieldWrite(InstanceKey I, InstanceKey F) {
         if (isBogusKey(I)) {
          return EmptyIterator.instance();
        } else {
          return super.getPointerKeysForReflectedFieldWrite(I, F);
        }
      }

      @Override
      protected PointerKey getInstanceFieldPointerKeyForConstant(InstanceKey I, ConstantKey F) {
        Object v = F.getValue();
        String strVal = JSCallGraphUtil.simulateToStringForPropertyNames(v);
        // if we know the string representation of the constant, use it...
        if (strVal != null) {
          IField f = I.getConcreteType().getField(Atom.findOrCreateUnicodeAtom(strVal));
          return getPointerKeyForInstanceField(I, f);
          
        // ...otherwise it is some unknown string
        } else {
          return ReflectedFieldPointerKey.mapped(new ConcreteTypeKey(getFieldNameType(F)), I);
        }
      }

      /**
       * All values used as property names get implicitly converted to strings in JavaScript.
       * @see com.ibm.wala.cast.ipa.callgraph.DelegatingAstPointerKeys#getFieldNameType(com.ibm.wala.ipa.callgraph.propagation.InstanceKey)
       */
      @Override
      protected IClass getFieldNameType(InstanceKey F) {
        return F.getConcreteType().getClassHierarchy().lookupClass(JavaScriptTypes.String);
      }      
    });
  }

}
