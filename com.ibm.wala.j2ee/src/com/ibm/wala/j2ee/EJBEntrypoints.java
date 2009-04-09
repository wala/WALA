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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jem.java.Method;
import org.eclipse.jst.j2ee.commonarchivecore.internal.Archive;
import org.eclipse.jst.j2ee.commonarchivecore.internal.EARFile;
import org.eclipse.jst.j2ee.commonarchivecore.internal.EJBJarFile;
import org.eclipse.jst.j2ee.ejb.EJBJar;
import org.eclipse.jst.j2ee.ejb.EnterpriseBean;
import org.eclipse.jst.j2ee.ejb.MessageDriven;

import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.NewSiteReference;
import com.ibm.wala.ipa.callgraph.Entrypoint;
import com.ibm.wala.ipa.callgraph.impl.DefaultEntrypoint;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.MemberReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.strings.ImmutableByteArray;
import com.ibm.wala.util.warnings.Warnings;

/**
 * 
 * This class provides an enumeration of EJB methods as listed in EJB jar file
 * deployment descriptors
 * 
 * @author sfink
 */
public class EJBEntrypoints implements Iterable<Entrypoint>, EJBConstants {
  static final boolean DEBUG = false;

  private final IClassHierarchy cha;

  /**
   * The set of Entrypoint for call graph construction.
   */
  private List<Entrypoint> entrypoints = new LinkedList<Entrypoint>();

  /**
   * Should we include only MDB entrypoints
   */
  private final boolean JUST_MDBS;

  /**
   * Method names in the Entity Bean interface
   */
  private final static Map<Atom, ImmutableByteArray> entityBeanMethodNames = makeEntityBeanMethodMap();

  private final static Map<Atom, ImmutableByteArray> makeEntityBeanMethodMap() {
    HashMap<Atom, ImmutableByteArray> result = HashMapFactory.make(10);
    result.put(ejbActivateAtom, new ImmutableByteArray(ejbActivateSig));
    result.put(ejbLoadAtom, new ImmutableByteArray(ejbLoadSig));
    result.put(ejbPassivateAtom, new ImmutableByteArray(ejbPassivateSig));
    result.put(ejbRemoveAtom, new ImmutableByteArray(ejbRemoveSig));
    result.put(ejbStoreAtom, new ImmutableByteArray(ejbStoreSig));
    result.put(setEntityContextAtom, new ImmutableByteArray(setEntityContextSig));
    result.put(unsetEntityContextAtom, new ImmutableByteArray(unsetEntityContextSig));
    return result;
  }

  /**
   * Method names in the Session Bean interface
   */
  private final static Map<Atom, ImmutableByteArray> sessionBeanMethodNames = makeSessionBeanMethodMap();

  private final static Map<Atom, ImmutableByteArray> makeSessionBeanMethodMap() {
    HashMap<Atom, ImmutableByteArray> result = HashMapFactory.make(5);
    result.put(ejbActivateAtom, new ImmutableByteArray(ejbActivateSig));
    result.put(ejbPassivateAtom, new ImmutableByteArray(ejbPassivateSig));
    result.put(ejbRemoveAtom, new ImmutableByteArray(ejbRemoveSig));
    result.put(setSessionContextAtom, new ImmutableByteArray(setSessionContextSig));
    return result;
  }

  /**
   * Governing deployment descriptor information
   */
  private DeploymentMetaData deployment;

  private final J2EEClassTargetSelector classTargetSelector;

  /**
   * Create the set of EJB entrypoints that are defined in an analysis scope
   * 
   * @param scope
   *          representation of the analysis scope.
   */
  @SuppressWarnings({ "unchecked" })
  public EJBEntrypoints(IClassHierarchy cha, J2EEAnalysisScope scope, DeploymentMetaData deployment, boolean justMDBs,
      J2EEClassTargetSelector classTargetSelector) {
    this.cha = cha;
    this.deployment = deployment;
    this.JUST_MDBS = justMDBs;
    this.classTargetSelector = classTargetSelector;
    ClassLoaderReference loader = scope.getApplicationLoader();

    addEntrypointForSyntheticContainer();
    for (Iterator<Module> i = scope.getModules(loader).iterator(); i.hasNext();) {
      Module M = (Module) i.next();
      // get the file as an EJB Jar archive
      Archive archive = J2EEUtil.getArchive(M);
      if (archive.isEJBJarFile()) {
        addEntrypointsForEJBJarFile(scope, loader, archive);
      } else if (archive.isEARFile()) {
        EARFile ear = (EARFile) archive;
        for (Iterator<EJBJarFile> it = ear.getEJBJarFiles().iterator(); it.hasNext();) {
          EJBJarFile j = (EJBJarFile) it.next();
          addEntrypointsForEJBJarFile(scope, loader, j);
        }
      }
    }
  }

