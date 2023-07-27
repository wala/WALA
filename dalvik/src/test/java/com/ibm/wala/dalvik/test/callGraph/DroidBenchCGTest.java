/*
 * Copyright (c) 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.dalvik.test.callGraph;

import static com.ibm.wala.dalvik.test.util.Util.walaProperties;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.NullProgressMonitor;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.io.FileUtil;
import java.io.File;
import java.net.URI;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;
import org.junit.jupiter.api.Named;

public abstract class DroidBenchCGTest extends DalvikCallGraphTestBase {

  private static MethodReference ref(String type, String name, String sig) {
    return MethodReference.findOrCreate(
        TypeReference.findOrCreate(ClassLoaderReference.Application, type), name, sig);
  }

  private static final Map<String, Set<MethodReference>> uncalledFunctions = HashMapFactory.make();

  static {
    Set<MethodReference> x = HashSetFactory.make();
    x.add(ref("Lde/ecspride/data/User", "setPwd", "(Lde/ecspride/data/Password;)V"));
    x.add(ref("Lde/ecspride/data/Password", "setPassword", "(Ljava/lang/String;)V"));
    uncalledFunctions.put("PrivateDataLeak1.apk", x);

    x = HashSetFactory.make();
    x.add(ref("Lde/ecspride/Datacontainer", "getSecret", "()Ljava/lang/String;"));
    x.add(ref("Lde/ecspride/Datacontainer", "getDescription", "()Ljava/lang/String;"));
    uncalledFunctions.put("FieldSensitivity1.apk", x);

    x = HashSetFactory.make();
    x.add(ref("Lde/ecspride/Datacontainer", "getSecret", "()Ljava/lang/String;"));
    uncalledFunctions.put("FieldSensitivity2.apk", x);

    x = HashSetFactory.make();
    x.add(ref("Lde/ecspride/Datacontainer", "getDescription", "()Ljava/lang/String;"));
    uncalledFunctions.put("FieldSensitivity3.apk", x);

    x = HashSetFactory.make();
    x.add(ref("Lde/ecspride/ConcreteClass", "foo", "()Ljava/lang/String;"));
    uncalledFunctions.put("Reflection1.apk", x);

    x = HashSetFactory.make();
    x.add(ref("Ledu/mit/dynamic_dispatch/A", "f", "()Ljava/lang/String;"));
    uncalledFunctions.put("VirtualDispatch2.apk", x);

    x = HashSetFactory.make();
    x.add(ref("Ledu/mit/dynamic_dispatch/A", "f", "()Ljava/lang/String;"));
    uncalledFunctions.put("VirtualDispatch2.apk", x);
  }

  public static Set<IMethod> assertUserCodeReachable(CallGraph cg, Set<MethodReference> uncalled) {
    Set<IMethod> result = HashSetFactory.make();
    for (IClass cls :
        Iterator2Iterable.make(
            cg.getClassHierarchy()
                .getLoader(ClassLoaderReference.Application)
                .iterateAllClasses())) {
      if (cls.isInterface()) {
        continue;
      }
      if (!cls.getName().toString().startsWith("Landroid")
          && !cls.getName().toString().equals("Lde/ecspride/R$styleable")) {
        for (IMethod m : cls.getDeclaredMethods()) {
          if (!m.isInit() && !m.isAbstract() && !uncalled.contains(m.getReference())) {
            if (!cg.getNodes(m.getReference()).isEmpty()) {
              System.err.println("found " + m);
            } else {
              result.add(m);
            }
          }
        }
      }
    }
    return result;
  }

  protected void runTest(final TestParameters testParameters) throws Exception {

    final var androidLibs = testParameters.getAndroidLibs();
    final var androidJavaJar = testParameters.getAndroidJavaJar();
    final var apkFile = testParameters.getApkFile();
    final var uncalled = testParameters.getUncalled();

    System.err.println("testing " + apkFile + "...");
    Pair<CallGraph, PointerAnalysis<InstanceKey>> x =
        makeAPKCallGraph(
            androidLibs,
            androidJavaJar,
            apkFile,
            new NullProgressMonitor(),
            ReflectionOptions.ONE_FLOW_TO_CASTS_APPLICATION_GET_METHOD);
    // System.err.println(x.fst);
    Set<IMethod> bad = assertUserCodeReachable(x.fst, uncalled);
    assertion(bad + " should be empty", bad.isEmpty());
    System.err.println("...success testing " + apkFile);
  }

  protected void assertion(String string, boolean empty) {
    assertTrue(empty, string);
  }

  private static final Set<String> skipTests = HashSetFactory.make();

  static {
    // serialization issues
    skipTests.add("ServiceCommunication1.apk");
    skipTests.add("Parcel1.apk");

    // Button2 has issues when using the fake Android jar
    skipTests.add("Button2.apk");
  }

  public static Stream<Named<TestParameters>> generateData(
      final URI[] androidLibs, final File androidJavaJar, final String filter) {

    return generateData(getDroidBenchRoot(), androidLibs, androidJavaJar, filter);
  }

  public static String getDroidBenchRoot() {
    String f = walaProperties.getProperty("droidbench.root");
    if (f == null || !new File(f).exists()) {
      f = "build/DroidBench";
    }

    System.err.println("Use " + f + " as droid bench root");
    assert new File(f).exists() : "Use " + f + " as droid bench root";
    assert new File(f + "/apk/").exists() : "Use " + f + " as droid bench root";
    return f;
  }

  public static Stream<Named<TestParameters>> generateData(
      String droidBenchRoot,
      final URI[] androidLibs,
      final File androidJavaJar,
      final String filter) {
    final Stream.Builder<Named<TestParameters>> testParameters = Stream.builder();
    final var apkRoot = new File(droidBenchRoot + "/apk/");
    FileUtil.recurseFiles(
        f -> {
          Set<MethodReference> uncalled = uncalledFunctions.get(f.getName());
          if (uncalled == null) {
            uncalled = Collections.emptySet();
          }
          testParameters.add(
              Named.of(
                  apkRoot.toPath().relativize(f.toPath()).toString(),
                  new TestParameters(androidLibs, androidJavaJar, f.getAbsolutePath(), uncalled)));
        },
        t ->
            (filter == null || t.getAbsolutePath().contains(filter))
                && t.getName().endsWith("apk")
                && !skipTests.contains(t.getName()),
        apkRoot);
    return testParameters.build().sorted(Comparator.comparing(Named::getName));
  }

  protected static class TestParameters {

    private final URI[] androidLibs;
    private final File androidJavaJar;
    private final String apkFile;
    private final Set<MethodReference> uncalled;

    public TestParameters(
        URI[] androidLibs, File androidJavaJar, String apkFile, Set<MethodReference> uncalled) {
      this.androidLibs = androidLibs;
      this.androidJavaJar = androidJavaJar;
      this.apkFile = apkFile;
      this.uncalled = uncalled;
    }

    public URI[] getAndroidLibs() {
      return androidLibs;
    }

    public File getAndroidJavaJar() {
      return androidJavaJar;
    }

    public String getApkFile() {
      return apkFile;
    }

    public Set<MethodReference> getUncalled() {
      return uncalled;
    }
  }
}
