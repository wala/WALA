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
package com.ibm.wala.j2ee.util;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.Iterator;

import org.eclipse.jst.j2ee.commonarchivecore.internal.ApplicationClientFile;
import org.eclipse.jst.j2ee.commonarchivecore.internal.Archive;
import org.eclipse.jst.j2ee.commonarchivecore.internal.EARFile;
import org.eclipse.jst.j2ee.commonarchivecore.internal.EJBJarFile;
import org.eclipse.jst.j2ee.commonarchivecore.internal.File;
import org.eclipse.jst.j2ee.commonarchivecore.internal.WARFile;

import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.j2ee.J2EEUtil;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.io.FileSuffixes;

/**
 *
 * A wrapper around a WCCM Archive.
 * 
 * @author sfink
 */
public class TopLevelArchiveModule implements Module {

  /**
   * Path to classpath root in a war
   */
  private static final String WAR_CP_ROOT = "WEB-INF/classes/";

  /**
   * Path into which WSAD generates JSP implementations
   */
  private static final String WAR_JSP_CP_ROOT = "WEB-INF/classes/WEB-INF/";

  private static final String ORG_APACHE_JSP = "org/apache/jsp/";

  public final static byte WAR_FILE = 0;
  public final static byte OTHER_JAR_FILE = 1;
  public final static byte APPLICATION_CLIENT_FILE = 2;
  public final static byte EAR_FILE = 3;
  public final static byte EJB_JAR_FILE = 4;
  
  private boolean ignoreDependentJars = false;
  

  private static final boolean DEBUG = false;
  /**
   * The JarFileModule that represents this archive.
   * If this is null, it means that this is a nested archive.
   * TODO: We'll need to clean up the class hierarchy so that BloatedArchive
   * does not extend TopLevelArchive ... in which case we can assert that
   * jarFile != null;
   */
  private final JarFileModule jarFile;

  public TopLevelArchiveModule(JarFileModule jarFile) {
    this.jarFile = jarFile;
  }

  /**
   * @param A an ARCHIVE
   * @return one of EAR_FILE, JAR_FILE, WAR_FILE, or APPLICATION_CLIENT_FILE
   */
  public static byte getTypeCode(Archive A) {
    if (A instanceof EARFile) {
      return EAR_FILE;
    } else if (A instanceof EJBJarFile) {
      return EJB_JAR_FILE;
    } else if (A instanceof WARFile) {
      return WAR_FILE;
    } else if (A instanceof ApplicationClientFile) {
      return APPLICATION_CLIENT_FILE;
    } else {
      return OTHER_JAR_FILE;
    }
  }

