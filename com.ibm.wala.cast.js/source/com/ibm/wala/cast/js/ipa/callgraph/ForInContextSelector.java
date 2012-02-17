/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.cast.js.ipa.callgraph;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import com.ibm.wala.cast.ir.ssa.AbstractReflectiveGet;
import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.ir.ssa.AstIsDefinedInstruction;
import com.ibm.wala.cast.ir.ssa.EachElementGetInstruction;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.propagation.ConcreteTypeKey;
import com.ibm.wala.ipa.callgraph.propagation.ConstantKey;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.cfa.OneLevelSiteContextSelector;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.ReflectiveMemberAccess;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;

public class ForInContextSelector implements ContextSelector {

  public static class IntegerContextItem implements ContextItem {
    private final int value;
    
    public IntegerContextItem(int value) {
      this.value = value;
    }

    public int getValue() {
      return value;
    }
    
    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + value;
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      IntegerContextItem other = (IntegerContextItem) obj;
      if (value != other.value)
        return false;
      return true;
    }
  }

  public final static ContextKey FORIN_KEY = new ContextKey() { };
  public final static ContextKey FORIN_PARM_INDEX = new ContextKey() { };
  
  public static final ContextItem FORIN_MARKER = new ContextItem() { };
  
  public static final String HACK_METHOD_STR = "_forin_body";
  
  // if this flag is set to true, functions are given ForInContexts based on their name
  // if it is false, any function that uses its first argument as a property name will be given a ForInContext
  public static final boolean USE_NAME_TO_SELECT_CONTEXT = false;

  public static boolean USE_CPA_IN_BODIES = false;
  
  public static boolean USE_1LEVEL_IN_BODIES = true;

  public static boolean DEPENDENT_THRU_READS = true;
  
  public static class SelectiveCPAContext implements Context {
      protected final Context base;
      
      private final Map<ContextKey, InstanceKey> parameterObjs;

      private final int hashCode;
      
      private static Map<ContextKey, InstanceKey> makeMap(InstanceKey[] x) {
        Map<ContextKey, InstanceKey> result = new HashMap<ContextKey, InstanceKey>();
        for(int i = 0; i < x.length; i++) {
          if (x[i] != null) {
            result.put(ContextKey.PARAMETERS[i], x[i]);
          }
        }
        return result;
      }
      
      public SelectiveCPAContext(Context base, InstanceKey[] x) {
        this(base, makeMap(x));
      }
      
      public SelectiveCPAContext(Context base, Map<ContextKey, InstanceKey> parameterObjs) {
       this.base = base;
       this.parameterObjs = parameterObjs;
       hashCode = base.hashCode() ^ parameterObjs.hashCode();
      }

      public ContextItem get(ContextKey name) {
        if (parameterObjs.containsKey(name)) {
          return new FilteredPointerKey.SingleInstanceFilter(parameterObjs.get(name));
        } else {
          return base.get(name);
        }
      }
      
      public int hashCode() {
        return hashCode;
      }

      @Override
      public boolean equals(Object other) {
        return other != null &&
            getClass().equals(other.getClass()) &&
            base.equals(((SelectiveCPAContext)other).base) &&
            parameterObjs.equals(((SelectiveCPAContext)other).parameterObjs);
      }     

  
  }
  
  public class ForInContext extends SelectiveCPAContext {
    
    ForInContext(Context base, InstanceKey obj) {
      super(base, Collections.singletonMap(ContextKey.PARAMETERS[index], obj));
    }
    
    public ContextItem get(ContextKey key) {
      if (FORIN_KEY.equals(key)) {
        return FORIN_MARKER;
      } else if(FORIN_PARM_INDEX.equals(key)) {
        return new IntegerContextItem(index);
      } else {
        return super.get(key);
      }
    }
    
    @Override
    public String toString() {
      return "for in hack filter for " + get(ContextKey.PARAMETERS[index]) + " over " + this.base;
    }
    
  }
    
  private final ContextSelector base;
  private final ContextSelector oneLevel;
  private final int index;
  
  private void collectValues(DefUse du, SSAInstruction inst, MutableIntSet values) {
    if (inst instanceof SSAGetInstruction) {
      SSAGetInstruction g = (SSAGetInstruction) inst;
      values.add(g.getRef());
      if (g.getRef() != -1) {
        collectValues(du, du.getDef(g.getRef()), values);
      }
    } else if (inst instanceof AbstractReflectiveGet) {
      AbstractReflectiveGet g = (AbstractReflectiveGet) inst;
      values.add(g.getObjectRef());
      collectValues(du, du.getDef(g.getObjectRef()), values);
      values.add(g.getMemberRef());
      collectValues(du, du.getDef(g.getMemberRef()), values);
    }
  }
  
  private IntSet identifyDependentParameters(CGNode caller, CallSiteReference site) {
    MutableIntSet dependentParameters = IntSetUtil.make();
    SSAAbstractInvokeInstruction inst = caller.getIR().getCalls(site)[0];
    DefUse du = caller.getDU();
    for(int i = 0; i < inst.getNumberOfParameters(); i++) {
      MutableIntSet values = IntSetUtil.make();
      values.add(inst.getUse(i));
      if (DEPENDENT_THRU_READS) {
        collectValues(du, du.getDef(inst.getUse(i)), values);
      }
      if (values.contains(index+1)) {
        dependentParameters.add(i);
      }
    }
    return dependentParameters;
  }
  
  public ForInContextSelector(ContextSelector base) {
    this(2, base);
  }
  
  public ForInContextSelector(int index, ContextSelector base) {
    this.index = index;
    this.base = base;
    this.oneLevel = new OneLevelSiteContextSelector(base);
  }
  
  private final HashMap<MethodReference, Boolean> forInOnFirstArg_cache = HashMapFactory.make();
  public static final HashMap<MethodReference, DefUse> du_cache = HashMapFactory.make();
  public static final IRFactory<IMethod> factory = AstIRFactory.makeDefaultFactory();
  
  // determine whether the method performs a for-in loop over the properties of its index'th argument
  private boolean forInOnFirstArg(IMethod method) {
    MethodReference mref = method.getReference();
    if(method.getNumberOfParameters() < index)
      return false;
    Boolean b = forInOnFirstArg_cache.get(mref);
    if(b != null)
      return b;
    DefUse du = getDefUse(method);
    for(SSAInstruction use : Iterator2Iterable.make(du.getUses(index+1))) {
      if(use instanceof EachElementGetInstruction) {
        forInOnFirstArg_cache.put(mref, true);
        return true;
      }
    }
    forInOnFirstArg_cache.put(mref, false);
    return false;
  }

  public static DefUse getDefUse(IMethod method) {
    MethodReference mref = method.getReference();
    DefUse du = du_cache.get(mref);
    if(du == null) {
      IR ir = factory.makeIR(method, Everywhere.EVERYWHERE, SSAOptions.defaultOptions());
      du_cache.put(mref, du = new DefUse(ir));
    }
    return du;
  }
  
  private enum Frequency { NEVER, SOMETIMES, ALWAYS };
  private final HashMap<MethodReference, Frequency> usesFirstArgAsPropertyName_cache = HashMapFactory.make();
  
  // determine whether the method never/sometimes/always uses its first argument as a property name
  private Frequency usesFirstArgAsPropertyName(IMethod method) {
    MethodReference mref = method.getReference();
    if(method.getNumberOfParameters() < index)
      return Frequency.NEVER;
    Frequency f = usesFirstArgAsPropertyName_cache.get(mref);
    if(f != null)
      return f;
    boolean usedAsPropertyName = false, usedAsSomethingElse = false;
    DefUse du = getDefUse(method);
    for(SSAInstruction use : Iterator2Iterable.make(du.getUses(index+1))) {
      if(use instanceof ReflectiveMemberAccess) {
        ReflectiveMemberAccess rma = (ReflectiveMemberAccess)use;
        if(rma.getMemberRef() == index+1) {
          usedAsPropertyName = true;
          continue;
        }
      } else if(use instanceof AstIsDefinedInstruction) {
        AstIsDefinedInstruction aidi = (AstIsDefinedInstruction)use;
        if(aidi.getNumberOfUses() > 1 && aidi.getUse(1) == index+1) {
          usedAsPropertyName = true;
          continue;
        }
      }
      usedAsSomethingElse = true;
    }
    if(!usedAsPropertyName)
      f = Frequency.NEVER;
    else if(usedAsSomethingElse)
      f = Frequency.SOMETIMES;
    else
      f = Frequency.ALWAYS;
    usesFirstArgAsPropertyName_cache.put(mref, f);
    return f;
  }

  // simulate effect of ToString conversion on key
  private InstanceKey simulateToString(IClassHierarchy cha, InstanceKey key) {
    IClass stringClass = cha.lookupClass(JavaScriptTypes.String);
    IClass numberClass = cha.lookupClass(JavaScriptTypes.Number);
    if(key instanceof ConstantKey) {
      Object value = ((ConstantKey)key).getValue();
      if(value instanceof String) {
        return key;
      } else if(value instanceof Number) {
        Double dval = ((Number)value).doubleValue();
        return new ConstantKey<Double>(dval, numberClass);
      } else if(value instanceof Boolean) {
        Boolean bval = (Boolean)value;
        return new ConstantKey<String>(bval.toString(), stringClass);
      } else if(value == null) {
        return new ConstantKey<String>("null", stringClass);
      }
    } /*else if(key != null && key.getConcreteType() == numberClass) {
      return key;
    }*/
    return new ConcreteTypeKey(stringClass);    
  }
  
  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, final InstanceKey[] receiver) {
    Context baseContext = base.getCalleeTarget(caller, site, callee, receiver);
    String calleeFullName = callee.getDeclaringClass().getName().toString();
    String calleeShortName = calleeFullName.substring(calleeFullName.lastIndexOf('/')+1);
    if(USE_NAME_TO_SELECT_CONTEXT) {
      if(calleeShortName.contains(HACK_METHOD_STR) && receiver.length > index) {
        // we assume that the argument is only used as a property name, so we can do ToString
        return new ForInContext(baseContext, simulateToString(caller.getClassHierarchy(), receiver[index]));
      }
    } else if(receiver.length > index) {
      Frequency f = usesFirstArgAsPropertyName(callee);
      if(f == Frequency.ALWAYS) {
        return new ForInContext(baseContext, simulateToString(caller.getClassHierarchy(), receiver[index]));
      } else if(f == Frequency.SOMETIMES) {
        if(receiver[index] == null) {
          IClass undef = caller.getClassHierarchy().lookupClass(JavaScriptTypes.Undefined);
          return new ForInContext(baseContext, new ConcreteTypeKey(undef));
        } else {
          return new ForInContext(baseContext, receiver[index]);
        }
      }
    }
    if (USE_CPA_IN_BODIES && FORIN_MARKER.equals(caller.getContext().get(FORIN_KEY))) {
      return new SelectiveCPAContext(baseContext, receiver);
    } else if (USE_1LEVEL_IN_BODIES && FORIN_MARKER.equals(caller.getContext().get(FORIN_KEY))) {
      if (! identifyDependentParameters(caller, site).isEmpty()) {
        // RECURSION CHECK: only add one level of caller-site contexts if the caller and callee methods are distinct
        if (!caller.getMethod().equals(callee)) {
          return oneLevel.getCalleeTarget(caller, site, callee, receiver);
        }
      } else {
        return baseContext;
      }
    } 
    return baseContext;
  }
  
  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    if (USE_CPA_IN_BODIES && FORIN_MARKER.equals(caller.getContext().get(FORIN_KEY))) {
      // what about base.getRelevantParameters() here?
      return identifyDependentParameters(caller, site);
    } else if (caller.getIR().getCalls(site)[0].getNumberOfUses() > index) {
      return IntSetUtil.make(new int[]{index}).union(base.getRelevantParameters(caller, site));
    } else {
      return base.getRelevantParameters(caller, site);
    }
  }

}