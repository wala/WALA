/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.util.io;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

public class TemporaryFile {

  private final static String outputDir;

  static {
    String dir = System.getProperty("java.io.tmpdir");

    while (dir.endsWith(File.separator))
      dir = dir.substring(0, dir.length()-1);
      
    dir = dir + File.separator;
    
    outputDir = dir;
  }

  public static File urlToFile(String fileName, URL input) throws IOException {
    File F = new File(outputDir + File.separator + fileName);
    return urlToFile(F , input);
  }

  public static File urlToFile(File F, URL input) throws IOException {
    return streamToFile(F, input.openStream());
  }
  
  public static File streamToFile(File F, InputStream... inputs) throws IOException {
    FileOutputStream output = new FileOutputStream(F);
    
    int read;
    byte[] buffer = new byte[ 1024 ];
    for(InputStream input : inputs) {
      while ( (read = input.read(buffer)) != -1 ) {
        output.write(buffer, 0, read);
      }
      input.close();
    }
    
    output.close();
    return F;
  }
  
  public static File stringToFile(File F, String... inputs) throws IOException {
    FileOutputStream output = new FileOutputStream(F);
    
    for(String input : inputs) {
      output.write(input.getBytes());
    }
    
    output.close();
    return F;
  }
}
