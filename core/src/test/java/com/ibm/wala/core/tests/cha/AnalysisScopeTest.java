package com.ibm.wala.core.tests.cha;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.util.config.AnalysisScopeReader;
import com.ibm.wala.core.util.io.FileProvider;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.Iterator2Collection;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.Test;
import com.google.gson.Gson;
import java.lang.reflect.Type;
import com.google.gson.reflect.TypeToken;

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
        Type type = new TypeToken<LinkedHashMap<String, Object>>(){}.getType();
        LinkedHashMap<String, Object> map = gson.fromJson(scope.toJson(), type);
        if(map.containsKey("Exclusions")) {
            assertEquals(List.of("java\\/awt\\/.*", "javax\\/swing\\/.*", "sun\\/awt\\/.*", "sun\\/swing\\/.*"), map.get("Exclusions"));
        }
        
        if (map.get("Loaders") instanceof Map) {
            Map<String, List<String>> loaders = (Map<String, List<String>>) map.get("Loaders");
            Set<String> loadKey = new HashSet<>(Arrays.asList("Primordial", "Extension", "Application", "Synthetic"));
            assertEquals(loaders.keySet(), loadKey);
            if(loaders.containsKey("Primordial")) {
                assertEquals(2, loaders.get("Primordial").size());
                assertEquals(true, loaders.get("Primordial").contains("Nested Jar File:primordial.jar.model"));
            }
            if(loaders.containsKey("Application")) {
                assertEquals(1, loaders.get("Application").size());
                assertEquals(true, loaders.get("Application").get(0).contains("com.ibm.wala.core.testdata_1.0.0.jar"));
            }
            if(loaders.containsKey("Extension")) {
                assertEquals(0, loaders.get("Extension").size());
            }
            if(loaders.containsKey("Synthetic")) {
                assertEquals(0, loaders.get("Synthetic").size());
            }
        }
    }
}
