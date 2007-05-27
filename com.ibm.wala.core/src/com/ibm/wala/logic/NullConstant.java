/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.logic;

/**
 * @author schandra_sf
 */

import java.util.Collection;
import java.util.Collections;

public class NullConstant extends IntConstant implements IConstant {
	
	// static NullConstant SINGLE = (NullConstant) new IntConstant(0);
	
    private NullConstant() {
      super(0);
    }
    
	public static NullConstant make() {
		return new NullConstant();
	}

	public Collection<Variable> getFreeVariables() {
		return Collections.emptySet();
	}

	public Kind getKind() {
		return Kind.CONSTANT;
	}

	public String toString() {
		return "null";
	}
}
