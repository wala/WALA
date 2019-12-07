package com.ibm.wala.cast.java.test;

import com.ibm.wala.cast.java.ecj.util.SourceDirCallGraph;
import com.ibm.wala.client.AbstractAnalysisEngine;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.debug.UnimplementedError;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import org.junit.Test;

public class ECJSourceDirTest extends IRTests {

  public ECJSourceDirTest() {
    super(null);
  }

  @Override
  protected AbstractAnalysisEngine<InstanceKey, CallGraphBuilder<InstanceKey>, ?> getAnalysisEngine(
      String[] mainClassDescriptors, Collection<String> sources, List<String> libs) {
    throw new UnimplementedError();
  }

  @Test
  public void testSourceDir()
      throws ClassHierarchyException, CallGraphBuilderCancelException, IOException {
    String sourceDir = getTestSrcPath();
    // any main class will do
    String mainClass = "LArray1";
    SourceDirCallGraph.runForSourceDir(sourceDir, mainClass);
  }
}
