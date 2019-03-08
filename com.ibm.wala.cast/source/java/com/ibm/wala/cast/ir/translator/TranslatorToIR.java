/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.ir.translator;

import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.classLoader.ModuleEntry;

/**
 * Type that performs the translation from the CAst to WALA IR (as extended for the language). The
 * generated IR and related information is stored within the translator.
 */
public interface TranslatorToIR {

  /**
   * translate the CAst rooted at S, corresponding to ModuleEntry N, to IR, and store the result
   * internally.
   */
  void translate(CAstEntity S, ModuleEntry N);
}
