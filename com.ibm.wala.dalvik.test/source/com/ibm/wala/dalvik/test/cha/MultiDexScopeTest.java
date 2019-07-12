package com.ibm.wala.dalvik.test.cha;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.dalvik.classLoader.DexFileModule;
import com.ibm.wala.dalvik.test.callGraph.DalvikCallGraphTestBase;
import com.ibm.wala.dalvik.test.callGraph.DroidBenchCGTest;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Iterator;
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
    scope = AnalysisScopeReader.readJavaScope("primordial.txt", new File(exclusions), loader);
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

  @Test
  public void testMultiDex() throws ClassHierarchyException, IOException {
    AnalysisScope scope, scope2;
    ClassHierarchy cha, cha2;
    String multidexApk = "data/multidex-test.apk";

    scope =
        AnalysisScopeReader.readJavaScope(
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
    cha = ClassHierarchyFactory.make(scope);

    // use setUpAndroidAnalysisScope
    scope2 = DalvikCallGraphTestBase.makeDalvikScope(null, null, multidexApk);
    cha2 = ClassHierarchyFactory.make(scope2);

    Assert.assertEquals(Integer.valueOf(getNumberOfAppClasses(cha)), Integer.valueOf(5));
    Assert.assertEquals(
        Integer.valueOf(getNumberOfAppClasses(cha)), Integer.valueOf(getNumberOfAppClasses(cha2)));
  }

  private static void extractDexFiles(String apkFileName, File outDir) throws IOException {
    try (ZipInputStream zis = new ZipInputStream(new FileInputStream(new File(apkFileName)))) {
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

  private static void extractFile(ZipInputStream zipIn, String outFileName) throws IOException {
    try (BufferedOutputStream bos =
        new BufferedOutputStream(new FileOutputStream(new File(outFileName)))) {
      byte[] buffer = new byte[4096];
      int read;

      while ((read = zipIn.read(buffer)) != -1) {
        bos.write(buffer, 0, read);
      }
    }
  }
}
