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
package com.ibm.wala.j2ee;

import com.ibm.wala.analysis.reflection.JavaTypeContext;
import com.ibm.wala.analysis.typeInference.TypeAbstraction;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.j2ee.util.ReceiverTypeInference;
import com.ibm.wala.j2ee.util.ReceiverTypeInferenceCache;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MemberReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.intset.EmptyIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.strings.Atom;

/**
 * This class provides context selection logic for special J2EE methods.
 */
public class J2EEContextSelector implements ContextSelector {

  private static final TypeName CacheableCommandImpl = TypeName.string2TypeName("Lcom/ibm/websphere/command/CacheableCommandImpl");

  private static final Atom ExecuteAtom = Atom.findOrCreateAsciiAtom("execute");

  private final static Descriptor ExecuteDesc = Descriptor.findOrCreateUTF8("()V");

  private final static TypeReference CacheableCommandImplClass = TypeReference.findOrCreate(ClassLoaderReference.Extension,
      CacheableCommandImpl);

  public final static MemberReference ExecuteMethod = MethodReference.findOrCreate(CacheableCommandImplClass, ExecuteAtom,
      ExecuteDesc);

  private final ReceiverTypeInferenceCache typeInference;

  public J2EEContextSelector(ReceiverTypeInferenceCache typeInference) {
    this.typeInference = typeInference;
  }

  /**
   * Analyze each call to Command.execute() in a different context
   */
  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] receiver) {
    if (callee.getReference().equals(ExecuteMethod)) {
      ReceiverTypeInference R = typeInference.findOrCreate(caller);
      if (R == null) {
        return null;
      }
      TypeAbstraction type = R.getReceiverType(site);
      if (type == null) {
        // Type inference failed; raise a severe warning
        return null;
      }
      return new JavaTypeContext(type);
    } else {
      return null;
    }
  }

  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    return EmptyIntSet.instance;
  }

}
