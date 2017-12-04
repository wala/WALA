/*******************************************************************************
 * Copyright (c) 2015 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.summaries;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.SyntheticClass;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.Constants;
import com.ibm.wala.shrikeBT.IInvokeInstruction.Dispatch;
import com.ibm.wala.shrikeCT.BootstrapMethodsReader.BootstrapMethod;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.SSAInstructionFactory;
import com.ibm.wala.ssa.SSAInvokeDynamicInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.strings.Atom;

public class LambdaSummaryClass extends SyntheticClass {

  private static WeakHashMap<BootstrapMethod, LambdaSummaryClass> summaries = new WeakHashMap<>();
  
  public static LambdaSummaryClass findOrCreate(CGNode caller, SSAInvokeDynamicInstruction inst) {
    if (! summaries.containsKey(inst.getBootstrap())) {
      String bootstrapCls = caller.getMethod().getDeclaringClass().getName().toString().replace("/", "$").substring(1);
      int bootstrapIndex = inst.getBootstrap().getIndexInClassFile();
      TypeReference ref = TypeReference.findOrCreate(ClassLoaderReference.Primordial, "Lwala/lambda" + "$" + bootstrapCls + "$" + bootstrapIndex);
      LambdaSummaryClass cls = new LambdaSummaryClass(ref, caller.getClassHierarchy(), inst);
      caller.getClassHierarchy().addClass(cls);
      summaries.put(inst.getBootstrap(), cls);
    }
    
    return summaries.get(inst.getBootstrap());
  }
  
  private final SSAInvokeDynamicInstruction invoke;
  
  private final Map<Atom,IField> fields;

  private final Map<Selector,IMethod> methods;

  public LambdaSummaryClass(TypeReference T, IClassHierarchy cha, SSAInvokeDynamicInstruction invoke) {
    super(T, cha);
    this.invoke = invoke;
    this.fields = makeFields();
    this.methods = Collections.singletonMap(trampoline().getSelector(), makeTrampoline());
  }

  @Override
  public boolean isPublic() {
    return false;
  }

  @Override
  public boolean isPrivate() {
    return false;
  }

  @Override
  public int getModifiers() throws UnsupportedOperationException {
    return Constants.ACC_FINAL | Constants.ACC_SUPER;
  }

  @Override
  public IClass getSuperclass() {
    return getClassHierarchy().getRootClass();
  }

  @Override
  public Collection<? extends IClass> getDirectInterfaces() {
    return Collections.singleton(getClassHierarchy().lookupClass(invoke.getDeclaredResultType()));
  }

  @Override
  public Collection<IClass> getAllImplementedInterfaces() {
    IClass iface = getClassHierarchy().lookupClass(invoke.getDeclaredResultType());
    Set<IClass> result = HashSetFactory.make(iface.getAllImplementedInterfaces());
    result.add(iface);
    return result;
  }

  @Override
  public IMethod getMethod(Selector selector) {
    return methods.get(selector);
  }

  @Override
  public IField getField(Atom name) {
    return fields.get(name);
  }

  @Override
  public IMethod getClassInitializer() {
    return null;
  }

  @Override
  public Collection<IMethod> getDeclaredMethods() {
    return methods.values();
  }

  @Override
  public Collection<IField> getAllInstanceFields() {
    return fields.values();
  }

  @Override
  public Collection<IField> getAllStaticFields() {
    return Collections.emptySet();
  }

  @Override
  public Collection<IField> getAllFields() {
     return getAllInstanceFields();
  }

  @Override
  public Collection<IMethod> getAllMethods() {
    return methods.values();
  }

  @Override
  public Collection<IField> getDeclaredInstanceFields() {
    return fields.values();
  }
   
  private Map<Atom,IField> makeFields() {
    Map<Atom,IField> result = HashMapFactory.make();
    for(int i = 0; i < invoke.getNumberOfParameters(); i++) {
      final int yuck = i;
      result.put(Atom.findOrCreateUnicodeAtom("c" + yuck), new IField() {
        @Override
        public IClass getDeclaringClass() {
          return LambdaSummaryClass.this;
        }
        @Override
        public Atom getName() {
          return Atom.findOrCreateUnicodeAtom("c" + yuck);
        }
        @Override
        public Collection<Annotation> getAnnotations() {
          return Collections.emptySet();
        }
        @Override
        public IClassHierarchy getClassHierarchy() {
          return LambdaSummaryClass.this.getClassHierarchy();
        }
        @Override
        public TypeReference getFieldTypeReference() {
          return invoke.getDeclaredTarget().getParameterType(yuck);
        }
        @Override
        public FieldReference getReference() {
          return FieldReference.findOrCreate(LambdaSummaryClass.this.getReference(), getName(), getFieldTypeReference());
        }
        @Override
        public boolean isFinal() {
          return true;
        }
        @Override
        public boolean isPrivate() {
          return true;
        }
        @Override
        public boolean isProtected() {
          return false;
        }
        @Override
        public boolean isPublic() {
           return false;
        }
        @Override
        public boolean isStatic() {
          return false;
        }
        @Override
        public boolean isVolatile() {
          return false;
        } 
      });
    }
    return result;
  }

  private MethodReference trampoline() {
    try {
      return MethodReference.findOrCreate(LambdaSummaryClass.this.getReference(), invoke.getDeclaredTarget().getName(), Descriptor.findOrCreateUTF8(getLambdaDeclaredSignature()));
    } catch (IllegalArgumentException | InvalidClassFileException e) {
      assert false : e;
      return null;
    }
  }
  
  private String getLambdaCalleeClass() throws IllegalArgumentException, InvalidClassFileException {
    int cpIndex = invoke.getBootstrap().callArgumentIndex(1);
    return "L" + invoke.getBootstrap().getCP().getCPHandleClass(cpIndex);
  }

  private String getLambdaCalleeName() throws IllegalArgumentException, InvalidClassFileException {
    int cpIndex = invoke.getBootstrap().callArgumentIndex(1);
    return invoke.getBootstrap().getCP().getCPHandleName(cpIndex);
  }

  private String getLambdaCalleeSignature() throws IllegalArgumentException, InvalidClassFileException {
    int cpIndex = invoke.getBootstrap().callArgumentIndex(1);
    return invoke.getBootstrap().getCP().getCPHandleType(cpIndex);
  }

  private String getLambdaDeclaredSignature() throws IllegalArgumentException, InvalidClassFileException {
    int cpIndex = invoke.getBootstrap().callArgumentIndex(0);
    return invoke.getBootstrap().getCP().getCPMethodType(cpIndex);
  }

  private int getLambdaCalleeKind() throws IllegalArgumentException, InvalidClassFileException {
    int cpIndex = invoke.getBootstrap().callArgumentIndex(1);
    return invoke.getBootstrap().getCP().getCPHandleKind(cpIndex);
  }

  private IMethod makeTrampoline() {
    SSAInstructionFactory insts = getClassLoader().getInstructionFactory();
    
    MethodReference ref = trampoline();
    MethodSummary summary = new MethodSummary(ref);
    
    int inst = 0;
    int args = invoke.getNumberOfParameters(), v = args + 1;
    for(int i = 0; i < invoke.getNumberOfParameters(); i++) {
      Atom f = Atom.findOrCreateUnicodeAtom("c" + i);
      summary.addStatement(insts.GetInstruction(inst++, v++, 1, getField(f).getReference()));
    }
    
    try {
      MethodReference callee = MethodReference.findOrCreate(ClassLoaderReference.Application, getLambdaCalleeClass(), getLambdaCalleeName(), getLambdaCalleeSignature());

      Dispatch code;
      boolean isNew = false;
      int new_v = -1;
      int kind = getLambdaCalleeKind();
      switch (kind) {
      case 5: code = Dispatch.VIRTUAL; break;
      case 6: code = Dispatch.STATIC; break;
      case 7: code = Dispatch.SPECIAL; break;
      case 8: code = Dispatch.SPECIAL; isNew = true; break;
      case 9: code = Dispatch.INTERFACE; break;
      default:
        throw new Error("unexpected dynamic invoke type " + kind);
      }
           
      int numParams = getClassHierarchy().resolveMethod(callee).getNumberOfParameters();
      int params[] = new int[ numParams ];
      for(int i = isNew? 1: 0; i < invoke.getNumberOfParameters(); i++) {
        params[i] = args + i + 1;
      }
      int n = 2;
      for(int i = invoke.getNumberOfParameters(); i < numParams; i++) {
        params[i] = n++;
      }
     
      if (isNew) {
        //v++;
        summary.addStatement(insts.NewInstruction(inst++, new_v=n++, NewSiteReference.make(inst, callee.getDeclaringClass())));
        params[0] = new_v;
      }

      if (callee.getReturnType().equals(TypeReference.Void)) {
        summary.addStatement(insts.InvokeInstruction(inst++, params, v++, CallSiteReference.make(inst, callee, code), null));
        if (isNew) {
          summary.addStatement(insts.ReturnInstruction(inst++, new_v, false));          
        } 
      } else {
        int ret = v++;
        summary.addStatement(insts.InvokeInstruction(inst++, ret, params, v++, CallSiteReference.make(inst, callee, code), null));
        summary.addStatement(insts.ReturnInstruction(inst++, ret, callee.getReturnType().isPrimitiveType()));
      }
    } catch (IllegalArgumentException | InvalidClassFileException e) {
      assert false : e.toString();
    }
    
    SummarizedMethod method = new SummarizedMethod(ref, summary, LambdaSummaryClass.this);
    return method;
  }
  
  @Override
  public Collection<IField> getDeclaredStaticFields() {
     return Collections.emptySet();
  }

  @Override
  public boolean isReferenceType() {
    return true;
  }

}
