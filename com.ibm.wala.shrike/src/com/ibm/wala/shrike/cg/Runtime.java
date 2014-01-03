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
      output = new PrintWriter(new OutputStreamWriter(new GZIPOutputStream(new FileOutputStream(fileName))));
    } catch (IOException e) {
      output = new PrintWriter(System.err);
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
      runtime.output.printf(runtime.callStack.peek() + "\t" + klass + "\t" + method + "\n");
    }

    runtime.callStack.push(klass + "\t" + method);
  }
  
  public static void termination(String klass, String method, Object receiver, boolean exception) {
    runtime.callStack.pop();
    if ("root".equals(runtime.callStack.peek())) {
      runtime.output.close();
    }
  }
  
  public static void pop(String klass, String method) {
 
  }
  
  public static void addToCallStack(String klass, String method, Object receiver) {

  }
}
