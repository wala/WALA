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
package com.ibm.wala.cast.tree;

public interface CAstMemberReference extends CAstReference {

  public static final CAstMemberReference FUNCTION = 
    new CAstMemberReference() {
      public String member() {
	return "the function body";
      }

      public CAstType type() {
	return null;
      }
      
      public String toString() {
	return "Any::FUNCTION CALL";
      }

      public int hashCode() {
        return toString().hashCode(); 
      }

      public boolean equals(Object o) {
	return o == this;
      }
    };

  String member();

}
