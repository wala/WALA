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
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.jst.j2ee.commonarchivecore.internal.Archive;
import org.eclipse.jst.j2ee.commonarchivecore.internal.EARFile;
import org.eclipse.jst.j2ee.commonarchivecore.internal.EJBJarFile;
import org.eclipse.jst.j2ee.ejb.CMRField;
import org.eclipse.jst.j2ee.ejb.EJBJar;
import org.eclipse.jst.j2ee.ejb.EJBRelation;
import org.eclipse.jst.j2ee.ejb.EJBRelationshipRole;
import org.eclipse.jst.j2ee.ejb.EnterpriseBean;

import com.ibm.wala.classLoader.Module;
import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.debug.Trace;

/**
 * 
 * A simple implementation of the DeploymentMetaData interface
 * 
 * @author sfink
 */
public class DeploymentMetaDataImpl implements DeploymentMetaData {

  static final boolean DEBUG = false;
  /**
   * A mapping from type reference for a bean to the BeanMetaData object that
   * describes the bean.
   */
  private HashMap<TypeReference, BeanMetaData> entities = HashMapFactory.make();

  /**
   * A mapping from type reference for a bean to the BeanMetaData object that
   * describes the bean.
   */
  private HashMap<TypeReference, BeanMetaData> sessions = HashMapFactory.make();

  /**
   * A mapping from type reference for a bean to the BeanMetaData object that
   * describes the bean.
   */
  private HashMap<TypeReference, BeanMetaData> MDBs = HashMapFactory.make();

  /**
   * A mapping from type reference for a bean to the BeanMetaData object that
   * describes the bean.
   */
  private HashMap<TypeReference, BeanMetaDataImpl> allBeans = HashMapFactory.make();

  /**
   * A mapping from type reference for a remote interface to the bean that
   * implements it.
   */
  private HashMap<TypeReference, BeanMetaData> remote2Bean = HashMapFactory.make();

  /**
   * A mapping from type reference for a home interface to the bean that
   * implements it.
   */
  private HashMap<TypeReference, BeanMetaData> home2Bean = HashMapFactory.make();
  /**
   * A mapping from type reference for a local interface to the bean that
   * implements it.
   */
  private HashMap<TypeReference, BeanMetaData> local2Bean = HashMapFactory.make();
  /**
   * A mapping from type reference for a local home interface to the bean that
   * implements it.
   */
  private HashMap<TypeReference, BeanMetaData> localHome2Bean = HashMapFactory.make();

  /**
   * All container-generated getters and setters; a mapping from MethodReference ->
   * FieldReference
   */
  private HashMap<Object, FieldReference> getters = HashMapFactory.make();
  private HashMap<Object, FieldReference> setters = HashMapFactory.make();

  /**
   * All finder methods; a mapping from MethodReference -> Bean
   */
  private HashMap<MethodReference, BeanMetaData> finder2Bean = HashMapFactory.make();

  /**
   * All CMR Getters as a mapping from MethodReference to FieldReference
   */
  private HashMap<MethodReference, FieldReference> cmrGetters = HashMapFactory.make();

  /**
   * All CMR Setters as a mapping from MethodReference to FieldReference
   */
  private HashMap<MethodReference, FieldReference> cmrSetters = HashMapFactory.make();

  /**
   * For CMR fields, a mapping from FieldReference -> BeanMetaData
   */
  private HashMap<FieldReference, BeanMetaData> cmrField2Bean = HashMapFactory.make();

  /**
   * For CMR fields, a mapping from FieldReference -> EJBRelationshipRole
   */
  private HashMap<FieldReference, EJBRelationshipRole> cmrField2Role = HashMapFactory.make();

  /**
   * A mapping between opposite fields in CMRs
   */
  private HashMap<FieldReference, FieldReference> oppositeFields = HashMapFactory.make();

  /**
   * Method DeploymentMetaDataImpl.
   * 
   * @param scope
   *          the analysis scope which defines the EJB jar files to analyze
   */
  @SuppressWarnings({ "restriction", "unchecked" })
  public DeploymentMetaDataImpl(AnalysisScope scope) {
    ClassLoaderReference loader = scope.getApplicationLoader();

    for (Iterator<Module> i = scope.getModules(loader).iterator(); i.hasNext();) {
      Module module = (Module) i.next();
      // get the file as an EJB Jar archive
      Archive archive = J2EEUtil.getArchive(module);
      if (archive != null) {
        if (archive.isEJBJarFile()) {
          processEJBJarFile(loader, (EJBJarFile) archive);
        } else if (archive.isEARFile()) {
          for (Iterator<EJBJarFile> it = ((EARFile) archive).getEJBJarFiles().iterator(); it.hasNext();) {
            EJBJarFile j = (EJBJarFile) it.next();
            processEJBJarFile(loader, j);
          }
        }
      }
    }
  }

