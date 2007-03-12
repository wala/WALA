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
package com.ibm.wala.ipa.callgraph;

/**
 *
 * This just exists to enforce strong typing.
 * 
 * @author sfink
 */
public interface ContextKey {
  
  /**
   * A property of contexts that might be generally useful: the "caller" method ...
   * used for call-string context schemes.
   */
  public final static ContextKey CALLER = new ContextKey() {
  };
  
  /**
   * A property of contexts that might be generally useful: the "call site" method ...
   * used for call-string context schemes.
   */
  public final static ContextKey CALLSITE = new ContextKey() {
  };
  
  /**
   * A property of contexts that might be generally useful: an identifier for the
   * receiver object ... used for object-sensitivity context policies.
   * 
   * Known implementations (ContextItems) for RECEIVER include TypeAbstraction and
   * InstanceKey
   */
  public final static ContextKey RECEIVER = new ContextKey() {
  };
  
  /**
   * A property of contexts that might be generally useful: an identifier 
   * for the type filters applied to the receiver object ... used for 
   * filtering propagation across dynamic dispatched
   * 
   *  Implementations (ContextItems) for FILTER are to be instances of
   * FilteredContextKey.TypeFilter
   * 
   */
  public final static ContextKey FILTER = new ContextKey() {
  };
  
}
