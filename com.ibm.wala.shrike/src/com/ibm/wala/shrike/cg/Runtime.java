package com.ibm.wala.shrike.cg;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Stack;

import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.config.SetOfClasses;

public class Runtime {
  private static final Runtime runtime = 
      new Runtime(System.getProperty("dynamicCGFile"), System.getProperty("dynamicCGFilter"));
  
  private PrintWriter output;
  private SetOfClasses filter;
  
  private Stack<String> callStack;
  
  private Runtime(String fileName, String filterFileName) {
    callStack = new Stack<String>();
    callStack.push("root");

    try {
      filter = new FileOfClasses(new FileInputStream(filterFileName));
    } catch (Exception e) {
      filter = null;
    }

    try {
      output = new PrintWriter(new FileWriter(fileName));
    } catch (IOException e) {
      output = new PrintWriter(System.err);
    }
  }

  public static void execution(String klass, String method, Object receiver) {
    if (runtime.filter == null || ! runtime.filter.contains(klass)) {
      runtime.output.printf(runtime.callStack.peek() + "\t" + klass + "\t" + method + "\n");
      runtime.output.flush();
    }

    runtime.callStack.push(klass + "\t" + method);
  }
  
  public static void termination(String klass, String method, Object receiver, boolean exception) {
    runtime.callStack.pop();
  }
  
  public static void pop(String klass, String method) {
 
  }
  
  public static void addToCallStack(String klass, String method, Object receiver) {

  }
}
