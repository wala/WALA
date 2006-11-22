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
package com.ibm.wala.util.internationalization;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;

public final class GenericStringBundle implements IStringBundle {

  public GenericStringBundle(final String resourceBundleName, final ClassLoader classLoader) {
    this.resourceBundle = ResourceBundle.getBundle(resourceBundleName, Locale.getDefault(), classLoader);
  }

  // --- Interface methods implementation

  public String get(final String messageID) {
    return this.resourceBundle.getString(messageID);
  }

  public String get(final String messageID, final Object argument) {
    return get(messageID, new Object[] { argument });
  }

  public String get(final String messageID, final Object[] arguments) {
    return MessageFormat.format(get(messageID), arguments);
  }

  public String get(final Class clazz, final String secondPartOfID) {
    return get(concat(clazz, secondPartOfID));
  }

  public String get(final Class clazz, final String secondPartOfID, final Object argument) {
    return get(concat(clazz, secondPartOfID), new Object[] { argument });
  }

  public String get(final Class clazz, final String secondPartOfID, final Object[] arguments) {
    return get(concat(clazz, secondPartOfID), arguments);
  }

  // --- Private code

  private String concat(final Class clazz, final String secondPartOfID) {
    return shortName(clazz.getName()).concat(".").concat(secondPartOfID); //$NON-NLS-1$
  }

  private String shortName(final String className) {
    final int index = className.lastIndexOf('.');
    return (index != -1) ? className.substring(index + 1) : className;
  }

  private final ResourceBundle resourceBundle;

}
