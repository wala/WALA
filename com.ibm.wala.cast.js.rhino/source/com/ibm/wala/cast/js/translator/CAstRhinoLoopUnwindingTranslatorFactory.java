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
package com.ibm.wala.cast.js.translator;

import com.ibm.wala.cast.ir.translator.TranslatorToCAst;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.classLoader.SourceModule;

public class CAstRhinoLoopUnwindingTranslatorFactory 
  extends JavaScriptLoopUnwindingTranslatorFactory 
{
  public CAstRhinoLoopUnwindingTranslatorFactory(int unwindFactor) {
    super(unwindFactor);
  }

  public CAstRhinoLoopUnwindingTranslatorFactory() {
    this(3);
  }

  @Override
  protected TranslatorToCAst translateInternal(CAst Ast, SourceModule M, String N) {
    return new CAstRhinoTranslator(M, true);
  }
}

