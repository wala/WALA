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

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.jem.java.JavaParameter;
import org.eclipse.jem.java.Method;
import org.eclipse.jst.j2ee.commonarchivecore.internal.Archive;
import org.eclipse.jst.j2ee.commonarchivecore.internal.CommonarchiveFactory;
import org.eclipse.jst.j2ee.commonarchivecore.internal.exception.OpenFailureException;
import org.eclipse.jst.j2ee.commonarchivecore.internal.impl.CommonarchiveFactoryImpl;
import org.eclipse.jst.j2ee.ejb.EnterpriseBean;

import com.ibm.wala.classLoader.ClassFileModule;
import com.ibm.wala.classLoader.JarFileModule;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.CallGraph;
import com.ibm.wala.j2ee.util.TopLevelArchiveModule;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MemberReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.strings.StringStuff;

/**
 * Miscellaneous utilities for J2EE processing
 */
public class J2EEUtil {

  public static final TypeReference EJB_HOME = TypeReference.findOrCreateClass(ClassLoaderReference.Extension, "javax/ejb",
      "EJBHome");

  public static final TypeReference EJB_LOCAL_HOME = TypeReference.findOrCreateClass(ClassLoaderReference.Extension, "javax/ejb",
      "EJBLocalHome");

  public static final TypeReference EJB_OBJECT = TypeReference.findOrCreateClass(ClassLoaderReference.Extension, "javax/ejb",
      "EJBObject");

  public static final TypeReference EJB_LOCAL_OBJECT = TypeReference.findOrCreateClass(ClassLoaderReference.Extension, "javax/ejb",
      "EJBLocalObject");

  /**
   * Get the WCCM archive representing a particular module
   * 
   * @param M the module to analyze
   * @return Archive, or null if no WCCM conversion is possible
   */
  public static Archive getArchive(Module M) {
    CommonarchiveFactory factory = CommonarchiveFactoryImpl.getActiveFactory();
    try {
      if (M instanceof JarFileModule) {
        String fileName = ((JarFileModule) M).getAbsolutePath();
        return factory.openArchive(fileName);
      } else if (M instanceof TopLevelArchiveModule) {
        TopLevelArchiveModule AM = (TopLevelArchiveModule) M;
        return AM.materializeArchive();
      } else if (M instanceof ClassFileModule) {
        return null;
      } else {
        return null;
      }

    } catch (OpenFailureException e) {
      e.printStackTrace();
      Assertions.UNREACHABLE();
      return null;
    }
  }

  /**
   * Create a ClassReference to represent an EnterpriseBean
   * 
   * @param b the bean in question
   * @param loader reference to the class loader that loads the bean
   */
  public static TypeReference ejb2TypeReference(EnterpriseBean b, ClassLoaderReference loader) {
    String klass = b.getEjbClass().getQualifiedNameForReflection();
    klass = StringStuff.deployment2CanonicalTypeString(klass);
    TypeReference c = TypeReference.findOrCreate(loader, TypeName.string2TypeName(klass));
    return c;
  }

  /**
   * Create a method reference from a finder
   * 
   * @param method etools method representation
   * @return MethodReference that represents the method.
   */
  public static MethodReference createMethodReference(Method method, ClassLoaderReference loader) {

    String tString = method.getJavaClass().getQualifiedName();
    tString = StringStuff.deployment2CanonicalTypeString(tString);
    TypeReference T = TypeReference.findOrCreate(loader, TypeName.string2TypeName(tString));
    String sig = J2EEUtil.buildDescriptor(method);
    Descriptor D = Descriptor.findOrCreateUTF8(sig);
    Atom name = Atom.findOrCreateUnicodeAtom(method.getName());
    MethodReference ref = MethodReference.findOrCreate(T, name, D);
    return ref;
  }

