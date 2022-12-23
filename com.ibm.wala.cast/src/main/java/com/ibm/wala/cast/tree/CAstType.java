/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
/*
 * Created on Aug 30, 2005
 */
package com.ibm.wala.cast.tree;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

public interface CAstType {
  /** Returns the fully-qualified (e.g. bytecode-compliant for Java) type name. */
  String getName();

  Collection<CAstType> getSupertypes();

  interface Primitive extends CAstType {
    // Need anything else? The name pretty much says it all...
  }

  interface Reference extends CAstType {}

  interface Class extends Reference {
    boolean isInterface();

    Collection<CAstQualifier> getQualifiers();
  }

  interface Array extends Reference {
    int getNumDimensions();

    CAstType getElementType();
  }

  interface Function extends Reference {
    CAstType getReturnType();

    List<CAstType> getArgumentTypes();

    Collection<CAstType> getExceptionTypes();

    int getArgumentCount();
  }

  interface Method extends Function {
    CAstType getDeclaringType();

    boolean isStatic();
  }

  interface Complex extends CAstType {

    CAstType getType();
  }

  interface Union extends Complex {

    Iterable<CAstType> getConstituents();
  }

  CAstType DYNAMIC =
      new CAstType() {

        @Override
        public String getName() {
          return "DYNAMIC";
        }

        @Override
        public Collection<CAstType> /*<CAstType>*/ getSupertypes() {
          return Collections.emptySet();
        }
      };
}
