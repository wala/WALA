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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MemberReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.strings.Atom;

/**
 * This class provides an iterator of entrypoints that are implementations of org.apache.struts.action.Action
 * 
 * @author sfink
 */
public class StrutsEntrypoints implements Iterable<Entrypoint>, EJBConstants {

  public final static Atom executeName = Atom.findOrCreateUnicodeAtom("execute");

  /**
   * Should we attempt to speculate that methods are dispatched to based on the method descriptor?
   */
  public final static boolean SPECULATE_DISPATCH_ACTIONS = true;

  private final static String executeDescString = "(Lorg/apache/struts/action/ActionMapping;Lorg/apache/struts/action/ActionForm;Ljavax/servlet/ServletRequest;Ljavax/servlet/ServletResponse;)Lorg/apache/struts/action/ActionForward;";

  private final static String httpExecuteDescString = "(Lorg/apache/struts/action/ActionMapping;Lorg/apache/struts/action/ActionForm;Ljavax/servlet/http/HttpServletRequest;Ljavax/servlet/http/HttpServletResponse;)Lorg/apache/struts/action/ActionForward;";
  
  private final static Descriptor executeDesc = Descriptor.findOrCreateUTF8(executeDescString);

  private final static Descriptor httpExecuteDesc = Descriptor.findOrCreateUTF8(httpExecuteDescString);

  private final static String plugInInitDescString = "(Lorg/apache/struts/action/ActionServlet;Lorg/apache/struts/config/ModuleConfig;)V";
  
  private final static String plugInDestroyDescString = "()V";

  private final static Atom plugInInitName = Atom.findOrCreateUnicodeAtom("init");

  private final static Atom plugInDestroyName = Atom.findOrCreateUnicodeAtom("destroy");
  
private final static Descriptor plugInInitDesc = Descriptor.findOrCreateUTF8(plugInInitDescString);
  
  private final static Descriptor plugInDestroyDesc = Descriptor.findOrCreateUTF8(plugInDestroyDescString);
  
  private final static TypeName actionName = TypeName.string2TypeName("Lorg/apache/struts/action/Action");

  private final static TypeName actionFormName = TypeName.string2TypeName("Lorg/apache/struts/action/ActionForm");
  
  private final static TypeName plugInName = TypeName.string2TypeName("Lorg/apache/struts/action/PlugIn");

  private Map<MethodReference, Entrypoint> entrypoints = HashMapFactory.make();

  /**
   * Set of action implementations found.
   */
  private Set<IClass> actions = HashSetFactory.make();

  /**
   * Set of plugin implementations found.
   */
  private Set<IClass> plugins = HashSetFactory.make();
  
  
  /**
   * This map controls selection of concrete types for parameters to some servlet methods.
   */
  private final static HashMap<TypeName, TypeReference> concreteParameterMap = HashMapFactory.make(2);
  static {
    concreteParameterMap.put(ServletEntrypoints.httpServletRequest, ServletEntrypoints.WalaHttpServletRequest);
    concreteParameterMap.put(ServletEntrypoints.httpServletResponse, ServletEntrypoints.WalaHttpServletResponse);
    concreteParameterMap.put(ServletEntrypoints.servletRequest, ServletEntrypoints.WalaHttpServletRequest);
    concreteParameterMap.put(ServletEntrypoints.servletResponse, ServletEntrypoints.WalaHttpServletResponse);
  }

  /**
   * @param scope scope of analysis
   * @param cha loaded class hierarchy
   */
  public StrutsEntrypoints(AnalysisScope scope, IClassHierarchy cha) {

    TypeReference actionType = TypeReference.findOrCreate(scope.getApplicationLoader(), actionName);
    IClass actionClass = cha.lookupClass(actionType);

    if (actionClass == null) {
      return;
    }

    TypeReference plugInType = TypeReference.findOrCreate(scope.getApplicationLoader(), plugInName);
    IClass plugInClass = cha.lookupClass(plugInType);    
    if (Assertions.verifyAssertions) {
      Assertions._assert(plugInClass != null);
    }
    
    ClassLoaderReference appLoaderRef = scope.getApplicationLoader();
    IClassLoader appLoader = cha.getLoader(appLoaderRef);

    for (Iterator<IClass> it = appLoader.iterateAllClasses(); it.hasNext();) {
      IClass klass = (IClass) it.next();
      if (isConcreteStrutsAction(klass)) {
        actions.add(klass);
        TypeReference type = klass.getReference();
        MethodReference M = MethodReference.findOrCreate(type, executeName, httpExecuteDesc);

        IMethod im = cha.resolveMethod(M);
        if (im != null) {
          entrypoints.put(M, new StrutsActionEntrypoint(klass, im, cha));
        }

        if (SPECULATE_DISPATCH_ACTIONS) {
          addSpeculativeDispatchMethods(klass, cha);
        }
      }
      if (isConcreteStrutsPlugIn(klass)) {
        plugins.add(klass);
        TypeReference type = klass.getReference();
        MethodReference M = MethodReference.findOrCreate(type, plugInInitName, plugInInitDesc);

        IMethod im = cha.resolveMethod(M);
        if (im != null) {
          entrypoints.put(M, new StrutsPlugInEntrypoint(klass, im, cha));
        }
        
        M = MethodReference.findOrCreate(type, plugInDestroyName, plugInDestroyDesc);
        
        im = cha.resolveMethod(M);
        if (im != null) {
          entrypoints.put(M, new StrutsPlugInEntrypoint(klass, im, cha));
        }
                
      }
    }
  }

