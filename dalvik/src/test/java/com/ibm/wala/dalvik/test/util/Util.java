package com.ibm.wala.dalvik.test.util;

import static com.ibm.wala.properties.WalaProperties.ANDROID_RT_DEX_DIR;
import static com.ibm.wala.properties.WalaProperties.ANDROID_RT_JAVA_JAR;

import com.android.tools.r8.CompilationFailedException;
import com.android.tools.r8.D8;
import com.android.tools.r8.D8Command;
import com.android.tools.r8.OutputMode;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.NestedJarFileModule;
import com.ibm.wala.core.util.io.FileProvider;
import com.ibm.wala.dalvik.test.callGraph.DalvikCallGraphTestBase;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.io.TemporaryFile;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class Util {

  public static Properties walaProperties;

  static {
    try {
      walaProperties = WalaProperties.loadProperties();
    } catch (WalaException e) {
      walaProperties = null;
    }
  }

  public static String getJavaJar(AnalysisScope javaScope) throws IOException {
    Module javaJar = javaScope.getModules(javaScope.getApplicationLoader()).iterator().next();
    if (javaJar instanceof JarFileModule) {
      String javaJarPath = ((JarFileModule) javaJar).getAbsolutePath();
      return javaJarPath;
    } else {
      assert javaJar instanceof NestedJarFileModule : javaJar;
      File F = File.createTempFile("android", ".jar");
      // F.deleteOnExit();
      System.err.println(F.getAbsolutePath());
      TemporaryFile.streamToFile(F, ((NestedJarFileModule) javaJar).getNestedContents());
      return F.getAbsolutePath();
    }
  }

  public static File convertJarToDex(String jarFile) throws IOException {
    Path tmpDir = Files.createTempDirectory("dex");
    tmpDir.toFile().deleteOnExit();
    try {
      D8.run(
          D8Command.builder()
              .addLibraryFiles(Paths.get(androidJavaLib().getAbsolutePath()))
              .setMinApiLevel(26)
              .setOutput(tmpDir, OutputMode.DexIndexed)
              .addProgramFiles(Paths.get(jarFile))
              .build());
    } catch (CompilationFailedException e) {
      throw new RuntimeException("Unexpected compilation failure in D8!", e);
    }
    return tmpDir.resolve("classes.dex").toFile();
  }

  public static File androidJavaLib() throws IOException {
    if (walaProperties != null && walaProperties.getProperty(ANDROID_RT_JAVA_JAR) != null) {
      return new File(walaProperties.getProperty(ANDROID_RT_JAVA_JAR));
    } else {
      File F = File.createTempFile("android", ".jar");
      F.deleteOnExit();
      TemporaryFile.urlToFile(F, Util.class.getClassLoader().getResource("android.jar"));
      return F;
    }
  }

  public static URI[] androidLibs() {
    List<URI> libs = new ArrayList<>();
    if (System.getenv("ANDROID_BOOT_OAT") != null) {
      libs.add(new File(System.getenv("ANDROID_CORE_OAT")).toURI());
      libs.add(new File(System.getenv("ANDROID_BOOT_OAT")).toURI());

    } else if (walaProperties != null && walaProperties.getProperty(ANDROID_RT_DEX_DIR) != null) {
      for (File lib :
          new File(walaProperties.getProperty(ANDROID_RT_DEX_DIR))
              .listFiles((dir, name) -> name.startsWith("boot") && name.endsWith("oat"))) {
        libs.add(lib.toURI());
      }
    } else {
      assert "Dalvik".equals(System.getProperty("java.vm.name"));
      for (File f :
          new File("/system/framework/")
              .listFiles(
                  pathname -> {
                    String name = pathname.getName();
                    return (name.startsWith("core") || name.startsWith("framework"))
                        && (name.endsWith("jar") || name.endsWith("apk"));
                  })) {
        System.out.println("adding " + f);
        libs.add(f.toURI());
      }
    }
    return libs.toArray(new URI[0]);
  }

  public static IClassHierarchy makeCHA() throws IOException, ClassHierarchyException {
    File F = File.createTempFile("walatest", ".jar");
    F.deleteOnExit();
    TemporaryFile.urlToFile(
        F, new FileProvider().getResource("com.ibm.wala.core.testdata_1.0.0a.jar"));
    File androidDex = convertJarToDex(F.getAbsolutePath());
    AnalysisScope dalvikScope =
        DalvikCallGraphTestBase.makeDalvikScope(null, null, androidDex.getAbsolutePath());
    return ClassHierarchyFactory.make(dalvikScope);
  }
}
