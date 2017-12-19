/******************************************************************************
 * Copyright (c) 2002 - 2016 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     Brian Pfretzschner - initial implementation
 *****************************************************************************/
package com.ibm.wala.cast.js.nodejs;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.json.JSONObject;

import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.ssa.JavaScriptInvoke;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.callgraph.propagation.ConcreteTypeKey;
import com.ibm.wala.ipa.callgraph.propagation.ConstantKey;
import com.ibm.wala.ipa.callgraph.propagation.InstanceKey;
import com.ibm.wala.ipa.callgraph.propagation.PointerAnalysis;
import com.ibm.wala.ipa.callgraph.propagation.PointerKey;
import com.ibm.wala.ipa.callgraph.propagation.PropagationCallGraphBuilder;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.intset.OrdinalSet;
import com.ibm.wala.util.ssa.ClassLookupException;

/**
 * This class is used by WALA internals to resolve to what functions a call
 * could potentially invoke.
 * 
 * @author Brian Pfretzschner &lt;brian.pfretzschner@gmail.com&gt;
 */
public class NodejsRequireTargetSelector implements MethodTargetSelector {

	private File rootDir;
	private MethodTargetSelector base;
	private PropagationCallGraphBuilder builder;
	
	private HashMap<String, IMethod> previouslyRequired = HashMapFactory.make();

	public NodejsRequireTargetSelector(File rootDir, MethodTargetSelector base) {
		this.rootDir = rootDir;
		this.base = base;
	}

	public void setCallGraphBuilder(PropagationCallGraphBuilder builder) {
		this.builder = builder;
	}

