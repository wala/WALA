package com.ibm.wala.dalvik.test.cha;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.dalvik.classLoader.DexFileModule;
import com.ibm.wala.dalvik.test.callGraph.DroidBenchCGTest;
import com.ibm.wala.dalvik.test.util.Util;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.config.AnalysisScopeReader;
import org.jf.dexlib2.DexFileFactory;
import org.jf.dexlib2.Opcodes;
import org.jf.dexlib2.dexbacked.DexBackedDexFile;
import org.jf.dexlib2.iface.MultiDexContainer;
import org.junit.Assert;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Iterator;

public class MultiDexScopeTest {

    private static void addAPKtoScope(ClassLoaderReference loader, AnalysisScope scope, String fileName){
        File apkFile = new File(fileName);
        MultiDexContainer<? extends DexBackedDexFile> multiDex = null;
        try {
            multiDex = DexFileFactory.loadDexContainer(apkFile, Opcodes.forApi(24));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        try{
            for (String dexEntry : multiDex.getDexEntryNames()) {
                System.out.println("Adding dex file: " +dexEntry + " of file:" + fileName);
                scope.addToScope(loader, new DexFileModule(apkFile, dexEntry,24));
            }
        }catch (IOException e){
            throw new RuntimeException(e);
        }
    }

    private static AnalysisScope setUpTestScope(String apkName, String exclusions, ClassLoader loader) throws IOException {
        AnalysisScope scope;
        scope = AnalysisScopeReader.readJavaScope("primordial.txt", new File(exclusions), loader);
        scope.setLoaderImpl(ClassLoaderReference.Application,
                "com.ibm.wala.dalvik.classLoader.WDexClassLoaderImpl");

        addAPKtoScope(ClassLoaderReference.Application, scope, apkName);
        return scope;
    }

    private static int getNumberOfAppClasses(ClassHierarchy cha){
        Iterator<IClass> classes = cha.iterator();
        int numberOfClasses = 0;
        while(classes.hasNext()){
            if(classes.next().getClassLoader().getName().toString().equals("Application"))
                numberOfClasses++;
        }
        return  numberOfClasses;
    }

    @Test
    public void testAPK() throws ClassHierarchyException, IOException {
        AnalysisScope scope, scope2;
        ClassHierarchy cha, cha2;
        String testAPK = DroidBenchCGTest.getDroidBenchRoot() + "/apk/Aliasing/Merge1.apk";

        scope = setUpTestScope(testAPK,"", MultiDexScopeTest.class.getClassLoader());
        cha = ClassHierarchyFactory.make(scope);
        
        scope2 = Util.makeDalvikScope(null,null, testAPK);
        cha2 = ClassHierarchyFactory.make(scope2);

        Assert.assertEquals(Integer.valueOf(getNumberOfAppClasses(cha)), Integer.valueOf(getNumberOfAppClasses(cha2)));
    }

    @Test
    public void testMultiDex() throws ClassHierarchyException, IOException {
        AnalysisScope scope, scope2;
        ClassHierarchy cha, cha2;
        String multidexApk =  "data/multidex-test.apk";

        scope = setUpTestScope(multidexApk,"", MultiDexScopeTest.class.getClassLoader());
        cha = ClassHierarchyFactory.make(scope);

        scope2 = Util.makeDalvikScope(null,null, multidexApk);
        cha2 = ClassHierarchyFactory.make(scope2);

        Assert.assertEquals(Integer.valueOf(getNumberOfAppClasses(cha)),Integer.valueOf(5));
        Assert.assertNotEquals(Integer.valueOf(getNumberOfAppClasses(cha)), Integer.valueOf(getNumberOfAppClasses(cha2)));

    }



}
