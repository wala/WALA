/*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released under the terms listed below.  
 *
 */
/*
 *
 * Copyright (c) 2009-2012,
 *
 *  Adam Fuchs          <afuchs@cs.umd.edu>
 *  Avik Chaudhuri      <avik@cs.umd.edu>
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

package org.scandroid.domain;

import org.scandroid.flow.types.FlowType;

@SuppressWarnings("rawtypes")
public class DomainElement {
	// the code element in question
	// alternate framing: the /current/ fact about the element
	public final CodeElement codeElement;
	// the taint (probably from some other point in the code) that affects the
	// code element in question
	// alternate framing: the /initial/ fact about the element
	public final FlowType taintSource;

	public DomainElement(CodeElement codeElement, FlowType taintSource) {
		this.codeElement = codeElement;
		this.taintSource = taintSource;
	}
/*
	@Override
	public boolean equals(Object other) {
		if (other == null || !(other instanceof DomainElement))
			return false;
		DomainElement otherDE = (DomainElement) other;
		if (taintSource != null) {
			return codeElement.equals(otherDE.codeElement)
					&& taintSource.equals(otherDE.taintSource);
		}
		return codeElement.equals(otherDE.codeElement)
				&& otherDE.taintSource == null;
	}	
	
	@Override
	public int hashCode() {
		if (taintSource == null)
			return codeElement.hashCode();
		return codeElement.hashCode() ^ taintSource.hashCode();
	}
 */
	
	
	
	@Override
	public String toString() {
		return codeElement.toString() + ", " + taintSource;
	}



	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result
				+ ((codeElement == null) ? 0 : codeElement.hashCode());
		result = prime * result
				+ ((taintSource == null) ? 0 : taintSource.hashCode());
		return result;
	}



	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DomainElement other = (DomainElement) obj;
		if (codeElement == null) {
			if (other.codeElement != null)
				return false;
		} else if (!codeElement.equals(other.codeElement))
			return false;
		if (taintSource == null) {
			if (other.taintSource != null)
				return false;
		} else if (!taintSource.equals(other.taintSource))
			return false;
		return true;
	}
}
