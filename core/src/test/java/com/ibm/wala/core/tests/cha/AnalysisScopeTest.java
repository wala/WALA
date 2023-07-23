package com.ibm.wala.core.tests.cha;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.util.config.AnalysisScopeReader;
import com.ibm.wala.core.util.io.FileProvider;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.junit.Test;

public class AnalysisScopeTest {

  @Test
  public void testJarInputStream() throws IOException, ClassHierarchyException {
    AnalysisScope scope =
        AnalysisScopeReader.instance.readJavaScope(
            TestConstants.WALA_TESTDATA,
            new FileProvider().getFile("J2SEClassHierarchyExclusions.txt"),
            AnalysisScopeTest.class.getClassLoader());
    // assumes com.ibm.wala.core is the current working directory
    Path bcelJarPath = Paths.get("build", "extractBcel", "bcel-5.2.jar");
    scope.addInputStreamForJarToScope(
        ClassLoaderReference.Application, new FileInputStream(bcelJarPath.toString()));
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    assertNotNull(
        "couldn't find expected class",
        cha.lookupClass(
            TypeReference.findOrCreate(
                ClassLoaderReference.Application, "Lorg/apache/bcel/verifier/Verifier")));
  }

  @Test
  public void testBaseScope() throws IOException, ClassHierarchyException {
    AnalysisScope scope =
        AnalysisScopeReader.instance.readJavaScope(
            "primordial-base.txt", null, AnalysisScopeTest.class.getClassLoader());
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    assertNotNull(
        "couldn't find expected class",
        cha.lookupClass(
            TypeReference.findOrCreate(ClassLoaderReference.Application, "Ljava/util/ArrayList")));
    assertNull(
        "found unexpected class",
        cha.lookupClass(
            TypeReference.findOrCreate(
                ClassLoaderReference.Application, "Ljava/awt/AlphaComposite")));
  }
}
