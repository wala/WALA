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

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.zip.ZipEntry;

public class NestedJarFileModule extends AbstractNestedJarFileModule {

  private final JarFileModule parent;

  private final ZipEntry entry;

  public NestedJarFileModule(JarFileModule parent, ZipEntry entry) {
    super(parent);
    this.parent = parent;
    this.entry = entry;
    if (parent == null) {
      throw new IllegalArgumentException("null parent");
    }
    if (entry == null) {
      throw new IllegalArgumentException("null entry");
    }
  }

  @Override
  public InputStream getNestedContents() {
    return new ByteArrayInputStream(parent.getContents(entry));
  }

  @Override
  public String toString() {
    return "Nested Jar File:" + entry.getName();
  }

  @Override
  public int hashCode() {
    final int prime = 31;
    int result = 1;
    result = prime * result + ((parent == null) ? 0 : parent.hashCode());
    return result;
  }

  @Override
  public boolean equals(Object obj) {
    if (this == obj) return true;
    if (obj == null) return false;
    if (getClass() != obj.getClass()) return false;
    NestedJarFileModule other = (NestedJarFileModule) obj;
    if (parent == null) {
      if (other.parent != null) return false;
    } else if (!parent.equals(other.parent)) return false;
    return true;
  }
}
