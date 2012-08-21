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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cast.js.ipa.summaries.JavaScriptSummarizedFunction;
import com.ibm.wala.cast.js.ipa.summaries.JavaScriptSummary;
import com.ibm.wala.cast.js.loader.JSCallSiteReference;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.ssa.JSInstructionFactory;
import com.ibm.wala.cast.js.types.JavaScriptMethods;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.summaries.MethodSummary;
import com.ibm.wala.ssa.ConstantValue;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Pair;

/**
 * generates instructions to simulate the semantics of JS constructor invocations
 *
 */
public class JavaScriptConstructTargetSelector implements MethodTargetSelector {
  private static final boolean DEBUG = false;

  private final IClassHierarchy cha;

  private final MethodTargetSelector base;

  private final Map<Object, IMethod> constructors = HashMapFactory.make();

  class JavaScriptConstructor extends JavaScriptSummarizedFunction {
    private final String toStringExtra;

    private JavaScriptConstructor(MethodReference ref, MethodSummary summary, IClass declaringClass, String toStringExtra) {
      super(ref, summary, declaringClass);
      this.toStringExtra = toStringExtra;
    }

    private JavaScriptConstructor(MethodReference ref, MethodSummary summary, IClass declaringClass) {
      this(ref, summary, declaringClass, "");
    }

    public String toString() {
      return "<ctor for " + getReference().getDeclaringClass() + toStringExtra + ">";
    }
  }

  public JavaScriptConstructTargetSelector(IClassHierarchy cha, MethodTargetSelector base) {
    this.cha = cha;
    this.base = base;
  }

  private IMethod record(Object key, IMethod m) {
    constructors.put(key, m);
    return m;
  }

  private IMethod makeNullaryValueConstructor(IClass cls, Object value) {
    JSInstructionFactory insts = (JSInstructionFactory)cls.getClassLoader().getInstructionFactory();
    MethodReference ref = JavaScriptMethods.makeCtorReference(cls.getReference());
    JavaScriptSummary S = new JavaScriptSummary(ref, 1);

    S.addStatement(insts.GetInstruction(4, 1, "prototype"));
    S.getNextProgramCounter();
    
    S.addStatement(insts.NewInstruction(5, NewSiteReference.make(S.getNextProgramCounter(), cls.getReference())));

    S.addStatement(insts.SetPrototype(5, 4));
    //S.addStatement(insts.PutInstruction(5, 4, "__proto__"));
    S.getNextProgramCounter();
    
    S.addConstant(new Integer(8), new ConstantValue(value));
    S.addStatement(insts.PutInstruction(5, 8, "$value"));
    S.getNextProgramCounter();
    
    S.addStatement(insts.ReturnInstruction(5, false));
    S.getNextProgramCounter();
    
    //S.addConstant(9, new ConstantValue("__proto__"));
    
    return new JavaScriptConstructor(ref, S, cls);
  }

  private IMethod makeUnaryValueConstructor(IClass cls) {
    JSInstructionFactory insts = (JSInstructionFactory)cls.getClassLoader().getInstructionFactory();
   MethodReference ref = JavaScriptMethods.makeCtorReference(cls.getReference());
    JavaScriptSummary S = new JavaScriptSummary(ref, 2);

    S.addStatement(insts.GetInstruction(5, 1, "prototype"));
    S.getNextProgramCounter();
    
    S.addStatement(insts.NewInstruction(6, NewSiteReference.make(S.getNextProgramCounter(), cls.getReference())));

    S.addStatement(insts.SetPrototype(6, 5));
    //S.addStatement(insts.PutInstruction(6, 5, "__proto__"));
    S.getNextProgramCounter();
    
    S.addStatement(insts.PutInstruction(6, 2, "$value"));
    S.getNextProgramCounter();
    
    S.addStatement(insts.ReturnInstruction(6, false));
    S.getNextProgramCounter();
 
    //S.addConstant(7, new ConstantValue("__proto__"));

    return new JavaScriptConstructor(ref, S, cls);
  }

  private IMethod makeValueConstructor(IClass cls, int nargs, Object value) {
    if (nargs == 0 || nargs == 1) {

      Object key = Pair.make(cls, new Integer(nargs));
      if (constructors.containsKey(key))
        return constructors.get(key);

      else
        return record(key, (nargs == 0) ? makeNullaryValueConstructor(cls, value) : makeUnaryValueConstructor(cls));
    } else {
        // not a legal call, likely due to dataflow imprecision
        return null;
    }
  }

