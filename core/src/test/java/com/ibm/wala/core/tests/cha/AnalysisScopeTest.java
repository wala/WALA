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
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;

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
                // new FileProvider().getFile("J2SEClassHierarchyExclusions.txt"),
                new FileProvider().getFile("GUIExclusions.txt"),
                AnalysisScopeTest.class.getClassLoader());
        Gson gson = new Gson();
        Type type = new TypeToken<LinkedHashMap<String, Object>>(){}.getType();
        LinkedHashMap<String, Object> map = gson.fromJson(scope.toJson(), type);
        System.out.println(map);
        if(map.containsKey("Exclusions")) {
            String[] exclusions = scope.getExclusions().toString().split("\\|");
            ArrayList<String> arr2 = new ArrayList<>();
            for(int i = 0; i < exclusions.length; i++){
                String word = exclusions[i];
                word = word.replace("(", "");
                word = word.replace(")", "");
                arr2.add(word);
            }
            assertEquals(arr2, map.get("Exclusions"));
        }
        Type type2 = new TypeToken<LinkedHashMap<String, ArrayList<String>>>(){}.getType();
        LinkedHashMap<String, ArrayList<String>> loaders = gson.fromJson(gson.toJson(map.get("Loaders")), type2);
        if(loaders.containsKey("Primordial")) {
            boolean flag = true;
            for (int i = 0; i < scope.getModules(scope.getPrimordialLoader()).size(); i++) {
                String s1 = scope.getModules(scope.getPrimordialLoader()).get(i).toString();
                if (!loaders.get("Primordial").contains(s1)) {
                    flag = false;
                    break;
                }
            }
            assertEquals(true, flag);
        }
        if(loaders.containsKey("Extension")) {
            boolean flag = true;
            for (int i = 0; i < scope.getModules(scope.getExtensionLoader()).size(); i++) {
                String s1 = scope.getModules(scope.getExtensionLoader()).get(i).toString();
                if (!loaders.get("Extension").contains(s1)) {
                    flag = false;
                    break;
                }
            }
            assertEquals(true, flag);
        }
        if(loaders.containsKey("Application")) {
            boolean flag = true;
            for (int i = 0; i < scope.getModules(scope.getApplicationLoader()).size(); i++) {
                String s1 = scope.getModules(scope.getApplicationLoader()).get(i).toString();
                if (!loaders.get("Application").contains(s1)) {
                    flag = false;
                    break;
                }
            }
            assertEquals(true, flag);
        }
        if(loaders.containsKey("Synthetic")) {
            boolean flag = true;
            for (int i = 0; i < scope.getModules(scope.getSyntheticLoader()).size(); i++) {
                String s1 = scope.getModules(scope.getSyntheticLoader()).get(i).toString();
                if (!loaders.get("Synthetic").contains(s1)) {
                    flag = false;
                    break;
                }
            }
            assertEquals(true, flag);
        }
    }
}
