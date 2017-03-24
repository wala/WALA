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
package com.ibm.wala.cast.js.translator;

import com.ibm.wala.cast.ir.translator.RewritingTranslatorToCAst;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.classLoader.ModuleEntry;

public class CAstRhinoTranslator extends RewritingTranslatorToCAst {    
  public CAstRhinoTranslator(ModuleEntry m, boolean replicateForDoLoops) {
    super(m, new RhinoToAstTranslator(new CAstImpl(), m, m.getName(), replicateForDoLoops));
   }
}
