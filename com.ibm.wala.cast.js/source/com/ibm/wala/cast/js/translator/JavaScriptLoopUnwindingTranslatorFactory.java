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
import com.ibm.wala.cast.tree.rewrite.AstLoopUnwinder;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.classLoader.SourceModule;

public abstract class JavaScriptLoopUnwindingTranslatorFactory 
  implements JavaScriptTranslatorFactory 
{
  private final int unwindFactor;

  protected JavaScriptLoopUnwindingTranslatorFactory(int unwindFactor) {
    this.unwindFactor = unwindFactor;
  }

  JavaScriptLoopUnwindingTranslatorFactory() {
    this(3);
  }

  protected abstract TranslatorToCAst translateInternal(CAst Ast, SourceModule M, String N);

  @Override
  public TranslatorToCAst make(CAst ast, final ModuleEntry M) {
	  String N;
	  if (M instanceof SourceFileModule) {
		  N = ((SourceFileModule) M).getClassName();
	  } else {
	      N = M.getName();
	  }

	  TranslatorToCAst xlator = translateInternal(ast, (SourceModule)M, N);
	  xlator.addRewriter(ast1 -> new AstLoopUnwinder(ast1, true, unwindFactor), false);
	  
	  return xlator;
  }
}

