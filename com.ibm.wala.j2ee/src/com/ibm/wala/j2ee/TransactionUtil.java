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

package com.ibm.wala.j2ee;

import java.util.Collections;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;

import org.eclipse.jem.java.Method;
import org.eclipse.jst.j2ee.commonarchivecore.internal.Archive;
import org.eclipse.jst.j2ee.commonarchivecore.internal.EARFile;
import org.eclipse.jst.j2ee.commonarchivecore.internal.EJBJarFile;
import org.eclipse.jst.j2ee.ejb.AssemblyDescriptor;
import org.eclipse.jst.j2ee.ejb.EJBJar;
import org.eclipse.jst.j2ee.ejb.EnterpriseBean;
import org.eclipse.jst.j2ee.ejb.MethodElement;
import org.eclipse.jst.j2ee.ejb.MethodElementKind;
import org.eclipse.jst.j2ee.ejb.MethodTransaction;

import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.util.collections.HashSetFactory;

/**
 * Utilities for dealing with transaction declarations.
 * 
 * @author sfink
 */
public class TransactionUtil {

  private static final boolean DEBUG = false;

  private final static String EJBHOME = "javax.ejb.EJBHome";
  private final static String EJBLOCALHOME = "javax.ejb.EJBLocalHome";
  private final static String EJBOBJECT = "javax.ejb.EJBObject";
  private final static String EJBLOCALOBJECT = "javax.ejb.EJBLocalObject";

  /**
   * Create a set of objects to represent transaction entrypoints
   * defined in this module.
   */
  @SuppressWarnings({ "unchecked" })
  public static Set<DeploymentDeclaredTransaction> createDeclaredTransactionEntries(Archive A, ClassLoaderReference loader) {

    if (DEBUG) {
      System.err.println(("createDeclaredTransactionEntries: " + A + " type " + A.getClass()));
    }

    if (A.isEJBJarFile()) {
      // extract the deployment descriptor
      EJBJar DD = ((EJBJarFile) A).getDeploymentDescriptor();
      return createDeclaredTransactionEntries(DD, loader);
    } else if (A.isEARFile()) {
      EARFile ear = (EARFile) A;
      Set<DeploymentDeclaredTransaction> result = HashSetFactory.make();
      for (Iterator it = ear.getEJBJarFiles().iterator(); it.hasNext();) {
        EJBJarFile j = (EJBJarFile) it.next();
        result.addAll(createDeclaredTransactionEntries(j, loader));
      }
      return result;
    } else {
      return Collections.emptySet();
    }
  }

  /**
   * Create a set of objects to represent transaction entrypoints
   * defined in this module.
   */
  @SuppressWarnings("unchecked")
  private static Set<DeploymentDeclaredTransaction> createDeclaredTransactionEntries(EJBJar DD, ClassLoaderReference loader) {

    if (DEBUG) {
      System.err.println(("createDeclaredTransactionEntries: " + DD));
    }

    TreeSet<DeploymentDeclaredTransaction> result = new TreeSet<DeploymentDeclaredTransaction>();
    AssemblyDescriptor AD = DD.getAssemblyDescriptor();
    if (AD == null) {
      System.err.println("Warning: no assembly descriptor found for EJBJar: " + DD);
      return Collections.emptySet();
    }
    for (Iterator it = AD.getMethodTransactions().iterator(); it.hasNext();) {
      MethodTransaction T = (MethodTransaction) it.next();
      if (DEBUG) {
        System.err.println(("got MethodTransaction " + T));
      }
      int TType = T.getTransactionAttribute().getValue();
      for (Iterator elements = T.getMethodElements().iterator(); elements.hasNext();) {
        MethodElement M = (MethodElement) elements.next();
        EnterpriseBean b = M.getEnterpriseBean();
        int elementKind = M.getType().getValue();
        Method[] methods = M.getMethods();
        for (int i = 0; i < methods.length; i++) {
          int kind = (elementKind == MethodElementKind.UNSPECIFIED) ? deduceKind(b, methods[i]) : elementKind;
          if (kind != MethodElementKind.UNSPECIFIED) {
            result.add(new DeploymentDeclaredTransaction(b, methods[i], M,  loader, kind, TType));
          }
        }
      }
    }
    return result;
  }

  /**
   * Figure out the EJB interface to which a method belongs 
   */
  private static int deduceKind(EnterpriseBean b, Method method) {
    String home = b.getHomeInterfaceName();
    String localHome = b.getLocalHomeInterfaceName();
    String local = b.getLocalInterfaceName();
    String remote = b.getRemoteInterfaceName();
    String name = method.getJavaClass().getJavaName();

    if (DEBUG) {
      System.err.println(("deduceKind: " + b + " " + name));
    }

    if (name.equals(home) || name.equals(EJBHOME)) {
      return MethodElementKind.HOME;
    } else if (name.equals(localHome) || name.equals(EJBLOCALHOME)) {
      return MethodElementKind.LOCAL_HOME;
    } else if (name.equals(local) || name.equals(EJBLOCALOBJECT)) {
      return MethodElementKind.LOCAL;
    } else if (name.equals(remote) || name.equals(EJBOBJECT)) {
      return MethodElementKind.REMOTE;
    } else if (b.isMessageDriven() && method.getName().equals("onMessage")) {
      // treat message-driven transactions like remote ones
      return MethodElementKind.REMOTE;
    } else {
      // some other type we don't handle; give up.
      return MethodElementKind.UNSPECIFIED;
    }
  }
}
