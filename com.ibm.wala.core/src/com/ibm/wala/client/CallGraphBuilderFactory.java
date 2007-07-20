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
package com.ibm.wala.client;

import com.ibm.wala.ipa.callgraph.AnalysisCache;
import com.ibm.wala.ipa.callgraph.AnalysisOptions;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CallGraphBuilder;
import com.ibm.wala.ipa.cha.IClassHierarchy;

/**
 * 
 * A factory for call graph builders; tailored to J2EE
 * 
 * @author sfink
 */
public interface CallGraphBuilderFactory {

  public final static String IMPL_PROPERTY = "callGraphBuilderFactoryImplementation";
  
  public final static String RTA_BUILDER_FACTORY = "com.ibm.wala.j2ee.client.impl.RTABuilderFactory";

  public final static String XTA_BUILDER_FACTORY = "com.ibm.wala.j2ee.client.impl.XTABuilderFactory";

  public final static String XTA_CONTAINER_BUILDER_FACTORY = "com.ibm.wala.j2ee.client.impl.XTAContainerBuilderFactory";

  public final static String ZERO_CFA_BUILDER_FACTORY = "com.ibm.wala.j2ee.client.impl.ZeroCFABuilderFactory";

  public final static String ZERO_CONTAINER_CFA_BUILDER_FACTORY = "com.ibm.wala.j2ee.client.impl.ZeroContainerCFABuilderFactory";

  public final static String ZERO_ONE_CFA_BUILDER_FACTORY = "com.ibm.wala.j2ee.client.impl.ZeroOneCFABuilderFactory";

  public final static String ZERO_ONE_CONTAINER_CFA_BUILDER_FACTORY = "com.ibm.wala.j2ee.client.impl.ZeroOneContainerCFABuilderFactory";

  public final static String OBJECT_SENSITIVE_CONTAINER_HACK_CFA_BUILDER_FACTORY = "com.ibm.wala.j2ee.client.impl.ObjectSensitiveContainerHackCFABuilderFactory";

  /**
   * @param options
   *          options that govern call graph construction
   * @param cha
   *          governing class hierarchy
   * @param scope
   *          representation of the analysis scope
   * @param keepPointsTo
   *          preserve PointsTo graph for posterity?
   *  
   */
  CallGraphBuilder make(AnalysisOptions options, AnalysisCache cache, IClassHierarchy cha, AnalysisScope scope, boolean keepPointsTo);
}
