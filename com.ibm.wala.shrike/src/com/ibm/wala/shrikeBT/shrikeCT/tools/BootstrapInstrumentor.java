/*******************************************************************************
 * Copyright (c) 2002,2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.shrikeBT.shrikeCT.tools;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.Set;

import com.ibm.wala.shrikeBT.ConstantInstruction;
import com.ibm.wala.shrikeBT.ConstantPoolReader;
import com.ibm.wala.shrikeBT.Constants;
import com.ibm.wala.shrikeBT.Decoder.InvalidBytecodeException;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.shrikeBT.IInvokeInstruction.Dispatch;
import com.ibm.wala.shrikeBT.InvokeDynamicInstruction;
import com.ibm.wala.shrikeBT.InvokeInstruction;
import com.ibm.wala.shrikeBT.MethodData;
import com.ibm.wala.shrikeBT.ReturnInstruction;
import com.ibm.wala.shrikeBT.shrikeCT.CTDecoder;
import com.ibm.wala.shrikeBT.shrikeCT.CTUtils;
import com.ibm.wala.shrikeBT.shrikeCT.ClassInstrumenter;
import com.ibm.wala.shrikeBT.shrikeCT.OfflineInstrumenter;
import com.ibm.wala.shrikeCT.BootstrapMethodsReader.BootstrapMethod;
import com.ibm.wala.shrikeCT.ClassReader;
import com.ibm.wala.shrikeCT.ClassWriter;
import com.ibm.wala.shrikeCT.CodeReader;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.collections.HashSetFactory;

public class BootstrapInstrumentor {
  final private PrintWriter w;

  private int idx;
  
  /**
   * Get ready to print a class to the given output stream.
   */
  public BootstrapInstrumentor(PrintWriter w) {
    this.w = w;
  }

  public static void main(String[] args) throws Exception {
    PrintWriter w = new PrintWriter(new BufferedWriter(new OutputStreamWriter(System.out)));
    BootstrapInstrumentor p = new BootstrapInstrumentor(w);
    p.doit(args);
  }

  public void doit(String[] args) throws Exception {
    OfflineInstrumenter oi = new OfflineInstrumenter();
    oi.parseStandardArgs(args);
   
    oi.setPassUnmodifiedClasses(true);
    
    ClassInstrumenter ci;
    oi.beginTraversal();
    while ((ci = oi.nextClass()) != null) {
      try {
        idx = 0;
        Set<MethodData> bss = doClass(ci);
        ClassWriter cw = ci.emitClass();
        for(MethodData md : bss) {
          CTUtils.compileAndAddMethodToClassWriter(md, cw, null);
        }
        oi.outputModifiedClass(ci, cw);
      } finally {
        w.flush();
      }
    }

    oi.close();
  }

  private Set<MethodData> dumpAttributes(ClassInstrumenter ci, int i, ClassReader.AttrIterator attrs) throws InvalidClassFileException,
      InvalidBytecodeException, IOException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException {
    Set<MethodData> result = HashSetFactory.make();
    ClassReader cr = ci.getReader();
    for (; attrs.isValid(); attrs.advance()) {
      String name = attrs.getName();
      if (name.equals("Code")) {
        CodeReader code = new CodeReader(attrs);

        CTDecoder decoder = new CTDecoder(code);
        decoder.decode();
        ConstantPoolReader cpr = decoder.getConstantPool();
        IInstruction[] origInsts = decoder.getInstructions();
        for(IInstruction inst : origInsts) {
          if (inst instanceof InvokeDynamicInstruction) {
            InvokeDynamicInstruction x = (InvokeDynamicInstruction) inst;
            BootstrapMethod m = x.getBootstrap();

            IInstruction insts[] = new IInstruction[ m.callArgumentCount() + 8];
            int arg = 0;

            insts[arg++] = InvokeInstruction.make("()Ljava/lang/invoke/MethodHandles$Lookup;", "java/lang/invoke/MethodHandles", "lookup", Dispatch.STATIC);

            insts[arg++] = ConstantInstruction.makeString(x.getMethodName());
            
            insts[arg++] = ConstantInstruction.makeString(x.getMethodSignature());
            insts[arg++] = ConstantInstruction.makeClass(cr.getName());
            insts[arg++] = InvokeInstruction.make("()Ljava/lang/ClassLoader;", "java/lang/Class", "getClassLoader", Dispatch.VIRTUAL);
            insts[arg++] = InvokeInstruction.make("(Ljava/lang/String;Ljava/lang/ClassLoader;)Ljava/lang/invoke/MethodType;", "java/lang/invoke/MethodType", "fromMethodDescriptorString", Dispatch.STATIC);
            
            for(int an = 0; an < m.callArgumentCount(); an++) {
              insts[arg++] = ConstantInstruction.make(cpr, m.callArgumentIndex(an));
            }
            
            insts[arg++] = InvokeInstruction.make(m.methodType(), m.methodClass(), m.methodName(), ((InvokeDynamicInstruction) inst).getInvocationCode());
            insts[arg++] = ReturnInstruction.make("Ljava/lang/invoke/CallSite;");
            
            result.add(MethodData.makeWithDefaultHandlersAndInstToBytecodes(Constants.ACC_PUBLIC|Constants.ACC_STATIC, cr.getName(), "bs" + (idx++), "()Ljava/lang/invoke/CallSite;", insts));
          }
        }
      }
    }
    
    return result;
  }



  /**
   * Print a class.
   * @throws InvocationTargetException 
   * @throws IllegalAccessException 
   * @throws SecurityException 
   * @throws NoSuchMethodException 
   * @throws ClassNotFoundException 
   * 
   * @throws IllegalArgumentException if cr is null
   * @throws NoSuchFieldException 
   */
  public Set<MethodData> doClass(final ClassInstrumenter ci) throws InvalidClassFileException, InvalidBytecodeException, IOException, ClassNotFoundException, NoSuchMethodException, SecurityException, IllegalAccessException, IllegalArgumentException, InvocationTargetException, NoSuchFieldException {
    ClassReader cr = ci.getReader();
    ClassReader.AttrIterator attrs = new ClassReader.AttrIterator();
    cr.initClassAttributeIterator(attrs);
    int methodCount = cr.getMethodCount();
    
    Set<MethodData> result = HashSetFactory.make();
    for (int i = 0; i < methodCount; i++) {
      cr.initMethodAttributeIterator(i, attrs);
      result.addAll(dumpAttributes(ci, i, attrs));
    }
    
    return result;
  }
}