  /**
   * @param loader
   *          governing class loader for the application
   * @param archive
   *          WCCM object which holds the ejb jarfile
   */
  @SuppressWarnings({ "restriction", "unchecked" })
  private void processEJBJarFile(ClassLoaderReference loader, EJBJarFile archive) {
    // extract the deployment descriptor
    EJBJar DD = null;
    try {
      DD = archive.getDeploymentDescriptor();
    } catch (Throwable e) {
      e.printStackTrace();
      Assertions.UNREACHABLE("Failed to load deployment descriptor for " + archive);
    }

    // add each bean to the bean map
    for (Iterator bi = DD.getEnterpriseBeans().iterator(); bi.hasNext();) {
      EnterpriseBean b = (EnterpriseBean) bi.next();
      TypeReference c = J2EEUtil.ejb2TypeReference(b, loader);

      BeanMetaDataImpl bmd = new BeanMetaDataImpl(b, DD, c);
      allBeans.put(c, bmd);
      if (bmd.isContainerManaged()) {
        entities.put(c, bmd);
      } else if (bmd.isSessionBean()) {
        sessions.put(c, bmd);
      } else if (bmd.isMessageDrivenBean()) {
        MDBs.put(c, bmd);
      } else if (bmd.isBeanManaged()) {
        entities.put(c, bmd);
      } else {
        Assertions.UNREACHABLE("unexpected bean type" + bmd);
      }

      String homeName = b.getHomeInterfaceName();
      mapEJBInterface(loader, bmd, homeName, home2Bean);

      String remoteName = b.getRemoteInterfaceName();
      mapEJBInterface(loader, bmd, remoteName, remote2Bean);

      String localName = b.getLocalInterfaceName();
      mapEJBInterface(loader, bmd, localName, local2Bean);

      String localHomeName = b.getLocalHomeInterfaceName();
      mapEJBInterface(loader, bmd, localHomeName, localHome2Bean);
    }

    // cache the set of getter and setter methods
    computeGettersAndSetters();
    computeFinders();
    computeCMRs(DD, loader);
  }

  /**
   * Method mapEJBInterface.
   * 
   * @param loader
   *          the interface's defining class loader
   * @param bmd
   *          data about the bean
   * @param iName
   *          name of the interface
   * @param iMap
   *          mapping from TypeReferece -> BeanMetaData
   */
  private void mapEJBInterface(ClassLoaderReference loader, BeanMetaData bmd, String iName, HashMap<TypeReference, BeanMetaData> iMap) {

    if (iName == null)
      return;
    TypeReference iFace = J2EEUtil.getTypeForInterface(loader, iName);

    // here's a bit of a kludge .. if there's a choice of beans for an
    // interface,
    // choose the last CMP implementation discovered
    BeanMetaData old = iMap.get(iFace);
    if (old != null) {
      if (old.isContainerManaged()) {
        // don't overwrite
        return;
      }
    }

    iMap.put(iFace, bmd);
  }


  public BeanMetaData getBeanMetaData(TypeReference type) {
    return allBeans.get(type);
  }

  /**
   * @see com.ibm.wala.j2ee.DeploymentMetaData#getAllCMPFields()
   */
  public Set<FieldReference> getAllCMPFields() {
    HashSet<FieldReference> result = HashSetFactory.make();
    for (Iterator<Map.Entry<TypeReference, BeanMetaData>> i = entities.entrySet().iterator(); i.hasNext();) {
      Map.Entry<TypeReference,BeanMetaData> entry = (Map.Entry<TypeReference,BeanMetaData>) i.next();
      BeanMetaData bean = (BeanMetaData) entry.getValue();
      result.addAll(bean.getCMPFields());
    }
    return result;
  }

  /**
   * Return a Set of container managed relationship (cmr) fields.
   * 
   * @see com.ibm.wala.j2ee.DeploymentMetaData#getAllCMRFields()
   * @return Set of container managed relationship fields.
   */
  public Set<Object> getAllCMRFields() {
    HashSet<Object> result = HashSetFactory.make();
    for (Iterator<Map.Entry<TypeReference, BeanMetaData>> i = entities.entrySet().iterator(); i.hasNext();) {
      Map.Entry<TypeReference,BeanMetaData> entry = (Map.Entry<TypeReference,BeanMetaData>) i.next();
      BeanMetaData bean = (BeanMetaData) entry.getValue();
      result.addAll(bean.getCMRFields());
    }
    return result;
  }

