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
package com.ibm.wala.util.internationalization;

/**
 * Implementation of this interface can read messages in a resource bundle with the appropriate key.
 * 
 * @author egeay
 */
public interface IStringBundle {
  
  /**
   * Returns the message related to the given ID transmitted by parameter.
   * 
   * @param messageID The ID for the message in the resource bundle.
   */
  public String get( final String messageID );
  
  /**
   * Returns the parameterized message related to the ID and argument transmitted by parameters.
   * 
   * @param messageID The ID for the message in the resource bundle.
   * @param argument The argument for this parameterized message.
   */
  public String get( final String messageID, final Object argument );
  
  /**
   * Returns the parameterized message related to the ID and arguments transmitted by parameters.
   * 
   * @param messageID The ID for the message in the resource bundle.
   * @param arguments The arguments for this parameterized message.
   */
  public String get( final String messageID, final Object[] arguments );
  
  /**
   * Returns the message related to the ID which is a concatenation of next expression:
   * <code>
   *   clazz.getName() + '.' + secondPartOfID
   * </code>
   * 
   * @param clazz The class in which the externalized message is present. Represents the first part
   * of the ID construction.
   * @param secondPartOfID The second part of the ID used for the final ID construction.
   */
  public String get( final Class clazz, final String secondPartOfID );
  
  /**
   * Returns the parameterized message related to the argument passed by parameter and the ID 
   * which is a concatenation of next expression:
   * <code>
   *   clazz.getName() + '.' + secondPartOfID
   * </code>
   * 
   * @param clazz The class in which the externalized message is present. Represents the first part
   * of the ID construction.
   * @param secondPartOfID The second part of the ID used for the final ID construction.
   * @param argument The argument for this parameterized message.
   */
  public String get( final Class clazz, final String secondPartOfID, final Object argument );
  
  /**
   * Returns the parameterized message related to the arguments passed by parameter and the ID 
   * which is a concatenation of next expression:
   * <code>
   *   clazz.getName() + '.' + secondPartOfID
   * </code>
   * 
   * @param clazz The class in which the externalized message is present. Represents the first part
   * of the ID construction.
   * @param secondPartOfID The second part of the ID used for the final ID construction.
   * @param arguments The arguments for this parameterized message.
   */
  public String get( final Class clazz, final String secondPartOfID, final Object[] arguments );
  
}
