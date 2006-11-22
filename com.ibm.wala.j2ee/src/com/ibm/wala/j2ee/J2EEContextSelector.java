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
import com.ibm.wala.analysis.typeInference.ReceiverTypeInference;
import com.ibm.wala.analysis.typeInference.ReceiverTypeInferenceCache;
import com.ibm.wala.analysis.typeInference.TypeAbstraction;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.Atom;
import com.ibm.wala.util.warnings.ResolutionFailure;
import com.ibm.wala.util.warnings.WarningSet;

/**
 * 
 * This class provides context selection logic for special J2EE methods.
 * 
 * @author sfink
 */
public class J2EEContextSelector implements ContextSelector {

  private static final TypeName CacheableCommandImpl = TypeName.string2TypeName("Lcom/ibm/websphere/command/CacheableCommandImpl");

  private static final Atom ExecuteAtom = Atom.findOrCreateAsciiAtom("execute");

  private final static Descriptor ExecuteDesc = Descriptor.findOrCreateUTF8("()V");

  private final static TypeReference CacheableCommandImplClass = TypeReference.findOrCreate(ClassLoaderReference.Extension,
      CacheableCommandImpl);

  public final static MethodReference ExecuteMethod = MethodReference.findOrCreate(CacheableCommandImplClass, ExecuteAtom,
      ExecuteDesc);

  private final ReceiverTypeInferenceCache typeInference;

  private final WarningSet warnings;

  public J2EEContextSelector(ReceiverTypeInferenceCache typeInference, WarningSet warnings) {
    this.typeInference = typeInference;
    this.warnings = warnings;
  }

  /**
   * Analyze each call to Command.execute() in a different context
   * 
   * @see com.ibm.wala.ipa.callgraph.ContextSelector#getCalleeTarget(com.ibm.wala.ipa.callgraph.CGNode,
   *      com.ibm.wala.classLoader.CallSiteReference,
   *      com.ibm.wala.classLoader.IMethod)
   */
  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey receiver) {
    if (callee.getReference().equals(ExecuteMethod)) {
      ReceiverTypeInference R = typeInference.findOrCreate(caller);
      if (R == null) {
        return null;
      }
      TypeAbstraction type = R.getReceiverType(site);
      if (type == null) {
        // Type inference failed; raise a severe warning
        warnings.add(ResolutionFailure.create(caller, site));
        return null;
      }
      return new JavaTypeContext(type);
    } else {
      return null;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.ContextSelector#getBoundOnNumberOfTargets(com.ibm.wala.ipa.callgraph.CGNode,
   *      com.ibm.wala.classLoader.CallSiteReference,
   *      com.ibm.wala.classLoader.IMethod)
   */
  public int getBoundOnNumberOfTargets(CGNode caller, CallSiteReference site, IMethod callee) {

    if (callee.getReference().equals(ExecuteMethod)) {
      return 1;
    } else {
      return -1;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.rta.RTAContextInterpreter#setWarnings(com.ibm.wala.util.warnings.WarningSet)
   */
  public void setWarnings(WarningSet newWarnings) {
    // this object is not bound to a WarningSet
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.ContextSelector#contextIsIrrelevant(com.ibm.wala.ipa.callgraph.CGNode,
   *      com.ibm.wala.classLoader.CallSiteReference)
   */
  public boolean contextIsIrrelevant(CGNode node, CallSiteReference site) {
    Atom name = site.getDeclaredTarget().getName();
    Descriptor d = site.getDeclaredTarget().getDescriptor();
    if (name.equals(ExecuteAtom) && d.equals(ExecuteDesc)) {
      return false;
    } else {
      return true;
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.ContextSelector#allSitesDispatchIdentically(com.ibm.wala.types.MethodReference)
   */
  public boolean allSitesDispatchIdentically(CGNode node, CallSiteReference site) {
//    Atom name = site.getDeclaredTarget().getName();
//    Descriptor d = site.getDeclaredTarget().getDescriptor();
//    if (name.equals(ExecuteAtom) && d.equals(ExecuteDesc)) {
//      return false;
//    } else {
//      return true;
//    }
    // todo: fix me
    return false;
  }

  public boolean mayUnderstand(CGNode caller, CallSiteReference site, IMethod targetMethod, InstanceKey instance) {
    if (targetMethod.getReference().equals(ExecuteMethod)) {
      return true;
    } else {
      return false;
    }
  }

}
