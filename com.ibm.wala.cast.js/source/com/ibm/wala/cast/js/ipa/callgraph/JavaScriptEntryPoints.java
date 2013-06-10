/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.js.ipa.callgraph;

import com.ibm.wala.cast.ipa.callgraph.ScriptEntryPoints;
import com.ibm.wala.cast.js.loader.JSCallSiteReference;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.cha.IClassHierarchy;

public class JavaScriptEntryPoints extends ScriptEntryPoints {

  @Override
  protected CallSiteReference makeScriptSite(IMethod m, int pc) {
    return new JSCallSiteReference(pc);
  }

  public JavaScriptEntryPoints(IClassHierarchy cha, IClassLoader loader) {
    super(cha, loader.lookupClass(JavaScriptTypes.Script.getName()));
  }
    
}
	  
