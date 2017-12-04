/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.analysis.reflection;

import java.util.Collection;

import com.ibm.wala.analysis.typeInference.PointType;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.ConstantKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.intset.EmptyIntSet;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;

/**
 * Produces {@link com.ibm.wala.analysis.reflection.GetMethodContext} if appropriate.
 * @author Michael Heilmann
 * @see com.ibm.wala.analysis.reflection.GetMethodContext
 * @see com.ibm.wala.analysis.reflection.GetMethodContextInterpreter
 */
public class GetMethodContextSelector implements ContextSelector {
  
  /**
   * If <tt>true</tt>, debug information is emitted.
   */
  protected static final boolean DEBUG = false;

  /**
   * whether to only follow get method calls on application classes, ignoring system ones
   */
  private final boolean applicationClassesOnly;
  
  public GetMethodContextSelector(boolean applicationClassesOnly) {
    this.applicationClassesOnly = applicationClassesOnly;
  }

  /**
   *  If
   *  <ul>
   *    <li>the {@link CallSiteReference} invokes either {@link java.lang.Class#getMethod} or {@link java.lang.Class#getDeclaredMethod},</li> 
   *    <li>and the receiver is a type constant and</li>
   *    <li>the first argument is a constant,</li>
   *  </ul>
   *  then return a {@link GetMethodContextSelector}.
   */
  @Override
  public Context getCalleeTarget(CGNode caller,CallSiteReference site,IMethod callee,InstanceKey[] receiver) {
    if (receiver != null && receiver.length > 0 && mayUnderstand(callee, receiver[0])) {
      if (DEBUG) {
        System.out.print("site := " + site + ", receiver := " + receiver[0]);
      }
      // If the first argument is a constant ...
      IR ir = caller.getIR();
      SymbolTable symbolTable = ir.getSymbolTable();
      SSAAbstractInvokeInstruction[] invokeInstructions = caller.getIR().getCalls(site);
      if (invokeInstructions.length != 1) {
        return null;
      }
      int use = invokeInstructions[0].getUse(1);
      if (symbolTable.isStringConstant(invokeInstructions[0].getUse(1))) {
        String sym = symbolTable.getStringValue(use);
        if (DEBUG) {
          System.out.println(invokeInstructions);
          System.out.println(", with constant := `" + sym + "`");
          for (InstanceKey instanceKey:receiver) {
            System.out.println(" " + instanceKey);
          }
        }
        // ... return an GetMethodContext.
        ConstantKey ck = makeConstantKey(caller.getClassHierarchy(),sym);
        if (DEBUG) {
          System.out.println(ck);
        }
        
        IClass type = getTypeConstant(receiver[0]);
        if (!applicationClassesOnly || 
            !(type.getClassLoader().getReference().equals(ClassLoaderReference.Primordial) ||
              type.getClassLoader().getReference().equals(ClassLoaderReference.Extension))) {
          return new GetMethodContext(new PointType(type),ck);
        }
      }
      if (DEBUG) {
        System.out.println(", with constant := no");
      }
      // Otherwise, return null.
      // TODO Remove this, just fall-through.
      return null;
    }
    return null;
  }

  /**
   * If <tt>instance</tt> is a {@link ConstantKey} and its value is an instance of {@link IClass},
   * return that value. Otherwise, return <tt>null</tt>.
   */
  private static IClass getTypeConstant(InstanceKey instance) {
    if (instance instanceof ConstantKey) {
      ConstantKey c = (ConstantKey) instance;
      if (c.getValue() instanceof IClass) {
        return (IClass) c.getValue();
      }
    }
    return null;
  }
  
  /**
   * Create a constant key for a string.
   * @param cha the class hierarchy
   * @param str the string
   * @return the constant key
   */
  protected static ConstantKey<String> makeConstantKey(IClassHierarchy cha,String str) {
    IClass cls = cha.lookupClass(TypeReference.JavaLangString);
    ConstantKey<String> ck = new ConstantKey<>(str,cls);
    return ck;
  }

  private static final Collection<MethodReference> UNDERSTOOD_METHOD_REFS = HashSetFactory.make();

  static {
    UNDERSTOOD_METHOD_REFS.add(GetMethodContextInterpreter.GET_METHOD);
    UNDERSTOOD_METHOD_REFS.add(GetMethodContextInterpreter.GET_DECLARED_METHOD);
  }

  /**
   * This object understands a dispatch to {@link java.lang.Class#getMethod(String, Class...)}
   * or {@link java.lang.Class#getDeclaredMethod} when the receiver is a type constant.
   */
  private static boolean mayUnderstand(IMethod targetMethod,InstanceKey instance) {
    return UNDERSTOOD_METHOD_REFS.contains(targetMethod.getReference())
        && getTypeConstant(instance) != null;
  }

  /**
   * TODO
   *  MH: Shouldn't be the first TWO parameters be relevant?
   *      Documentation is not too helpful about the implications.
   */
  private static final IntSet thisParameter = IntSetUtil.make(new int[]{0});

  @Override
  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    if (UNDERSTOOD_METHOD_REFS.contains(site.getDeclaredTarget())) {
      return thisParameter;
    } else {
      return EmptyIntSet.instance;
    }
  } 
}
