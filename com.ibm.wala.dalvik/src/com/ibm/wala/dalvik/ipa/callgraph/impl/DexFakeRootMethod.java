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
 *  Galois, Inc. (Aaron Tomb <atomb@galois.com>, Rogan Creswick <creswick@galois.com>)
 *  Steve Suh    <suhsteve@gmail.com>
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
package com.ibm.wala.dalvik.ipa.callgraph.impl;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.IAnalysisCacheView;
import com.ibm.wala.ipa.callgraph.impl.AbstractRootMethod;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.callgraph.impl.FakeRootClass;
import com.ibm.wala.ipa.callgraph.impl.FakeRootMethod;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MemberReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.warnings.Warning;
import com.ibm.wala.util.warnings.Warnings;

/**
 *  @deprecated building the Android-model uses a "normal" fake-root now
 */
@Deprecated
public class DexFakeRootMethod extends AbstractRootMethod {

	public static final Atom name = Atom.findOrCreateAsciiAtom("DexFakeRootMethod");

	public static final Descriptor descr = Descriptor.findOrCreate(new TypeName[0], TypeReference.VoidName);

	public static final MethodReference rootMethod = MethodReference.findOrCreate(FakeRootClass.FAKE_ROOT_CLASS, name, descr);
	
	public static Map<TypeReference, Integer> referenceTypeMap = new HashMap<>();
	
//	public static Set<TypeReference> referenceTypeSet = new HashSet<TypeReference>();
	
	public DexFakeRootMethod(final IClassHierarchy cha, AnalysisOptions options, IAnalysisCacheView cache) {
		super(rootMethod, cha, options, cache);
	}

	@Override
	public SSANewInstruction addAllocation(TypeReference T) {
		return addAllocation(T, true);
	}

	private SSANewInstruction addAllocation(TypeReference T, boolean invokeCtor) {
		if (T == null) {
			throw new IllegalArgumentException("T is null");
		}
		int instance = nextLocal++;
		SSANewInstruction result = null;
		if (T.isReferenceType()) {
			NewSiteReference ref = NewSiteReference.make(statements.size(), T);
			if (T.isArrayType()) {
				int[] sizes = new int[T.getDimensionality()];
				Arrays.fill(sizes, getValueNumberForIntConstant(1));
				result = insts.NewInstruction(statements.size(), instance, ref, sizes);
			} else {
				result = insts.NewInstruction(statements.size(), instance, ref);
			}
			statements.add(result);

			IClass klass = cha.lookupClass(T);
			if (klass == null) {
				Warnings.add(AllocationFailure.create(T));
				return null;
			}

			if (klass.isArrayClass()) {
				int arrayRef = result.getDef();
				TypeReference e = klass.getReference().getArrayElementType();
				while (e != null && !e.isPrimitiveType()) {
					// allocate an instance for the array contents
					NewSiteReference n = NewSiteReference.make(statements.size(), e);
					int alloc = nextLocal++;
					SSANewInstruction ni = null;
					if (e.isArrayType()) {
						int[] sizes = new int[T.getDimensionality()];
						Arrays.fill(sizes, getValueNumberForIntConstant(1));
						ni = insts.NewInstruction(statements.size(), alloc, n, sizes);
					} else {
						ni = insts.NewInstruction(statements.size(), alloc, n);
					}
					statements.add(ni);

					// emit an astore
					SSAArrayStoreInstruction store = insts.ArrayStoreInstruction(statements.size(), arrayRef, getValueNumberForIntConstant(0), alloc, e);
					statements.add(store);

					e = e.isArrayType() ? e.getArrayElementType() : null;
					arrayRef = alloc;
				}
			}
			if (invokeCtor) {
				IMethod ctor = cha.resolveMethod(klass, MethodReference.initSelector);
				if (ctor!=null) {
					int[] allocSites = null;
					referenceTypeMap.put(T, instance);

					if (!ctor.getDeclaringClass().getName().toString().equals(klass.getName().toString())) {
						boolean found = false;
						for (IMethod im: klass.getAllMethods()) {
							if (im.getDeclaringClass().getName().toString().equals(klass.getName().toString()) && 
									im.getSelector().getName().toString().equals(MethodReference.initAtom.toString())) {
								ctor = im;
								allocSites = new int[ctor.getNumberOfParameters()];
								allocSites[0] = instance;
								for (int j = 1; j < ctor.getNumberOfParameters(); j++) {
									if (im.getParameterType(j).isPrimitiveType()) {
										allocSites[j] = addLocal();
									}
									else if (referenceTypeMap.containsKey(im.getParameterType(j))) {
										allocSites[j] = referenceTypeMap.get(im.getParameterType(j)).intValue();
									}
									else {
										SSANewInstruction n = addAllocation(im.getParameterType(j), invokeCtor); 
										allocSites[j] = (n == null) ? -1 : n.getDef();
										referenceTypeMap.put(im.getParameterType(j), allocSites[j]);
									}									
								}
								found = true;
								break;
							}
						}
						if (!found) {
							Set<IClass> implementors = cha.getImplementors(T);
							int[] values = new int[implementors.size()];
							int countErrors = 0;
							int index = 0;
							for (IClass ic: implementors){
								int value;
								
								if (referenceTypeMap.containsKey(ic.getReference())) {
									value = referenceTypeMap.get(ic.getReference()).intValue();
								}
								else {
									SSANewInstruction n = addAllocation(ic.getReference(), invokeCtor);
									value = (n == null) ? -1 : n.getDef();
									referenceTypeMap.put(ic.getReference(), value);
								}
								if (value == -1) {
									countErrors++;
								} else {
									values[index - countErrors] = value;
								}
								index++;
							}

							if (countErrors > 0) {
								int[] oldValues = values;
								values = new int[oldValues.length - countErrors];
								System.arraycopy(oldValues, 0, values, 0, values.length);
							}
							if (values.length > 1) {
								instance = addPhi(values);
								referenceTypeMap.put(T, instance);
							}
						}
					}
					if (allocSites!=null)
						for (int allocSite : allocSites) {
							if (allocSite == -1) {
								Warnings.add(AllocationFailure.create(T));
								return null;
							}
						}
					addInvocation(allocSites==null?new int[] {instance}:allocSites, CallSiteReference.make(statements.size(), ctor.getReference(),
							IInvokeInstruction.Dispatch.SPECIAL));
				}
			}
		}
		cache.invalidate(this, Everywhere.EVERYWHERE);
		return result;
	}
	

	private static class AllocationFailure extends Warning {

		final TypeReference t;

		AllocationFailure(TypeReference t) {
			super(Warning.SEVERE);
			this.t = t;
		}

		@Override
		public String getMsg() {
			return getClass().toString() + " : " + t;
		}

		public static AllocationFailure create(TypeReference t) {
			return new AllocationFailure(t);
		}
	}
	
	/**
	 * @return true iff m is the fake root method.
	 * @throws IllegalArgumentException if m is null
	 */
	public static boolean isFakeRootMethod(MemberReference m) {
		if (m == null) {
			throw new IllegalArgumentException("m is null");
		}
		return m.equals(rootMethod);
	}

	/**
	 * @return true iff block is a basic block in the fake root method
	 * @throws IllegalArgumentException if block is null
	 */
	public static boolean isFromFakeRoot(IBasicBlock<?> block) {
		if (block == null) {
			throw new IllegalArgumentException("block is null");
		}
		IMethod m = block.getMethod();
		return FakeRootMethod.isFakeRootMethod(m.getReference());
	}

	public static MethodReference getRootMethod() {
		return rootMethod;
	}

}
