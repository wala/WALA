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
package com.ibm.wala.classLoader;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class ResourceJarFileModule extends AbstractNestedJarFileModule {

  private final URL resourceURL;

  public ResourceJarFileModule(URL resourceURL) {
    super(new SourceURLModule(resourceURL));
    this.resourceURL = resourceURL;
  }

  @Override
  protected InputStream getNestedContents() throws IOException {
    return resourceURL.openConnection().getInputStream();
  }
}