  private void computeGettersAndSetters() {
    for (Iterator<Map.Entry<TypeReference, BeanMetaData>> i = entities.entrySet().iterator(); i.hasNext();) {
      Map.Entry<TypeReference, BeanMetaData> entry = (Map.Entry<TypeReference, BeanMetaData>) i.next();
      BeanMetaData bean = (BeanMetaData) entry.getValue();
      getters.putAll(bean.getGetterMethods());
      setters.putAll(bean.getSetterMethods());
    }
  }

  /**
   * Set up the finder2Bean relation, which maps each finder method to its bean
   */
  private void computeFinders() {
    for (Iterator<Map.Entry<TypeReference, BeanMetaData>> i = entities.entrySet().iterator(); i.hasNext();) {
      Map.Entry<TypeReference, BeanMetaData> entry = (Map.Entry<TypeReference, BeanMetaData>) i.next();
      BeanMetaData bean = (BeanMetaData) entry.getValue();
      for (Iterator<MethodReference> j = bean.getFinders().iterator(); j.hasNext();) {
        MethodReference m = j.next();
        if (DEBUG) {
          Trace.println("Found finder " + m);
        }
        finder2Bean.put(m, bean);
      }
    }
  }

  /**
   * Set up mappings for CMRs
   * <ul>
   * <li>set up mapping from MethodReference -> FieldReference for CMR getters
   * <li>set up mapping from FieldReference -> BeanMetaData for CMR Fields
   * </ul>
   */
  @SuppressWarnings("unchecked")
  private void computeCMRs(EJBJar DD, ClassLoaderReference loader) {
    for (Iterator<BeanMetaData> i = entities.values().iterator(); i.hasNext();) {
      BeanMetaData bean = i.next();
      cmrGetters.putAll(bean.getCMRGetters());
      cmrSetters.putAll(bean.getCMRSetters());
    }
    if (DD.getEjbRelations() != null) {
      for (Iterator<EJBRelation> it = DD.getEjbRelations().iterator(); it.hasNext();) {
        EJBRelation relation = (EJBRelation) it.next();
        EJBRelationshipRole firstRole = relation.getFirstRole();
        EJBRelationshipRole secondRole = relation.getSecondRole();
        CMRField firstField = firstRole.getCmrField();
        FieldReference f1 = null;
        FieldReference f2 = null;
        if (firstField != null) {
          EnterpriseBean sourceBean = firstRole.getSourceEntity();
          TypeReference b = J2EEUtil.ejb2TypeReference(sourceBean, loader);
          BeanMetaData sourceBeanData = entities.get(b);
          f1 = sourceBeanData.getField(firstField);
          EnterpriseBean otherBean = secondRole.getSourceEntity();
          TypeReference o = J2EEUtil.ejb2TypeReference(otherBean, loader);
          cmrField2Bean.put(f1, entities.get(o));
          cmrField2Role.put(f1, firstRole);
        }
        CMRField secondField = secondRole.getCmrField();
        if (secondField != null) {
          EnterpriseBean sourceBean = secondRole.getSourceEntity();
          TypeReference b = J2EEUtil.ejb2TypeReference(sourceBean, loader);
          BeanMetaData sourceBeanData = entities.get(b);
          f2 = sourceBeanData.getField(secondField);
          EnterpriseBean otherBean = firstRole.getSourceEntity();
          TypeReference o = J2EEUtil.ejb2TypeReference(otherBean, loader);
          cmrField2Bean.put(f2, entities.get(o));
          cmrField2Role.put(f2, secondRole);
        }
        if (firstField != null && secondField != null) {
          oppositeFields.put(f1, f2);
          oppositeFields.put(f2, f1);
        }
      }
    }
  }

  /**
   * @see com.ibm.wala.j2ee.DeploymentMetaData#isHomeInterface(TypeReference)
   */
  public boolean isHomeInterface(TypeReference t) {
    return home2Bean.keySet().contains(t);
  }

  /**
   * @see com.ibm.wala.j2ee.DeploymentMetaData#isRemoteInterface(TypeReference)
   */
  public boolean isRemoteInterface(TypeReference t) {
    return remote2Bean.keySet().contains(t);
  }

  /**
   * @see com.ibm.wala.j2ee.DeploymentMetaData#isHomeInterface(TypeReference)
   */
  public boolean isLocalInterface(TypeReference t) {
    return local2Bean.keySet().contains(t);
  }

  /**
   * @see com.ibm.wala.j2ee.DeploymentMetaData#isRemoteInterface(TypeReference)
   */
  public boolean isLocalHomeInterface(TypeReference t) {
    return localHome2Bean.keySet().contains(t);
  }

  public boolean isEJBInterface(TypeReference t) {
    return isHomeInterface(t) || isRemoteInterface(t) || isLocalInterface(t) || isLocalHomeInterface(t);
  }

