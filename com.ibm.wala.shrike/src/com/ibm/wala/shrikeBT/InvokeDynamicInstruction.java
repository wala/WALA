/******************************************************************************
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

package com.ibm.wala.shrikeBT;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodHandles.Lookup;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import com.ibm.wala.shrikeCT.BootstrapMethodsReader.BootstrapMethod;

public class InvokeDynamicInstruction extends Instruction implements IInvokeInstruction {
  protected BootstrapMethod bootstrap;
  protected String methodName;
  protected String methodType;
  
  public InvokeDynamicInstruction(short opcode, BootstrapMethod bootstrap, String methodName, String methodType) {
    super(opcode);
    this.bootstrap = bootstrap;
    this.methodName = methodName;
    this.methodType = methodType;
  }

  ConstantPoolReader getLazyConstantPool() {
    return null;
  }

  @Override
  public boolean isPEI() {
    return true;
  }

  @Override
  public Dispatch getInvocationCode() {
    int invokeType = getBootstrap().invokeType();
    switch (invokeType) {
    case 5: return Dispatch.VIRTUAL;
    case 6: return Dispatch.STATIC;
    case 7: return Dispatch.SPECIAL;
    case 9: return Dispatch.INTERFACE;
    default:
      throw new Error("unexpected dynamic invoke type " + invokeType);
    }
  }

  @Override
  final public int getPoppedCount() {
    return (getInvocationCode().equals(Dispatch.STATIC) ? 0 : 1) + Util.getParamsCount(getMethodSignature());
  }

  @Override
  final public String getPushedType(String[] types) {
    String t = Util.getReturnType(getMethodSignature());
    if (t.equals(Constants.TYPE_void)) {
      return null;
    } else {
      return t;
    }
  }

  @Override
  final public byte getPushedWordSize() {
    String t = getMethodSignature();
    int index = t.lastIndexOf(')');
    return Util.getWordSize(t, index + 1);
  }
  
  public BootstrapMethod getBootstrap() {
    return bootstrap;
  }
  
  @Override
  public String getMethodSignature() {
     return methodType;
  }

  @Override
  public String getMethodName() {
    return methodName;
  }

  @Override
  public String getClassType() {
    return "L" + getBootstrap().methodClass();
  }

  @Override
  public void visit(Visitor v) {
    v.visitInvoke(this);
  }

  @Override
  public String toString() {
    return "InvokeDynamic [" + getBootstrap() + "] " + getMethodName() + getMethodSignature();
  }

  final static class Lazy extends InvokeDynamicInstruction {
    final private ConstantPoolReader cp;

    final private int index;

    Lazy(short opcode, ConstantPoolReader cp, int index) {
      super(opcode, null, null, null);
      this.index = index;
      this.cp = cp;
    }

    int getCPIndex() {
      return index;
    }

    @Override
    public BootstrapMethod getBootstrap() {
      if (bootstrap == null) {
        bootstrap = cp.getConstantPoolDynamicBootstrap(index);
      }
      return bootstrap;
    }
    
    @Override
    public String getMethodName() {
      if (methodName == null) {
        methodName = cp.getConstantPoolDynamicName(index);
      }
      return methodName;
    }

    @Override
    public String getMethodSignature() {
      if (methodType == null) {
        methodType = cp.getConstantPoolDynamicType(index);
      }
      return methodType;
    }
    
    @Override
    ConstantPoolReader getLazyConstantPool() {
      return cp;
    }
  }

  public CallSite bootstrap(Class<?> cl) throws ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException {
    ClassLoader classLoader = cl.getClassLoader();
    ClassLoader bootstrapCL = classLoader;

    Class<?> bootstrapClass = Class.forName(getBootstrap().methodClass().replace('/', '.'), false, bootstrapCL);
    MethodType bt = makeMethodType( bootstrapCL, bootstrap.methodType());
    Method bootstrap = bootstrapClass.getMethod(this.bootstrap.methodName(), bt.parameterList().toArray(new Class[ bt.parameterCount() ]));
    Object[] args = new Object[ bt.parameterCount() ];
    
    Lookup myLookup = MethodHandles.lookup().in(cl);
    Field impl_lookup = Lookup.class.getDeclaredField("IMPL_LOOKUP"); // get the required field via reflections
    impl_lookup.setAccessible(true); // set it accessible
    Lookup lutrusted = (Lookup) impl_lookup.get(myLookup); // get the value of IMPL_LOOKUP from the Lookup instance and save it in a new Lookup object
    args[0] = lutrusted;
    args[1] = getMethodName();
    args[2] = makeMethodType(classLoader, getMethodSignature()); 
    for(int i = 3; i < bt.parameterCount(); i++) {
      args[i] = getBootstrap().callArgument(bootstrapCL,i-3);
    }
    
    bootstrap.setAccessible(true);
    
    System.err.println(cl + " : " + bootstrap);
    
    return (CallSite) bootstrap.invoke(null, args);
  }

  public static MethodType makeMethodType(ClassLoader classLoader, String descriptor) throws ClassNotFoundException {
    String returnType = Util.makeClass(Util.getReturnType(descriptor));
    Class<?> returnClass = Class.forName(returnType, false, classLoader);
    String[] paramTypes = Util.getParamsTypes(null, descriptor);
    Class<?>[] paramClasses = new Class[ paramTypes.length ];
    for(int i = 0; i < paramTypes.length; i++) {
      paramClasses[i] = Class.forName(Util.makeClass(paramTypes[i]), false, classLoader);
    }
    MethodType mt = MethodType.methodType(returnClass, paramClasses);
    return mt;
  }
  
  static InvokeDynamicInstruction make(ConstantPoolReader cp, int index, int mode) {
    if (mode != OP_invokedynamic) {
      throw new IllegalArgumentException("Unknown mode: " + mode);
    }
    return new Lazy((short) mode, cp, index);
  }

}
