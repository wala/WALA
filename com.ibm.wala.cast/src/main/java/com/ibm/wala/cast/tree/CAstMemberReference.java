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
package com.ibm.wala.cast.tree;

public interface CAstMemberReference extends CAstReference {

  public static final CAstMemberReference FUNCTION =
      new CAstMemberReference() {
        @Override
        public String member() {
          return "the function body";
        }

        @Override
        public CAstType type() {
          return null;
        }

        @Override
        public String toString() {
          return "Any::FUNCTION CALL";
        }

        @Override
        public int hashCode() {
          return toString().hashCode();
        }

        @Override
        public boolean equals(Object o) {
          return o == this;
        }
      };

  String member();
}