  /* (non-Javadoc)
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return "ArchiveModule:" + materializeArchive().getName();
  }

  /**
   * @return one of EAR_FILE, JAR_FILE, WAR_FILE, or APPLICATION_CLIENT_FILE
   */
  public byte getType() {
    return getTypeCode(materializeArchive());
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.classLoader.Module#getEntries()
   */
  @SuppressWarnings({ "unchecked" })
  public Iterator<ModuleEntry> getEntries() {
    if (DEBUG) {
      System.err.println(("ArchiveModule.getEntries(): " + this));
    }
    Archive A = materializeArchive();
    Collection files = A.getFiles();
    Collection<ModuleEntry> entries = HashSetFactory.make(files.size());
    for (Iterator<File> it = files.iterator(); it.hasNext(); ) {
      File f = (File)it.next();
      if (f.isArchive()) {
        byte code = getTypeCode((Archive)f);
        if (ignoreDependentJars && code == OTHER_JAR_FILE) {
          continue;
        }
      }
      entries.add(new Entry(f));
    }
    return entries.iterator();
  }

  /**
   * @return a WCCM Archive representing this module
   */
  public Archive materializeArchive() {
    return J2EEUtil.getArchive(jarFile);
  }

  /**
   * @return true iff the jar file suffix is .war
   */
  protected boolean isWarFile() {
    if (jarFile == null) {
      // this is a nested archive which should override this method.
      Assertions.UNREACHABLE();
      return false;
    }
    return jarFile.getJarFile().getName().indexOf(".war") > -1;
  }
  
  /**
   * @param b
   */
  public void setIgnoreDependentJars(boolean b) {
    ignoreDependentJars = b;
  }

  private class Entry implements ModuleEntry {

    private File F;

    Entry(File F) {
      this.F = F;
    }

    /* (non-Javadoc)
     * @see com.ibm.wala.classLoader.ModuleEntry#getName()
     */
    public String getName() {
      return F.getURI();
    }

    /* (non-Javadoc)
     * @see com.ibm.wala.classLoader.ModuleEntry#isClassFile()
     */
    public boolean isClassFile() {
      return FileSuffixes.isClassFile(F.getName());
    }

    /* (non-Javadoc)
     * @see com.ibm.wala.classLoader.ModuleEntry#getInputStream()
     */
    public InputStream getInputStream() {
      try {
        return F.getInputStream();
      } catch (FileNotFoundException e) {
        e.printStackTrace();
        Assertions.UNREACHABLE();
        return null;
      } catch (IOException e) {
        e.printStackTrace();
        Assertions.UNREACHABLE();
        return null;
      }
    }

    /* (non-Javadoc)
     * @see java.lang.Object#toString()
     */
    public String toString() {
      return getName();
    }

    /* (non-Javadoc)
     * @see com.ibm.wala.classLoader.ModuleEntry#isModuleFile()
     */
    public boolean isModuleFile() {
      return F.isArchive();
    }

    /* (non-Javadoc)
     * @see com.ibm.wala.classLoader.ModuleEntry#asModule()
     */
    public Module asModule() {
      if (Assertions.verifyAssertions) {
        assert isModuleFile();
      }
      return new BloatedArchiveModule((Archive) F);
    }

    /* (non-Javadoc)
     * @see com.ibm.wala.classLoader.ModuleEntry#getClassName()
     */
    public String getClassName() {
      String name = getName();
      if (isWarFile()) {
        if (name.indexOf("/") == 0) {
          name = name.substring(1);
        }
        if (name.indexOf(WAR_JSP_CP_ROOT) == 0) {
          // this should be a JSP implementation.
          name = name.substring(WAR_JSP_CP_ROOT.length());
          name = ORG_APACHE_JSP + name;
        } else if (name.indexOf(WAR_CP_ROOT) == 0) {
          name = name.substring(WAR_CP_ROOT.length());
          // if it looks like the default package, assume it's a jsp.
          if (name.indexOf("/") == -1) {
            name = ORG_APACHE_JSP + name;
          }
        }
      }
      return FileSuffixes.stripSuffix(name);
    }
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
      return F.hashCode() * 593;
    }

    /* (non-Javadoc)
     * @see com.ibm.wala.classLoader.ModuleEntry#isSourceFile()
     */
    public boolean isSourceFile() {
      return false;
    }

  }

  // TODO: clean up this class hierarchy; it sucks.
  // an instance of BloatedArchiveModule should not be held onto for long.
  public static class BloatedArchiveModule extends TopLevelArchiveModule {

    private final Archive A;

    public BloatedArchiveModule(Archive A) {
      super(null);
      this.A = A;
    }

    /* (non-Javadoc)
     * @see com.ibm.wala.j2ee.util.TopLevelArchiveModule#materializeArchive()
     */
    public Archive materializeArchive() {
      return A;
    }

    /* (non-Javadoc)
     * @see com.ibm.wala.j2ee.util.TopLevelArchiveModule#isWarFile()
     */
    protected boolean isWarFile() {
      return getTypeCode(A) == WAR_FILE;
    }
    /* (non-Javadoc)
     * @see com.ibm.wala.j2ee.util.TopLevelArchiveModule#isWarFile()
     */
    protected boolean isApplicationClientFile() {
      return getTypeCode(A) == APPLICATION_CLIENT_FILE;
    }
    
    
    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    public int hashCode() {
      return A.hashCode() * 2027;
    }

  }


}
