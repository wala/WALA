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

import java.util.Iterator;

import com.ibm.wala.util.graph.Graph;

/**
 *  This interface encapsulates the result of a proagation-based
 * bit-vector dataflow analysis done over a set of PointerKey objects
 * related by dataflow.  There are several aspects of the analysis
 * summarized by this interface:
 *
 * 1) The graph embedded in an AbstractAnalysis corresponds to the
 *    dataflow induces over the keys, so one PointerKey is a graph
 *    successor of another one if there is dataflow from the first to
 *    second.
 *
 * 2) Each key maps to an IntSet denoting which bits are set for that
 *    PointerKey.  These bits correspond to whatever analysis property
 *    has been analyzed.
 *
 * 3) The bits each have some interpretation, and the AbstractAnalysis
 *    provides a mapping from each bit index to an object representing
 *    its interpretation.  
 *
 * 4) The assumption is that each bit starts at some set of
 *    PointerKeys and flows to others via dataflow.  The
 *    AbstractAnalysis provides an iterator over the sources of each
 *    bit.
 *
 * 5) In object-oriented programs, there is non-local data flow
 *    induced by field accesses.  The set of constraints at a field
 *    access depend upon the container object.  Thus, for each
 *    constraint induced by a field access, there is a corresponding
 *    PointerKey that is the container object;  AbstractAnalysis
 *    provides an explicit accessor of that relationship. 
 *
 * @author Julian Dolby
 */
public interface AbstractAnalysis extends Graph {

  /**
   * Get the set of bits that are set for the given key.  Do not
   * mutate the set returned, since it can be internal solver state.
   * (It is not an Iterator for efficiency reasons.)
   *
   * @param key The PointerKey or other object of interest
   * @return The set of bits enabled for that key.
   */
  Object getValue(Object key);

  /**
   *  Get the interpretation of the given bit.  The AbstractAnalysis
   * provides no particular semantics for this method, since it
   * depends upon the analysis being done.
   *
   * @param index The bit index of interest
   * @return The meaning of the given bit.
   */    
  Object getInterpretation(int index);

  int getEncoding(Object thing);

  /**
   *  Get the PointerKeys that represent sources of the dataflow
   * property represented by the given bit.
   *
   * @param index The bit index of interest
   * @return The PointerKeys that are sources for the given bit.
   */
  Iterator getSources(int index);

  /**
   *  Get the PointerKey representing the container object that
   * induced dataflow between the given field and local value.  Can
   * return null if no such container exists.
   *
   * @param xval An lval or rval of a field or array access
   * @param field The field involved
   * @return The container object, if any
   */
  Iterator getContainerReadLink(Object xval, Object field);

  Iterator getContainerWriteLink(Object xval, Object field);

  /**
   *  Get the concrete type (i.e. InstanceKey) that contains the
   * given pointer key, which must be a pointer key of a field of
   * some kind.
   *
   * @return The containing instance key
   */
  Object getContainer(Object fieldKey);

  boolean isField(Object v);

}
