package com.ibm.wala.core.tests.cha;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.util.config.AnalysisScopeReader;
import com.ibm.wala.core.util.io.FileProvider;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.config.StringFilter;
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
import java.util.stream.Collectors;
import org.assertj.core.api.InstanceOfAssertFactories;
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
    assertThat(
            cha.lookupClass(
                TypeReference.findOrCreate(
                    ClassLoaderReference.Application, "Lorg/apache/bcel/verifier/Verifier")))
        .isNotNull();
  }

  @Test
  public void testBaseScope() throws IOException, ClassHierarchyException {
    AnalysisScope scope =
        AnalysisScopeReader.instance.readJavaScope(
            "primordial-base.txt", null, AnalysisScopeTest.class.getClassLoader());
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    assertThat(
            cha.lookupClass(
                TypeReference.findOrCreate(
                    ClassLoaderReference.Application, "Ljava/util/ArrayList")))
        .isNotNull();
    assertThat(
            cha.lookupClass(
                TypeReference.findOrCreate(
                    ClassLoaderReference.Application, "Ljava/awt/AlphaComposite")))
        .isNull();
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
    assertThat(map.get("Exclusions"))
        .isEqualTo(
            List.of("java\\/awt\\/.*", "javax\\/swing\\/.*", "sun\\/awt\\/.*", "sun\\/swing\\/.*"));

    @SuppressWarnings("unchecked")
    Map<String, List<String>> loaders = (Map<String, List<String>>) map.get("Loaders");
    Set<String> loaderKeys =
        new HashSet<>(List.of("Primordial", "Extension", "Application", "Synthetic"));
    assertThat(loaderKeys).isEqualTo(loaders.keySet());
    assertThat(loaders.get("Primordial"))
        .hasSize(2)
        .contains("Nested Jar File:primordial.jar.model");
    assertThat(loaders.get("Application"))
        .singleElement(InstanceOfAssertFactories.STRING)
        .contains("com.ibm.wala.core.testdata_1.0.0.jar");
    assertThat(loaders.get("Extension")).isEmpty();
    assertThat(loaders.get("Synthetic")).isEmpty();
  }

  @Test
  public void testToJsonCustom() throws IOException {
    AnalysisScope scope;
    scope = AnalysisScope.createJavaAnalysisScope();
    String[] stdlibs = WalaProperties.getJ2SEJarFiles();
    Arrays.sort(stdlibs);
    for (String stdlib : stdlibs) {
      scope.addToScope(ClassLoaderReference.Primordial, new JarFile(stdlib));
      scope.addToScope(ClassLoaderReference.Application, new JarFile(stdlib));
    }
    scope.setExclusions((StringFilter) null);
    Gson gson = new Gson();
    Type type = new TypeToken<LinkedHashMap<String, Object>>() {}.getType();
    LinkedHashMap<String, Object> map = gson.fromJson(scope.toJson(), type);
    assertThat(map.get("Exclusions")).isEqualTo(List.of());

    @SuppressWarnings("unchecked")
    Map<String, List<String>> loaders = (Map<String, List<String>>) map.get("Loaders");
    Set<String> loaderKeys =
        new HashSet<>(List.of("Primordial", "Extension", "Application", "Synthetic"));
    assertThat(loaderKeys).isEqualTo(loaders.keySet());
    final var expectedStdLibs =
        Arrays.stream(stdlibs).map(stdlib -> "JarFileModule:" + stdlib).collect(Collectors.toSet());
    assertThat(loaders.get("Primordial")).containsExactlyInAnyOrderElementsOf(expectedStdLibs);
    assertThat(loaders.get("Application")).containsExactlyInAnyOrderElementsOf(expectedStdLibs);
    assertThat(loaders.get("Extension")).isEmpty();
    assertThat(loaders.get("Synthetic")).isEmpty();
  }
}