  /**
   * create a method for constructing an Object with no arguments passed
   */
  private IMethod makeNullaryObjectConstructor(IClass cls) {
    JSInstructionFactory insts = (JSInstructionFactory)cls.getClassLoader().getInstructionFactory();
    MethodReference ref = JavaScriptMethods.makeCtorReference(JavaScriptTypes.Object);
    JavaScriptSummary S = new JavaScriptSummary(ref, 1);

    S.addStatement(insts.GetInstruction(4, 1, "prototype"));
    S.getNextProgramCounter();
    
    S.addStatement(insts.NewInstruction(5, NewSiteReference.make(S.getNextProgramCounter(), JavaScriptTypes.Object)));

    S.addStatement(insts.SetPrototype(5, 4));
    //S.addStatement(insts.PutInstruction(5, 4, "__proto__"));
    S.getNextProgramCounter();
    
    S.addStatement(insts.ReturnInstruction(5, false));
    S.getNextProgramCounter();
    
    //S.addConstant(6, new ConstantValue("__proto__"));

    return new JavaScriptConstructor(ref, S, cls);
  }

  private IMethod makeUnaryObjectConstructor(IClass cls) {
    JSInstructionFactory insts = (JSInstructionFactory)cls.getClassLoader().getInstructionFactory();
    MethodReference ref = JavaScriptMethods.makeCtorReference(JavaScriptTypes.Object);
    JavaScriptSummary S = new JavaScriptSummary(ref, 2);

    S.addStatement(insts.ReturnInstruction(2, false));
    S.getNextProgramCounter();
    
    return new JavaScriptConstructor(ref, S, cls);
  }

  private IMethod makeObjectConstructor(IClass cls, int nargs) {
    if (nargs == 0 || nargs == 1) {

      Object key = Pair.make(cls, new Integer(nargs));
      if (constructors.containsKey(key))
        return constructors.get(key);

      else
        return record(key, (nargs == 0) ? makeNullaryObjectConstructor(cls) : makeUnaryObjectConstructor(cls));
    
    } else {
      // not a legal call, likely the result of analysis imprecision
      return null;
    }
  }
  
  private IMethod makeObjectCall(IClass cls, int nargs) {
    assert nargs == 0;

    Object key = Pair.make(cls, new Integer(nargs));
    if (constructors.containsKey(key))
      return constructors.get(key);

    else
      return record(key, makeNullaryObjectConstructor(cls));
  }

  private IMethod makeArrayLengthConstructor(IClass cls) {
    JSInstructionFactory insts = (JSInstructionFactory)cls.getClassLoader().getInstructionFactory();
   MethodReference ref = JavaScriptMethods.makeCtorReference(JavaScriptTypes.Array);
    JavaScriptSummary S = new JavaScriptSummary(ref, 2);

    S.addStatement(insts.GetInstruction(5, 1, "prototype"));
    S.getNextProgramCounter();
    
    S.addStatement(insts.NewInstruction(6, NewSiteReference.make(S.getNextProgramCounter(), JavaScriptTypes.Array)));

    S.addStatement(insts.SetPrototype(6, 5));
    //S.addStatement(insts.PutInstruction(6, 5, "__proto__"));
    S.getNextProgramCounter();
    
    S.addStatement(insts.PutInstruction(6, 2, "length"));
    S.getNextProgramCounter();
    
    S.addStatement(insts.ReturnInstruction(6, false));
    S.getNextProgramCounter();
  
    //S.addConstant(7, new ConstantValue("__proto__"));

    return new JavaScriptConstructor(ref, S, cls);
  }

  private IMethod makeArrayContentsConstructor(IClass cls, int nargs) {
    JSInstructionFactory insts = (JSInstructionFactory)cls.getClassLoader().getInstructionFactory();
    MethodReference ref = JavaScriptMethods.makeCtorReference(JavaScriptTypes.Array);
    JavaScriptSummary S = new JavaScriptSummary(ref, nargs + 1);

    S.addConstant(new Integer(nargs + 3), new ConstantValue("prototype"));
    S.addStatement(insts.PropertyRead(nargs + 4, 1, nargs + 3));
    S.getNextProgramCounter();
    
    S.addStatement(
        insts.NewInstruction(nargs + 5, NewSiteReference.make(S.getNextProgramCounter(),
            JavaScriptTypes.Array)));

    S.addStatement(insts.SetPrototype(nargs + 5, nargs + 4));
    //S.addStatement(insts.PutInstruction(nargs + 5, nargs + 4, "__proto__"));
    S.getNextProgramCounter();
    
    S.addConstant(new Integer(nargs + 7), new ConstantValue(nargs));
    S.addStatement(insts.PutInstruction(nargs + 5, nargs + 7, "length"));
    S.getNextProgramCounter();
    
    int vn = nargs + 9;
    for (int i = 0; i < nargs; i++, vn += 2) {
      S.addConstant(new Integer(vn), new ConstantValue(i));
      S.addStatement(insts.PropertyWrite(nargs + 5, vn, i + 1));
      S.getNextProgramCounter();
      }

    S.addStatement(insts.ReturnInstruction(5, false));
    S.getNextProgramCounter();
    
    //S.addConstant(vn, new ConstantValue("__proto__"));

    return new JavaScriptConstructor(ref, S, cls);
  }

