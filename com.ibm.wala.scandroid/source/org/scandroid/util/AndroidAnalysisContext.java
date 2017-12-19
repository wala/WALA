/*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released under the terms listed below.  
 *
 */
/**
 *
 * Copyright (c) 2009-2012,
 *
 *  Adam Fuchs          <afuchs@cs.umd.edu>
 *  Avik Chaudhuri      <avik@cs.umd.edu>
 *  Steve Suh           <suhsteve@gmail.com>
 *  Galois, Inc. (Adam Foltzer <acfoltzer@galois.com)
 *
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 *
 * 3. The names of the contributors may not be used to endorse or promote
 * products derived from this software without specific prior written
 * permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 *
 */

package org.scandroid.util;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.google.common.collect.Lists;
import com.google.common.collect.Queues;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.dalvik.util.AndroidAnalysisScope;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.ClassTargetSelector;
import com.ibm.wala.ipa.callgraph.ContextSelector;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.callgraph.propagation.SSAContextInterpreter;
import com.ibm.wala.ipa.callgraph.propagation.SSAPropagationCallGraphBuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXCFABuilder;
import com.ibm.wala.ipa.callgraph.propagation.cfa.ZeroXInstanceKeys;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.summaries.BypassClassTargetSelector;
import com.ibm.wala.ipa.summaries.BypassMethodTargetSelector;
import com.ibm.wala.ipa.summaries.MethodSummary;
import com.ibm.wala.ipa.summaries.XMLMethodSummaryReader;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.io.FileProvider;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.warnings.Warnings;

public class AndroidAnalysisContext {
	private static final String methodSpec = "MethodSummaries.xml";
	private static final String pathToSpec = "data";

	private final ISCanDroidOptions options;
	private final AnalysisScope scope;
	private final ClassHierarchy cha;
	
	public AndroidAnalysisContext() {
		throw new IllegalArgumentException();
	}

	public AndroidAnalysisContext(ISCanDroidOptions options)
			throws IllegalArgumentException, ClassHierarchyException,
			IOException {
		this(options, "Java60RegressionExclusions.txt");
	}

	/**
	 * @param exclusions
	 * @throws IOException
	 * @throws IllegalArgumentException
	 * @throws ClassHierarchyException
	 */
	public AndroidAnalysisContext(ISCanDroidOptions options, String exclusions)
			throws IOException, IllegalArgumentException, ClassHierarchyException {
		
		this.options = options;
		scope = AndroidAnalysisScope.setUpAndroidAnalysisScope(options.getClasspath(), exclusions, getClass().getClassLoader(), options.getAndroidLibrary());
		
		cha = ClassHierarchyFactory.make(scope);

		/*
		if (options.classHierarchyWarnings()) {
			// log ClassHierarchy warnings
			for (Warning w : Iterator2Iterable.make(Warnings.iterator())) {
				
			}
		}
		*/
		Warnings.clear();
	}
	
	

	public static SSAPropagationCallGraphBuilder makeVanillaZeroOneCFABuilder(
			AnalysisOptions options, IAnalysisCacheView cache, IClassHierarchy cha,
			AnalysisScope scope, ContextSelector customSelector,
			SSAContextInterpreter customInterpreter,
			InputStream summariesStream, MethodSummary extraSummary) {

		if (options == null) {
			throw new IllegalArgumentException("options is null");
		}
		Util.addDefaultSelectors(options, cha);
		// addDefaultBypassLogic(options, scope, Util.class.getClassLoader(),
		// cha);
		// addBypassLogic(options, scope,
		// AndroidAppLoader.class.getClassLoader(), methodSpec, cha);
		addBypassLogic(options, scope, summariesStream, cha, extraSummary);

		return ZeroXCFABuilder.make(cha, options, cache, customSelector,
				customInterpreter, ZeroXInstanceKeys.ALLOCATIONS
						| ZeroXInstanceKeys.CONSTANT_SPECIFIC);
	}

	/**
	 * @param options
	 *            options that govern call graph construction
	 * @param cha
	 *            governing class hierarchy
	 * @param scope
	 *            representation of the analysis scope
	 * @param customSelector
	 *            user-defined context selector, or null if none
	 * @param customInterpreter
	 *            user-defined context interpreter, or null if none
	 * @return a 0-CFA Call Graph Builder.
	 * @throws IllegalArgumentException
	 *             if options is null
	 *             
	 * TODO: move
	 */
	public static SSAPropagationCallGraphBuilder makeZeroCFABuilder(
			AnalysisOptions options, IAnalysisCacheView cache, IClassHierarchy cha,
			AnalysisScope scope, ContextSelector customSelector,
			SSAContextInterpreter customInterpreter,
			List<InputStream> arrayList, MethodSummary extraSummary) {
				return makeZeroCFABuilder(options, cache, cha, scope,
						customSelector, customInterpreter, Lists.newArrayList(arrayList),
						extraSummary);
			}

