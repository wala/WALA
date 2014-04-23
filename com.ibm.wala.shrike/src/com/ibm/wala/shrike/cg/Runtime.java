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
        String line = runtime.callStacks.get().peek() + "\t" + klass + "\t" + method + "\n";
        synchronized (runtime) {
          runtime.output.printf(line);
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
