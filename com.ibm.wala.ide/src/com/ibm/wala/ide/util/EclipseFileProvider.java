/*******************************************************************************
 * Copyright (c) 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.ide.util;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.jar.JarFile;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.Plugin;
import org.osgi.framework.Bundle;

import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.ide.plugin.CorePlugin;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.io.FileProvider;

public class EclipseFileProvider extends FileProvider {

  /**
   * the plug-in to use.  If <code>null</code>, {@link CorePlugin#getDefault()} is used.
   */
  private final Plugin plugIn;
  
  public EclipseFileProvider() {
    this(null);
  }
  
  public EclipseFileProvider(Plugin plugIn) {
    this.plugIn = plugIn;
  }
  /**
   * This class uses reflection to access classes and methods that are only
   * available when Eclipse is running as an IDE environment. The choice to use
   * reflection is related to builds: with this design the build doesn't need to
   * provide IDE bundles during compilation and hence can spot invalid uses of
   * such classes through this bundle.
   * 
   * Because of this class, this bundle must OPTIONALY require
   * 'org.eclipse.core.resources'.
   */
  private static final class EclipseUtil {
    private static Object workspaceRoot = null;
    private static Method workspaceRoot_getFile = null;

    public static Module getJarFileModule(String fileName) {
      // Using reflection to enable this code to be built without the
      // org.eclipse.core.resources bundle
      //
      try {
        if (workspaceRoot_getFile == null) {
          Class<?> cls = Class.forName("org.eclipse.core.resources.ResourcesPlugin");
          Method getWorkspaceMethod = cls.getDeclaredMethod("getWorkspace");
          Object workspace = getWorkspaceMethod.invoke(null);
          Method getRoot = workspace.getClass().getDeclaredMethod("getRoot");
          workspaceRoot = getRoot.invoke(workspace);
          workspaceRoot_getFile = workspaceRoot.getClass().getMethod("getFile", IPath.class);
        }

        IPath path = new Path(fileName);
        if (workspaceRoot_getFile.invoke(workspaceRoot, path) != null) {
          try (final JarFile jar = new JarFile(fileName, false)) {
            return new JarFileModule(jar);
          }
        }
      } catch (Exception e) {
      }
      return null;
    }
  }
  
  @Override
  public Module getJarFileModule(String fileName, ClassLoader loader) throws IOException {
    if (CorePlugin.getDefault() == null) {
      return getJarFileFromClassLoader(fileName, loader);
    } else if (plugIn != null) {
      return getFromPlugin(plugIn, fileName);
    } else if (CorePlugin.IS_RESOURCES_BUNDLE_AVAILABLE) {
      Module module = EclipseUtil.getJarFileModule(fileName);
      if (module != null) {
        return module;
      }
    }
    return getFromPlugin(CorePlugin.getDefault(), fileName);
  }
  
  /**
   * @param fileName
   * @return the jar file packaged with this plug-in of the given name, or null
   *         if not found.
   */
  private JarFileModule getFromPlugin(Plugin p, String fileName) throws IOException {
    URL url = getFileURLFromPlugin(p, fileName);
    if (url == null) return null;
    try (final JarFile jar = new JarFile(filePathFromURL(url))) {
      return new JarFileModule(jar);
    }
  }
  
  /**
   * get a file URL for a file from a plugin
   * 
   * @param fileName
   *          the file name
   * @return the URL, or <code>null</code> if the file is not found
   * @throws IOException
   */
  private static URL getFileURLFromPlugin(Plugin p, String fileName) throws IOException {
    try {
      URL url = FileLocator.find(p.getBundle(), new Path(fileName), null);
      if (url == null) {
        // try lib/fileName
        String libFileName = "lib/" + fileName;
        url = FileLocator.find(p.getBundle(), new Path(libFileName), null);
        if (url == null) {
          // try bin/fileName
          String binFileName = "bin/" + fileName;
          url = FileLocator.find(p.getBundle(), new Path(binFileName), null);
          if (url == null) {
            // try it as an absolute path?
            File f = new File(fileName);
            if (!f.exists()) {
              // give up
              return null;
            } else {
              url = f.toURI().toURL();
            }
          }
        }
      }
      url = FileLocator.toFileURL(url);
      url = fixupFileURLSpaces(url);
      return url;
    } catch (ExceptionInInitializerError e) {
      throw new IOException("failure to get file URL for " + fileName, e);
    }
  }

  /**
   * escape spaces in a URL, primarily to work around a bug in
   * {@link File#toURL()}
   * 
   * @param url
   * @return an escaped version of the URL
   */
  private static URL fixupFileURLSpaces(URL url) {
    String urlString = url.toExternalForm();
    StringBuffer fixedUpUrl = new StringBuffer();
    int lastIndex = 0;
    while (true) {
      int spaceIndex = urlString.indexOf(' ', lastIndex);

      if (spaceIndex < 0) {
        fixedUpUrl.append(urlString.substring(lastIndex));
        break;
      }

      fixedUpUrl.append(urlString.substring(lastIndex, spaceIndex));
      fixedUpUrl.append("%20");
      lastIndex = spaceIndex + 1;
    }
    try {
      return new URL(fixedUpUrl.toString());
    } catch (MalformedURLException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
    }
    return null;
  }
  
  @Override
  public URL getResource(String fileName, ClassLoader loader) {
    if (fileName == null) {
      throw new IllegalArgumentException("null fileName");
    }
    Plugin p = plugIn == null ? CorePlugin.getDefault() : plugIn;
    if (p == null && loader == null) {
      throw new IllegalArgumentException("null loader");
    }
    return (p == null) ? loader.getResource(fileName) : FileLocator.find(p.getBundle(),
        new Path(fileName), null);
  }
  
  @Override
  public File getFile(String fileName, ClassLoader loader) throws IOException {
    Plugin p = plugIn == null ? CorePlugin.getDefault() : plugIn;
    if (p == null) {
      return getFileFromClassLoader(fileName, loader); 
    } else {
        try {
          return getFileFromPlugin(p, fileName);
        } catch (IOException e) {
          return getFileFromClassLoader(fileName, loader); 
        }
    }
  }
  
  /**
   * @param fileName
   * @return the jar file packaged with this plug-in of the given name, or null
   *         if not found.
   * @throws IllegalArgumentException
   *           if p is null
   */
  public File getFileFromPlugin(Plugin p, String fileName) throws IOException {

    if (p == null) {
      throw new IllegalArgumentException("p is null");
    }
    if (fileName == null) {
      throw new IllegalArgumentException("null fileName");
    }
    URL url = getFileURLFromPlugin(p, fileName);
    if (url == null) {
      throw new FileNotFoundException(fileName);
    }
    return new File(filePathFromURL(url));
  }

  /**
   * This is fragile.  Use with care.
   * @return a String representing the path to the wala.core plugin installation
   */
  public static String getWalaCorePluginHome() {
    if (CorePlugin.getDefault() == null) {
       return null;
    }
    String install = Platform.getInstallLocation().getURL().getPath();
    Bundle b = Platform.getBundle("com.ibm.wala.core");
    String l = b.getLocation();
    if (l.startsWith("update@")) {
      l = l.replace("update@", "");
    }
    if (l.startsWith("reference:file:")) {
      return l.replace("reference:file:","");
    } else {
      return install + File.separator + l;
    }
  }

}
