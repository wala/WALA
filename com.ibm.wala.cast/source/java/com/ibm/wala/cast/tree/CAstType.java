/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
/*
 * Created on Aug 30, 2005
 */
package com.ibm.wala.cast.tree;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public interface CAstType {
    /**
     * Returns the fully-qualified (e.g. bytecode-compliant for Java) type name.
     */
    String getName();

    Collection<CAstType> getSupertypes();

    public interface Primitive extends CAstType {
	// Need anything else? The name pretty much says it all...
    }

    public interface Reference extends CAstType {
    }

    public interface Class extends Reference {
      boolean isInterface();

      Collection<CAstQualifier> getQualifiers();
    }

    public interface Array extends Reference {
	int getNumDimensions();
	CAstType getElementType();
    }

    public interface Function extends Reference {
	CAstType getReturnType();

	List<CAstType> getArgumentTypes();
	Collection<CAstType> getExceptionTypes();

	int getArgumentCount();
    }

    public interface Method extends Function {
	CAstType getDeclaringType();
    }

    public interface Complex extends CAstType {
      
      CAstType getType();

    }
    
    public static final CAstType DYNAMIC = new CAstType() {
    
      @Override
      public String getName() {
	return "DYNAMIC";
      }

      @Override
      public Collection<CAstType>/*<CAstType>*/ getSupertypes() {
	return Collections.EMPTY_SET;
      }
 
   };
}
