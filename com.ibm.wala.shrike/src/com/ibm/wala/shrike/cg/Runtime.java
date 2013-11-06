package com.ibm.wala.shrike.cg;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Stack;

public class Runtime {
  private static final Runtime runtime = new Runtime(System.getProperty("dynamicCGFile"));
  
  private PrintWriter output;
  
  private Stack<String> callStack;
  
  private Runtime(String fileName) {
    callStack = new Stack<String>();
    callStack.push("root");

    try {
      output = new PrintWriter(new FileWriter(fileName));
    } catch (IOException e) {
      output = new PrintWriter(System.err);
    }
  }

  public static void execution(String method, Object receiver) {

  }
  
  public static void termination(String method, Object receiver, boolean exception) {
 
  }
  
  public static void pop(String method) {
    runtime.callStack.pop();
  }
  
  public static void addToCallStack(String method, Object receiver) {
    runtime.output.printf(runtime.callStack.peek() + "\t" + method + "\n");
    runtime.callStack.push(method);
    runtime.output.flush();
  }
}
