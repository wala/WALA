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

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.AbstractRootMethod;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MemberReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.strings.Atom;

/**
 * This class provides an iterator of {@link Entrypoint}s that are implementations of org.apache.struts.action.Action
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

  private final static Atom plugInInitName = Atom.findOrCreateUnicodeAtom("init");

  private final static Atom plugInDestroyName = Atom.findOrCreateUnicodeAtom("destroy");

  private final static Descriptor plugInInitDesc = Descriptor
      .findOrCreateUTF8("(Lorg/apache/struts/action/ActionServlet;Lorg/apache/struts/config/ModuleConfig;)V");

  private final static Descriptor plugInDestroyDesc = Descriptor.findOrCreateUTF8("()V");

  private final static TypeName actionName = TypeName.string2TypeName("Lorg/apache/struts/action/Action");

  public final static TypeName actionFormName = TypeName.string2TypeName("Lorg/apache/struts/action/ActionForm");

  private final static TypeName plugInName = TypeName.string2TypeName("Lorg/apache/struts/action/PlugIn");

  private final static TypeName requestProcessorName = TypeName.string2TypeName("Lorg/apache/struts/action/RequestProcessor");

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
   * Set of request processor implementations found.
   */
  private Set<IClass> requestProcessors = HashSetFactory.make();

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

    for (Iterator<IClass> it = getCandidateEntryClasses(cha); it.hasNext();) {
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
          entrypoints.put(M, new StrutsPlugInEntrypoint(im, cha));
        }

        M = MethodReference.findOrCreate(type, plugInDestroyName, plugInDestroyDesc);

        im = cha.resolveMethod(M);
        if (im != null) {
          entrypoints.put(M, new StrutsPlugInEntrypoint(im, cha));
        }
      }
      if (isConcreteRequestProcessor(klass)) {
        requestProcessors.add(klass);
        TypeReference requestProcessorType = TypeReference.findOrCreate(ClassLoaderReference.Application, requestProcessorName);
        IClass requestProcessorClass = klass.getClassHierarchy().lookupClass(requestProcessorType);
        for (IMethod m : klass.getDeclaredMethods()) {
          // if the method overrides a method in RequestProcessor, make it an entrypoint
          MethodReference mref = m.getReference();
          if (cha.getPossibleTargets(requestProcessorClass, mref).contains(m)) {
            // bingo
            // TODO exclude <init>()?
            entrypoints.put(mref, new StrutsRequestProcessorEntrypoint(klass, m, cha));
          }
        }
      }
    }
  }

  public static boolean isConcreteRequestProcessor(IClass klass) {
    TypeReference requestProcessorType = TypeReference.findOrCreate(ClassLoaderReference.Application, requestProcessorName);
    IClass requestProcessorClass = klass.getClassHierarchy().lookupClass(requestProcessorType);
    if (requestProcessorClass == null) {
      return false;
    }
    if (klass.isAbstract()) {
      return false;
    }
    if (klass.getReference().equals(requestProcessorType)) {
      return false;
    }
    if (klass.getClassHierarchy().isAssignableFrom(requestProcessorClass, klass)) {
      return true;
    }
    return false;
  }

  public static boolean isConcreteStrutsPlugIn(IClass klass) {
    TypeReference plugInType = TypeReference.findOrCreate(ClassLoaderReference.Application, plugInName);
    IClass plugInClass = klass.getClassHierarchy().lookupClass(plugInType);
    if (plugInClass == null) {
      return false;
    }
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
   * 
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
      c = c.getSuperclass();
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
    result.append("\n");
    result.append("RequestProcessors:");
    Iterator<IClass> it2 = requestProcessors.iterator();
    if (it2.hasNext()) {
      while (it2.hasNext()) {
        result.append("\n   ");
        result.append(it2.next());
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
        assert tarray.length == 1;
        TypeReference T = tarray[0];
        TypeName n = T.getName();
        TypeReference Tprime = concreteParameterMap.get(n);
        if (Tprime != null) {
          T = Tprime;
        }
        return new TypeReference[] { T };
      }
    }

    @Override
    protected int makeArgument(AbstractRootMethod m, int i) {
      TypeName n = getParameterTypes(i)[0].getName();
      if (n.equals(actionFormName)) {
        // invoke a synthetic factory method that creates ActionForm objects
        MethodReference declaredTarget = MethodReference.findOrCreate(ActionFormFactoryMethod.factoryClassRef,
            ActionFormFactoryMethod.name, ActionFormFactoryMethod.descr);
        CallSiteReference site = CallSiteReference.make(0, declaredTarget, IInvokeInstruction.Dispatch.STATIC);
        SSAInvokeInstruction factoryInv = m.addInvocation(new int[0], site);
        return factoryInv.getDef();
      } else {
        return super.makeArgument(m, i);
      }
    }

  }

  private static class StrutsRequestProcessorEntrypoint extends DefaultEntrypoint {

    private final TypeReference receiver;

    public StrutsRequestProcessorEntrypoint(IClass concreteType, IMethod method, IClassHierarchy cha) {
      super(method, cha);
      receiver = concreteType.getReference();
    }

    @Override
    public TypeReference[] getParameterTypes(int i) {
      if (i == 0) {
        return new TypeReference[] { receiver };
      } else {
        TypeReference[] tarray = super.getParameterTypes(i);
        assert tarray.length == 1;
        TypeReference T = tarray[0];
        TypeName n = T.getName();
        TypeReference Tprime = concreteParameterMap.get(n);
        if (Tprime != null) {
          T = Tprime;
        }
        return new TypeReference[] { T };
      }
    }

  }

  /**
   * An entrypoint which assumes all ServletRequest and ServletResponses are of the HTTP flavor. TODO: get rid of this and just use
   * {@link DefaultEntrypoint}? --MS
   */
  private static class StrutsPlugInEntrypoint extends DefaultEntrypoint {

    public StrutsPlugInEntrypoint(IMethod method, IClassHierarchy cha) {
      super(method, cha);
    }
  }

  /**
   * return the set of classes that should be examined when searching for struts entrypoints.
   */
  protected Iterator<IClass> getCandidateEntryClasses(IClassHierarchy cha) {
    IClassLoader appLoader = cha.getLoader(ClassLoaderReference.Application);
    return appLoader.iterateAllClasses();
  }
}
