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
package com.ibm.wala.examples.properties;

import java.io.File;
import java.net.URL;
import java.util.Properties;

import com.ibm.wala.properties.WalaProperties;
import com.ibm.wala.util.io.FileProvider;

public final class WalaExamplesProperties {

  public static final String PDFVIEW_EXE = "pdfview_exe"; //$NON-NLS-1$

  public static final String DOT_EXE = "dot_exe"; //$NON-NLS-1$

  public final static String PROPERTY_FILENAME = "wala.examples.properties"; //$NON-NLS-1$

  public static Properties loadProperties()  {

    try {
      Properties result = WalaProperties.loadPropertiesFromFile(WalaExamplesProperties.class.getClassLoader(), PROPERTY_FILENAME);

      return result;
    } catch (Exception e) {
      e.printStackTrace();
      throw new IllegalStateException("Unable to set up wala examples properties ", e);
    }
  }

  public static String getWalaCoreTestsHomeDirectory()  {
    final URL url = WalaExamplesProperties.class.getClassLoader().getResource(PROPERTY_FILENAME);
    if (url == null) {
      throw new IllegalStateException("failed to find URL for wala.examples.properties");
    }

    return new File((new FileProvider()).filePathFromURL(url)).getParentFile().getParentFile().getAbsolutePath();
  }

}
