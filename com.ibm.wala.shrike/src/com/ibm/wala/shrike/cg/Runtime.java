package com.ibm.wala.shrike.cg;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

public class Runtime {
  private static final Runtime runtime = new Runtime(System.getProperty("dynamicCGFile"));
  
  private PrintWriter output;
  
  private Runtime(String fileName) {
    try {
      output = new PrintWriter(new FileWriter(fileName));
    } catch (IOException e) {
      output = new PrintWriter(System.err);
    }
  }

  public static void execution(String method, Object receiver) {
    runtime.output.printf("starting %s\n", method);
    runtime.output.flush();
  }
  
  public static void termination(String method, Object receiver, boolean exception) {
    runtime.output.printf("ending %s\n", method);    
    runtime.output.flush();
  }
  
  public static void pop(String method) {
    
  }
  
  public static void addToCallStack(String method, Object receiver) {
    runtime.output.printf("calling %s\n", method);    
    runtime.output.flush();
  }
}
