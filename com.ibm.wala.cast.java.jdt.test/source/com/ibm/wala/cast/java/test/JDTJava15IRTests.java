package com.ibm.wala.cast.java.test;

import java.io.IOException;
import java.util.List;

import junit.framework.Assert;

import org.eclipse.core.runtime.NullProgressMonitor;

import com.ibm.wala.cast.java.client.JavaSourceAnalysisEngine;
import com.ibm.wala.cast.java.translator.jdt.JDTJavaSourceAnalysisEngine;
import com.ibm.wala.core.tests.callGraph.CallGraphTestUtil;
import com.ibm.wala.core.tests.plugin.CoreTestsPlugin;
import com.ibm.wala.core.tests.util.EclipseTestUtil;
import com.ibm.wala.eclipse.util.EclipseProjectPath;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.io.FileProvider;

public class JDTJava15IRTests extends IRTests {

	public JDTJava15IRTests() {
	    super("Test 1.5 features for JDT front end for WALA CAst", "com.ibm.wala.cast.java.test.data");
	}
	
	@Override
	public void setUp() {
		EclipseTestUtil.importZippedProject(TestPlugin.getDefault(), "test_project.zip", new NullProgressMonitor());
		System.err.println("finish importing project");
	}	

	public void tearDown() {
		EclipseTestUtil.destroyProject("com.ibm.wala.cast.java.test.data");
	}

	@Override
	protected JavaSourceAnalysisEngine getAnalysisEngine(final String[] mainClassDescriptors) {
		JavaSourceAnalysisEngine engine = new JDTJavaSourceAnalysisEngine() {
			protected Iterable<Entrypoint> makeDefaultEntrypoints(AnalysisScope scope, IClassHierarchy cha) {
				return Util.makeMainEntrypoints(EclipseProjectPath.SOURCE_REF, cha, mainClassDescriptors);
			}
		};
		       
		try {
			engine.setExclusionsFile(FileProvider.getFileFromPlugin(CoreTestsPlugin.getDefault(), CallGraphTestUtil.REGRESSION_EXCLUSIONS).getAbsolutePath());
		} catch (IOException e) {
			Assert.assertFalse("Cannot find exclusions file", true);
		}
		    
		return engine;
	}
	
	private void runSimple15Test(List assertions) {
		 runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), assertions, true);		
	}
	
	public void testAnonGeneNullarySimple() {
		 runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
	}

	public void testAnonymousGenerics() {
		 runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
	}
	
	public void testBasicsGenerics() {
		 runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
	}
	
	public void testCocovariant() {
		 runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
	}

	public void testCustomGenericsAndFields() {
		 runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
	}

	public void testEnumSwitch() {
		 runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
	}

	public void testExplicitBoxingTest() {
		 runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
	}

	public void testGenericArrays() {
		 runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
	}

	public void testGenericMemberClasses() {
		 runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
	}

	public void testGenericSuperSink() {
		 runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
	}

	public void testMethodGenerics() {
		 runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
	}

	public void testMoreOverriddenGenerics() {
		 runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
	}

	public void testNotSoSimpleEnums() {
		 runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
	}

	public void testOverridesOnePointFour() {
		 runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
	}

	public void testSimpleEnums() {
		 runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
	}

	public void testSimpleEnums2() {
		 runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
	}

	public void testVarargs() {
		 runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
	}

	public void testVarargsCovariant() {
		 runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
	}

	public void testVarargsOverriding() {
		 runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
	}

	public void testWildcards() {
		 runTest(singlePkgTestSrc("javaonepointfive"), rtJar, simplePkgTestEntryPoint("javaonepointfive"), emptyList, true);
	}

}

