/*******************************************************************************
 * Copyright (c) 2002,2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.shrike.tools;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarOutputStream;

public class ExtractMatchingClasses {
  private static boolean matchEntry(JarFile[] matches, JarEntry e) {
    for (int i = 0; i < matches.length; i++) {
      if (matches[i].getEntry(e.getName()) != null) {
        return true;
      }
    }
    return false;
  }

  private static void readFully(InputStream s, byte[] b) throws IOException {
    int offset = 0;
    while (offset < b.length) {
      int read = s.read(b, offset, b.length - offset);
      offset += read;
    }
  }

  public static void main(String[] args) throws Exception, IllegalArgumentException {
    if (args.length < 2) {
      throw new IllegalArgumentException("Invalid command line");
    }
    String in = args[0];
    String out = args[1];
    String[] match = new String[args.length - 2];
    System.arraycopy(args, 2, match, 0, match.length);

    JarFile inJar = new JarFile(in);
    JarOutputStream outJar = new JarOutputStream(new FileOutputStream(out));
    JarFile[] matches = new JarFile[match.length];
    for (int i = 0; i < match.length; i++) {
      matches[i] = new JarFile(match[i]);
    }

    for (Enumeration<JarEntry> e = inJar.entries(); e.hasMoreElements();) {
      JarEntry entry =  e.nextElement();

      if (matchEntry(matches, entry)) {
        outJar.putNextEntry(entry);
        byte[] data = new byte[(int) entry.getSize()];
        InputStream stream = inJar.getInputStream(entry);
        readFully(stream, data);
        outJar.write(data);
        outJar.flush();
      }
    }
    outJar.close();
  }
}