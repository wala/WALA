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
 *  Galois, Inc. (Aaron Tomb <atomb@galois.com>, 
 *                Rogan Creswick <creswick@galois.com>, 
 *                Adam Foltzer <acfoltzer@galois.com>)
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

package org.scandroid.spec;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.util.strings.StringStuff;

/**
 * @author creswick
 *
 */
public class StaticSpecs implements ISpecs {

	private final ClassHierarchy cha;
	private final String methodSignature;
    private final Collection<IField> fields;

	public StaticSpecs(ClassHierarchy cha, String methodSignature) {
		this.cha = cha;
		this.methodSignature = methodSignature;
		this.fields = collectFields();
	}

	/* (non-Javadoc)
	 * @see org.scandroid.spec.ISpecs#getEntrypointSpecs()
	 */
	@Override
	public MethodNamePattern[] getEntrypointSpecs() {
		return new MethodNamePattern[0];
	}

	/* (non-Javadoc)
	 * @see org.scandroid.spec.ISpecs#getSourceSpecs()
	 */
	@Override
	public SourceSpec[] getSourceSpecs() {
//		List<SourceSpec> specs = Lists.newArrayList();
//
//		for (IField field : fields) {
//			specs.add(new StaticFieldSourceSpec(field));
//		}
//
//		return specs.toArray(new SourceSpec[] {});
		return new SourceSpec[] {};
	}

	private List<IField> collectFields() {
		List<IField> fields = new ArrayList<>();
		Iterator<IClass> itr = cha.iterator();
		while (itr.hasNext()) {
			IClass cls = itr.next();
			for (IField field : cls.getAllStaticFields()) {
				if (field.getFieldTypeReference().isReferenceType()) {
					fields.add(field);
				}
			}
		}
		return fields;
	}

	/* (non-Javadoc)
	 * @see org.scandroid.spec.ISpecs#getSinkSpecs()
	 */
	@Override
	public SinkSpec[] getSinkSpecs() {		
		List<SinkSpec> specs = new ArrayList<>();
		Collection<IMethod> methods = cha.getPossibleTargets(StringStuff.makeMethodReference(methodSignature));
		for (IField field : fields) {
			if (!field.isFinal()) {
				for (IMethod method : methods) {
					specs.add(new StaticFieldSinkSpec(field, method));
				}
			}
		}
		return specs.toArray(new SinkSpec[] {});
	}

}