  /**
   * Add an entrypoint which initializes the J2EE Container model
   */
  private void addEntrypointForSyntheticContainer() {
    final J2EEContainerModel klass = new J2EEContainerModel(deployment, cha);
    entrypoints.add(new DefaultEntrypoint(klass.getClassInitializer().getReference(), cha) {
      public TypeReference[] getParameterTypes(int i) {
        if (i == 0) {
          return new TypeReference[] { classTargetSelector.getAllocatedTarget(null, NewSiteReference.make(0, klass.getReference()))
              .getReference() };
        } else {
          Assertions.UNREACHABLE();
          return null;
        }
      }
    });
  }

  /**
   * @param scope
   *          analysis scope
   * @param loader
   *          application class loader
   * @param archive
   *          WCCM representation of EJB jar file.
   */
  @SuppressWarnings({ "unchecked" })
  private void addEntrypointsForEJBJarFile(J2EEAnalysisScope scope, ClassLoaderReference loader, Archive archive) {
    // extract the deployment descriptor
    EJBJar DD = ((EJBJarFile) archive).getDeploymentDescriptor();

    // iterate over each bean described
    for (Iterator bi = DD.getEnterpriseBeans().iterator(); bi.hasNext();) {
      EnterpriseBean b = (EnterpriseBean) bi.next();

      if (b.hasRemoteClient() && !JUST_MDBS) {
        // add each method in the remote interface as an entrypoint
        addRemoteMethods(b, b.getRemoteMethodsForDeployment(), loader);
        // add each method in the home interface as an entrypoint
        addHomeMethods(b, b.getHomeMethodsForDeployment(), loader);

        if (scope.useEJBLifecycleEntrypoints()) {
          addEJBLifecycleEntrypoints(b, loader);
        }
      } else if (b.isMessageDriven()) {
        addMessageDestination((MessageDriven) b, loader);
      }
    }

    // prune the set of entrypoints based on transaction declarations
    pruneEntrypointsByTransactions(archive, loader);
  }

  /**
   * @param b
   * @param loader
   */
  private void addMessageDestination(MessageDriven b, ClassLoaderReference loader) {

    TypeReference T = J2EEUtil.ejb2TypeReference(b, loader);
    MethodReference e = MethodReference.findOrCreate(T, onMessageAtom, onMessageDesc);
    if (DEBUG) {
      System.err.println(("Add entrypoint: " + e + " from bean " + b));
    }
    IMethod m = cha.resolveMethod(e);
    if (m == null) {
      Warnings.add(LoadFailure.create(e));
      return;
    }
    entrypoints.add(new MDBEntrypoint(m, cha, T));
  }

  /**
   * Remove any entrypoints that are marked as MANDATORY transactions; we can't
   * call these from the outside world without raising an exception.
   * 
   * TODO: this algorithm is inefficient; but it shouldn't matter, since this is
   * a one-time cost on a small set, I hope. If this is a problem, represent the
   * entrypoints as a Map from MethodReference -> Entrypoint, to allow constant
   * time deletions.
   */
  private void pruneEntrypointsByTransactions(Archive A, ClassLoaderReference loader) {
    Set<DeploymentDeclaredTransaction> S = TransactionUtil.createDeclaredTransactionEntries(A, loader);
    Set<MethodReference> M = HashSetFactory.make(); // set of MethodReferences to prune.
    for (Iterator<DeploymentDeclaredTransaction> it = S.iterator(); it.hasNext();) {
      DeploymentDeclaredTransaction X = (DeploymentDeclaredTransaction) it.next();
      if (X.isMandatory()) {
        M.add(X.getMethodReference());
      }
    }
    // walk through the list of entrypoints and delete any in set M.
    Set<Entrypoint> toRemove = HashSetFactory.make();
    for (Iterator<Entrypoint> it = entrypoints.iterator(); it.hasNext();) {
      Entrypoint E = it.next();
      MemberReference m = E.getMethod().getReference();
      if (M.contains(m)) {
        toRemove.add(E);
      }
    }
    entrypoints.removeAll(toRemove);
  }

