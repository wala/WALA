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
import java.util.Map;

import com.ibm.wala.analysis.typeInference.ConeType;
import com.ibm.wala.analysis.typeInference.PointType;
import com.ibm.wala.analysis.typeInference.TypeAbstraction;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.classLoader.SyntheticMethod;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ipa.callgraph.MethodTargetSelector;
import com.ibm.wala.ipa.callgraph.impl.FakeRootMethod;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ipa.summaries.BypassSyntheticClass;
import com.ibm.wala.ipa.summaries.MethodSummary;
import com.ibm.wala.ipa.summaries.SummarizedMethod;
import com.ibm.wala.j2ee.util.ReceiverTypeInference;
import com.ibm.wala.j2ee.util.ReceiverTypeInferenceCache;
import com.ibm.wala.shrikeBT.BytecodeConstants;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.ssa.SSAFieldAccessInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.warnings.ResolutionFailure;
import com.ibm.wala.util.warnings.Warnings;

/**
 * 
 * Special bypass rules divined from an EJB deployment descriptor.
 * 
 * TODO: refactor this class using the delegation model
 * 
 * @author sfink
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public class J2EEMethodTargetSelector implements MethodTargetSelector, BytecodeConstants, EJBConstants {
  static final boolean DEBUG = false;

  private final static Atom addAtom = Atom.findOrCreateUnicodeAtom("add");

  private final static Descriptor addDesc = Descriptor.findOrCreateUTF8("(Ljava/lang/Object;)Z");

  private final static MethodReference addMethod = MethodReference.findOrCreate(TypeReference.JavaUtilCollection, addAtom, addDesc);

  private final static Atom elementsAtom = Atom.findOrCreateUnicodeAtom("elements");

  private final static Descriptor elementsDesc = Descriptor.findOrCreateUTF8("()Ljava/util/Enumeration");

  private final static MethodReference elementsMethod = MethodReference.findOrCreate(TypeReference.JavaUtilVector, elementsAtom,
      elementsDesc);

  private final static MethodReference hashSetInit = MethodReference.findOrCreate(TypeReference.JavaUtilHashSet,
      MethodReference.initAtom, MethodReference.defaultInitDesc);

  private final static MethodReference vectorInit = MethodReference.findOrCreate(TypeReference.JavaUtilVector,
      MethodReference.initAtom, MethodReference.defaultInitDesc);

  private final static Atom JAVAX_EJB = J2EEUtil.EJB_HOME.getName().getPackage();

  /**
   * deployment information
   */
  private final DeploymentMetaData deployment;

  /**
   * the governing class hierarchy
   */
  private final IClassHierarchy cha;

  /**
   * A cache of synthetic methods generated so far. Mapping from MethodReference ->
   * SyntheticMethods
   */
  private final Map<MethodReference, SyntheticMethod> map = HashMapFactory.make();

  /**
   * A cache of TypeInference results
   */
  private final ReceiverTypeInferenceCache typeInference;

  /**
   * Governing analysis scope
   */
  private final AnalysisScope scope;

  /**
   * the method target selector to use if this one does not bypass it
   */
  private final MethodTargetSelector parent;

  /**
   * A mapping for EJB entity contract method names
   * 
   * TODO: split for Home vs. Remote interfaces, etc....
   */
  private static final HashMap<Atom, Atom[]> entityContractMap = HashMapFactory.make(10);
  static {
    entityContractMap.put(CREATE, new Atom[] { EJB_CREATE, EJB_POST_CREATE });
    entityContractMap.put(REMOVE, new Atom[] { EJB_REMOVE });
    entityContractMap.put(GET_PRIMARY_KEY, new Atom[] { GET_PRIMARY_KEY });
    entityContractMap.put(GET_EJB_META_DATA, new Atom[] { GET_EJB_META_DATA });
    entityContractMap.put(GET_EJB_HOME, new Atom[] { GET_EJB_HOME });
    entityContractMap.put(GET_HANDLE, new Atom[] { GET_HANDLE });
    entityContractMap.put(IS_IDENTICAL, new Atom[] { IS_IDENTICAL });
  }

  /**
   * A mapping from EJB entity contract method name to classes allocated by
   * these methods.
   */
  private static final HashMap<Atom, TypeReference> entityContractExceptionMap = HashMapFactory.make(5);
  static {
    entityContractExceptionMap.put(CREATE, CreateExceptionClass);
    entityContractExceptionMap.put(REMOVE, RemoveExceptionClass);
  }

  public J2EEMethodTargetSelector(AnalysisScope scope, MethodTargetSelector parent, DeploymentMetaData deployment,
      IClassHierarchy cha, ReceiverTypeInferenceCache typeInference) {
    this.scope = scope;
    this.deployment = deployment;
    this.parent = parent;
    this.cha = cha;
    this.typeInference = typeInference;
  }

  /**
   * Handle a call to an entity's remote or local interface
   * 
   * @param m
   *          a call to a remote or local interface
   * @return a synthetic method which serves as a target implementation for a
   *         call to m, or null if there's a problem
   */
  private SyntheticMethod hijackEntityInterface(MethodReference m) {
    if (isJavaLangObjectMethod(m))
      return null;

    if (entityContractMap.containsKey(m.getName())) {
      return findOrCreateEntityContractMethod(m);
    }

    TypeReference entityType = m.getDeclaringClass();
    BeanMetaData bean = deployment.getBeanForInterface(entityType);
    // resolve the method via the class hierarchy first
    IMethod resolved = cha.resolveMethod(m);
    if (resolved == null) {
      Warnings.add(LoadFailure.create(m));
      return null;
    }
    m = resolved.getReference();
    SyntheticMethod S = map.get(m);
    if (S == null) {
      if (DEBUG) {
        Trace.println("EJBBypass: create Synthetic case A for " + m);
      }
      if (Assertions.verifyAssertions) {
        if (bean == null) {
          Assertions._assert(false, "no bean bound for " + entityType);
        }
      }
      IMethod ejbMethod = cha.resolveMethod(MethodReference.findOrCreate(bean.getEJBClass(), m.getName(), m.getDescriptor()));

      if (Assertions.verifyAssertions) {
        Assertions._assert(ejbMethod != null, "Could not find method " + bean.getEJBClass() + " " + m.getName() + " "
            + m.getDescriptor());
      }

      MethodSummary summ = new MethodSummary(m);
      int nextLocal = summ.getNumberOfParameters() + 1;

      // 1. extract bean object from container object
      // TODO: we pretend that the Entity is stateless, which it is not.
      int beanVN = nextLocal++;
      summ.addStatement(new SSAGetInstruction(beanVN, J2EEContainerModel.getBeanFieldRef(bean)));

      // 2. call corresponding method on bean object
      int EXCEPTION_VN = nextLocal++;
      int returnVN = nextLocal++;

      int[] params = new int[summ.getNumberOfParameters()];
      params[0] = beanVN;
      for (int i = 1; i < params.length; i++) {
        params[i] = i + 1;
      }

      MethodReference ref = ejbMethod.getReference();
      CallSiteReference site = CallSiteReference.make(summ.getNextProgramCounter(), ref, IInvokeInstruction.Dispatch.VIRTUAL);

      if (summ.getReturnType() != null) {
        summ.addStatement(new SSAInvokeInstruction(returnVN, params, EXCEPTION_VN, site));
        summ.addStatement(new SSAReturnInstruction(returnVN, summ.getReturnType().isPrimitiveType()));
      } else {
        summ.addStatement(new SSAInvokeInstruction(params, EXCEPTION_VN, site));
      }

      // 3. throw RemoteException if remote interface
      if (deployment.isRemoteInterface(m.getDeclaringClass())) {
        int xobj = nextLocal++;
        summ.addStatement(new SSANewInstruction(xobj, NewSiteReference.make(summ.getNextProgramCounter(), RemoteExceptionClass)));
        summ.addStatement(new SSAThrowInstruction(xobj));
      }

      int xobj = nextLocal++;
      summ.addStatement(new SSANewInstruction(xobj, NewSiteReference.make(summ.getNextProgramCounter(), EJBExceptionClass)));
      summ.addStatement(new SSAThrowInstruction(xobj));

      IClass C = cha.lookupClass(m.getDeclaringClass());
      S = new SummarizedEJBMethod(bean, m, summ, C);
      map.put(m, S);
    }

    if (DEBUG) {
      Trace.println("EJBBypass: case A return " + S);
    }
    return S;
  }

  /**
   * TODO: refactor extract common code with other hijack methods
   * 
   * @param m
   * @return a Synthetic method representing the a container-implemented method
   *         for the onMessage MDB entrypoint
   */
  private SyntheticMethod hijackOnMessageEntrypoint(MethodReference m) {

    TypeReference mdbType = m.getDeclaringClass();
    BeanMetaData bean = deployment.getBeanMetaData(mdbType);
    // resolve the method via the class hierarchy first
    IMethod resolved = cha.resolveMethod(m);
    if (resolved == null) {
      Warnings.add(LoadFailure.create(m));
      return null;
    }
    m = resolved.getReference();
    SyntheticMethod S = map.get(m);
    if (S == null) {
      if (DEBUG) {
        Trace.println("EJBBypass: create Synthetic case A for " + m);
      }
      if (Assertions.verifyAssertions) {
        if (bean == null) {
          Assertions._assert(false, "no bean bound for " + mdbType);
        }
      }
      MethodSummary summ = new MethodSummary(m);
      int nextLocal = summ.getNumberOfParameters() + 1;

      // 1. extract bean object from container object
      // TODO: we pretend that the Entity is stateless, which it is not.
      int beanVN = nextLocal++;
      summ.addStatement(new SSAGetInstruction(beanVN, J2EEContainerModel.getBeanFieldRef(bean)));

      // 2. call corresponding method on bean object
      int EXCEPTION_VN = nextLocal++;
      int returnVN = nextLocal++;

      int[] params = new int[summ.getNumberOfParameters()];
      params[0] = beanVN;
      for (int i = 1; i < params.length; i++) {
        params[i] = i + 1;
      }

      MethodReference ref = resolved.getReference();
      CallSiteReference site = CallSiteReference.make(summ.getNextProgramCounter(), ref, IInvokeInstruction.Dispatch.VIRTUAL);

      if (summ.getReturnType() != null) {
        summ.addStatement(new SSAInvokeInstruction(returnVN, params, EXCEPTION_VN, site));
        summ.addStatement(new SSAReturnInstruction(returnVN, summ.getReturnType().isPrimitiveType()));
      } else {
        summ.addStatement(new SSAInvokeInstruction(params, EXCEPTION_VN, site));
      }

      // 3. throw RemoteException if remote interface
      if (deployment.isRemoteInterface(m.getDeclaringClass())) {
        int xobj = nextLocal++;
        summ.addStatement(new SSANewInstruction(xobj, NewSiteReference.make(summ.getNextProgramCounter(), RemoteExceptionClass)));
        summ.addStatement(new SSAThrowInstruction(xobj));
      }

      int xobj = nextLocal++;
      summ.addStatement(new SSANewInstruction(xobj, NewSiteReference.make(summ.getNextProgramCounter(), EJBExceptionClass)));
      summ.addStatement(new SSAThrowInstruction(xobj));

      IClass C = cha.lookupClass(m.getDeclaringClass());
      S = new SummarizedEJBMethod(bean, m, summ, C);
      map.put(m, S);
    }

    if (DEBUG) {
      Trace.println("EJBBypass: case A return " + S);
    }
    return S;
  }

  /**
   * @param m
   * @return a Synthetic method representing a servlet entrypoint to call
   */
  private SyntheticMethod hijackServletEntrypoint(MethodReference m) {

    SyntheticMethod S = map.get(m);
    if (S == null) {
      MethodSummary summ = new MethodSummary(m);
      int nextLocal = summ.getNumberOfParameters() + 1;

      int EXCEPTION_VN = nextLocal++;
      int returnVN = nextLocal++;

      // allocate a ServletConfig object
      if (!m.getDeclaringClass().getClassLoader().equals(ClassLoaderReference.Primordial)) {
        int servletConfigVN = nextLocal++;
        NewSiteReference newConfig = NewSiteReference.make(summ.getNextProgramCounter(), ServletEntrypoints.WalaServletConfigModel);
        summ.addStatement(new SSANewInstruction(servletConfigVN, newConfig));

        // initialize the servlet
        CallSiteReference site = CallSiteReference.make(summ.getNextProgramCounter(), ServletEntrypoints.servletInit,
            IInvokeInstruction.Dispatch.INTERFACE);
        int[] params = new int[2];
        params[0] = 1; // "this" pointer
        params[1] = servletConfigVN;
        summ.addStatement(new SSAInvokeInstruction(params, EXCEPTION_VN, site));
      }

      int[] params = new int[summ.getNumberOfParameters()];
      params[0] = 1; // "this" pointer
      for (int i = 1; i < params.length; i++) {
        params[i] = i + 1;
      }
      // invoke the desired entrypoint
      CallSiteReference site = CallSiteReference.make(summ.getNextProgramCounter(), m, IInvokeInstruction.Dispatch.VIRTUAL);
      if (!summ.getReturnType().equals(TypeReference.Void)) {
        summ.addStatement(new SSAInvokeInstruction(returnVN, params, EXCEPTION_VN, site));
        summ.addStatement(new SSAReturnInstruction(returnVN, summ.getReturnType().isPrimitiveType()));
      } else {
        summ.addStatement(new SSAInvokeInstruction(params, EXCEPTION_VN, site));
      }

      IClass C = cha.lookupClass(m.getDeclaringClass());
      S = new SummarizedMethod(m, summ, C);
      map.put(m, S);
    }

    return S;
  }

  /**
   * Handle a call to an entity's home or local home
   * 
   * @param m
   *          a call to a home or local home interface
   * @return a synthetic method which serves as a target implementation for a
   *         call to m
   */
  private SyntheticMethod hijackHomeInterface(MethodReference m) {
    if (isJavaLangObjectMethod(m))
      return null;

    TypeReference type = m.getDeclaringClass();
    IClass C = cha.lookupClass(type);
    BeanMetaData bean = deployment.getBeanForInterface(type);
    if (Assertions.verifyAssertions) {
      Assertions._assert(bean != null, "null bean for " + type);
    }
    boolean local = deployment.isLocalHomeInterface(type);

    if (deployment.isFinder(m)) {
      MethodSummary summ = (local) ? new LocalHomeFinderSummary(m) : new RemoteFinderSummary(m);
      SummarizedEJBMethod S = new SummarizedEJBMethod(bean, m, summ, C);
      map.put(m, S);
      return S;
    } else if (entityContractMap.containsKey(m.getName())) {
      return findOrCreateEntityContractMethod(m);
    } else if (isHomeMethod(m, bean)) {
      MethodSummary summ = new HomeMethodSummary(m, bean);
      SummarizedEJBMethod S = new SummarizedEJBMethod(bean, m, summ, C);
      map.put(m, S);
      return S;
    } else
      return null;
  }

  /**
   * Handle a call to a CMP or CMR getter or setter
   * 
   * @param m
   *          a call to a CMP or CMR getter of setter
   * @return a synthetic method which serves as a target implementation for a
   *         call to m
   */
  private SyntheticMethod hijackCMPBeanMethods(MethodReference m) {

    TypeReference type = m.getDeclaringClass();
    BeanMetaData bean = deployment.getBeanForInterface(type);
    SyntheticMethod S = null;
    if (deployment.isCMPGetter(m)) {
      // get declaring instance from class hierarchy
      m = cha.resolveMethod(m).getReference();
      // return a summary for a CMP getter
      S = map.get(m);
      if (S == null) {
        if (deployment.isCMRGetter(m)) {
          CMRGetterSummary g = new CMRGetterSummary(m);
          S = createSummarizedMethod(bean, m, g);
        } else {
          GetterSummary g = new GetterSummary(m);
          S = createSummarizedMethod(bean, m, g);
        }
      }
      return S;
    } else if (deployment.isCMPSetter(m)) {
      // get declaring instance from class hierarchy
      m = cha.resolveMethod(m).getReference();
      // return a summary for CMP setter
      S = map.get(m);
      if (S == null) {
        if (deployment.isCMRSetter(m)) {
          CMRSetterSummary s = new CMRSetterSummary(m);
          S = createSummarizedMethod(bean, m, s);
        } else {
          SetterSummary s = new SetterSummary(m);
          S = createSummarizedMethod(bean, m, s);
        }
      }
      return S;
    } else
      return null;
  }

  /**
   * If m is a special EJB-container generated method, return the IMethod that
   * represents the modelled target of a call to m. Else, return null;
   */
  private SyntheticMethod methodReferenceIntercept(MethodReference m) {
    TypeReference type = m.getDeclaringClass();
    if (DEBUG) {
      Trace.println("EJBBypass: intercept? " + m);
    }

    if (type.getClassLoader().equals(ClassLoaderReference.Primordial)) {
      // small optimization: we know we will never hijack calls to the
      // primordial loader
      return null;
    }

    if (deployment.isLocalInterface(type) || deployment.isRemoteInterface(type)) {
      return hijackEntityInterface(m);
    } else if (deployment.isLocalHomeInterface(type) || deployment.isHomeInterface(type)) {
      return hijackHomeInterface(m);
    } else if (deployment.isContainerManaged(type)) {
      return hijackCMPBeanMethods(m);
    } else {
      return null;
    }
  }

  private boolean isJavaLangObjectMethod(MethodReference m) {
    IClass object = cha.lookupClass(TypeReference.JavaLangObject);
    IMethod declaredMethod = object.getMethod(m.getSelector());
    return declaredMethod != null;
  }

  private SyntheticMethod createSummarizedMethod(BeanMetaData bean, MethodReference m, MethodSummary g) {
    IClass C = cha.lookupClass(m.getDeclaringClass());
    SyntheticMethod S = new SummarizedEJBMethod(bean, m, g, C);
    map.put(m, S);
    return S;
  }

  /**
   * @param m
   * @return a Synthetic method representing the a container-implemented method
   *         for the EJB Entity contract
   */
  private SyntheticMethod findOrCreateEntityContractMethod(MethodReference m) {
    if (DEBUG) {
      Trace.println("findOrCreateEntityContractMethod " + m);
    }
    SyntheticMethod S = map.get(m);
    if (S == null) {

      TypeReference rType = m.getReturnType();
      TypeReference receiverType = m.getDeclaringClass();
      IClass receiverClass = cha.lookupClass(receiverType);
      BeanMetaData bean = deployment.getBeanForInterface(receiverType);
      TypeReference ejbType = bean.getEJBClass();
      IClass ejbClass = cha.lookupClass(ejbType);

      MethodSummary summ = new MethodSummary(m);
      int nextLocal = summ.getNumberOfParameters() + 1;

      // get ejb object from pool
      int alloc = nextLocal++;
      summ.addStatement(new SSAGetInstruction(alloc, J2EEContainerModel.getBeanFieldRef(bean)));

      // create+return entity object, if appropriate
      if (rType != TypeReference.Void) {

        // create result object
        int ret = nextLocal++;
        summ.addStatement(new SSANewInstruction(ret, NewSiteReference.make(summ.getNextProgramCounter(), rType)));

        // return it
        summ.addStatement(new SSAReturnInstruction(ret, false));
      }

      // call contract methods
      Atom[] names = entityContractMap.get(m.getName());
      for (int i = 0; i < names.length; i++) {
        Atom name = names[i];
        MethodReference ref = makeEntityContractMethod(bean, m, ejbType, name);
        CallSiteReference site = CallSiteReference.make(summ.getNextProgramCounter(), ref, ejbClass.isInterface() ? IInvokeInstruction.Dispatch.INTERFACE
            : IInvokeInstruction.Dispatch.VIRTUAL);
        if (cha.resolveMethod(ejbClass, ref.getSelector()) == null)
          continue;
        int[] params = new int[summ.getNumberOfParameters()];
        // set up the dispatch to the bean object
        params[0] = alloc;
        for (int j = 1; j < params.length; j++) {
          params[j] = j + 1;
        }
        // note that we reserve value number 2 to hold the exceptional result
        // of the call.
        if (!ref.getReturnType().equals(TypeReference.Void)) {
          summ.addStatement(new SSAInvokeInstruction(nextLocal++, params, nextLocal++, site));
        } else {
          summ.addStatement(new SSAInvokeInstruction(params, nextLocal++, site));
        }
      }

      final TypeReference t = entityContractExceptionMap.get(m.getName());
      if (t != null) {
        int ex = nextLocal++;
        summ.addStatement(new SSANewInstruction(ex, NewSiteReference.make(summ.getNextProgramCounter(), t)));
        summ.addStatement(new SSAThrowInstruction(ex));
      }

      if (deployment.isRemoteInterface(m.getDeclaringClass()) || deployment.isHomeInterface(m.getDeclaringClass())) {
        int xobj = nextLocal++;
        summ.addStatement(new SSANewInstruction(xobj, NewSiteReference.make(summ.getNextProgramCounter(), RemoteExceptionClass)));
        summ.addStatement(new SSAThrowInstruction(xobj));
      }

      int ejbException = nextLocal++;
      summ
          .addStatement(new SSANewInstruction(ejbException, NewSiteReference.make(summ.getNextProgramCounter(), EJBExceptionClass)));
      summ.addStatement(new SSAThrowInstruction(ejbException));

      S = new SummarizedMethod(m, summ, receiverClass);
      map.put(m, S);
    }
    return S;
  }

  /**
   * @param bean
   *          metadata regarding the EJB
   * @param ifaceMethod
   *          name of a method called on an EJB interface
   * @param ejbType
   *          concrete type the method should dispatch to
   * @param methodName
   *          name of the bean method to dispatch to
   * @return a method reference representing the dispatch target
   */
  private MethodReference makeEntityContractMethod(BeanMetaData bean, MethodReference ifaceMethod, TypeReference ejbType,
      Atom methodName) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(bean != null);
    }
    TypeName returnType = TypeReference.VoidName;
    if (methodName.equals(EJB_CREATE) && bean.isContainerManagedEntity()) {
      // ejbCreate returns the primary key type.
      returnType = bean.getPrimaryKeyType().getName();
    }
    MethodReference ref = MethodReference.findOrCreate(ejbType, methodName, Descriptor.findOrCreate(ifaceMethod.getDescriptor()
        .getParameters(), returnType));
    return ref;
  }

  private class GetterSummary extends MethodSummary {
    public GetterSummary(final MethodReference method) {
      super(method);
      FieldReference field = deployment.getCMPField(method);
      int nextLocal = 2;
      TypeReference T = field.getFieldType();
      if (T.isReferenceType()) {
        T = cha.lookupClass(T).getReference();
      }

      // the reference dispatched on is value number 1, and
      // we store the result in value number nextLocal++

      // TODO: we should make sure that finders populate all fields.
      int result = nextLocal++;
      SSAFieldAccessInstruction f = new SSAGetInstruction(result, 1, field);
      addStatement(f);

      SSAReturnInstruction r = new SSAReturnInstruction(result, field.getFieldType().isPrimitiveType());
      addStatement(r);

      if (deployment.isRemoteInterface(method.getDeclaringClass())) {
        int xobj = nextLocal++;
        addStatement(new SSANewInstruction(xobj, NewSiteReference.make(getNextProgramCounter(), RemoteExceptionClass)));
        addStatement(new SSAThrowInstruction(xobj));
      }
      int xobj = nextLocal++;
      addStatement(new SSANewInstruction(xobj, NewSiteReference.make(getNextProgramCounter(), EJBExceptionClass)));
      addStatement(new SSAThrowInstruction(xobj));
    }
  }

  /**
   * A synthetic model of a CMR getter method
   */
  private class CMRGetterSummary extends MethodSummary {
    public CMRGetterSummary(final MethodReference method) {
      super(method);
      FieldReference field = deployment.getCMPField(method);
      int nextLocal = 2;
      TypeReference T = field.getFieldType();
      T = cha.lookupClass(T).getReference();

      BeanMetaData getteeBean = deployment.getCMRBean(field);
      TypeReference getteeType = getteeBean.getLocalInterface();
      TypeReference getteeHomeType = getteeBean.getLocalHomeInterface();
      TypeReference keyType = getteeBean.getPrimaryKeyType();

      Atom finderName = Atom.findOrCreateUnicodeAtom("findByPrimaryKey");
      Descriptor finderDesc = Descriptor.findOrCreate(new TypeName[] { keyType.getName() }, getteeType.getName());
      IMethod finder = cha.resolveMethod(MethodReference.findOrCreate(getteeHomeType, finderName, finderDesc));
      if (finder == null) {
        // Here's a bad kludge ... it appears that the primary key type might be
        // a subclass of the
        // argument of the descriptor. TODO: figure out how this happens.
        // temp fix: Try Ojbect instead ...
        finderDesc = Descriptor.findOrCreate(new TypeName[] { TypeReference.JavaLangObject.getName() }, getteeType.getName());
        finder = cha.resolveMethod(MethodReference.findOrCreate(getteeHomeType, finderName, finderDesc));
        if (Assertions.verifyAssertions) {
          Assertions._assert(finder != null, "failed to find findByPrimaryKey for " + getteeType);
        }
      }

      // allocate type of primary key
      // TODO: should probably pretend the object has a field of this type
      NewSiteReference nsr = NewSiteReference.make(getNextProgramCounter(), keyType);
      int keyAlloc = nextLocal++;
      addStatement(new SSANewInstruction(keyAlloc, nsr));

      // allocate local home type
      // TODO: this is fake; should model container providing home somehow
      NewSiteReference lhnr = NewSiteReference.make(getNextProgramCounter(), getteeHomeType);
      int localHomeAlloc = nextLocal++;
      addStatement(new SSANewInstruction(localHomeAlloc, lhnr));

      // call findByPrimaryKey
      int fr = nextLocal++;
      int ignoredExceptions = nextLocal++;
      CallSiteReference fcsr = CallSiteReference.make(getNextProgramCounter(), finder.getReference(), IInvokeInstruction.Dispatch.INTERFACE);
      addStatement(new SSAInvokeInstruction(fr, new int[] { localHomeAlloc, keyAlloc }, ignoredExceptions, fcsr));

      // return result, as set if appropriate
      if (T.equals(TypeReference.JavaUtilSet) || T.equals(TypeReference.JavaUtilCollection)) {
        // assume HashSet is the type of the returned collection.
        int setObj = nextLocal++;
        NewSiteReference setRef = NewSiteReference.make(getNextProgramCounter(), TypeReference.JavaUtilHashSet);
        SSANewInstruction n = new SSANewInstruction(setObj, setRef);
        addStatement(n);

        int initIgnoredExceptions = nextLocal++;
        CallSiteReference initRef = CallSiteReference.make(getNextProgramCounter(), hashSetInit, IInvokeInstruction.Dispatch.SPECIAL);
        addStatement(new SSAInvokeInstruction(new int[] { setObj }, initIgnoredExceptions, initRef));

        int ignoredResult = nextLocal++;
        int moreIgnoredExceptions = nextLocal++;
        CallSiteReference addRef = CallSiteReference.make(getNextProgramCounter(), addMethod, IInvokeInstruction.Dispatch.INTERFACE);
        SSAInvokeInstruction addCall = new SSAInvokeInstruction(ignoredResult, new int[] { setObj, fr }, moreIgnoredExceptions,
            addRef);
        addStatement(addCall);

        SSAReturnInstruction r = new SSAReturnInstruction(setObj, false);
        addStatement(r);
      } else {

        SSAReturnInstruction r = new SSAReturnInstruction(fr, false);
        addStatement(r);
      }

      // put in a bogus getfield to pacify old clients that expect
      // to see a getfield here. TODO: rewrite the old clients to
      // avoid needing this instruction.
      int ignore = nextLocal++;
      SSAFieldAccessInstruction f = new SSAGetInstruction(ignore, 1, field);
      addStatement(f);

      if (deployment.isRemoteInterface(method.getDeclaringClass())) {
        int xobj = nextLocal++;
        addStatement(new SSANewInstruction(xobj, NewSiteReference.make(getNextProgramCounter(), RemoteExceptionClass)));
        addStatement(new SSAThrowInstruction(xobj));
      }
      int xobj = nextLocal++;
      addStatement(new SSANewInstruction(xobj, NewSiteReference.make(getNextProgramCounter(), EJBExceptionClass)));
      addStatement(new SSAThrowInstruction(xobj));
    }
  }

  /**
   * A synthetic model of a CMP setter method
   */
  private class SetterSummary extends MethodSummary {
    public SetterSummary(final MethodReference method) {
      super(method);
      // the reference dispatched on is value number 1, and
      // the value stored is value number 2
      SSAFieldAccessInstruction f = new SSAPutInstruction(1, 2, deployment.getCMPField(method));
      addStatement(f);
    }
  }

  /**
   * A synthetic model of a CMR setter method
   */
  private class CMRSetterSummary extends MethodSummary {
    public CMRSetterSummary(final MethodReference method) {
      super(method);
      // the reference dispatched on is value number 1, and
      // the value stored is value number 2
      FieldReference field = deployment.getCMPField(method);
      TypeReference T = field.getFieldType();
      int nextLocal = 3;
      T = cha.lookupClass(T).getReference();
      if (T.equals(TypeReference.JavaUtilSet) || T.equals(TypeReference.JavaUtilCollection)) {
        // assume HashSet is the type of the returned collection.
        int setObj = nextLocal++;
        NewSiteReference setRef = NewSiteReference.make(getNextProgramCounter(), TypeReference.JavaUtilHashSet);
        SSANewInstruction allocSet = new SSANewInstruction(setObj, setRef);
        addStatement(allocSet);

        int initIgnoredExceptions = nextLocal++;
        CallSiteReference initRef = CallSiteReference.make(getNextProgramCounter(), hashSetInit, IInvokeInstruction.Dispatch.SPECIAL);
        addStatement(new SSAInvokeInstruction(new int[] { setObj }, initIgnoredExceptions, initRef));

        int ignoredResult = nextLocal++;
        int moreIgnoredExceptions = nextLocal++;
        CallSiteReference addRef = CallSiteReference.make(getNextProgramCounter(), addMethod,IInvokeInstruction.Dispatch.INTERFACE);
        SSAInvokeInstruction addCall = new SSAInvokeInstruction(ignoredResult, new int[] { setObj, 2 }, moreIgnoredExceptions,
            addRef);
        addStatement(addCall);

        SSAFieldAccessInstruction f2 = new SSAPutInstruction(1, setObj, field);
        addStatement(f2);
      } else {
        SSAFieldAccessInstruction f2 = new SSAPutInstruction(1, 2, field);
        addStatement(f2);
      }

      // create a bogus instance of the the bean on the other side
      // of the relationship. TODO: is it OK to use the local interface?
      // perhaps sometimes should use the remote interface ...

      BeanMetaData otherBean = deployment.getCMRBean(field);
      TypeReference otherType = otherBean.getLocalInterface();
      NewSiteReference newRef = NewSiteReference.make(getNextProgramCounter(), otherType);
      int otherInstance = nextLocal++;
      SSANewInstruction n = new SSANewInstruction(otherInstance, newRef);
      addStatement(n);

      // model a putfield on the other instance
      // TODO: for now it doesn't matter what we shove in this field ... since
      // when we call the getter we'll return a synthetic answer.
      // fix this if and when it becomes a problem.
      FieldReference oppField = deployment.getOppositeField(field);
      if (oppField == null) {
        // this is a problem ... the field is not navigable .... need to do
        // something better, like
        // create a synthetic field.
        Warnings.add(LoadFailure.create(field));
        return;
      }
      TypeReference otherT = oppField.getFieldType();
      otherT = cha.lookupClass(otherT).getReference();
      if (otherT.equals(TypeReference.JavaUtilSet) || otherT.equals(TypeReference.JavaUtilCollection)) {
        // assume HashSet is the type of the returned collection.
        int setObj = nextLocal++;
        NewSiteReference setRef = NewSiteReference.make(getNextProgramCounter(), TypeReference.JavaUtilHashSet);
        SSANewInstruction allocSet = new SSANewInstruction(setObj, setRef);
        addStatement(allocSet);

        int initIgnoredExceptions = nextLocal++;
        CallSiteReference initRef = CallSiteReference.make(getNextProgramCounter(), hashSetInit, IInvokeInstruction.Dispatch.SPECIAL);
        addStatement(new SSAInvokeInstruction(new int[] { setObj }, initIgnoredExceptions, initRef));

        int ignoredResult = nextLocal++;
        int moreIgnoredExceptions = nextLocal++;
        CallSiteReference addRef = CallSiteReference.make(getNextProgramCounter(), addMethod,IInvokeInstruction.Dispatch.INTERFACE);
        SSAInvokeInstruction addCall = new SSAInvokeInstruction(ignoredResult, new int[] { setObj, 1 }, moreIgnoredExceptions,
            addRef);
        addStatement(addCall);

        SSAFieldAccessInstruction f2 = new SSAPutInstruction(otherInstance, setObj, oppField);
        addStatement(f2);
      } else {
        SSAFieldAccessInstruction f2 = new SSAPutInstruction(otherInstance, 1, oppField);
        addStatement(f2);
      }
    }
  }

  private boolean isHomeMethod(MethodReference method, BeanMetaData bean) {
    TypeReference ejbType = bean.getEJBClass();
    IClass ejbClass = cha.lookupClass(ejbType);

    Atom newName = Atom.findOrCreateUnicodeAtom("ejb" + method.getName().toString());

    TypeReference rType = method.getReturnType();

    // call approriate method
    MethodReference ref = MethodReference.findOrCreate(ejbType, newName, Descriptor.findOrCreate(method.getDescriptor()
        .getParameters(), rType.getName()));

    return cha.resolveMethod(ejbClass, ref.getSelector()) != null;
  }

  /**
   * model for a user-defined method on a home or local home interface
   */
  private class HomeMethodSummary extends MethodSummary {
    /**
     * @param method
     *          the interface method summarized
     */
    public HomeMethodSummary(MethodReference method, BeanMetaData bean) {
      super(method);
      TypeReference ejbType = bean.getEJBClass();
      IClass ejbClass = cha.lookupClass(ejbType);

      Atom newName = Atom.findOrCreateUnicodeAtom("ejb" + method.getName().toString());
      int nextLocal = getNumberOfParameters() + 1;

      // get ejb object from pool
      int ejbObject = nextLocal++;
      addStatement(new SSAGetInstruction(ejbObject, J2EEContainerModel.getBeanFieldRef(bean)));

      TypeReference rType = method.getReturnType();

      // call approriate method
      MethodReference ref = MethodReference.findOrCreate(ejbType, newName, Descriptor.findOrCreate(method.getDescriptor()
          .getParameters(), rType.getName()));
      CallSiteReference site = CallSiteReference.make(getNextProgramCounter(), ref, IInvokeInstruction.Dispatch.VIRTUAL);
      if (cha.resolveMethod(ejbClass, ref.getSelector()) == null) {
        return;
      }

      int[] params = new int[getNumberOfParameters()];
      // set up the dispatch to the bean object
      params[0] = ejbObject;
      for (int j = 1; j < params.length; j++) {
        params[j] = j + 1;
      }
      // note that we reserve a value number to hold the exceptional result
      // of the call.
      if (rType.equals(TypeReference.Void)) {
        addStatement(new SSAInvokeInstruction(params, nextLocal++, site));
      } else {
        int ret = nextLocal++;
        addStatement(new SSAInvokeInstruction(ret, params, nextLocal++, site));
        addStatement(new SSAReturnInstruction(ret, rType.isPrimitiveType()));
      }

    }
  }

  // TODO!!! I think a finder really should may populate each field
  // in the found object. Probably should create a Hydrate method
  // summary for this.
  private class LocalHomeFinderSummary extends MethodSummary {
    protected int nextLocal;

    public LocalHomeFinderSummary(final MethodReference method) {
      super(method);

      nextLocal = method.getNumberOfParameters() + 2;
      TypeReference rType = cha.lookupClass(method.getReturnType()).getReference();
      TypeReference homeType = method.getDeclaringClass();
      TypeReference beanType = deployment.getFinderBeanType(method);
      BeanMetaData beanData = deployment.getBeanMetaData(beanType);
      TypeReference entType = (deployment.isLocalHomeInterface(homeType) ? beanData.getLocalInterface() : beanData
          .getRemoteInterface());

      // create interface object
      int result2 = nextLocal++;
      NewSiteReference ref2 = NewSiteReference.make(getNextProgramCounter(), entType);
      SSANewInstruction a2 = new SSANewInstruction(result2, ref2);
      addStatement(a2);

      if (rType.equals(TypeReference.JavaUtilCollection) || rType.equals(TypeReference.JavaUtilSet)) {
        // assume that the finder returns a HashSet
        int result3 = nextLocal++;
        NewSiteReference ref3 = NewSiteReference.make(getNextProgramCounter(), TypeReference.JavaUtilHashSet);
        SSANewInstruction a3 = new SSANewInstruction(result3, ref3);
        addStatement(a3);

        int initIgnoredExceptions = nextLocal++;
        CallSiteReference initRef = CallSiteReference.make(getNextProgramCounter(), hashSetInit, IInvokeInstruction.Dispatch.SPECIAL);
        addStatement(new SSAInvokeInstruction(new int[] { result3 }, initIgnoredExceptions, initRef));

        int ignoredResult = nextLocal++;
        int ignoredExceptions = nextLocal++;
        CallSiteReference addRef = CallSiteReference.make(getNextProgramCounter(), addMethod, IInvokeInstruction.Dispatch.INTERFACE);
        SSAInvokeInstruction addCall = new SSAInvokeInstruction(ignoredResult, new int[] { result3, result2 }, ignoredExceptions,
            addRef);
        addStatement(addCall);

        SSAReturnInstruction r = new SSAReturnInstruction(result3, false);
        addStatement(r);
      } else if (rType.equals(TypeReference.JavaUtilEnum)) {
        int result3 = nextLocal++;
        NewSiteReference ref3 = NewSiteReference.make(getNextProgramCounter(), TypeReference.JavaUtilVector);
        SSANewInstruction a3 = new SSANewInstruction(result3, ref3);
        addStatement(a3);

        int initIgnoredExceptions = nextLocal++;
        CallSiteReference initRef = CallSiteReference.make(getNextProgramCounter(), vectorInit, IInvokeInstruction.Dispatch.SPECIAL);
        addStatement(new SSAInvokeInstruction(new int[] { result3 }, initIgnoredExceptions, initRef));

        int ignoredResult = nextLocal++;
        int ignoredExceptions = nextLocal++;
        CallSiteReference addRef = CallSiteReference.make(getNextProgramCounter(), addMethod, IInvokeInstruction.Dispatch.INTERFACE);
        SSAInvokeInstruction addCall = new SSAInvokeInstruction(ignoredResult, new int[] { result3, result2 }, ignoredExceptions,
            addRef);
        addStatement(addCall);

        int result4 = nextLocal++;
        int moreIgnoredExceptions = nextLocal++;
        CallSiteReference elementsRef = CallSiteReference.make(getNextProgramCounter(), elementsMethod, IInvokeInstruction.Dispatch.VIRTUAL);
        SSAInvokeInstruction elementsCall = new SSAInvokeInstruction(result4, new int[] { result3 }, moreIgnoredExceptions,
            elementsRef);
        addStatement(elementsCall);

        SSAReturnInstruction r = new SSAReturnInstruction(result4, false);
        addStatement(r);
      } else {
        int xobj = nextLocal++;
        addStatement(new SSANewInstruction(xobj, NewSiteReference.make(getNextProgramCounter(), ObjectNotFoundExceptionClass)));
        addStatement(new SSAThrowInstruction(xobj));

        SSAReturnInstruction r = new SSAReturnInstruction(result2, false);
        addStatement(r);
      }

      int xobj2 = nextLocal++;
      addStatement(new SSANewInstruction(xobj2, NewSiteReference.make(getNextProgramCounter(), FinderExceptionClass)));
      addStatement(new SSAThrowInstruction(xobj2));

      int xobj3 = nextLocal++;
      addStatement(new SSANewInstruction(xobj3, NewSiteReference.make(getNextProgramCounter(), EJBExceptionClass)));
      addStatement(new SSAThrowInstruction(xobj3));

    }
  }

  private class RemoteFinderSummary extends LocalHomeFinderSummary {
    public RemoteFinderSummary(final MethodReference method) {
      super(method);

      int xobj = nextLocal++;
      addStatement(new SSANewInstruction(xobj, NewSiteReference.make(getNextProgramCounter(), RemoteExceptionClass)));
      addStatement(new SSAThrowInstruction(xobj));

    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.cha.MethodBypass#getBypass(com.ibm.wala.classLoader.MethodReference)
   */
  public IMethod getCalleeTarget(CGNode N, CallSiteReference site, IClass receiver) {
    MethodReference m = site.getDeclaredTarget();

    // If the declared target is something generic like EJBObject, perform
    // type inference to try and resolve a more specific bean receiver for
    // this call.
    if (isEJBSuperInterface(m.getDeclaringClass())) {
      IClass inferred = getReceiverClassFromTypeInference(N, site);
      if (inferred != null) {
        receiver = inferred;
      }
    }

    IClass servlet = cha.lookupClass(ServletEntrypoints.Servlet);
    // special logic for MDB onMessage entrypoints
    if (FakeRootMethod.isFakeRootMethod(N.getMethod().getReference())) {
      if (deployment.isMessageDriven(m.getDeclaringClass())) {
        if (m.getName().equals(onMessageAtom) && m.getDescriptor().equals(onMessageDesc)) {
          return hijackOnMessageEntrypoint(m);
        }
      }
      // special logic for servlet entrypoints
      else if (receiver != null && cha.implementsInterface(receiver, servlet)) {
        IMethod resolved = cha.resolveMethod(receiver, m.getSelector());
        if (!resolved.isInit() && !resolved.isClinit()) {
          return hijackServletEntrypoint(m);
        }
      }
    }

    // encode the receiver type in the method reference m
    m = specializeForReceiverType(receiver, m);

    // first try to intercept the call before delegating
    // [SJF]: I don't think this should be necessary ... let the delegate go ...
    // we'll clean up later anyway.
    // update ... it seems this is necessary to pass the AutoProfile
    // ATKTest ... leave it in for now.
    SyntheticMethod X = methodReferenceIntercept(m);
    if (X != null) {
      return X;
    }

    // now delegate
    IMethod target = parent.getCalleeTarget(N, site, receiver);
    if (target == null) {
      return null;
    }

    // delegation is done ... try to intercept now.
    X = methodReferenceIntercept(target.getReference());
    if (X != null) {
      return X;
    } else {
      return target;
    }
  }

  /**
   * @param receiver
   * @param m
   * @return a method reference based on m, but encoding the receiver as the
   *         declared class ... or m is receiver is null
   */
  private MethodReference specializeForReceiverType(IClass receiver, MethodReference m) {
    // create a MethodReference m which encodes the receiver type.
    if (receiver != null) {
      if (receiver.getClassLoader().getReference().equals(scope.getSyntheticLoader())) {
        if (receiver instanceof BypassSyntheticClass)
          return MethodReference.findOrCreate(((BypassSyntheticClass) receiver).getRealType().getReference(), m.getName(), m
              .getDescriptor());
        else
          return MethodReference.findOrCreate(receiver.getReference(), m.getName(), m.getDescriptor());
      } else {
        return MethodReference.findOrCreate(receiver.getReference(), m.getName(), m.getDescriptor());
      }
    }
    return m;
  }

  /**
   * @param N
   *          governing node
   * @param site
   *          a call to something generic like EJBObject
   * @return an estimate for the receiver class based on local type inference,
   *         or null if type inference doesn't help
   */
  private IClass getReceiverClassFromTypeInference(CGNode N, CallSiteReference site) {
    ReceiverTypeInference R = typeInference.findOrCreate(N);
    TypeAbstraction type = null;
    if (R != null) {
      type = R.getReceiverType(site);
    }
    if (type == null) {
      // Type inference failed; raise a severe warning
      Warnings.add(ResolutionFailure.create(N, site));
      return null;
    } else {
      // Type inference succeeded; modify m to reflect the more specific
      // receiver
      if (type instanceof PointType) {
        return ((PointType) type).getType();
      } else if (type instanceof ConeType) {
        return ((ConeType) type).getType();
      } else {
        Assertions.UNREACHABLE("Unexpected type" + type);
        return null;
      }
    }
  }

  /**
   * @param T
   * @return true iff T is one of the EJB Superinterfaces defined in J2EEUtil
   */
  private boolean isEJBSuperInterface(TypeReference T) {
    TypeName tName = T.getName();
    Atom pack = tName.getPackage();
    if (pack == null || !pack.equals(JAVAX_EJB)) {
      return false;
    }

    IClass klass = cha.lookupClass(T);
    if (klass == null) {
      return false;
    } else {
      TypeReference k = klass.getReference();
      return (k.equals(J2EEUtil.EJB_HOME) || k.equals(J2EEUtil.EJB_LOCAL_HOME) || k.equals(J2EEUtil.EJB_LOCAL_OBJECT) || k
          .equals(J2EEUtil.EJB_OBJECT));
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.MethodTargetSelector#mightReturnSyntheticMethod(com.ibm.wala.ipa.callgraph.CGNode,
   *      com.ibm.wala.classLoader.CallSiteReference)
   */
  public boolean mightReturnSyntheticMethod(CGNode caller, CallSiteReference site) {
    // TODO optimize this!
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.ipa.callgraph.MethodTargetSelector#mightReturnSyntheticMethod(com.ibm.wala.types.MethodReference)
   */
  public boolean mightReturnSyntheticMethod(MethodReference declaredTarget) {
    // TODO optimize this!
    return true;
  }
}
