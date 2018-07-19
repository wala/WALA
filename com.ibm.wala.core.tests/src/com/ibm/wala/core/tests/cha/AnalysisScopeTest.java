package com.ibm.wala.core.tests.cha;

import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.FileProvider;

import org.junit.Assert;
import org.junit.Test;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

public class AnalysisScopeTest {

  @Test
  public void testJarInputStream() throws IOException, ClassHierarchyException {
    AnalysisScope scope = AnalysisScopeReader.readJavaScope(TestConstants.WALA_TESTDATA,
        (new FileProvider()).getFile("J2SEClassHierarchyExclusions.txt"),
        AnalysisScopeTest.class.getClassLoader());
    // assumes com.ibm.wala.core.tests is the current working directory
    Path bcelJarPath = Paths.get(
        System.getProperty("user.dir"),
        "..",
        "com.ibm.wala.core.testdata",
        "bcel-5.2.jar"
    );
    scope.addInputStreamForJarToScope(ClassLoaderReference.Application, new FileInputStream
        (bcelJarPath.toString()));
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    Assert.assertNotNull("couldn't find expected class", cha.lookupClass(
        TypeReference.findOrCreate(ClassLoaderReference.Application,
            "Lorg/apache/bcel/verifier/Verifier")));
  }
}
