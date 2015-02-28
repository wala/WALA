/*******************************************************************************
 * Copyright (c) 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.dalvik.test.callGraph;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameters;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisOptions.ReflectionOptions;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.CancelException;
import com.ibm.wala.util.Predicate;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.functions.VoidFunction;
import com.ibm.wala.util.io.FileUtil;

//@Ignore
@RunWith(Parameterized.class)
public class DroidBenchCGTest extends DalvikCallGraphTestBase {

	private static MethodReference ref(String type, String name, String sig) {
		return MethodReference.findOrCreate(TypeReference.findOrCreate(ClassLoaderReference.Application, type), name, sig);
	}
	private static final Map<String,Set<MethodReference>> uncalledFunctions = HashMapFactory.make();
	static {
		Set<MethodReference> x = HashSetFactory.make();
		x.add(ref("Lde/ecspride/data/User", "setPwd", "(Lde/ecspride/data/Password;)V"));
		x.add(ref("Lde/ecspride/data/Password", "setPassword", "(Ljava/lang/String;)V"));
		uncalledFunctions.put("PrivateDataLeak1.apk",  x);

		x = HashSetFactory.make();
		x.add(ref("Lde/ecspride/Datacontainer", "getSecret", "()Ljava/lang/String;"));
		x.add(ref("Lde/ecspride/Datacontainer", "getDescription", "()Ljava/lang/String;"));
		uncalledFunctions.put("FieldSensitivity1.apk",  x);

		x = HashSetFactory.make();
		x.add(ref("Lde/ecspride/Datacontainer", "getSecret", "()Ljava/lang/String;"));
		uncalledFunctions.put("FieldSensitivity2.apk",  x);

		x = HashSetFactory.make();
		x.add(ref("Lde/ecspride/Datacontainer", "getDescription", "()Ljava/lang/String;"));
		uncalledFunctions.put("FieldSensitivity3.apk",  x);

		x = HashSetFactory.make();
		x.add(ref("Lde/ecspride/ConcreteClass", "foo", "()Ljava/lang/String;"));
		uncalledFunctions.put("Reflection1.apk",  x);

		x = HashSetFactory.make();
		x.add(ref("Ledu/mit/dynamic_dispatch/A", "f", "()Ljava/lang/String;"));
		uncalledFunctions.put("VirtualDispatch2.apk",  x);
	}
	
	private void assertUserCodeReachable(CallGraph cg) throws InvalidClassFileException {
		for(Iterator<IClass> clss = cg.getClassHierarchy().getLoader(ClassLoaderReference.Application).iterateAllClasses();
			clss.hasNext(); ) 
		{
			IClass cls = clss.next();
			if (cls.isInterface()) {
				continue;
			}
			if (! cls.getName().toString().startsWith("Landroid") && ! cls.getName().toString().equals("Lde/ecspride/R$styleable")) {
				for(IMethod m : cls.getDeclaredMethods()) {
					if (!m.isInit() && !m.isAbstract() && !uncalled.contains(m.getReference())) {
						Assert.assertFalse(m + "(" + m.getSourcePosition(0) + ") cannot be called in " + apkFile, cg.getNodes(m.getReference()).isEmpty());
						System.err.println("found " + m);
					}
				}
			}
		}
	}

	private final String apkFile;
	
	private final Set<MethodReference> uncalled;
	
	public DroidBenchCGTest(String apkFile, Set<MethodReference> uncalled) {
		this.apkFile = apkFile;
		this.uncalled = uncalled;
	}
	
	@Test
	public void test() throws IOException, ClassHierarchyException, CancelException, InvalidClassFileException, IllegalArgumentException, URISyntaxException {
		System.err.println("testing " + apkFile + "...");
		Pair<CallGraph,PointerAnalysis<InstanceKey>> x = makeAPKCallGraph(apkFile, ReflectionOptions.ONE_FLOW_TO_CASTS_APPLICATION_GET_METHOD);
		//System.err.println(x.fst);
		assertUserCodeReachable(x.fst);
		System.err.println("...success testing " + apkFile);
	}
	  
	private static final Set<String> skipTests = HashSetFactory.make();
	static {
	  // serialization issues
	  skipTests.add("ServiceCommunication1.apk");
    skipTests.add("Parcel1.apk");
	}
	@Parameters //(name="DroidBench: {0}")
	public static Collection<Object[]> generateData() {
	  String f = walaProperties.getProperty("droidbench.root");
	  if (f == null || !new File(f).exists()) {
	    f = System.getProperty("user.dir") + "/../../DroidBench";
	  }

	  System.err.println("Use " + f + " as droid bench root");
	  assert new File(f).exists() : "Use " + f + " as droid bench root";
	  assert new File(f + "/apk/").exists() : "Use " + f + " as droid bench root";
	  String droidBenchRoot = f;

	  final List<Object[]> files = new LinkedList<Object[]>();
	  FileUtil.recurseFiles(new VoidFunction<File>() {
	    @Override
	    public void apply(File f) {
	      Set<MethodReference> uncalled = uncalledFunctions.get(f.getName());
	      if (uncalled == null) {
	        uncalled = Collections.emptySet();
	      }
	      files.add(new Object[]{ f.getAbsolutePath(), uncalled }); 
	    }
	  }, new Predicate<File>() {
	    @Override
	    public boolean test(File t) {
	      return t.getName().endsWith("apk") && ! skipTests.contains(t.getName().toString());
	    } }, new File(droidBenchRoot + "/apk/"));
	  return files;
	}
}
