package com.ibm.wala.dalvik.test.cha;

import static org.junit.Assume.assumeFalse;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.util.config.AnalysisScopeReader;
import com.ibm.wala.dalvik.classLoader.DexFileModule;
import com.ibm.wala.dalvik.classLoader.DexIRFactory;
import com.ibm.wala.dalvik.test.callGraph.DalvikCallGraphTestBase;
import com.ibm.wala.dalvik.test.callGraph.DroidBenchCGTest;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.callgraph.CallGraphBuilderCancelException;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.PlatformUtil;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.MultiDexContainer;
import org.junit.Assert;
import org.junit.Test;

public class MultiDexScopeTest {

  private static void addAPKtoScope(
      ClassLoaderReference loader, AnalysisScope scope, String fileName) {
    File apkFile = new File(fileName);
    MultiDexContainer<? extends DexBackedDexFile> multiDex = null;
    try {
      multiDex = DexFileFactory.loadDexContainer(apkFile, Opcodes.forApi(24));
    } catch (IOException e) {
      throw new RuntimeException(e);
    }

    try {
      for (String dexEntry : multiDex.getDexEntryNames()) {
        System.out.println("Adding dex file: " + dexEntry + " of file:" + fileName);
        scope.addToScope(loader, new DexFileModule(apkFile, dexEntry, 24));
      }
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private static AnalysisScope setUpTestScope(String apkName, String exclusions, ClassLoader loader)
      throws IOException {
    AnalysisScope scope;
    scope =
        AnalysisScopeReader.instance.readJavaScope("primordial.txt", new File(exclusions), loader);
    scope.setLoaderImpl(
        ClassLoaderReference.Application, "com.ibm.wala.dalvik.classLoader.WDexClassLoaderImpl");

    addAPKtoScope(ClassLoaderReference.Application, scope, apkName);
    return scope;
  }

  private static int getNumberOfAppClasses(ClassHierarchy cha) {
    Iterator<IClass> classes = cha.iterator();
    int numberOfClasses = 0;
    while (classes.hasNext()) {
      if (classes.next().getClassLoader().getName().toString().equals("Application"))
        numberOfClasses++;
    }
    return numberOfClasses;
  }

  @Test
  public void testAPK() throws ClassHierarchyException, IOException {
    // Known to be broken on Windows, but not intentionally so.  Please fix if you know how!
    // <https://github.com/wala/WALA/issues/608>
    assumeFalse(PlatformUtil.onWindows());

    AnalysisScope scope, scope2;
    ClassHierarchy cha, cha2;
    String testAPK = DroidBenchCGTest.getDroidBenchRoot() + "/apk/Aliasing/Merge1.apk";

    scope = setUpTestScope(testAPK, "", MultiDexScopeTest.class.getClassLoader());
    cha = ClassHierarchyFactory.make(scope);

    scope2 = DalvikCallGraphTestBase.makeDalvikScope(null, null, testAPK);
    cha2 = ClassHierarchyFactory.make(scope2);

    Assert.assertEquals(
        Integer.valueOf(getNumberOfAppClasses(cha)), Integer.valueOf(getNumberOfAppClasses(cha2)));
  }

  public AnalysisScope manuallyInitScope() throws IOException {

    String multidexApk = "src/test/resources/multidex-test.apk";
    AnalysisScope scope =
        AnalysisScopeReader.instance.readJavaScope(
            "primordial.txt", new File(""), MultiDexScopeTest.class.getClassLoader());
    scope.setLoaderImpl(
        ClassLoaderReference.Application, "com.ibm.wala.dalvik.classLoader.WDexClassLoaderImpl");

    try {
      // extract dex files to disk and add them manually to scope
      File dexTmpDir = new File(System.getProperty("java.io.tmpdir"));
      extractDexFiles(multidexApk, dexTmpDir);

      File dex1 = new File(dexTmpDir + File.separator + "classes.dex");
      scope.addToScope(ClassLoaderReference.Application, DexFileModule.make(dex1));
      dex1.delete();

      File dex2 = new File(dexTmpDir + File.separator + "classes2.dex");
      scope.addToScope(ClassLoaderReference.Application, DexFileModule.make(dex2));
      dex2.delete();
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
    return scope;
  }

  @Test
  public void testMultiDex() throws ClassHierarchyException, IOException {
    AnalysisScope scope, scope2;
    ClassHierarchy cha, cha2;
    String multidexApk = "src/test/resources/multidex-test.apk";

    scope = manuallyInitScope();
    cha = ClassHierarchyFactory.make(scope);

    scope2 = DalvikCallGraphTestBase.makeDalvikScope(null, null, multidexApk);
    cha2 = ClassHierarchyFactory.make(scope2);

    Assert.assertEquals(Integer.valueOf(getNumberOfAppClasses(cha)), Integer.valueOf(5));
    Assert.assertEquals(
        Integer.valueOf(getNumberOfAppClasses(cha)), Integer.valueOf(getNumberOfAppClasses(cha2)));
  }

  private static void extractDexFiles(String apkFileName, File outDir) throws IOException {
    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(apkFileName))) {
      ZipEntry entry;

      while ((entry = zis.getNextEntry()) != null) {
        if (!entry.isDirectory()) {
          if (entry.getName().startsWith("classes") && entry.getName().endsWith(".dex")) {
            extractFile(zis, outDir + File.separator + entry.getName());
          }
        }
        zis.closeEntry();
      }
    }
  }

  @Test
  public void testCGCreationFromDexSource()
      throws ClassHierarchyException, IOException, CallGraphBuilderCancelException {
    AnalysisScope scope = manuallyInitScope();
    ClassHierarchy cha = ClassHierarchyFactory.make(scope);
    AnalysisCacheImpl cache = new AnalysisCacheImpl(new DexIRFactory());

    TypeReference b2 = TypeReference.find(ClassLoaderReference.Application, "Ltest/B2");
    TypeReference b = TypeReference.find(ClassLoaderReference.Application, "Ltest/B");

    MethodReference callerRef =
        MethodReference.findOrCreate(b2, "<init>", "(ILjava/lang/String;)V");
    MethodReference calleeRef = MethodReference.findOrCreate(b, "<init>", "(I)V");

    IClass b2Class = cha.lookupClass(b2);
    IMethod targetMethod = b2Class.getMethod(Selector.make("<init>(ILjava/lang/String;)V"));

    ArrayList<DefaultEntrypoint> entrypoints = new ArrayList<>();
    entrypoints.add(new DefaultEntrypoint(targetMethod, cha));
    AnalysisOptions options = new AnalysisOptions();
    options.setAnalysisScope(scope);
    options.setEntrypoints(entrypoints);
    options.setReflectionOptions(AnalysisOptions.ReflectionOptions.NONE);

    CallGraphBuilder<InstanceKey> cgb = Util.makeRTABuilder(options, cache, cha);
    CallGraph cg1 = cgb.makeCallGraph(options, null);
    findEdge(cg1, callerRef, calleeRef);

    cgb = Util.makeZeroOneContainerCFABuilder(options, cache, cha, null, null);
    CallGraph cg2 = cgb.makeCallGraph(options, null);
    findEdge(cg2, callerRef, calleeRef);
  }

  public static void findEdge(CallGraph cg, MethodReference callerRef, MethodReference calleeRef) {
    Set<CGNode> callerNodes = cg.getNodes(callerRef);
    Assert.assertFalse(callerNodes.isEmpty());
    CGNode callerNode = callerNodes.iterator().next();

    Set<CGNode> calleeNodes = cg.getNodes(calleeRef);
    Assert.assertFalse(calleeNodes.isEmpty());
    CGNode calleeNode = calleeNodes.iterator().next();
    Assert.assertTrue(cg.hasEdge(callerNode, calleeNode));
  }

  private static void extractFile(ZipInputStream zipIn, String outFileName) throws IOException {
    try (BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(outFileName))) {
      byte[] buffer = new byte[4096];
      int read;

      while ((read = zipIn.read(buffer)) != -1) {
        bos.write(buffer, 0, read);
      }
    }
  }
}