  private boolean isConcreteStrutsPlugIn(IClass klass) {
    TypeReference plugInType = TypeReference.findOrCreate(ClassLoaderReference.Application, plugInName);
    IClass plugInClass = klass.getClassHierarchy().lookupClass(plugInType);
    if (klass.isAbstract()) {
      return false;
    }
    if (klass.getReference().equals(plugInType)) {
      return false;
    }    
    if (klass.getClassHierarchy().isAssignableFrom(plugInClass, klass)) {
      return true;
    }
    return false;
  }

  public static boolean isConcreteStrutsAction(IClass klass) {
    TypeReference actionType = TypeReference.findOrCreate(ClassLoaderReference.Application, actionName);
    IClass actionClass = klass.getClassHierarchy().lookupClass(actionType);
    if (actionClass == null) {
      return false;
    }
    if (klass.isAbstract()) {
      return false;
    }
    if (klass.getReference().equals(actionType)) {
      return false;
    }
    if (klass.getClassHierarchy().isSubclassOf(klass, actionClass)) {
      return true;
    }
    return false;
  }

  /**
   * Add any methods that look like they might be DispatchAction targets, based on the method signature.
   * 
   * TODO: instead, parse the struts xml directly.
   * @param klass an Action
   */
  private void addSpeculativeDispatchMethods(IClass klass, IClassHierarchy cha) {
    IClass c = klass;
    while (c != null) {
      for (Iterator<IMethod> it = c.getDeclaredMethods().iterator(); it.hasNext();) {
        IMethod M = (IMethod) it.next();
        Descriptor D = M.getDescriptor();
        if (D.equals(executeDesc) || D.equals(httpExecuteDesc)) {
          MethodReference m = MethodReference.findOrCreate(klass.getReference(), M.getName(), M.getDescriptor());
          entrypoints.put(m, new StrutsActionEntrypoint(klass, M, cha));
        }
      }
      try {
        c = c.getSuperclass();
      } catch (ClassHierarchyException e) {
        e.printStackTrace();
        Assertions.UNREACHABLE();
      }
    }
  }

  public Iterator<Entrypoint> iterator() {
    return entrypoints.values().iterator();
  }

  public String toString() {
    StringBuffer result = new StringBuffer();
    result.append("Actions:");
    Iterator<IClass> it = actions.iterator();
    if (it.hasNext()) {
      while (it.hasNext()) {
        result.append("\n   ");
        result.append(it.next());
      }
    } else {
      result.append("   none");
    }
    result.append("\n");
    result.append("PlugIns:");
    Iterator<IClass> it1 = plugins.iterator();
    if (it1.hasNext()) {
      while (it1.hasNext()) {
        result.append("\n   ");
        result.append(it1.next());
      }
    } else {
      result.append("   none");
    }
    return result.toString();
  }

  /**
   * @return true iff m is an entrypoint recorded by this class
   */
  public boolean contains(MemberReference m) {
    return entrypoints.keySet().contains(m);
  }

  /**
   * An entrypoint which assumes all ServletRequest and ServletResponses are of the HTTP flavor.
   */
  private static class StrutsActionEntrypoint extends DefaultEntrypoint {
    private final TypeReference receiver;

    public StrutsActionEntrypoint(IClass concreteType, IMethod method, IClassHierarchy cha) {
      super(method, cha);
      receiver = concreteType.getReference();
    }

    @Override
    public TypeReference[] getParameterTypes(int i) {
      if (i == 0) {
        return new TypeReference[] { receiver };
      } else {
        TypeReference[] tarray = super.getParameterTypes(i);
        if (Assertions.verifyAssertions) {
          Assertions._assert(tarray.length == 1);
        }
        TypeReference T = tarray[0];
        TypeName n = T.getName();
        TypeReference Tprime = concreteParameterMap.get(n);
        if (Tprime != null) {
          T = Tprime;
        }
        if (n.equals(actionFormName)) {
          // return an array of all possible subtypes of ActionForm in the class hierarchy
          Collection<IClass> subcs = getCha().computeSubClasses(T);
          Set<TypeReference> subs = HashSetFactory.make();
          for (IClass cs : subcs) {
            if (!cs.isAbstract() && !cs.isInterface()) {
              TypeReference reference = cs.getReference();
              subs.add(reference);
            }
          }
          return subs.toArray(new TypeReference[subs.size()]);
        }
        return new TypeReference[] { T };
      }
    }
  }
  
  /**
   * An entrypoint which assumes all ServletRequest and ServletResponses are of the HTTP flavor.
   */
  private static class StrutsPlugInEntrypoint extends DefaultEntrypoint {
//    private final TypeReference receiver;
//
    public StrutsPlugInEntrypoint(IClass concreteType, IMethod method, IClassHierarchy cha) {
      super(method, cha);
//      receiver = concreteType.getReference();
    }
  }
}
