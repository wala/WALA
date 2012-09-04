/*******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ipa.callgraph.propagation;

/**
 * A PointerKey instance serves as the representative for an equivalence class
 * of pointers. (or more generally ...locations, if we allow primitives).
 * 
 * For example, a PointerKey for 0-CFA might be - a <CGNode,int> pair, where the
 * int represents an SSA value number. This PointerKey would represent all
 * values of the pointer of a particular local variable. - a <FieldReference>,
 * representing the set of instances of a given field in the heap, or of a
 * particular static field.
 * 
 * A PointerKey for 0-1-CFA, with 1-level of InstanceVar context in the Grove et
 * al. terminology, would instead of FieldReference, use a - <InstanceKey,
 * FieldReference> pair
 */
public interface PointerKey {

}
