package com.ibm.wala.core.tests.cha;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.CoreMatchers.hasItem;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
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
import java.lang.reflect.Type;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.jar.JarFile;
import com.ibm.wala.properties.WalaProperties;
import org.junit.jupiter.api.Test;

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
        cha.lookupClass(
            TypeReference.findOrCreate(
                ClassLoaderReference.Application, "Lorg/apache/bcel/verifier/Verifier")),
        "couldn't find expected class");
  }

  @Test
  public void testBaseScope() throws IOException, ClassHierarchyException {
    AnalysisScope scope =
        AnalysisScopeReader.instance.readJavaScope(
            "primordial-base.txt", null, AnalysisScopeTest.class.getClassLoader());
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    assertNotNull(
        cha.lookupClass(
            TypeReference.findOrCreate(ClassLoaderReference.Application, "Ljava/util/ArrayList")),
        "couldn't find expected class");
    assertNull(
        cha.lookupClass(
            TypeReference.findOrCreate(
                ClassLoaderReference.Application, "Ljava/awt/AlphaComposite")),
        "found unexpected class");
  }

  @Test
  public void testToJson() throws IOException {
    AnalysisScope scope =
        AnalysisScopeReader.instance.readJavaScope(
            TestConstants.WALA_TESTDATA,
            new FileProvider().getFile("GUIExclusions.txt"),
            AnalysisScopeTest.class.getClassLoader());
    Gson gson = new Gson();
    Type type = new TypeToken<LinkedHashMap<String, Object>>() {}.getType();
    LinkedHashMap<String, Object> map = gson.fromJson(scope.toJson(), type);
    assertEquals(
        List.of("java\\/awt\\/.*", "javax\\/swing\\/.*", "sun\\/awt\\/.*", "sun\\/swing\\/.*"),
        map.get("Exclusions"));

    @SuppressWarnings("unchecked")
    Map<String, List<String>> loaders = (Map<String, List<String>>) map.get("Loaders");
    Set<String> loaderKeys =
        new HashSet<>(List.of("Primordial", "Extension", "Application", "Synthetic"));
    assertEquals(loaders.keySet(), loaderKeys);
    assertEquals(2, loaders.get("Primordial").size());
    assertThat(loaders.get("Primordial"), hasItem("Nested Jar File:primordial.jar.model"));
    assertEquals(1, loaders.get("Application").size());
    assertThat(
        loaders.get("Application").get(0), containsString("com.ibm.wala.core.testdata_1.0.0.jar"));
    assertEquals(0, loaders.get("Extension").size());
    assertEquals(0, loaders.get("Synthetic").size());
  }

  @Test
  public void testToJsonCustom() throws IOException {
    AnalysisScope scope;
    scope = AnalysisScope.createJavaAnalysisScope();
    AnalysisScope tempScope =
        AnalysisScopeReader.instance.readJavaScope(
            TestConstants.WALA_TESTDATA,
            new FileProvider().getFile("GUIExclusions.txt"),
            AnalysisScopeTest.class.getClassLoader());
    scope.setExclusions(tempScope.getExclusions());
    String[] stdlibs = WalaProperties.getJ2SEJarFiles();
    Arrays.sort(stdlibs);
    int cnt = 0;
    for (String stdlib : stdlibs) {
      scope.addToScope(ClassLoaderReference.Primordial, new JarFile(stdlib));
      scope.addToScope(ClassLoaderReference.Application, new JarFile(stdlib));
      cnt++;
      if(cnt == 5) {
        break;
      }
    }
    Gson gson = new Gson();
    Type type = new TypeToken<LinkedHashMap<String, Object>>() {}.getType();
    LinkedHashMap<String, Object> map = gson.fromJson(scope.toJson(), type);
    assertEquals(
        List.of("java\\/awt\\/.*", "javax\\/swing\\/.*", "sun\\/awt\\/.*", "sun\\/swing\\/.*"),
        map.get("Exclusions"));

    @SuppressWarnings("unchecked")
    Map<String, List<String>> loaders = (Map<String, List<String>>) map.get("Loaders");
    Set<String> loaderKeys =
        new HashSet<>(List.of("Primordial", "Extension", "Application", "Synthetic"));
    assertEquals(loaders.keySet(), loaderKeys);
    assertEquals(5, loaders.get("Primordial").size());
    assertThat(loaders.get("Primordial"), hasItem("JarFileModule:/Users/aakgna/Library/Java/JavaVirtualMachines/corretto-11.0.15/Contents/Home/jmods/java.base.jmod"));
    assertEquals(5, loaders.get("Application").size());
    assertThat(
        loaders.get("Application").get(0), containsString("JarFileModule:/Users/aakgna/Library/Java/JavaVirtualMachines/corretto-11.0.15/Contents/Home/jmods/java.base.jmod"));
    assertEquals(0, loaders.get("Extension").size());
    assertEquals(0, loaders.get("Synthetic").size());
  }
}