  /**
   * @see com.ibm.wala.j2ee.DeploymentMetaData#getBeanForInterface(TypeReference)
   */
  public BeanMetaData getBeanForInterface(TypeReference t) {
    BeanMetaData bmd = remote2Bean.get(t);
    if (bmd == null) {
      bmd = home2Bean.get(t);
    }
    if (bmd == null) {
      bmd = local2Bean.get(t);
    }
    if (bmd == null) {
      bmd = localHome2Bean.get(t);
    }

    return bmd;
  }

  public TypeReference getCMPType(TypeReference t) {
    BeanMetaData bmd = entities.get(t);
    return (bmd == null) ? null : bmd.getEJBClass();
  }

  /**
   * @see com.ibm.wala.j2ee.DeploymentMetaData#isContainerManaged(TypeReference)
   */
  public boolean isContainerManaged(TypeReference t) {
    BeanMetaData bmd = allBeans.get(t);
    if (bmd == null) {
      return false;
    }
    return bmd.isContainerManaged();
  }

  /**
   * @see com.ibm.wala.j2ee.DeploymentMetaData#getCMPField(MethodReference)
   */
  public FieldReference getCMPField(MethodReference mr) {
    if (getters.get(mr) != null) {
      return getters.get(mr);
    } else {
      return setters.get(mr);
    }
  }

  /**
   * @see com.ibm.wala.j2ee.DeploymentMetaData#isCMPGetter(MethodReference)
   */
  public boolean isCMPGetter(MethodReference mr) {
    return getters.keySet().contains(mr);
  }

  /**
   * @see com.ibm.wala.j2ee.DeploymentMetaData#isCMPSetter(MethodReference)
   */
  public boolean isCMPSetter(MethodReference mr) {
    return setters.keySet().contains(mr);
  }

  /**
   * @see com.ibm.wala.j2ee.DeploymentMetaData#getAllFinders()
   */
  public Collection<MethodReference> getAllFinders() {
    return finder2Bean.keySet();
  }

  /**
   * @see com.ibm.wala.j2ee.DeploymentMetaData#getAllCMRGetters()
   */
  public Map<MethodReference, FieldReference> getAllCMRGetters() {
    return cmrGetters;
  }

  /**
   * @see com.ibm.wala.j2ee.DeploymentMetaData#getCMRBean(FieldReference)
   */
  public BeanMetaData getCMRBean(FieldReference field) {
    return cmrField2Bean.get(field);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.j2ee.DeploymentMetaData#getFinderBeanType(com.ibm.wala.classLoader.MethodReference)
   */
  public TypeReference getFinderBeanType(MethodReference method) {
    BeanMetaData bean = finder2Bean.get(method);
    if (Assertions.verifyAssertions) {
      Assertions._assert(bean != null);
    }
    return bean.getEJBClass();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.j2ee.DeploymentMetaData#isFinder(com.ibm.wala.classLoader.MethodReference)
   */
  public boolean isFinder(MethodReference ref) {
    if (DEBUG) {
      boolean result = finder2Bean.keySet().contains(ref);
      Trace.println("isFinder ? " + ref + " " + result);
    }
    return finder2Bean.keySet().contains(ref);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.j2ee.DeploymentMetaData#iterateEntities()
   */
  public Iterator<BeanMetaData> iterateEntities() {
    return entities.values().iterator();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.j2ee.DeploymentMetaData#iterateSessions()
   */
  public Iterator<BeanMetaData> iterateSessions() {
    return sessions.values().iterator();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.j2ee.DeploymentMetaData#iterateSessions()
   */
  public Iterator<BeanMetaData> iterateMDBs() {
    return MDBs.values().iterator();
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.j2ee.DeploymentMetaData#isCMRGetter(com.ibm.wala.classLoader.MethodReference)
   */
  public boolean isCMRGetter(MethodReference method) {
    return cmrGetters.keySet().contains(method);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.j2ee.DeploymentMetaData#isCMRGetter(com.ibm.wala.classLoader.MethodReference)
   */
  public boolean isCMRSetter(MethodReference method) {
    return cmrSetters.keySet().contains(method);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.j2ee.DeploymentMetaData#getOppositeField(com.ibm.wala.classLoader.FieldReference)
   */
  public FieldReference getOppositeField(FieldReference field) {
    return oppositeFields.get(field);
  }

  /*
   * (non-Javadoc)
   * 
   * @see com.ibm.wala.j2ee.DeploymentMetaData#getCMRRole(com.ibm.wala.classLoader.FieldReference)
   */
  public EJBRelationshipRole getCMRRole(FieldReference field) {
    return cmrField2Role.get(field);
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.j2ee.DeploymentMetaData#isMessageDriven(com.ibm.wala.classLoader.TypeReference)
   */
  public boolean isMessageDriven(TypeReference type) {
    return MDBs.containsKey(type);
  }
}