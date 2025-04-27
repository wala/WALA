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
package com.ibm.wala.util.config;

import org.assertj.core.api.AbstractObjectAssert;
import org.assertj.core.util.CheckReturnValue;

public class SetOfClassesAssert extends AbstractObjectAssert<SetOfClassesAssert, SetOfClasses> {

  public SetOfClassesAssert(final SetOfClasses actual) {
    super(actual, SetOfClassesAssert.class);
  }

  @CheckReturnValue
  public static SetOfClassesAssert assertThat(SetOfClasses actual) {
    return new SetOfClassesAssert(actual);
  }

  public SetOfClassesAssert contains(final String element) {
    isNotNull();
    if (!actual.contains(element)) {
      failWithMessage(
          "\nExpecting that actual `SetOfClasses` contains element <%s> but does not.", element);
    }
    return this;
  }
}
