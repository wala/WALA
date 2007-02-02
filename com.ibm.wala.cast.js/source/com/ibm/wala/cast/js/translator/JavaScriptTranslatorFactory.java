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

import com.ibm.wala.cast.ir.translator.TranslatorToIR;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;

public interface JavaScriptTranslatorFactory {

  TranslatorToIR make(JavaScriptLoader loader);

  public static class CAstRhinoFactory implements JavaScriptTranslatorFactory {

    public TranslatorToIR make(JavaScriptLoader loader) {
       return new CAstRhinoTranslator(loader);
    }
  }

}
