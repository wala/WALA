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

package com.ibm.wala.shrike.cg;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.util.Stack;
import java.util.zip.GZIPOutputStream;

import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.config.SetOfClasses;

public class Runtime {
  private static final Runtime runtime = 
      new Runtime(System.getProperty("dynamicCGFile"), System.getProperty("dynamicCGFilter"));
  
  private PrintWriter output;
  private SetOfClasses filter;
  private boolean handleUninstrumentedCode = false;
  
  private ThreadLocal<Stack<String>> callStacks = new ThreadLocal<Stack<String>>() {

    @Override
    protected Stack<String> initialValue() {
      Stack<String> callStack = new Stack<String>();
      callStack.push("root");
      return callStack;
    }
 
  };
  
  private Runtime(String fileName, String filterFileName) {
    try {
      filter = new FileOfClasses(new FileInputStream(filterFileName));
    } catch (Exception e) {
      filter = null;
    }

    try {
      output = new PrintWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(fileName))));
    } catch (IOException e) {
      output = new PrintWriter(System.err);
    }
    
    handleUninstrumentedCode = Boolean.parseBoolean(System.getProperty("dynamicCGHandleMissing", "false"));
    
    java.lang.Runtime.getRuntime().addShutdownHook(new Thread() {
      @Override
      public void run() {
        endTrace();
      }
    });
  }

  public static void endTrace() {
    if (runtime.output != null) {
      runtime.output.close();
      runtime.output = null;
    }
  }
  
  public static Object NULL_TAG = new Object() {
    @Override
    public String toString() {
      return "NULL TAG";
    }
  };
  
  public static void execution(String klass, String method, Object receiver) {
    if (runtime.filter == null || ! runtime.filter.contains(klass)) {
      if (runtime.output != null) {
        String caller = runtime.callStacks.get().peek();
        
        checkValid: {
          if (runtime.handleUninstrumentedCode) {
            StackTraceElement[] stack = (new Throwable()).getStackTrace();
            if (stack.length > 2) {
              // frames: me(0), callee(1), caller(2)
              StackTraceElement callerFrame = stack[2];
              if (! caller.contains(callerFrame.getMethodName()) ||
                  ! caller.contains(callerFrame.getClassName().replace('.', '/'))) {
                break checkValid;
              }
            }
          }
        
          String line = caller + "\t" + klass + "\t" + method + "\n";
          synchronized (runtime) {
            runtime.output.printf(line);
          }
        }
      }
    }

    runtime.callStacks.get().push(klass + "\t" + method);
  }
  
  public static void termination(String klass, String method, Object receiver, boolean exception) {
    runtime.callStacks.get().pop();
  }
  
  public static void pop(String klass, String method) {
 
  }
  
  public static void addToCallStack(String klass, String method, Object receiver) {

  }
}
