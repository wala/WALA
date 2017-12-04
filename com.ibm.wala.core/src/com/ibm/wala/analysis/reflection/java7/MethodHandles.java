/*******************************************************************************
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.analysis.reflection.java7;

import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.propagation.ConstantKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.summaries.MethodSummary;
import com.ibm.wala.ipa.summaries.SummarizedMethod;
import com.ibm.wala.shrikeBT.IInvokeInstruction.Dispatch;
import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRView;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.FilterIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.MapIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;

public class MethodHandles {

  private static final IntSet self = IntSetUtil.make(new int[0]);
  
  private static ContextKey METHOD_KEY = new ContextKey() {
    @Override
    public String toString() {
      return "METHOD_KEY";
    }
  };
  
  public static class MethodItem implements ContextItem {
    private final MethodReference method;

    public MethodItem(MethodReference method) {
      super();
      this.method = method;
    }

    public MethodReference getMethod() {
      return method;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((method == null) ? 0 : method.hashCode());
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
      MethodItem other = (MethodItem) obj;
      if (method == null) {
        if (other.method != null)
          return false;
      } else if (!method.equals(other.method))
        return false;
      return true;
    }
  }
  
  public static class MethodContext implements Context {
    private final Context base;
    private final MethodReference method;
        
    public MethodContext(Context base, MethodReference method) {
      this.base = base;
      this.method = method;
    }

    @Override
    public ContextItem get(ContextKey name) {
      if (METHOD_KEY.equals(name)) {
        return new MethodItem(method);
      } else {
        return base.get(name);
      }
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((base == null) ? 0 : base.hashCode());
      result = prime * result + ((method == null) ? 0 : method.hashCode());
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
      MethodContext other = (MethodContext) obj;
      if (base == null) {
        if (other.base != null)
          return false;
      } else if (!base.equals(other.base))
        return false;
      if (method == null) {
        if (other.method != null)
          return false;
      } else if (!method.equals(other.method))
        return false;
      return true;
    }
    
    @Override
    public String toString() {
      return "ctxt:" + method.getName();
    }
  }
  
  public static class ContextSelectorImpl implements ContextSelector {
    private final ContextSelector base;
    
    public ContextSelectorImpl(ContextSelector base) {
      this.base = base;
    }

    @Override
    public Context getCalleeTarget(CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] actualParameters) {
      Context baseContext = base.getCalleeTarget(caller, site, callee, actualParameters);
      if ((isInvoke(callee) || isType(callee)) && callee.getDeclaringClass().getReference().equals(TypeReference.JavaLangInvokeMethodHandle)) {
        if (actualParameters != null && actualParameters.length > 0) {
          InstanceKey selfKey = actualParameters[0];
          if (selfKey instanceof ConstantKey && ((ConstantKey)selfKey).getConcreteType().getReference().equals(TypeReference.JavaLangInvokeMethodHandle)) {
            MethodReference ref = ((IMethod) ((ConstantKey)selfKey).getValue()).getReference();
            return new MethodContext(baseContext, ref);
          }
        }
      }
      return baseContext;
    }

    @Override
    public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
      return self;
    }
  }
  
  private static boolean isInvoke(IMethod node) {
    return node.getName().toString().startsWith("invoke");
  }

  private static boolean isType(IMethod node) {
    return node.getName().toString().equals("type");
  }

  private static boolean isInvoke(CGNode node) {
    return isInvoke(node.getMethod());
  }

  private static boolean isType(CGNode node) {
    return isType(node.getMethod());
  }

  public static class ContextInterpreterImpl implements SSAContextInterpreter {
    private final Map<CGNode, SoftReference<IR>> irs = HashMapFactory.make();

    @Override
    public Iterator<NewSiteReference> iterateNewSites(CGNode node) {
      return getIR(node).iterateNewSites();
    }

    public Iterator<FieldReference> iterateFields(CGNode node, Predicate<SSAInstruction> filter) {
      return 
          new MapIterator<>(
              new FilterIterator<>(getIR(node).iterateNormalInstructions(), filter), 
              object -> ((SSAFieldAccessInstruction)object).getDeclaredField());
    }
    
    @Override
    public Iterator<FieldReference> iterateFieldsRead(CGNode node) {
      return iterateFields(node, SSAGetInstruction.class::isInstance);
    }
    
    @Override
    public Iterator<FieldReference> iterateFieldsWritten(CGNode node) {
      return iterateFields(node, SSAPutInstruction.class::isInstance);
    }

    @Override
    public boolean recordFactoryType(CGNode node, IClass klass) {
       return false;
    }

    @Override
    public boolean understands(CGNode node) {
      return (isInvoke(node) || isType(node)) && node.getContext() instanceof MethodContext;
    }

    @Override
    public Iterator<CallSiteReference> iterateCallSites(CGNode node) {
      return getIR(node).iterateCallSites();
    }

    @Override
    public IR getIR(CGNode node) {
      if (!irs.containsKey(node) || irs.get(node).get() == null) {
        MethodSummary code = new MethodSummary(node.getMethod().getReference());
        SummarizedMethod m = new SummarizedMethod(node.getMethod().getReference(), code, node.getMethod().getDeclaringClass());
        SSAInstructionFactory insts = node.getMethod().getDeclaringClass().getClassLoader().getLanguage().instructionFactory();
        assert node.getContext() instanceof MethodContext;
        MethodReference ref = ((MethodContext)node.getContext()).method;
        boolean isStatic = node.getClassHierarchy().resolveMethod(ref).isStatic();
        if (isInvoke(node)) {
          String name = node.getMethod().getName().toString();
          if ("invokeWithArguments".equals(name)) {
            int nargs = ref.getNumberOfParameters();
            int params[] = new int[nargs];
            for(int i = 0; i < nargs; i++) {
              code.addConstant(i+nargs+3, new ConstantValue(i));
              code.addStatement(insts.ArrayLoadInstruction(code.getNextProgramCounter(), i+3, 1, i+nargs+3, TypeReference.JavaLangObject));
              params[i] = i+3;
            }           
            CallSiteReference site = CallSiteReference.make(nargs+1, ref, isStatic? Dispatch.STATIC: Dispatch.SPECIAL);
            code.addStatement(insts.InvokeInstruction(code.getNextProgramCounter(), 2*nargs+3, params, 2*nargs+4, site, null));
            code.addStatement(insts.ReturnInstruction(code.getNextProgramCounter(), 2*nargs+3, false));
          } else {
            // int nargs = node.getMethod().getNumberOfParameters();
          }
        } else {
          assert isType(node);
          code.addStatement(insts.LoadMetadataInstruction(code.getNextProgramCounter(), 2, TypeReference.JavaLangInvokeMethodType, ref.getDescriptor()));
          code.addStatement(insts.ReturnInstruction(code.getNextProgramCounter(), 2, false));
        }
        irs.put(node, new SoftReference<>(m.makeIR(node.getContext(), SSAOptions.defaultOptions())));
      }

      return irs.get(node).get();
    }

    @Override
    public IRView getIRView(CGNode node) {
      return getIR(node);
    }

    @Override
    public DefUse getDU(CGNode node) {
      return new DefUse(getIR(node));
    }

    @Override
    public int getNumberOfStatements(CGNode node) {
      return getIR(node).getInstructions().length;
    }

    @Override
    public ControlFlowGraph<SSAInstruction, ISSABasicBlock> getCFG(CGNode n) {
      return getIR(n).getControlFlowGraph();
    }
    
  }
}
