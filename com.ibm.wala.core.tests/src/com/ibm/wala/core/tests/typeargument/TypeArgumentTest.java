package com.ibm.wala.core.tests.typeargument;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.wala.classLoader.ClassLoaderFactory;
import com.ibm.wala.classLoader.ClassLoaderFactoryImpl;
import com.ibm.wala.classLoader.FieldImpl;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.core.tests.util.TestConstants;
import com.ibm.wala.core.tests.util.WalaTestCase;
import com.ibm.wala.ipa.callgraph.AnalysisCacheImpl;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.types.generics.ClassTypeSignature;
import com.ibm.wala.types.generics.TypeSignature;
import com.ibm.wala.util.config.AnalysisScopeReader;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.warnings.Warnings;

public class TypeArgumentTest extends WalaTestCase {
    private static final ClassLoader MY_CLASSLOADER = TypeArgumentTest.class.getClassLoader();

    private static AnalysisScope scope;

    private static ClassHierarchy cha;

    private static AnalysisOptions options;

    private static IAnalysisCacheView cache;
    public static void main(String[] args) {
      justThisTest(TypeArgumentTest.class);
	}
    
    @BeforeClass
    public static void beforeClass() throws Exception {

      scope = AnalysisScopeReader.readJavaScope(TestConstants.WALA_TESTDATA, (new FileProvider()).getFile("J2SEClassHierarchyExclusions.txt"), MY_CLASSLOADER);

      options = new AnalysisOptions(scope, null);
      cache = new AnalysisCacheImpl();
      ClassLoaderFactory factory = new ClassLoaderFactoryImpl(scope.getExclusions());

      try {
        cha = ClassHierarchyFactory.make(scope, factory);
      } catch (ClassHierarchyException e) {
        throw new Exception(e);
      }
    }

    @AfterClass
    public static void afterClass() throws Exception {
      Warnings.clear();
      scope = null;
      cha = null;
      options = null;
      cache = null;
    }
    @Test public void test() {
      for (IClass iClass:cha) {
        for (IField field:iClass.getAllFields()) {
          TypeSignature sig = ((FieldImpl) field)
            .getGenericSignature();
          if (sig == null) {
            continue;
          }
          if (sig.isClassTypeSignature()) {
            ((ClassTypeSignature)sig).getTypeArguments();
          }
        }
      }
    }
}
