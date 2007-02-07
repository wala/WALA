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
package com.ibm.wala.eclipse.cg.model;

import com.ibm.wala.cast.js.ipa.callgraph.*;
import com.ibm.wala.cast.js.types.*;
import com.ibm.wala.ipa.callgraph.*;
import com.ibm.wala.ipa.callgraph.impl.Util;
import com.ibm.wala.ipa.cha.ClassHierarchy;
import com.ibm.wala.util.graph.InferGraphRootsImpl;
import com.ibm.wala.util.warnings.WalaException;
import com.ibm.wala.util.warnings.WarningSet;

import org.eclipse.jdt.core.*;

import java.util.Collection;

/**
 * 
 * @author aying
 */
public class WalaWebPageCGModel extends WalaProjectCGModel {

  public WalaWebPageCGModel(String htmlScriptFile) {
    super(htmlScriptFile);
  }


  protected Entrypoints 
    getEntrypoints(AnalysisScope scope, ClassHierarchy cha) 
  {
    return 
      new JavaScriptEntryPoints(
        cha, 
	cha.getLoader(JavaScriptTypes.jsLoader));
  }

  protected Collection inferRoots(CallGraph cg) throws WalaException {
    return InferGraphRootsImpl.inferRoots(cg);
  }

}
