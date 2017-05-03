package com.ibm.wala.shrike.cg;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.Writer;
import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;
import java.security.ProtectionDomain;

import com.ibm.wala.shrikeBT.analysis.Analyzer.FailureException;
import com.ibm.wala.shrikeBT.analysis.ClassHierarchyStore;
import com.ibm.wala.shrikeBT.shrikeCT.CTUtils;
import com.ibm.wala.shrikeBT.shrikeCT.ClassInstrumenter;
import com.ibm.wala.shrikeBT.shrikeCT.OfflineInstrumenter;
import com.ibm.wala.shrikeCT.InvalidClassFileException;

public class OnlineDynamicCallGraph implements ClassFileTransformer {

  private ClassHierarchyStore cha = new ClassHierarchyStore();

  private Writer out = new PrintWriter(System.err);
  
  public OnlineDynamicCallGraph() throws IllegalArgumentException, IOException, InvalidClassFileException {
    OfflineInstrumenter libReader = new OfflineInstrumenter();
    for (String cps : new String[]{ System.getProperty("java.class.path"), System.getProperty("sun.boot.class.path") }) {
      for (String cp : cps.split(File.pathSeparator)) {
        File x = new File(cp);
        if (x.exists()) {
          if (x.isDirectory()) {
            libReader.addInputDirectory(x, x);
          } else {
            libReader.addInputJar(x);
          }
        }
      } 
    }
    
    ClassInstrumenter ci;
    while ((ci = libReader.nextClass()) != null) {
      CTUtils.addClassToHierarchy(cha, ci.getReader());
    }
  }
  
  @Override
  public byte[] transform(ClassLoader loader, String className, Class<?> classBeingRedefined, ProtectionDomain protectionDomain,
      byte[] classfileBuffer) throws IllegalClassFormatException {
    try {
      if (className.contains("com/ibm/wala") || className.contains("java/lang") || (className.contains("java/") && !className.matches("java/util/[A-Z]")) || className.contains("sun/")) {
        return classfileBuffer;
      } else {
        ClassInstrumenter ci = new ClassInstrumenter(className, classfileBuffer, cha);
        return OfflineDynamicCallGraph.doClass(ci, out).makeBytes();
      }
    } catch (InvalidClassFileException | IOException | FailureException e) {
      e.printStackTrace();
      System.err.println("got here with " + e.getMessage());
      throw new IllegalClassFormatException(e.getMessage());
    }
  }
  
  public static void premain(String agentArgs, Instrumentation inst) throws IllegalArgumentException, IOException, InvalidClassFileException {
    inst.addTransformer(new OnlineDynamicCallGraph());
  }

}
