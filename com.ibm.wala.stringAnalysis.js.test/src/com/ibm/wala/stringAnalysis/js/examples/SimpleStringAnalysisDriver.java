package com.ibm.wala.stringAnalysis.js.examples;

import java.io.File;
import java.util.Collections;

import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.stringAnalysis.js.client.JSStringAnalysisEngine;
import com.ibm.wala.util.debug.Assertions;

public class SimpleStringAnalysisDriver {

  public static void main(String[] args) {
    Assertions._assert(args.length == 3, "wrong number of parameters");

    String scriptPathName = args[0];
    File scriptFile = new File(scriptPathName);
    String testName = 
      scriptPathName.substring(scriptPathName.lastIndexOf('/'));

    JSStringAnalysisEngine engine = new JSStringAnalysisEngine();
    engine.setModuleFiles(
      Collections.singleton(new SourceFileModule(scriptFile, testName)));
    engine.buildDefaultCallGraph();
    
    String variableName = args[1];
    String patternString = args[2];
    System.err.println(engine.containsAll(variableName, patternString));
  }

}
