package com.ibm.wala.j2ee;

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
import java.util.Iterator;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.strings.Atom;

/**
 * This class provides an iterator of entrypoints that are implementations of Spring methods.
 * 
 * @author OmriW
 */
public class SpringEntrypoints implements Iterable<Entrypoint> {

  static final boolean DEBUG = false;

  public final static Atom handleRequestName = Atom.findOrCreateUnicodeAtom("handleRequest");

  private final static Descriptor handleRequestDesc = Descriptor
      .findOrCreateUTF8("(Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)Lorg/springframework/web/servlet/ModelAndView;");

  private final static Atom[] springMethodNames = { handleRequestName };

  private final static Descriptor[] springMethodDescs = { handleRequestDesc };

  private final static TypeName SpringControllerName = TypeName.string2TypeName("Lorg/springframework/web/servlet/mvc/Controller");

  public final static TypeReference SpringController = TypeReference.findOrCreate(ClassLoaderReference.Extension,
      SpringControllerName);

  private Set<Entrypoint> entrypoints = HashSetFactory.make();

  /**
   * Set of servlet (or ServletFilter) implementations found.
   */
  private Set<IClass> springControllers = HashSetFactory.make();

  private final IClassHierarchy cha;

  private boolean isInitialized;

  /**
   * @param cha loaded class hierarchy
   */
  public SpringEntrypoints(IClassHierarchy cha) {
    this.cha = cha;
  }

  private void init() {
    if (isInitialized) {
      return;
    }
    isInitialized = true;

    // TypeReference actionServletType = TypeReference.findOrCreate(scope.getApplicationLoader(), actionServlet);
    // IClass actionServletClass = cha.lookupClass(actionServletType);

    IClass springController = cha.lookupClass(SpringController);
    if (springController == null) {
      return;
    }

    for (Iterator<IClass> it = getCandidateEntryClasses(cha); it.hasNext();) {
      IClass klass = (IClass) it.next();
      if (klass != null) {
        if (DEBUG) {
          System.err.println((getClass() + " consider " + klass));
        }
        if (cha.lookupClass(klass.getReference()) == null) {
          continue;
        }
        if (cha.implementsInterface(klass, springController)) {
          springControllers.add(klass);
          final TypeReference type = klass.getReference();

          for (int i = 0; i < springMethodNames.length; i++) {
            Atom name = springMethodNames[i];
            Descriptor desc = springMethodDescs[i];
            MethodReference M = MethodReference.findOrCreate(type, name, desc);
            IMethod m = cha.resolveMethod(M);
            if (cha.resolveMethod(M) != null) {
              entrypoints.add(new DefaultEntrypoint(m, cha) {

                /**
                 * Assume all ServletRequest and ServletResponse are HTTP flavor.
                 */
                public TypeReference[] getParameterTypes(int i) {
                  if (i == 0) {
                    // "this" pointer
                    return new TypeReference[] { type };
                  } else {
                    TypeReference[] tArray = super.getParameterTypes(i);
                    if (Assertions.verifyAssertions) {
                      assert tArray.length == 1;
                    }
                    TypeReference T = tArray[0];
                    TypeName n = T.getName();
                    TypeReference Tp = ServletEntrypoints.getConcreteServletParameterType(n);
                    if (Tp != null) {
                      T = Tp;
                    }
                    return new TypeReference[] { T };
                  }
                }
              });
            }
          }
        }
      }
    }
  }

  /**
   * return the set of classes that should be examined when searching for servlet entrypoints.
   */
  protected Iterator<IClass> getCandidateEntryClasses(IClassHierarchy cha) {
    IClassLoader appLoader = cha.getLoader(ClassLoaderReference.Application);
    return appLoader.iterateAllClasses();
  }

  public Iterator<Entrypoint> iterator() {
    init();
    return entrypoints.iterator();
  }

  public String toString() {
    init();
    StringBuffer result = new StringBuffer();
    result.append("SpringControllers:");
    Iterator<IClass> it = springControllers.iterator();
    if (it.hasNext()) {
      while (it.hasNext()) {
        result.append("\n   ");
        result.append(it.next());
      }
    } else {
      result.append("   none");
    }
    return result.toString();
  }
}