  private IMethod makeArrayConstructor(IClass cls, int nargs) {
    Object key = Pair.make(cls, new Integer(nargs));
    if (constructors.containsKey(key))
      return constructors.get(key);

    else
      return record(key, (nargs == 1) ? makeArrayLengthConstructor(cls) : makeArrayContentsConstructor(cls, nargs));
  }

  private IMethod makeNullaryStringCall(IClass cls) {
    JSInstructionFactory insts = (JSInstructionFactory)cls.getClassLoader().getInstructionFactory();
    MethodReference ref = AstMethodReference.fnReference(JavaScriptTypes.String);
    JavaScriptSummary S = new JavaScriptSummary(ref, 1);

    S.addConstant(new Integer(2), new ConstantValue(""));
    S.addStatement(insts.ReturnInstruction(2, false));
    S.getNextProgramCounter();
    
    return new JavaScriptConstructor(ref, S, cls);
  }

  private IMethod makeUnaryStringCall(IClass cls) {
    JSInstructionFactory insts = (JSInstructionFactory)cls.getClassLoader().getInstructionFactory();
   MethodReference ref = AstMethodReference.fnReference(JavaScriptTypes.String);
    JavaScriptSummary S = new JavaScriptSummary(ref, 2);

    S.addStatement(insts.GetInstruction(4, 2, "toString"));
    S.getNextProgramCounter();
    
    CallSiteReference cs = new JSCallSiteReference(S.getNextProgramCounter());
    S.addStatement(insts.Invoke(4, 5, new int[] { 2 }, 6, cs));

    S.addStatement(insts.ReturnInstruction(5, false));
    S.getNextProgramCounter();
    
    return new JavaScriptConstructor(ref, S, cls);
  }

  private IMethod makeStringCall(IClass cls, int nargs) {
    assert nargs == 0 || nargs == 1;

    Object key = Pair.make(cls, new Integer(nargs));
    if (constructors.containsKey(key))
      return constructors.get(key);

    else
      return record(key, (nargs == 0) ? makeNullaryStringCall(cls) : makeUnaryStringCall(cls));
  }

  private IMethod makeNullaryNumberCall(IClass cls) {
    JSInstructionFactory insts = (JSInstructionFactory)cls.getClassLoader().getInstructionFactory();
    MethodReference ref = AstMethodReference.fnReference(JavaScriptTypes.Number);
    JavaScriptSummary S = new JavaScriptSummary(ref, 1);

    S.addConstant(new Integer(2), new ConstantValue(0.0));
    S.addStatement(insts.ReturnInstruction(2, false));
    S.getNextProgramCounter();
    
    return new JavaScriptConstructor(ref, S, cls);
  }

  private IMethod makeUnaryNumberCall(IClass cls) {
    JSInstructionFactory insts = (JSInstructionFactory)cls.getClassLoader().getInstructionFactory();
   MethodReference ref = AstMethodReference.fnReference(JavaScriptTypes.Number);
    JavaScriptSummary S = new JavaScriptSummary(ref, 2);

    S.addStatement(insts.GetInstruction(4, 2, "toNumber"));
    S.getNextProgramCounter();
    
    CallSiteReference cs = new JSCallSiteReference(S.getNextProgramCounter());
    S.addStatement(insts.Invoke(4, 5, new int[] { 2 }, 6, cs));

    S.addStatement(insts.ReturnInstruction(5, false));
    S.getNextProgramCounter();
    
    return new JavaScriptConstructor(ref, S, cls);
  }

  private IMethod makeNumberCall(IClass cls, int nargs) {
    assert nargs == 0 || nargs == 1;

    Object key = Pair.make(cls, new Integer(nargs));
    if (constructors.containsKey(key))
      return constructors.get(key);

    else
      return record(key, (nargs == 0) ? makeNullaryNumberCall(cls) : makeUnaryNumberCall(cls));
  }

