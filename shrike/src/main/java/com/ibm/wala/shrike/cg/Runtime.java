/*
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */

package com.ibm.wala.shrike.cg;

import com.ibm.wala.util.config.PatternsFilter;
import com.ibm.wala.util.config.StringFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;
import java.util.zip.GZIPOutputStream;

@SuppressWarnings("AvoidCommonTypeNames")
public class Runtime {
  public interface Policy {
    void callback(StackTraceElement[] stack, String klass, String method, Object receiver);
  }

  private static class DefaultCallbackPolicy implements Policy {
    @Override
    public void callback(StackTraceElement[] stack, String klass, String method, Object receiver) {
      // stack frames: Runtime.execution(0), callee(1), caller(2)
      String root =
          "<clinit>".equals(stack[1].getMethodName())
              ? "clinit"
              : "finalize".equals(stack[1].getMethodName()) ? "root" : "callbacks";
      String line = root + '\t' + bashToDescriptor(klass) + '\t' + String.valueOf(method) + '\n';
      synchronized (runtime) {
        if (runtime.output != null) {
          runtime.output.printf(line);
          runtime.output.flush();
        }
      }
    }
  }

  private static final Runtime runtime =
      new Runtime(
          System.getProperty("dynamicCGFile"),
          System.getProperty("dynamicCGFilter"),
          System.getProperty("policyClass", "com.ibm.wala.shrike.cg.Runtime$DefaultPolicy"));

  private PrintWriter output;
  private StringFilter filter;
  private Policy handleCallback;
  private final ThreadLocal<String> currentSite = new ThreadLocal<>();

  private record CallFrame(String name, String className, String methodName, int depth) {
    boolean isConstructor() {
      return "<init>".equals(methodName);
    }

    boolean isActive(StackTraceElement[] stack, int firstActiveIndex) {
      // Count from the bottom so nested calls do not change the constructor's position.
      int index = stack.length - 1 - depth;
      return index >= firstActiveIndex
          && index < stack.length
          && className.equals(stack[index].getClassName())
          && methodName.equals(stack[index].getMethodName());
    }
  }

  private final ThreadLocal<ArrayDeque<CallFrame>> callStacks =
      ThreadLocal.withInitial(
          () -> {
            ArrayDeque<CallFrame> callStack = new ArrayDeque<>();
            callStack.push(new CallFrame("root", null, null, -1));
            return callStack;
          });

  private void discardExitedConstructors(StackTraceElement[] stack, int firstActiveIndex) {
    // A verified constructor cannot catch failure of its initializing super/this call.
    // Remove its entry once that constructor has unwound and tracing resumes.
    ArrayDeque<CallFrame> frames = callStacks.get();
    while (frames.peek().isConstructor() && !frames.peek().isActive(stack, firstActiveIndex)) {
      frames.pop();
    }
  }

  private Runtime(String fileName, String filterFileName, String policyClassName) {
    try (final FileInputStream in = new FileInputStream(filterFileName)) {
      filter = new PatternsFilter(in);
    } catch (Exception e) {
      filter = null;
    }

    try {
      output =
          new PrintWriter(
              new OutputStreamWriter(
                  new GZIPOutputStream(new FileOutputStream(fileName)), StandardCharsets.UTF_8));
    } catch (IOException e) {
      output = new PrintWriter(System.err);
    }

    try {
      handleCallback =
          Class.forName(policyClassName)
              .asSubclass(Policy.class)
              .getDeclaredConstructor()
              .newInstance();
    } catch (InstantiationException
        | IllegalAccessException
        | ClassNotFoundException
        | IllegalArgumentException
        | InvocationTargetException
        | NoSuchMethodException
        | SecurityException e) {
      handleCallback = new DefaultCallbackPolicy();
    }

    java.lang.Runtime.getRuntime().addShutdownHook(new Thread(Runtime::endTrace));
  }

  public static void endTrace() {
    synchronized (runtime) {
      if (runtime.output != null) {
        runtime.output.close();
        runtime.output = null;
      }
    }
  }

  public static Object NULL_TAG =
      new Object() {
        @Override
        public String toString() {
          return "NULL TAG";
        }
      };

  public static String bashToDescriptor(String className) {
    if (className.startsWith("class ")) {
      className = className.substring(6);
    }
    if (className.indexOf('.') >= 0) {
      className = className.replace('.', '/');
    }
    return className;
  }

  public static void execution(String klass, String method, Object receiver) {
    StackTraceElement[] stack = new Throwable().getStackTrace();
    // The method at stack[1] is entering now, so it cannot own an existing frame.
    runtime.discardExitedConstructors(stack, 2);
    runtime.currentSite.remove();
    if (runtime.filter == null || !runtime.filter.test(bashToDescriptor(klass))) {
      if (runtime.output != null) {
        String caller = runtime.callStacks.get().peek().name();

        //
        // check for expected caller
        //
        boolean handled = false;
        if (runtime.handleCallback != null) {
          if (stack.length > 2) {
            // frames: Runtime.execution(0), callee(1), caller(2)
            StackTraceElement callerFrame = stack[2];
            if (!callerFrame.getMethodName().startsWith("$")) {
              if (!caller.contains(callerFrame.getMethodName())
                  || !caller.contains(bashToDescriptor(callerFrame.getClassName()))) {
                runtime.handleCallback.callback(stack, klass, method, receiver);
                handled = true;
              }
            }
          }
        }

        if (!handled) {
          String line =
              (method.contains("<clinit>") ? "clinit" : String.valueOf(caller))
                  + '\t'
                  + bashToDescriptor(klass)
                  + '\t'
                  + String.valueOf(method)
                  + '\n';
          synchronized (runtime) {
            if (runtime.output != null) {
              runtime.output.printf(line);
              runtime.output.flush();
            }
          }
        }
      }
    }

    runtime
        .callStacks
        .get()
        .push(
            new CallFrame(
                bashToDescriptor(klass) + '\t' + method,
                stack[1].getClassName(),
                stack[1].getMethodName(),
                stack.length - 2));
  }

  @SuppressWarnings("unused")
  public static void termination(String klass, String method, Object receiver, boolean exception) {
    if (runtime.callStacks.get().peek().isConstructor()) {
      runtime.discardExitedConstructors(new Throwable().getStackTrace(), 1);
    }
    runtime.callStacks.get().pop();
  }

  public static void pop() {
    if (runtime.currentSite.get() != null) {
      synchronized (runtime) {
        if (runtime.output != null) {
          runtime.output.printf("return from %s%n", runtime.currentSite.get());
          runtime.output.flush();
        }
      }

      runtime.currentSite.remove();
    }
  }

  public static void addToCallStack(String klass, String method, Object receiver) {
    if (runtime.callStacks.get().peek().isConstructor()) {
      runtime.discardExitedConstructors(new Throwable().getStackTrace(), 1);
    }
    String callerClass =
        runtime.callStacks.get().isEmpty()
            ? "BLOB"
            : runtime.callStacks.get().peek().name().split("\t")[0];
    String callerMethod =
        runtime.callStacks.get().isEmpty()
            ? "BLOB"
            : runtime.callStacks.get().peek().name().split("\t")[1];
    runtime.currentSite.set(
        "%s\t%s\t%s\t%s\t%s".formatted(callerClass, callerMethod, klass, method, receiver));
    //	  runtime.currentSite = klass + "\t" + method + "\t" + receiver;
    synchronized (runtime) {
      if (runtime.output != null) {
        runtime.output.printf("call to %s%n", runtime.currentSite.get());
        runtime.output.flush();
      }
    }
  }
}