  /**
   * @param loader a governing class loader reference
   * @param iName name of an interface
   * @return corresponding TypeReference
   */
  public static TypeReference getTypeForInterface(ClassLoaderReference loader, String iName) {
    if (Assertions.verifyAssertions) {
      assert iName != null;
    }
    iName = "L" + iName.replace('.', '/');
    TypeReference iFace = TypeReference.findOrCreate(loader, TypeName.string2TypeName(iName));
    return iFace;
  }

  /**
   * Build up a string representing the method's signature. Returns a string describing this Method. The string is formatted as the
   * method name, followed by a parenthesized, comma-separated list of the method's formal parameter types, followed by the return
   * type.
   * 
   * This implementation clone-and-owned from com.ibm.etools.java.impl.MethodImpl.getSignature()
   * 
   * TODO: Move me elsewhere.
   * 
   * For example:
   * 
   * (Ljava/lang/Object;)Z
   * 
   * @param method the Method in question
   * @return String a String representation of the signature.
   * @throws IllegalArgumentException if method is null
   */
  @SuppressWarnings("unchecked")
  public static String buildDescriptor(Method method) {

    if (method == null) {
      throw new IllegalArgumentException("method is null");
    }
    StringBuffer sb = new StringBuffer();

    sb.append("(");
    List params = method.getParameters();
    int parmSize = params.size();
    for (int j = 0; j < parmSize; j++) {
      JavaParameter param = (JavaParameter) params.get(j);
      if (param.isReturn())
        continue; // listParameters() includes return type in array
      String s = param.getJavaType().getQualifiedName();
      sb.append(StringStuff.deployment2CanonicalDescriptorTypeString(s));
    }
    sb.append(")");

    if (method.getReturnType() == null) {
      // SJF: I don't understand why this should ever happen.
      // TODO: look into this
      // for now, just assume void
      sb.append("V");
    } else {
      String r = method.getReturnType().getQualifiedName();
      sb.append(StringStuff.deployment2CanonicalDescriptorTypeString(r));
    }

    return sb.toString();
  }

  /**
   * We define the "logical entrypoints" for J2EE to be the normal call graph entrypoints; except for servlet entrypoints ... which
   * are treated a little specially.
   * 
   * @param cg governing call graph
   * @return Set of nodes which should be considered "entrypoints" for this analysis.
   */
  public static Set<CGNode> getLogicalEntrypointNodes(CallGraph cg) {
    HashSet<CGNode> result = HashSetFactory.make();
    for (Iterator<CGNode> it = cg.getEntrypointNodes().iterator(); it.hasNext();) {
      CGNode n = (CGNode) it.next();
      MemberReference m = n.getMethod().getReference();
      TypeReference t = m.getDeclaringClass();
      if (isServletFrameworkType(t)) {
        Set<CGNode> set = HashSetFactory.make();
        result.addAll(servletFrontier(cg, n, set));
      } else {
        result.add(n);
      }
    }
    return result;
  }

  private static boolean isServletFrameworkType(TypeReference t) {
    return t.equals(ServletEntrypoints.Servlet) || t.equals(ServletEntrypoints.HttpServlet)
        || t.equals(ServletEntrypoints.HttpJspBase);
  }

  /**
   * @param n a call graph node declared on javax.servlet.Servlet or javax.servlet.HttpServlet
   * @return the set of nodes S s.t. for m in S, m is reachable from n, m is not a method on Servlet or HttpServlet, and there is a
   *         predecessor p of m s.t. p is a method on Servlet or HttpServlet.
   */
  public static Collection<CGNode> servletFrontier(CallGraph cg, CGNode n, Set<CGNode> seen) {
    HashSet<CGNode> result = HashSetFactory.make();
    seen.add(n);
    for (Iterator<? extends CGNode> it = cg.getSuccNodes(n); it.hasNext();) {
      CGNode m = (CGNode) it.next();
      TypeReference t = m.getMethod().getDeclaringClass().getReference();
      if (isServletFrameworkType(t)) {
        if (!seen.contains(m))
          result.addAll(servletFrontier(cg, m, seen));
      } else {
        result.add(m);
      }
    }
    return result;
  }
}
