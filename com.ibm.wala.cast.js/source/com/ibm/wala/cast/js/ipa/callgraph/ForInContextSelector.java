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
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.ConcreteTypeKey;
import com.ibm.wala.ipa.callgraph.propagation.ConstantKey;
import com.ibm.wala.ipa.callgraph.propagation.FilteredPointerKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.cfa.OneLevelSiteContextSelector;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.MutableIntSet;

public class ForInContextSelector implements ContextSelector {

  public final static ContextKey FORIN_KEY = new ContextKey() {
    
  };
  
  public static final ContextItem FORIN_MARKER = new ContextItem() {
    
  };
  
  public static final String HACK_METHOD_STR = "_forin_body";

  public static boolean USE_CPA_IN_BODIES = false;
  
  public static boolean USE_1LEVEL_IN_BODIES = false;

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
  
  public static class ForInContext extends SelectiveCPAContext {
    
    ForInContext(Context base, InstanceKey obj) {
      super(base, Collections.singletonMap(ContextKey.PARAMETERS[2], obj));
    }
    
    public ContextItem get(ContextKey key) {
      if (FORIN_KEY.equals(key)) {
        return FORIN_MARKER;
      } else {
        return super.get(key);
      }
    }
    
    @Override
    public String toString() {
      return "for in hack filter for " + get(ContextKey.PARAMETERS[2]) + " over " + this.base;
    }
    
  }
    
  private final ContextSelector base;
  
  private final ContextSelector oneLevel;
  
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
      if (values.contains(3)) {
        dependentParameters.add(i);
      }
    }
    return dependentParameters;
  }
  
  public ForInContextSelector(ContextSelector base) {
    this.base = base;
    this.oneLevel = new OneLevelSiteContextSelector(base);
  }
  
  public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, final InstanceKey[] receiver) {
    Context baseContext = base.getCalleeTarget(caller, site, callee, receiver);
    if (callee.getDeclaringClass().getName().toString().contains(HACK_METHOD_STR)) {
      InstanceKey loopVar = receiver[2];
      IClass stringClass = caller.getClassHierarchy().lookupClass(JavaScriptTypes.String);
      if(loopVar instanceof ConstantKey) {
        // do a manual ToString conversion if necessary
        Object value = ((ConstantKey)loopVar).getValue();
        if(value instanceof String) {
          return new ForInContext(baseContext, loopVar);
        } else if(value instanceof Number) {
          Integer ival = ((Number)value).intValue();
          return new ForInContext(baseContext, new ConstantKey<String>(ival.toString(), stringClass));
        } else if(value instanceof Boolean) {
          Boolean bval = (Boolean)value;
          return new ForInContext(baseContext, new ConstantKey<String>(bval.toString(), stringClass));
        } else if(value == null) {
          return new ForInContext(baseContext, new ConstantKey<String>("null", stringClass));
        }
      }
      ConcreteTypeKey stringKey = new ConcreteTypeKey(stringClass);
      return new ForInContext(baseContext, stringKey);
    } else if (USE_CPA_IN_BODIES && FORIN_MARKER.equals(caller.getContext().get(FORIN_KEY))) {
      return new SelectiveCPAContext(baseContext, receiver);
    } else if (USE_1LEVEL_IN_BODIES && FORIN_MARKER.equals(caller.getContext().get(FORIN_KEY))) {
      if (! identifyDependentParameters(caller, site).isEmpty()) {
        return oneLevel.getCalleeTarget(caller, site, callee, receiver);        
      } else {
        return baseContext;
      }
    } else {
      return baseContext;
    }
  }
  
  public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
    if (USE_CPA_IN_BODIES && FORIN_MARKER.equals(caller.getContext().get(FORIN_KEY))) {
      return identifyDependentParameters(caller, site);
    } else if (caller.getIR().getCalls(site)[0].getNumberOfUses() > 2) {
      return IntSetUtil.make(new int[]{2}).union(base.getRelevantParameters(caller, site));
    } else {
      return base.getRelevantParameters(caller, site);
    }
  }

}