  private IMethod makeFunctionConstructor(IClass receiver, IClass cls) {
    JSInstructionFactory insts = (JSInstructionFactory)cls.getClassLoader().getInstructionFactory();
   Pair<IClass, IClass> tableKey = Pair.make(receiver, cls);
    if (constructors.containsKey(tableKey))
      return constructors.get(tableKey);

    MethodReference ref = JavaScriptMethods.makeCtorReference(receiver.getReference());
    JavaScriptSummary S = new JavaScriptSummary(ref, 1);

    S.addStatement(insts.GetInstruction(4, 1, "prototype"));
    S.getNextProgramCounter();
    
    S.addStatement(insts.NewInstruction(5, NewSiteReference.make(S.getNextProgramCounter(), cls.getReference())));

    S.addStatement(insts.NewInstruction(7, NewSiteReference.make(S.getNextProgramCounter(), JavaScriptTypes.Object)));

    S.addStatement(insts.SetPrototype(5, 4));
    //S.addStatement(insts.PutInstruction(5, 4, "__proto__"));
    S.getNextProgramCounter();
    
    S.addStatement(insts.PutInstruction(5, 7, "prototype"));
    S.getNextProgramCounter();
    
    S.addStatement(insts.PutInstruction(7, 5, "constructor"));
    S.getNextProgramCounter();
    
    // TODO we need to set v7.__proto__ to Object.prototype
    S.addStatement(insts.ReturnInstruction(5, false));
    S.getNextProgramCounter();
    
    //S.addConstant(8, new ConstantValue("__proto__"));

    if (receiver != cls)
      return record(tableKey, new JavaScriptConstructor(ref, S, receiver, "(" + cls.getReference().getName() + ")"));
    else
      return record(tableKey, new JavaScriptConstructor(ref, S, receiver));
  }

  private int ctorCount = 0;

  private IMethod makeFunctionConstructor(IR callerIR, SSAAbstractInvokeInstruction callStmt, IClass cls, int nargs) {
    SymbolTable ST = callerIR.getSymbolTable();

    if (nargs == 0) {
      return makeFunctionConstructor(cls, cls);
    } else if (nargs == 1) {
      if (ST.isStringConstant(callStmt.getUse(1))) {
        TypeReference ref = TypeReference.findOrCreate(JavaScriptTypes.jsLoader, TypeName.string2TypeName((String) ST
            .getStringValue(callStmt.getUse(1))));

        if (DEBUG) {
          System.err.println(("ctor type name is " + (String) ST.getStringValue(callStmt.getUse(1))));
        }

        IClass cls2 = cha.lookupClass(ref);
        if (cls2 != null) {
          return makeFunctionConstructor(cls, cls2);
        }
      }

      return makeFunctionConstructor(cls, cls);
    } else {
      assert nargs > 1;
      JavaScriptLoader cl = (JavaScriptLoader) cha.getLoader(JavaScriptTypes.jsLoader);

      for (int i = 1; i < callStmt.getNumberOfUses(); i++)
        if (!ST.isStringConstant(callStmt.getUse(i)))
          return makeFunctionConstructor(cls, cls);

      StringBuffer fun = new StringBuffer("function _fromctor (");
      for (int j = 1; j < callStmt.getNumberOfUses() - 1; j++) {
        if (j != 1)
          fun.append(",");
        fun.append(ST.getStringValue(callStmt.getUse(j)));
      }

      fun.append(") {");
      fun.append(ST.getStringValue(callStmt.getUse(callStmt.getNumberOfUses() - 1)));
      fun.append("}");

      try {
        String fileName = "ctor$" + ++ctorCount;
        File f = new File(System.getProperty("java.io.tmpdir") + File.separator + fileName);
        FileWriter FO = new FileWriter(f);
        FO.write(fun.toString());
        FO.close();
        
        Set<String> fnNames = JSCallGraphUtil.loadAdditionalFile(cha, cl, fileName, f.toURI().toURL());
        IClass fcls = null;
        for(String nm : fnNames) {
          if (nm.endsWith("_fromctor")) {
            fcls = cl.lookupClass(nm, cha);
          }
        }

        assert fcls != null : "cannot find class for " + fileName + " in " + f;
        
        f.delete();

        if (DEBUG)
          System.err.println(("looking for ctor " + ctorCount + " and got " + fcls));

        if (fcls != null)
          return makeFunctionConstructor(cls, fcls);

      } catch (IOException e) {

      }

      return makeFunctionConstructor(cls, cls);
    }
  }

 
  private IMethod makeFunctionObjectConstructor(IClass cls, int nargs) {
    JSInstructionFactory insts = (JSInstructionFactory)cls.getClassLoader().getInstructionFactory();
   Object key = Pair.make(cls, new Integer(nargs));
    if (constructors.containsKey(key))
      return constructors.get(key);

    MethodReference ref = JavaScriptMethods.makeCtorReference(cls.getReference());
    JavaScriptSummary S = new JavaScriptSummary(ref, nargs + 1);
    S.addStatement(insts.GetInstruction(nargs + 4, 1, "prototype"));
    S.getNextProgramCounter();
      
    S.addStatement(
        insts.NewInstruction(nargs + 5, 
                                     NewSiteReference.make(S.getNextProgramCounter(),
                                     JavaScriptTypes.Object)));

    S.addStatement(insts.SetPrototype(nargs + 5, nargs + 4));
    //S.addStatement(insts.PutInstruction(nargs + 5, nargs + 4, "__proto__"));
    S.getNextProgramCounter();
    
    CallSiteReference cs = new JSCallSiteReference(S.getNextProgramCounter());
    int[] args = new int[nargs + 1];
    args[0] = nargs + 5;
    for (int i = 0; i < nargs; i++)
      args[i + 1] = i + 2;
    S.addStatement(insts.Invoke(1, nargs + 7, args, nargs + 8, cs));

    S.addStatement(insts.ReturnInstruction(nargs + 7, false));
    S.getNextProgramCounter();
    
    S.addStatement(insts.ReturnInstruction(nargs + 5, false));
    S.getNextProgramCounter();
    
    //S.addConstant(nargs + 9, new ConstantValue("__proto__"));

    return record(key, new JavaScriptConstructor(ref, S, cls));
  }

