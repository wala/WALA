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
package com.ibm.wala.j2ee.client.impl;

import java.util.Properties;

import com.ibm.wala.j2ee.client.J2EECallGraphBuilderFactory;
import com.ibm.wala.util.debug.Assertions;

/**
 *
 * A factory for creating a call graph builder factory
 * 
 * @author Stephen Fink
 */

public class CallGraphBuilderFactoryFactory {
  
  /**
   * Construct a CallGraphBuilderFactory
   * 
   * @param props  Optionally, influence the construction of the engine.
   * @return A non-null AppAnalysisEngine instance.
   */
  public static J2EECallGraphBuilderFactory getCallGraphBuilderFactory(Properties props) {
    try {
      String klass = "com.ibm.wala.j2ee.client.impl.RTABuilderFactory";
      if (props != null) {
        klass = props.getProperty("analysis", "com.ibm.wala.j2ee.client.impl.RTABuilderFactory");
      }
      return (J2EECallGraphBuilderFactory) Class.forName(klass).newInstance();
    } catch (Exception e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
      return null;
    }
  }
}