  /**
   * @param b
   * @param methods
   * @param loader
   */
  private void addRemoteMethods(EnterpriseBean b, Method[] methods, ClassLoaderReference loader) {
    TypeReference T = J2EEUtil.ejb2TypeReference(b, loader);
    BeanMetaData bean = deployment.getBeanMetaData(T);

    TypeReference remoteInterface = bean.getRemoteInterface();
    addMethods(bean, remoteInterface, methods, loader);
  }

  /**
   * @param b
   * @param methods
   * @param loader
   */
  private void addHomeMethods(EnterpriseBean b, Method[] methods, ClassLoaderReference loader) {
    TypeReference T = J2EEUtil.ejb2TypeReference(b, loader);
    BeanMetaData bean = deployment.getBeanMetaData(T);
    TypeReference remoteInterface = bean.getHomeInterface();
    addMethods(bean, remoteInterface, methods, loader);
  }

  /**
   * @param b
   */
  private void addEJBLifecycleEntrypoints(EnterpriseBean b, ClassLoaderReference loader) {
    TypeReference type = J2EEUtil.ejb2TypeReference(b, loader);

    Iterator<Map.Entry<Atom, ImmutableByteArray>> i = b.isSession() ? sessionBeanMethodNames.entrySet().iterator() : entityBeanMethodNames.entrySet().iterator();
    while (i.hasNext()) {
      Map.Entry<Atom, ImmutableByteArray> entry =  i.next();
      Atom name = (Atom) entry.getKey();
      ImmutableByteArray sig = (ImmutableByteArray) entry.getValue();
      Descriptor D = Descriptor.findOrCreate(sig);
      MethodReference e = MethodReference.findOrCreate(type, name, D);
      if (DEBUG) {
        System.err.println(("Add entrypoint: " + e + " from bean " + b));
      }
      IMethod m = cha.resolveMethod(e);
      if (m == null) {
        Warnings.add(LoadFailure.create(m));
        continue;
      }
      entrypoints.add(new EJBLifecycleEntrypoint(m, cha, type));
    }

  }

  /**
   * Add some EJB methods to the entrypoint set
   * 
   * @param bean
   * @param interfaceType
   * @param methods
   *          array of Method from the bean
   * @param loader
   */
  private void addMethods(final BeanMetaData bean, TypeReference interfaceType, Method[] methods, ClassLoaderReference loader) {
    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];
      MethodReference declaredMethod = J2EEUtil.createMethodReference(method, loader);
      MethodReference target = MethodReference
          .findOrCreate(interfaceType, declaredMethod.getName(), declaredMethod.getDescriptor());
      if (DEBUG) {
        System.err.println(("Add entrypoint: " + target + " from method list"));
      }
      final IClass klass = cha.lookupClass(interfaceType);
      if (klass == null) {
        Warnings.add(LoadFailure.create(interfaceType));
        continue;
      }
      IMethod m = cha.resolveMethod(klass, target.getSelector());
      if (m == null) {
        Warnings.add(LoadFailure.create(target));
        continue;
      }
      entrypoints.add(new DefaultEntrypoint(m, cha) {
        public TypeReference[] getParameterTypes(int i) {
          if (i == 0) {
            return new TypeReference[] { classTargetSelector.getAllocatedTarget(null,
                NewSiteReference.make(0, klass.getReference())).getReference() };
          } else {
            return super.getParameterTypes(i);
          }
        }
      });
    }
  }

  public Iterator<Entrypoint> iterator() {
    return entrypoints.iterator();
  }

  /**
   * @see java.lang.Object#toString()
   */
  public String toString() {
    String result = "";
    for (Iterator<Entrypoint> i = entrypoints.iterator(); i.hasNext();) {
      result += i.next() + "\n";
    }
    return result;
  }
}