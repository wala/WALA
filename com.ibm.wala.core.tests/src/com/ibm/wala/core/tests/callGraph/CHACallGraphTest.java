package com.ibm.wala.core.tests.callGraph;

import java.io.IOException;

import org.junit.Test;

import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.cha.CHACallGraph;
import com.ibm.wala.ipa.callgraph.impl.AllApplicationEntrypoints;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.util.CancelException;

public class CHACallGraphTest {
  @Test public void testJava_cup() throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testCHA(TestConstants.JAVA_CUP, TestConstants.JAVA_CUP_MAIN);
  }
  
  public static CallGraph testCHA(String scopeFile, String mainClass) throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    AnalysisScope scope = CallGraphTestUtil.makeJ2SEAnalysisScope(scopeFile, CallGraphTestUtil.REGRESSION_EXCLUSIONS);
    ClassHierarchy cha = ClassHierarchy.make(scope);
    Iterable<Entrypoint> entrypoints;
    if (mainClass == null) {
      entrypoints = new AllApplicationEntrypoints(scope, cha);
    } else {
      entrypoints = com.ibm.wala.ipa.callgraph.impl.Util.makeMainEntrypoints(scope, cha, mainClass);
    }
    
    CHACallGraph CG = new CHACallGraph(cha);
    CG.init(entrypoints);
    
    return CG;
  }
  
  public static void main(String[] args) throws ClassHierarchyException, IllegalArgumentException, CancelException, IOException {
    testCHA(args[0], args.length>1? args[1]: null);
  }
}
