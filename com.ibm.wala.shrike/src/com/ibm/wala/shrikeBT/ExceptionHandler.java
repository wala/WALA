/*******************************************************************************
 * Copyright (c) 2002,2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.shrikeBT;

/**
 * An ExceptionHandler represents a single handler covering a single instruction. It simply tells us what kind of exception to catch
 * and where to dispatch the exception to.
 * 
 * ExceptionHandlers are immutable. It is quite legal to save a reference to an exception handler and use it in any other context.
 * We also treat arrays of ExceptionHandlers as immutable. Therefore the following code can be used to build an exception handler
 * table that specifies two handlers covering an entire block of code:
 * 
 * <pre>
 * 
 *   ExceptionHandler[] hs = {
 *     new ExceptionHandler(110, &quot;Ljava.lang.NullPointerException;&quot;),
 *     new ExceptionHandler(220, &quot;Ljava.io.IOException;&quot;);
 *   };
 *   for (int i = 0; i &lt; 100; i++) {
 *     handlers[i] = hs;
 *   }
 * </pre>
 */
final public class ExceptionHandler {

  int handler;

  final String catchClass;
  
  final Object catchClassLoader;

  /**
   * @param handler the label for the handler code
   * @param catchClass the type of exception that should be caught (in JVM format), or null if all exceptions should be caught (as
   *          with 'finally')
   */
  public ExceptionHandler(int handler, String catchClass, Object catchClassLoader) {
    this.handler = handler;
    this.catchClass = catchClass;
    this.catchClassLoader = catchClassLoader;
  }

  public ExceptionHandler(int handler, String catchClass) {
    this(handler, catchClass, null);
  }
  
  /**
   * @return the label of the handler code
   */
  public int getHandler() {
    return handler;
  }

  public Object getCatchClassLoader() {
    return catchClassLoader;
  }

  /**
   * @return the type of exceptions to be caught, or null if all exceptions should be caught
   */
  public String getCatchClass() {
    return catchClass;
  }

  public boolean equals(ExceptionHandler h) {
    if (h == null) {
      throw new IllegalArgumentException("h is null");
    }
    return h.handler == handler && (catchClass == null ? h.catchClass == null : catchClass.equals(h.catchClass));
  }

  @Override
  public int hashCode() {
    return 1069 * handler + ((catchClass == null) ? 0 : catchClass.hashCode());
  }

  @Override
  public boolean equals(Object o) {
    if (o instanceof ExceptionHandler) {
      return equals((ExceptionHandler) o);
    } else {
      return false;
    }
  }
}