	/**
	 * @param options
	 *            options that govern call graph construction
	 * @param cha
	 *            governing class hierarchy
	 * @param scope
	 *            representation of the analysis scope
	 * @param customSelector
	 *            user-defined context selector, or null if none
	 * @param customInterpreter
	 *            user-defined context interpreter, or null if none
	 * @return a 0-CFA Call Graph Builder.
	 * @throws IllegalArgumentException
	 *             if options is null
	 *             
     * TODO: move
	 */
	public static SSAPropagationCallGraphBuilder makeZeroCFABuilder(
			AnalysisOptions options, IAnalysisCacheView cache, IClassHierarchy cha,
			AnalysisScope scope, ContextSelector customSelector,
			SSAContextInterpreter customInterpreter,
			Collection<InputStream> summariesStreams, MethodSummary extraSummary) {

		if (options == null) {
			throw new IllegalArgumentException("options is null");
		}
		Util.addDefaultSelectors(options, cha);
		for (InputStream stream : summariesStreams) {
			addBypassLogic(options, scope, stream, cha, extraSummary);
		}

		return ZeroXCFABuilder.make(cha, options, cache, customSelector,
				customInterpreter, ZeroXInstanceKeys.NONE);
	}

	// public static void addBypassLogic(AnalysisOptions options, AnalysisScope
	// scope, ClassLoader cl, String xmlFile,
	// IClassHierarchy cha) throws IllegalArgumentException {
	public static void addBypassLogic(AnalysisOptions options,
			AnalysisScope scope, InputStream xmlIStream, IClassHierarchy cha,
			MethodSummary extraSummary) throws IllegalArgumentException {

		if (scope == null) {
			throw new IllegalArgumentException("scope is null");
		}
		if (options == null) {
			throw new IllegalArgumentException("options is null");
		}
		// if (cl == null) {
		// throw new IllegalArgumentException("cl is null");
		// }
		if (cha == null) {
			throw new IllegalArgumentException("cha cannot be null");
		}

		try {
			Set<TypeReference> summaryClasses = HashSetFactory.make();
			Map<MethodReference, MethodSummary> summaries = HashMapFactory.make();

			if (null != xmlIStream) {
				XMLMethodSummaryReader newSummaryXML = loadMethodSummaries(
						scope, xmlIStream);
				summaryClasses.addAll(newSummaryXML.getAllocatableClasses());
				summaries.putAll(newSummaryXML.getSummaries());
			}
			// for (MethodReference mr : summaries.keySet()) {
			// 
			// }    

			try (final InputStream s = new FileProvider().getInputStreamFromClassLoader(pathToSpec
					+ File.separator + methodSpec,
					AndroidAnalysisContext.class.getClassLoader())) {

				XMLMethodSummaryReader nativeSummaries = loadMethodSummaries(scope,
						s);

				summaries.putAll(nativeSummaries.getSummaries());
				summaryClasses.addAll(nativeSummaries.getAllocatableClasses());
				if (extraSummary != null) {
					summaries.put((MethodReference) extraSummary.getMethod(),
							extraSummary);
				}

				MethodTargetSelector ms = new BypassMethodTargetSelector(
						options.getMethodTargetSelector(), summaries,
						nativeSummaries.getIgnoredPackages(), cha);
				options.setSelector(ms);

				ClassTargetSelector cs = new BypassClassTargetSelector(
						options.getClassTargetSelector(), summaryClasses, cha,
						cha.getLoader(scope.getLoader(Atom
								.findOrCreateUnicodeAtom("Synthetic"))));
				options.setSelector(cs);
				
			}
		} catch (IOException e) {
			e.printStackTrace();
		}

	}

	private static XMLMethodSummaryReader loadMethodSummaries(
			AnalysisScope scope, InputStream xmlIStream) {
		try (InputStream s = xmlIStream != null ? xmlIStream :
			AndroidAnalysisContext.class.getClassLoader()
				.getResourceAsStream(
					pathToSpec + File.separator + methodSpec)) {
			return new XMLMethodSummaryReader(s, scope);
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	/**
	 * Returns all concrete classes implementing the given interface or any subinterfaces
	 * @param iRoot
	 */
	public Collection<IClass> concreteClassesForInterface(IClass iRoot) {
		Set<IClass> clazzes = HashSetFactory.make();
		Set<IClass> done = HashSetFactory.make();
		Deque<IClass> todo = Queues.newArrayDeque();
		todo.push(iRoot);
		
		while (!todo.isEmpty()) {
			IClass i = todo.pop();
			for (IClass clazz : cha.getImplementors(i.getReference())) {
				if (clazz.isInterface() && !done.contains(clazz)) {
					done.add(i);
					todo.push(clazz);
				} else if (!clazz.isAbstract()) {
					clazzes.add(clazz);
				}
			}
		}
		
		return clazzes;
	}
	
	public ISCanDroidOptions getOptions() {
		return options;
	}
	
	public AnalysisScope getScope() {
		return scope;
	}
	
	public ClassHierarchy getClassHierarchy() {
		return cha;
	}
}
