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

package com.ibm.wala.j2ee.client;

/**
 * Interface to information about a method. 
 *
 * @author sfink
 */
public interface IMethod {
  /**
   * This method returns something of the form com.foo.bar
   * 
   * @return String representation of the declaring class
   */
  public abstract IClass getDeclaringClass();
  /**
   * This method returns something like "Primordial" or "Application"
   * 
   * @return String representation of the name of the classloader
   */
  public abstract String getClassLoaderName();
  /**
   * This method returns something like createLargeOrder
   * 
   * @return String representation of the name of the method
   */
  public abstract String getName();
  /**
   * A descriptor is a string like: 
   *   (IILjava.lang.String;SLjava.sql.Date;)Ljava.lang.Integer;
   * @return String representation of the signature
   */
  public abstract String getDescriptor();
  /**
   * A signature is a string like: 
   *   com.foo.bar.createLargeOrder(IILjava/lang/String;SLjava/sql/Date;)Ljava/lang/Integer;
   * @return String representation of the signature
   */
  public abstract String getSignature();
  /**
   * A selector is a string like: 
   *   createLargeOrder(IILjava.lang.String;SLjava.sql.Date;)Ljava.lang.Integer;
   * @return String representation of the selector
   */
  public abstract String getSelector();
}