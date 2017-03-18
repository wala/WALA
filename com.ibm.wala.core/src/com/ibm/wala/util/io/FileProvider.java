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
package com.ibm.wala.util.io;// 5724-D15

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.JarURLConnection;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLDecoder;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.jar.JarInputStream;
import java.util.zip.ZipException;

import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.JarStreamModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.NestedJarFileModule;
import com.ibm.wala.classLoader.ResourceJarFileModule;
import com.ibm.wala.util.debug.Assertions;

/**
 * This class provides files that are packaged with this plug-in
 */
public class FileProvider {


  private final static int DEBUG_LEVEL = Integer.parseInt(System.getProperty("wala.debug.file", "0"));
  
  /**
   * @param fileName
   * @return the jar file packaged with this plug-in of the given name, or null
   *         if not found.
   */
  public Module getJarFileModule(String fileName) throws IOException {
    return getJarFileModule(fileName, FileProvider.class.getClassLoader());
  }

  public Module getJarFileModule(String fileName, ClassLoader loader) throws IOException {
    return getJarFileFromClassLoader(fileName, loader);
  }

  public URL getResource(String fileName) throws IOException {
    if (fileName == null) {
      throw new IllegalArgumentException("null fileName");
    }
    return getResource(fileName, FileProvider.class.getClassLoader());
  }

  public URL getResource(String fileName, ClassLoader loader) throws IOException {
    if (fileName == null) {
      throw new IllegalArgumentException("null fileName");
    }
    if (loader == null) {
      throw new IllegalArgumentException("null loader");
    }
    return loader.getResource(fileName);
  }

  public File getFile(String fileName) throws IOException {
    if (fileName == null) {
      throw new IllegalArgumentException("null fileName");
    }
    return getFile(fileName, FileProvider.class.getClassLoader());
  }

  public File getFile(String fileName, ClassLoader loader) throws IOException {
    return getFileFromClassLoader(fileName, loader);
  }

  /**
   * @throws FileNotFoundException
   */
  public File getFileFromClassLoader(String fileName, ClassLoader loader) throws FileNotFoundException {
    if (loader == null) {
      throw new IllegalArgumentException("null loader");
    }
    if (fileName == null) {
      throw new IllegalArgumentException("null fileName");
    }
    URL url = null;
    try {
      url = loader.getResource(fileName);
    } catch (Exception e) {
    }    
    if (DEBUG_LEVEL > 0) {
      System.err.println(("FileProvider got url: " + url + " for " + fileName));
    }
    if (url == null) {
      // couldn't load it from the class loader. try again from the
      // system classloader
      File f = new File(fileName);
      if (f.exists()) {
        return f;
      }
      throw new FileNotFoundException(fileName);
    } else {
      return new File(filePathFromURL(url));
    }
  }

  /**
   * First tries to read fileName from the ClassLoader loader.  If unsuccessful, attempts to read file from
   * the file system.  If that fails, throws a {@link FileNotFoundException}
   */
  public InputStream getInputStreamFromClassLoader(String fileName, ClassLoader loader) throws FileNotFoundException {
    if (loader == null) {
      throw new IllegalArgumentException("null loader");
    }
    if (fileName == null) {
      throw new IllegalArgumentException("null fileName");
    }
    InputStream is = loader.getResourceAsStream(fileName);
    if (is == null) {
      // couldn't load it from the class loader. try again from the
      // system classloader
      File f = new File(fileName);
      if (f.exists()) {
        return new FileInputStream(f);
      }
      throw new FileNotFoundException(fileName);
    }
    return is;
  }

  /**
   * @return the jar file packaged with this plug-in of the given name, or null
   *         if not found: wrapped as a JarFileModule or a NestedJarFileModule
   * @throws IOException
   */
  public Module getJarFileFromClassLoader(String fileName, ClassLoader loader) throws IOException {
    if (fileName == null) {
      throw new IllegalArgumentException("null fileName");
    }
    if (loader == null) {
      throw new IllegalArgumentException("null loader");
    }
    URL url = loader.getResource(fileName);
    if (DEBUG_LEVEL > 0) {
      System.err.println("FileProvider got url: " + url + " for " + fileName);
    }
    if (url == null) {
      // couldn't load it from the class loader. try again from the
      // system classloader
      try {
        return new JarFileModule(new JarFile(fileName, false));
      } catch (ZipException e) {
        throw new IOException("Could not find file: " + fileName, e);
      }
    }
    if (url.getProtocol().equals("jar")) {
      JarURLConnection jc = (JarURLConnection) url.openConnection();
      JarFile f = jc.getJarFile();
      JarEntry entry = jc.getJarEntry();
      JarFileModule parent = new JarFileModule(f);
      return new NestedJarFileModule(parent, entry);
    } else if (url.getProtocol().equals("rsrc")) {
      return new ResourceJarFileModule(url);
    } else if (url.getProtocol().equals("file")) {
      String filePath = filePathFromURL(url);
      return new JarFileModule(new JarFile(filePath, false));
    } else {
      final URLConnection in = url.openConnection();
      final JarInputStream jarIn = new JarInputStream(in.getInputStream(), false);
      return new JarStreamModule(jarIn);
    }
  }

  /**
   * Properly creates the String file name of a {@link URL}. This works around a
   * bug in the Sun implementation of {@link URL#getFile()}, which doesn't
   * properly handle file paths with spaces (see <a href=
   * "http://sourceforge.net/tracker/index.php?func=detail&aid=1565842&group_id=176742&atid=878458"
   * >bug report</a>). For now, fails with an assertion if the url is malformed.
   * 
   * @param url
   * @return the path name for the url
   * @throws IllegalArgumentException
   *           if url is null
   */
  public String filePathFromURL(URL url) {
    if (url == null) {
      throw new IllegalArgumentException("url is null");
    }
    // Old solution does not deal well with "<" | ">" | "#" | "%" |
    // <">  "{" | "}" | "|" | "\" | "^" | "[" | "]" | "`" since they may occur
    // inside an URL but are prohibited for an URI. See
    // http://www.faqs.org/rfcs/rfc2396.html Section 2.4.3
    // This solution works. See discussion at
    // http://stackoverflow.com/questions/4494063/how-to-avoid-java-net-urisyntaxexception-in-url-touri
    // we assume url has been properly encoded, so we decode it 
    try {
      URI uri = new File(URLDecoder.decode(url.getPath(), "UTF-8")).toURI();
      return uri.getPath();
    } catch (UnsupportedEncodingException e) {
      // this really shouldn't happen
      Assertions.UNREACHABLE();
      return null;
    }
  }

}
