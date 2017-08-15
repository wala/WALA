/*******************************************************************************
 * Copyright (c) 2013 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package reflection;

import java.util.Hashtable;

public class Reflect3 {

  public static void main(String[] args) throws ClassNotFoundException, IllegalAccessException, InstantiationException {
    Class<?> c = Class.forName("java.util.Properties");
    @SuppressWarnings("unchecked")
    Hashtable<Object, Object> h = (Hashtable<Object, Object>) c.newInstance();
    System.out.println(h.toString());
  }
  
  @SuppressWarnings("unused")
  private static class Hash extends Hashtable<Object, Object> {
    
  }
}
