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
package com.ibm.wala.cast.js.analysis.typeInference;

import com.ibm.wala.analysis.typeInference.PrimitiveType;
import com.ibm.wala.cast.js.types.JavaScriptTypes;
import com.ibm.wala.types.TypeReference;

public class JSPrimitiveType extends PrimitiveType {

  public static void init() {
    new JSPrimitiveType(JavaScriptTypes.Undefined, -1);

    new JSPrimitiveType(JavaScriptTypes.Null, -1);
    
    new JSPrimitiveType(JavaScriptTypes.Boolean, -1);
    
    new JSPrimitiveType(JavaScriptTypes.String, -1);
    
    new JSPrimitiveType(JavaScriptTypes.Number, -1);
    
    new JSPrimitiveType(JavaScriptTypes.Date, -1);
    
    new JSPrimitiveType(JavaScriptTypes.RegExp, -1);
  }

  public JSPrimitiveType(TypeReference reference, int size) {
    super(reference, size);
  }

}
