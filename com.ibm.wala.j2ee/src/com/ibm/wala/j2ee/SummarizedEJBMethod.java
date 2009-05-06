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
package com.ibm.wala.j2ee;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.ipa.summaries.MethodSummary;
import com.ibm.wala.ipa.summaries.SummarizedMethod;
import com.ibm.wala.types.MethodReference;

/**
 */
public class SummarizedEJBMethod extends SummarizedMethod {

  private final BeanMetaData bean;

  public SummarizedEJBMethod(BeanMetaData bean, MethodReference ref, MethodSummary summary, IClass declaringClass) {
    super(ref, summary, declaringClass);
    this.bean = bean;
  }

  public BeanMetaData getBean() {
    return bean;
  }

}
