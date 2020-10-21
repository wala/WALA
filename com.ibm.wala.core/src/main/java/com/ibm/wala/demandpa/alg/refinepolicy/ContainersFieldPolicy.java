/*
 * Copyright (c) 2007 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.demandpa.alg.refinepolicy;

import com.ibm.wala.ipa.cha.IClassHierarchy;
import java.util.regex.Pattern;

public class ContainersFieldPolicy extends ManualFieldPolicy {

  public ContainersFieldPolicy(IClassHierarchy cha) {
    super(cha, Pattern.compile("Ljava/util(?!(/logging)).*"));
  }
}
