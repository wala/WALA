/*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 *
 * This file is a derivative of code released under the terms listed below.
 *
 */
/*
 *
 * Copyright (c) 2009-2012,
 *
 *  Adam Fuchs          <afuchs@cs.umd.edu>
 *  Avik Chaudhuri      <avik@cs.umd.edu>
 *  Steve Suh           <suhsteve@gmail.com>
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. The names of the contributors may not be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */

package com.ibm.wala.dalvik.dex.instructions;

import com.ibm.wala.dalvik.classLoader.DexIMethod;
import com.ibm.wala.shrike.shrikeBT.IInvokeInstruction;
import com.ibm.wala.shrike.shrikeBT.IInvokeInstruction.IDispatch;
import com.ibm.wala.types.Descriptor;
import org.jf.dexlib2.Opcode;

public abstract class Invoke extends Instruction {

  public final int[] args;
  public final String clazzName;
  public final String methodName;
  public final String descriptor;

  protected Invoke(
      int instLoc,
      String clazzName,
      String methodName,
      String descriptor,
      int[] args,
      Opcode opcode,
      DexIMethod method) {
    super(instLoc, opcode, method);
    this.clazzName = clazzName;
    this.methodName = methodName;
    this.descriptor = descriptor;
    this.args = args;

    assert Descriptor.findOrCreateUTF8(descriptor) != null;
  }

  public static class InvokeVirtual extends Invoke {

    public InvokeVirtual(
        int instLoc,
        String clazzName,
        String methodName,
        String descriptor,
        int[] args,
        Opcode opcode,
        DexIMethod method) {
      super(instLoc, clazzName, methodName, descriptor, args, opcode, method);
    }

    @Override
    public IDispatch getInvocationCode() {
      return IInvokeInstruction.Dispatch.VIRTUAL;
    }

    @Override
    public String toString() {
      StringBuilder argString = new StringBuilder();
      argString.append('(');
      String sep = "";
      for (int r : args) {
        argString.append(sep).append(r);
        sep = ",";
      }
      argString.append(')');
      return "InvokeVirtual "
          + clazzName
          + ' '
          + methodName
          + ' '
          + descriptor
          + ' '
          + argString
          + ' '
          + pc;
    }
  }

  public static class InvokeSuper extends Invoke {
    public InvokeSuper(
        int instLoc,
        String clazzName,
        String methodName,
        String descriptor,
        int[] args,
        Opcode opcode,
        DexIMethod method) {
      super(instLoc, clazzName, methodName, descriptor, args, opcode, method);
      assert descriptor.contains("(");
    }

    @Override
    public IDispatch getInvocationCode() {
      // TODO: check that this is correct -- I suspect the invoke super in dex is for method
      // protection rather than dispatching
      return IInvokeInstruction.Dispatch.SPECIAL;
    }

    @Override
    public String toString() {
      StringBuilder argString = new StringBuilder();
      argString.append('(');
      String sep = "";
      for (int r : args) {
        argString.append(sep).append(r);
        sep = ",";
      }
      argString.append(')');
      return "InvokeSuper "
          + clazzName
          + ' '
          + methodName
          + ' '
          + descriptor
          + ' '
          + argString
          + ' '
          + pc;
    }
  }

  public static class InvokeDirect extends Invoke {

    public InvokeDirect(
        int instLoc,
        String clazzName,
        String methodName,
        String descriptor,
        int[] args,
        Opcode opcode,
        DexIMethod method) {
      super(instLoc, clazzName, methodName, descriptor, args, opcode, method);
    }

    @Override
    public IDispatch getInvocationCode() {
      return IInvokeInstruction.Dispatch.SPECIAL;
    }

    @Override
    public String toString() {
      StringBuilder argString = new StringBuilder();
      argString.append('(');
      String sep = "";
      for (int r : args) {
        argString.append(sep).append(r);
        sep = ",";
      }
      argString.append(')');
      return "InvokeDirect "
          + clazzName
          + ' '
          + methodName
          + ' '
          + descriptor
          + ' '
          + argString
          + ' '
          + pc;
    }
  }

  public static class InvokeStatic extends Invoke {

    public InvokeStatic(
        int instLoc,
        String clazzName,
        String methodName,
        String descriptor,
        int[] args,
        Opcode opcode,
        DexIMethod method) {
      super(instLoc, clazzName, methodName, descriptor, args, opcode, method);
    }

    @Override
    public IDispatch getInvocationCode() {
      return IInvokeInstruction.Dispatch.STATIC;
    }

    @Override
    public String toString() {
      StringBuilder argString = new StringBuilder();
      argString.append('(');
      String sep = "";
      for (int r : args) {
        argString.append(sep).append(r);
        sep = ",";
      }
      argString.append(')');
      return "InvokeStatic "
          + clazzName
          + ' '
          + methodName
          + ' '
          + descriptor
          + ' '
          + argString
          + ' '
          + pc;
    }
  }

  public static class InvokeInterface extends Invoke {

    public InvokeInterface(
        int instLoc,
        String clazzName,
        String methodName,
        String descriptor,
        int[] args,
        Opcode opcode,
        DexIMethod method) {
      super(instLoc, clazzName, methodName, descriptor, args, opcode, method);
    }

    @Override
    public IDispatch getInvocationCode() {
      return IInvokeInstruction.Dispatch.INTERFACE;
    }

    @Override
    public String toString() {
      StringBuilder argString = new StringBuilder();
      argString.append('(');
      String sep = "";
      for (int r : args) {
        argString.append(sep).append(r);
        sep = ",";
      }
      argString.append(')');
      return "InvokeInterface "
          + clazzName
          + ' '
          + methodName
          + ' '
          + descriptor
          + ' '
          + argString
          + ' '
          + pc;
    }
  }

  @Override
  public void visit(Visitor visitor) {
    visitor.visitInvoke(this);
  }

  public abstract IDispatch getInvocationCode();
}
