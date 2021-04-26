/*
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.analysis.reflection.java7;

import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.SyntheticMethod;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.Context;
import com.ibm.wala.ipa.callgraph.ContextItem;
import com.ibm.wala.ipa.callgraph.ContextKey;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.callgraph.propagation.ConstantKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.DelegatingSSAContextInterpreter;
import com.ibm.wala.ipa.summaries.MethodSummary;
import com.ibm.wala.ipa.summaries.SummarizedMethod;
import com.ibm.wala.shrike.shrikeBT.IInvokeInstruction.Dispatch;
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
import com.ibm.wala.util.intset.MutableIntSet;
import java.lang.ref.SoftReference;
import java.util.Iterator;
import java.util.Map;
import java.util.function.Predicate;

public class MethodHandles {

  private static final IntSet params = IntSetUtil.make(new int[] {1, 2});

  private static final IntSet self = IntSetUtil.make(new int[0]);

  private static final ContextKey METHOD_KEY =
      new ContextKey() {
        @Override
        public String toString() {
          return "METHOD_KEY";
        }
      };

  private static final ContextKey CLASS_KEY =
      new ContextKey() {
        @Override
        public String toString() {
          return "CLASS_KEY";
        }
      };

  private static final ContextKey NAME_KEY =
      new ContextKey() {
        @Override
        public String toString() {
          return "NAME_KEY";
        }
      };

  private static class HandlesItem<T> implements ContextItem {
    private final T item;

    public HandlesItem(T method) {
      this.item = method;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((item == null) ? 0 : item.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      HandlesItem<?> other = (HandlesItem<?>) obj;
      if (item == null) {
        if (other.item != null) return false;
      } else if (!item.equals(other.item)) return false;
      return true;
    }
  }

  public static class FindContext implements Context {
    private final Context base;
    private final TypeReference cls;
    private final String selector;

    public FindContext(Context base, TypeReference cls, String methodName) {
      this.base = base;
      this.cls = cls;
      this.selector = methodName;
    }

    @Override
    public ContextItem get(ContextKey name) {
      if (CLASS_KEY.equals(name)) {
        return new HandlesItem<>(cls);
      } else if (NAME_KEY.equals(name)) {
        return new HandlesItem<>(selector);
      } else {

        return base.get(name);
      }
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((base == null) ? 0 : base.hashCode());
      result = prime * result + ((cls == null) ? 0 : cls.hashCode());
      result = prime * result + ((selector == null) ? 0 : selector.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      FindContext other = (FindContext) obj;
      if (base == null) {
        if (other.base != null) return false;
      } else if (!base.equals(other.base)) return false;
      if (cls == null) {
        if (other.cls != null) return false;
      } else if (!cls.equals(other.cls)) return false;
      if (selector == null) {
        if (other.selector != null) return false;
      } else if (!selector.equals(other.selector)) return false;
      return true;
    }
  }

  private static class MethodContext implements Context {
    private final Context base;
    private final MethodReference method;

    public MethodContext(Context base, MethodReference method) {
      this.base = base;
      this.method = method;
    }

    @Override
    public ContextItem get(ContextKey name) {
      if (METHOD_KEY.equals(name)) {
        return new HandlesItem<>(method);
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
      if (this == obj) return true;
      if (obj == null) return false;
      if (getClass() != obj.getClass()) return false;
      MethodContext other = (MethodContext) obj;
      if (base == null) {
        if (other.base != null) return false;
      } else if (!base.equals(other.base)) return false;
      if (method == null) {
        if (other.method != null) return false;
      } else if (!method.equals(other.method)) return false;
      return true;
    }

    @Override
    public String toString() {
      return "ctxt:" + method.getName();
    }
  }

  private static class ContextSelectorImpl implements ContextSelector {
    private final ContextSelector base;

    public ContextSelectorImpl(ContextSelector base) {
      this.base = base;
    }

    @Override
    public Context getCalleeTarget(
        CGNode caller, CallSiteReference site, IMethod callee, InstanceKey[] actualParameters) {
      Context baseContext = base.getCalleeTarget(caller, site, callee, actualParameters);
      if ((isInvoke(callee) || isType(callee))
          && callee
              .getReference()
              .getDeclaringClass()
              .getName()
              .equals(TypeReference.JavaLangInvokeMethodHandle.getName())) {
        if (actualParameters != null && actualParameters.length > 0) {
          InstanceKey selfKey = actualParameters[0];
          if (selfKey instanceof ConstantKey
              && ((ConstantKey<?>) selfKey)
                  .getConcreteType()
                  .getReference()
                  .equals(TypeReference.JavaLangInvokeMethodHandle)) {
            MethodReference ref = ((IMethod) ((ConstantKey<?>) selfKey).getValue()).getReference();
            return new MethodContext(baseContext, ref);
          }
        }
      }

      if (isFindStatic(callee)
          && callee
              .getDeclaringClass()
              .getReference()
              .equals(TypeReference.JavaLangInvokeMethodHandlesLookup)) {
        if (actualParameters != null && actualParameters.length > 2) {
          InstanceKey classKey = actualParameters[1];
          InstanceKey nameKey = actualParameters[2];
          if (classKey instanceof ConstantKey
              && ((ConstantKey<?>) classKey)
                  .getConcreteType()
                  .getReference()
                  .equals(TypeReference.JavaLangClass)
              && nameKey instanceof ConstantKey
              && ((ConstantKey<?>) nameKey)
                  .getConcreteType()
                  .getReference()
                  .equals(TypeReference.JavaLangString)) {
            return new FindContext(
                baseContext,
                ((IClass) ((ConstantKey<?>) classKey).getValue()).getReference(),
                (String) ((ConstantKey<?>) nameKey).getValue());
          }
        }
      }

      return baseContext;
    }

    @Override
    public IntSet getRelevantParameters(CGNode caller, CallSiteReference site) {
      MutableIntSet x = IntSetUtil.makeMutableCopy(base.getRelevantParameters(caller, site));
      x.addAll(isFindStatic(site.getDeclaredTarget()) ? params : self);
      return x;
    }
  }

  private static class InvokeExactTargetSelector implements MethodTargetSelector {
    private final MethodTargetSelector base;
    private final Map<MethodReference, SyntheticMethod> impls = HashMapFactory.make();

    public InvokeExactTargetSelector(MethodTargetSelector base) {
      this.base = base;
    }

    @Override
    public IMethod getCalleeTarget(CGNode caller, CallSiteReference site, IClass receiver) {
      MethodReference target = site.getDeclaredTarget();
      if (isInvokeExact(target)) {
        if (!impls.containsKey(target)) {
          SyntheticMethod invokeExactTrampoline =
              new SyntheticMethod(
                  target,
                  receiver
                      .getClassHierarchy()
                      .lookupClass(TypeReference.JavaLangInvokeMethodHandle),
                  false,
                  false);
          impls.put(target, invokeExactTrampoline);
        }

        return impls.get(target);
      }

      return base.getCalleeTarget(caller, site, receiver);
    }
  }

  private static boolean isInvokeExact(MethodReference target) {
    return target
            .getDeclaringClass()
            .getName()
            .equals(TypeReference.JavaLangInvokeMethodHandle.getName())
        && target.getName().toString().equals("invokeExact");
  }

  private static boolean isFindStatic(MethodReference node) {
    return node.getName().toString().startsWith("findStatic");
  }

  private static boolean isFindStatic(IMethod node) {
    return isFindStatic(node.getReference());
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

  private static boolean isFindStatic(CGNode node) {
    return isFindStatic(node.getMethod());
  }

  private abstract static class HandlersContextInterpreterImpl implements SSAContextInterpreter {
    protected final Map<CGNode, SoftReference<IR>> irs = HashMapFactory.make();

    @Override
    public Iterator<NewSiteReference> iterateNewSites(CGNode node) {
      return getIR(node).iterateNewSites();
    }

    public Iterator<FieldReference> iterateFields(CGNode node, Predicate<SSAInstruction> filter) {
      return new MapIterator<>(
          new FilterIterator<>(getIR(node).iterateNormalInstructions(), filter),
          object -> ((SSAFieldAccessInstruction) object).getDeclaredField());
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
    public Iterator<CallSiteReference> iterateCallSites(CGNode node) {
      return getIR(node).iterateCallSites();
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

  private static class FindContextInterpreterImpl extends HandlersContextInterpreterImpl {

    @Override
    public boolean understands(CGNode node) {
      return isFindStatic(node) && node.getContext().isA(FindContext.class);
    }

    @Override
    public IR getIR(CGNode node) {
      if (!irs.containsKey(node) || irs.get(node).get() == null) {
        MethodSummary code = new MethodSummary(node.getMethod().getReference());
        SummarizedMethod m =
            new SummarizedMethod(
                node.getMethod().getReference(), code, node.getMethod().getDeclaringClass());
        SSAInstructionFactory insts =
            node.getMethod()
                .getDeclaringClass()
                .getClassLoader()
                .getLanguage()
                .instructionFactory();
        assert node.getContext().isA(FindContext.class);

        @SuppressWarnings("unchecked")
        IClass cls =
            node.getClassHierarchy()
                .lookupClass(((HandlesItem<TypeReference>) node.getContext().get(CLASS_KEY)).item);
        @SuppressWarnings("unchecked")
        String selector = ((HandlesItem<String>) node.getContext().get(NAME_KEY)).item;

        int vn = 10;
        for (IMethod handleMethod : cls.getAllMethods()) {
          if (handleMethod.getName().toString().contains(selector)) {
            code.addStatement(
                insts.LoadMetadataInstruction(
                    code.getNumberOfStatements(),
                    vn,
                    TypeReference.JavaLangInvokeMethodHandle,
                    handleMethod.getReference()));
            code.addStatement(insts.ReturnInstruction(code.getNumberOfStatements(), vn, false));
            vn++;
          }
        }
        irs.put(
            node, new SoftReference<>(m.makeIR(node.getContext(), SSAOptions.defaultOptions())));
      }

      return irs.get(node).get();
    }
  }

  private static class InvokeContextInterpreterImpl extends HandlersContextInterpreterImpl {

    @Override
    public boolean understands(CGNode node) {
      return (isInvoke(node) || isType(node)) && node.getContext().isA(MethodContext.class);
    }

    @Override
    public IR getIR(CGNode node) {
      if (!irs.containsKey(node) || irs.get(node).get() == null) {
        MethodSummary code = new MethodSummary(node.getMethod().getReference());
        SummarizedMethod m =
            new SummarizedMethod(
                node.getMethod().getReference(), code, node.getMethod().getDeclaringClass());
        SSAInstructionFactory insts =
            node.getMethod()
                .getDeclaringClass()
                .getClassLoader()
                .getLanguage()
                .instructionFactory();
        assert node.getContext().isA(MethodContext.class);
        MethodReference ref = ((MethodContext) node.getContext()).method;
        boolean isStatic = node.getClassHierarchy().resolveMethod(ref).isStatic();
        boolean isVoid = ref.getReturnType().equals(TypeReference.Void);
        if (isInvoke(node)) {
          String name = node.getMethod().getName().toString();
          switch (name) {
            case "invokeWithArguments":
              {
                int nargs = ref.getNumberOfParameters();
                int params[] = new int[nargs];
                for (int i = 0; i < nargs; i++) {
                  code.addConstant(i + nargs + 3, new ConstantValue(i));
                  code.addStatement(
                      insts.ArrayLoadInstruction(
                          code.getNumberOfStatements(),
                          i + 3,
                          1,
                          i + nargs + 3,
                          TypeReference.JavaLangObject));
                  params[i] = i + 3;
                }
                CallSiteReference site =
                    CallSiteReference.make(
                        nargs + 1, ref, isStatic ? Dispatch.STATIC : Dispatch.SPECIAL);
                code.addStatement(
                    insts.InvokeInstruction(
                        code.getNumberOfStatements(),
                        2 * nargs + 3,
                        params,
                        2 * nargs + 4,
                        site,
                        null));
                code.addStatement(
                    insts.ReturnInstruction(code.getNumberOfStatements(), 2 * nargs + 3, false));
                break;
              }
            case "invokeExact":
              {
                int nargs = node.getMethod().getReference().getNumberOfParameters();
                int params[] = new int[nargs];
                if (nargs == ref.getNumberOfParameters() + (isStatic ? 0 : 1)) {
                  for (int i = 0; i < nargs; i++) {
                    params[i] = i + 2;
                  }
                  CallSiteReference site =
                      CallSiteReference.make(0, ref, isStatic ? Dispatch.STATIC : Dispatch.SPECIAL);
                  if (isVoid) {
                    code.addStatement(
                        insts.InvokeInstruction(
                            code.getNumberOfStatements(), params, nargs + 2, site, null));
                  } else {
                    code.addStatement(
                        insts.InvokeInstruction(
                            code.getNumberOfStatements(),
                            nargs + 2,
                            params,
                            nargs + 3,
                            site,
                            null));
                    code.addStatement(
                        insts.ReturnInstruction(code.getNumberOfStatements(), nargs + 2, false));
                  }
                }
                break;
              }
          }
        } else {
          assert isType(node);
          code.addStatement(
              insts.LoadMetadataInstruction(
                  code.getNumberOfStatements(),
                  2,
                  TypeReference.JavaLangInvokeMethodType,
                  ref.getDescriptor()));
          code.addStatement(insts.ReturnInstruction(code.getNumberOfStatements(), 2, false));
        }
        irs.put(
            node, new SoftReference<>(m.makeIR(node.getContext(), SSAOptions.defaultOptions())));
      }

      return irs.get(node).get();
    }
  }

  public static void analyzeMethodHandles(
      AnalysisOptions options, SSAPropagationCallGraphBuilder builder) {
    options.setSelector(new InvokeExactTargetSelector(options.getMethodTargetSelector()));

    builder.setContextSelector(new ContextSelectorImpl(builder.getContextSelector()));
    builder.setContextInterpreter(
        new DelegatingSSAContextInterpreter(
            new InvokeContextInterpreterImpl(), builder.getCFAContextInterpreter()));
    builder.setContextInterpreter(
        new DelegatingSSAContextInterpreter(
            new FindContextInterpreterImpl(), builder.getCFAContextInterpreter()));
  }
}
