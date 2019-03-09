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
package com.ibm.wala.util.io;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

/** utilities for IO streams */
public class Streams {

  /**
   * @return byte[] holding the contents of the stream
   * @throws IllegalArgumentException if in == null
   */
  public static byte[] inputStream2ByteArray(InputStream in)
      throws IllegalArgumentException, IOException {
    if (in == null) {
      throw new IllegalArgumentException("in == null");
    }
    ByteArrayOutputStream b = new ByteArrayOutputStream();
    while (in.available() > 0) {
      byte[] data = new byte[in.available()];
      in.read(data);
      b.write(data);
    }
    byte[] data = b.toByteArray();
    return data;
  }
}
