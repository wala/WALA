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
package com.ibm.wala.ide.classloader;

import com.ibm.wala.classLoader.SourceFileModule;
import java.io.File;
import org.eclipse.core.resources.IFile;

/** A module which is a wrapper around a .java file */
public class EclipseSourceFileModule extends SourceFileModule {

  private final IFile f;

  public static EclipseSourceFileModule createEclipseSourceFileModule(IFile f) {
    if (f == null) {
      throw new IllegalArgumentException("null f");
    }
    return new EclipseSourceFileModule(f);
  }

  private EclipseSourceFileModule(IFile f) {
    super(new File(f.getLocation().toOSString()), f.getLocation().lastSegment(), null);
    this.f = f;
  }

  public IFile getIFile() {
    return f;
  }

  @Override
  public String toString() {
    return "EclipseSourceFileModule:" + getFile().toString();
  }
}