  private IMethod findOrCreateConstructorMethod(IR callerIR, SSAAbstractInvokeInstruction callStmt, IClass receiver, int nargs) {
    if (receiver.getReference().equals(JavaScriptTypes.Object))
      return makeObjectConstructor(receiver, nargs);
    else if (receiver.getReference().equals(JavaScriptTypes.Array))
      return makeArrayConstructor(receiver, nargs);
    else if (receiver.getReference().equals(JavaScriptTypes.StringObject))
      return makeValueConstructor(receiver, nargs, "");
    else if (receiver.getReference().equals(JavaScriptTypes.BooleanObject)) {
      assert nargs == 1;
      return makeValueConstructor(receiver, nargs, null);
    } else if (receiver.getReference().equals(JavaScriptTypes.NumberObject))
      return makeValueConstructor(receiver, nargs, new Integer(0));
    else if (receiver.getReference().equals(JavaScriptTypes.Function))
      return makeFunctionConstructor(callerIR, callStmt, receiver, nargs);
    else if (cha.isSubclassOf(receiver, cha.lookupClass(JavaScriptTypes.CodeBody)))
      return makeFunctionObjectConstructor(receiver, nargs);

    else {
      return null;
    }
  }

  @SuppressWarnings("unused")
  private IMethod findOrCreateCallMethod(IR callerIR, SSAAbstractInvokeInstruction callStmt, IClass receiver, int nargs) {
    if (receiver.getReference().equals(JavaScriptTypes.Object))
      return makeObjectCall(receiver, nargs);
    else if (receiver.getReference().equals(JavaScriptTypes.Array))
      return makeArrayConstructor(receiver, nargs);
    else if (receiver.getReference().equals(JavaScriptTypes.StringObject))
      return makeStringCall(receiver, nargs);
    else if (receiver.getReference().equals(JavaScriptTypes.NumberObject))
      return makeNumberCall(receiver, nargs);
    else if (receiver.getReference().equals(JavaScriptTypes.Function))
      return makeFunctionConstructor(callerIR, callStmt, receiver, nargs);
    else {
      return null;
    }
  }

  public IMethod getCalleeTarget(CGNode caller, CallSiteReference site, IClass receiver) {
    if (site.getDeclaredTarget().equals(JavaScriptMethods.ctorReference)) {
      assert cha.isSubclassOf(receiver, cha.lookupClass(JavaScriptTypes.Root));
      IR callerIR = caller.getIR();
      SSAAbstractInvokeInstruction callStmts[] = callerIR.getCalls(site);
      assert callStmts.length == 1;
      int nargs = callStmts[0].getNumberOfParameters();
      return findOrCreateConstructorMethod(callerIR, callStmts[0], receiver, nargs - 1);
    } else {
      return base.getCalleeTarget(caller, site, receiver);
    }
  }

  public boolean mightReturnSyntheticMethod(CGNode caller, CallSiteReference site) {
    return true;
  }

  public boolean mightReturnSyntheticMethod(MethodReference declaredTarget) {
    return true;
  }
}
