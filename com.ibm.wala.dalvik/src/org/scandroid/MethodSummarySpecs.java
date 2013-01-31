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
package org.scandroid;

import java.io.UTFDataFormatException;
import java.util.List;

import org.scandroid.spec.EntryArgSinkSpec;
import org.scandroid.spec.EntryArgSourceSpec;
import org.scandroid.spec.EntryRetSinkSpec;
import org.scandroid.spec.ISpecs;
import org.scandroid.spec.MethodNamePattern;
import org.scandroid.spec.SinkSpec;
import org.scandroid.spec.SourceSpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import com.google.common.collect.Lists;
import com.ibm.wala.ipa.summaries.MethodSummary;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;

/**
 * @author creswick
 *
 */
public class MethodSummarySpecs implements ISpecs {
	private static final Logger logger = LoggerFactory.getLogger(MethodSummarySpecs.class);

	private final MethodSummary methodRef;

	public MethodSummarySpecs(MethodSummary methodRef) {
		this.methodRef = methodRef;
	}
	
	/* (non-Javadoc)
	 * @see spec.ISpecs#getEntrypointSpecs()
	 */
	@Override
	public MethodNamePattern[] getEntrypointSpecs() {
		return new MethodNamePattern[0];
	}

	/* (non-Javadoc)
	 * @see spec.ISpecs#getSourceSpecs()
	 */
	@Override
	public SourceSpec[] getSourceSpecs() {
		try {
			return getSources(methodRef).toArray(new SourceSpec[] {});
		} catch (UTFDataFormatException e) {
			e.printStackTrace();
		}
		return new SourceSpec[] {};
	}

	/* (non-Javadoc)
	 * @see spec.ISpecs#getSinkSpecs()
	 */
	@Override
	public SinkSpec[] getSinkSpecs() {
		try {
			return getSinks(methodRef).toArray(new SinkSpec[] {});
		} catch (UTFDataFormatException e) {
			e.printStackTrace();
		}
		return new SinkSpec[] {};
	}

	private List<SinkSpec> getSinks(MethodSummary mSummary) throws UTFDataFormatException {
		List<SinkSpec> sinks = Lists.newArrayList();
		
		MethodReference mRef = (MethodReference) mSummary.getMethod();
		
		//
		// Add the args as EntryArgSinkSpecs:
		// 
		String className = mRef.getDeclaringClass().getName().toUnicodeString();
		String methodName = mRef.getName().toUnicodeString();
		String descriptor = mRef.getDescriptor().toUnicodeString();
		MethodNamePattern pattern = new MethodNamePattern(className, methodName, descriptor);
		
		int[] argNums = new int[mSummary.getNumberOfParameters()];
		for (int i = 0; i < argNums.length; i++) {
			argNums[i] = i;
		}
		sinks.add(new EntryArgSinkSpec(pattern, argNums));
		
		TypeReference typeRef = mRef.getReturnType();
		if (! typeRef.equals(TypeReference.Void)) {
			//
			// Add the return value as a EntryRetSinkSpec
			//
			sinks.add(new EntryRetSinkSpec(pattern));
		}
		
		logger.debug("found sinks: " + sinks.toString());
		return sinks;
	}

	private List<SourceSpec> getSources(MethodSummary mSummary) throws UTFDataFormatException {
		List<SourceSpec> sources = Lists.newArrayList();
		MethodReference mRef = (MethodReference) mSummary.getMethod();
		
		//
		// Add the args as EntryArgSourceSpecs:
		// 
		String className = mRef.getDeclaringClass().getName().toUnicodeString();
		String methodName = mRef.getName().toUnicodeString();
		String descriptor = mRef.getDescriptor().toUnicodeString();
		MethodNamePattern pattern = new MethodNamePattern(className, methodName, descriptor);

		int[] argNums = new int[mSummary.getNumberOfParameters()];
		for (int i = 0; i < argNums.length; i++) {
			argNums[i] = i;
		}
		sources.add(new EntryArgSourceSpec(pattern, argNums));
		
		logger.debug("found sources: " + sources.toString());
		return sources;
	}
}