	/**
	 * Basic idea: If the called method is named "__WALA__require", it is most likely
	 * the require-function mock from the module-wrapper. To figure out what file
	 * shall be required, pointer analysis is used to identify strings that can
	 * flow into the require call. That file is than loaded, wrapped into the
	 * module wrapper and returned as method that will be invoked. Therefore,
	 * there will never be an call graph edge to the require function call,
	 * the require function is replaced by the file that is included through
	 * the require call.
	 * 
	 * {@inheritDoc}
	 */
	@Override
	public IMethod getCalleeTarget(CGNode caller, CallSiteReference site, IClass receiver) {
		PointerAnalysis<InstanceKey> pointerAnalysis = builder.getPointerAnalysis();
		JavaScriptLoader jsLoader = (JavaScriptLoader) builder.getClassHierarchy().getLoader(JavaScriptTypes.jsLoader);
		
		IMethod calledMethod = base.getCalleeTarget(caller, site, receiver);
		
		if (calledMethod != null && calledMethod.getDeclaringClass().toString().endsWith("/__WALA__require")) {
			JavaScriptInvoke callInstr = getInvokeInstruction(caller, site);

			Set<String> targets = getRequireTargets(pointerAnalysis, caller, callInstr);
			if (targets.size() == 0) {
				// There is no possible call target
				throw new RuntimeException("No require target found in method: "+caller.getMethod());
			}
			
			for (String target : targets) {
				try {
					File workingDir = new File(receiver.getSourceFileName()).getParentFile();
					SourceModule sourceModule = resolve(rootDir, workingDir, target);
					if (previouslyRequired.containsKey(sourceModule.getClassName())) {
						return previouslyRequired.get(sourceModule.getClassName());
					}
					
					String className = "L" + sourceModule.getClassName() + "/nodejsModule";
					if (sourceModule instanceof NodejsRequiredSourceModule
							&& ((NodejsRequiredSourceModule) sourceModule).getFile().toString().endsWith(".json")) {
						className = "L" + sourceModule.getClassName() + "/jsonModule";
					}
					
					JSCallGraphUtil.loadAdditionalFile(builder.getClassHierarchy(), jsLoader, sourceModule);
					IClass script = builder.getClassHierarchy()
							.lookupClass(TypeReference.findOrCreate(jsLoader.getReference(), className));
					
					System.err.println(builder.getClassHierarchy());
					
					IMethod method = script.getMethod(AstMethodReference.fnSelector);
					previouslyRequired.put(sourceModule.getClassName(), method);
					
					return method;
				}
				catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		
		return calledMethod;
	}

	private static JavaScriptInvoke getInvokeInstruction(CGNode caller, CallSiteReference site) {
		IR callerIR = caller.getIR();
		SSAAbstractInvokeInstruction callInstrs[] = callerIR.getCalls(site);
		assert callInstrs.length == 1;
		return (JavaScriptInvoke) callInstrs[0];
	}

	private Set<String> getRequireTargets(PointerAnalysis<InstanceKey> pointerAnalysis, CGNode caller,
			JavaScriptInvoke callInstr) {
		HashSet<String> set = HashSetFactory.make();

		PointerKey pk = builder.getPointerKeyForLocal(caller, callInstr.getUse(2));
		OrdinalSet<InstanceKey> instanceKeys = pointerAnalysis.getPointsToSet(pk);

		for (InstanceKey instanceKey : instanceKeys) {
			if (instanceKey instanceof ConstantKey<?>) {
				Object value = ((ConstantKey<?>) instanceKey).getValue();
				if (value instanceof String) {
					set.add((String) value);
				}
				else {
					System.err.println("NodejsRequireTargetSelector: Unexpected value: " + value);
					return HashSetFactory.make();
				}
			}
			else if (instanceKey instanceof ConcreteTypeKey) {
				// Cannot do anything with this information...
			}
			else {
				System.err.println("NodejsRequireTargetSelector: Unexpected instanceKey: " + instanceKey.getClass() + " -- " + instanceKey);
				return HashSetFactory.make();
			}
		}

		return set;
	}

	/**
	 * Implements the Nodejs require.resolve algorithm,
	 * see https://nodejs.org/api/modules.html#modules_all_together
	 * 
	 * require(X) from module at path Y
	 * 1. If X is a core module,
	 *    a. return the core module
	 *    b. STOP
	 * 2. If X begins with './' or '/' or '../'
	 *    a. LOAD_AS_FILE(Y + X)
	 *    b. LOAD_AS_DIRECTORY(Y + X)
	 * 3. LOAD_NODE_MODULES(X, dirname(Y))
	 * 4. THROW "not found"
	 * 
	 * @param dir Y in the pseudo algorithm
	 * @param target X in the pseudo algorithm
	 * @throws IOException
	 */
	public static SourceFileModule resolve(File rootDir, File dir, String target) throws IOException {
		if (NodejsRequiredCoreModule.isCoreModule(target))
			return NodejsRequiredCoreModule.make(target);
		
		if (target.startsWith("./") || target.startsWith("/") || target.startsWith("../")) {
			SourceFileModule module = loadAsFile(rootDir, new File(dir, target));
			if (module != null) return module;
			
			module = loadAsDirectory(rootDir, new File(dir, target));
			if (module != null) return module;
		}
		
		SourceFileModule module = loadNodeModules(rootDir, dir, target);
		if (module != null) return module;
		
		throw new ClassLookupException("Required module not found: "+target+" in "+dir);
	}

	/**
	 * LOAD_AS_FILE(X)
	 * 1. If X is a file, load X as JavaScript text.  STOP
	 * 2. If X.js is a file, load X.js as JavaScript text.  STOP
	 * 3. If X.json is a file, parse X.json to a JavaScript Object.  STOP
	 * 4. If X.node is a file, load X.node as binary addon.  STOP
	 * 
	 * @param f
	 * @throws IOException
	 */
	private static SourceFileModule loadAsFile(File rootDir, File f) throws IOException {
		// 1.
		if (f.isFile())
			return NodejsRequiredSourceModule.make(rootDir, f);
		
		// 2.
		File jsFile = new File(f+".js");
		if (jsFile.isFile())
			return NodejsRequiredSourceModule.make(rootDir, jsFile);
		
		// 3.
		File jsonFile = new File(f+".json");
		if (jsonFile.isFile())
			return NodejsRequiredSourceModule.make(rootDir, jsonFile);
		
		// Skip 4. step
		
		return null;
	}
	
	/**
	 * LOAD_AS_DIRECTORY(X)
	 * 1. If X/package.json is a file,
	 *    a. Parse X/package.json, and look for "main" field.
	 *    b. let M = X + (json main field)
	 *    c. LOAD_AS_FILE(M)
	 * 2. If X/index.js is a file, load X/index.js as JavaScript text.  STOP
	 * 3. If X/index.json is a file, parse X/index.json to a JavaScript object. STOP
	 * 4. If X/index.node is a file, load X/index.node as binary addon.  STOP
	 * 
	 * @param d
	 * @throws IOException
	 */
	private static SourceFileModule loadAsDirectory(File rootDir, File d) throws IOException {
		// 1.
		File packageJsonFile = new File(d, "package.json");
		if (packageJsonFile.isFile()) {
			// 1.a.
			String packageJsonContent = FileUtils.readFileToString(packageJsonFile);
			JSONObject packageJson = new JSONObject(packageJsonContent);
			if (packageJson.has("main")) {
				String mainFileName = packageJson.getString("main");
				
				// 1.b.
				File mainFile = new File(d, mainFileName);
				
				// 1.c.
				return loadAsFile(rootDir, mainFile);
			}
		}
		
		// 2.
		File jsFile = new File(d, "index.js");
		if (jsFile.isFile())
			return NodejsRequiredSourceModule.make(rootDir, jsFile);
		
		// 3.
		File jsonFile = new File(d, "index.json");
		if (jsonFile.isFile())
			return NodejsRequiredSourceModule.make(rootDir, jsonFile);

		// Skip 4. step
		
		return null;
	}
	
	/**
	 * LOAD_NODE_MODULES(X, START)
	 * 1. let DIRS=NODE_MODULES_PATHS(START)
	 * 2. for each DIR in DIRS:
	 *    a. LOAD_AS_FILE(DIR/X)
	 *    b. LOAD_AS_DIRECTORY(DIR/X)
	 * 
	 * @param dir
	 * @param target
	 * @throws IOException
	 */
	private static SourceFileModule loadNodeModules(File rootDir, File d, String target) throws IOException {
		List<File> dirs = nodeModulePaths(rootDir, d);
		for (File dir : dirs) {
			SourceFileModule module = loadAsFile(rootDir, new File(dir, target));
			if (module != null) return module;
			
			module = loadAsDirectory(rootDir, new File(dir, target));
			if (module != null) return module;
		}
		
		return null;
	}

	/**
	 * NODE_MODULES_PATHS(START)
	 * 1. let PARTS = path split(START)
	 * 2. let I = count of PARTS - 1
	 * 3. let DIRS = []
	 * 4. while I &gt;= 0,
	 *    a. if PARTS[I] = "node_modules" CONTINUE
	 *    b. DIR = path join(PARTS[0 .. I] + "node_modules")
	 *    c. DIRS = DIRS + DIR
	 *    d. let I = I - 1
	 * 5. return DIRS
	 * 
	 * @param d
	 * @throws IOException
	 */
	private static List<File> nodeModulePaths(File rootDir, File d) throws IOException {
		LinkedList<File> dirs = new LinkedList<>();
		
		while (d.getCanonicalPath().startsWith(rootDir.getCanonicalPath()) && d.toPath().getNameCount() > 0) {
			// 4.a.
			if (!d.getName().equals("node_modules")) {
				// 4.b. and 4.c.
				dirs.add(new File(d, "node_modules"));
			}
			
			// 4.d.
			d = d.getParentFile();
		}
		
		return dirs;
	}

}
