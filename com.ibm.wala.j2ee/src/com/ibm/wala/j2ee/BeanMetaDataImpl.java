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
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jem.java.JavaClass;
import org.eclipse.jem.java.Method;
import org.eclipse.jst.j2ee.ejb.CMPAttribute;
import org.eclipse.jst.j2ee.ejb.CMRField;
import org.eclipse.jst.j2ee.ejb.ContainerManagedEntity;
import org.eclipse.jst.j2ee.ejb.EJBJar;
import org.eclipse.jst.j2ee.ejb.EnterpriseBean;
import org.eclipse.jst.j2ee.ejb.Entity;
import org.eclipse.jst.j2ee.ejb.Session;
import org.eclipse.jst.j2ee.ejb.TransactionType;

import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.Atom;
import com.ibm.wala.util.StringStuff;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.debug.Assertions;

/**
 *
 * Simple implementation of the BeanMetaData interface.
 * 
 * TODO: rework this class to have a hierarchy to distinguish between entities and
 * other beans.
 * 
 * @author sfink
 */
public class BeanMetaDataImpl implements BeanMetaData {

  /**
   * Meta-data describing this enterprise bean
   */
  private final EnterpriseBean bean;

  /**
   * Meta-data describing this enterprise bean's conatiner
   */
  private final EJBJar ejbJar;

  /**
   * Reference to Java class information for this bean
   */
  private final TypeReference klass;

  /**
   * Reference to home interface type
   */
  private TypeReference homeInterface;

  /**
   * Reference to local home interface type
   */
  private TypeReference localHomeInterface;

  /**
   * Reference to remote interface type
   */
  private TypeReference remoteInterface;

  /**
   * Reference to local interface type
   */
  private TypeReference localInterface;

  /**
   * Mapping from field name (Atom) to FieldReference for all
   * CMP fields in this bean.
   */
  private Map<Object, FieldReference> cmpFields;

  /**
   * Constructor BeanMetaDataImpl.
   * @param b
   */
  BeanMetaDataImpl(EnterpriseBean b, EJBJar ejbJar, TypeReference cl) {
    this.bean = b;
    this.klass = cl;
    this.ejbJar = ejbJar;
    computeEJBInterfaces();
    computeCMPFields();
  }

  /**
   * initialized the cached references to EJB interfaces for this bean.
   */
  private void computeEJBInterfaces() {
    String homeName = bean.getHomeInterfaceName();
    if (homeName != null) {
      homeInterface = J2EEUtil.getTypeForInterface(klass.getClassLoader(), homeName);
    }

    String localHomeName = bean.getLocalHomeInterfaceName();
    if (localHomeName != null) {
      localHomeInterface = J2EEUtil.getTypeForInterface(klass.getClassLoader(), localHomeName);
    }

    String remoteName = bean.getRemoteInterfaceName();
    if (remoteName != null) {
      remoteInterface = J2EEUtil.getTypeForInterface(klass.getClassLoader(), remoteName);
    }

    String localName = bean.getLocalInterfaceName();
    if (localName != null) {
      localInterface = J2EEUtil.getTypeForInterface(klass.getClassLoader(), localName);
    }
  }

  /**
   * compute a Set of FieldReferences, one corresponding to each
   * CMP field in this bean.
   */
  private void computeCMPFields() {
    cmpFields = Collections.emptyMap();
    if (bean instanceof ContainerManagedEntity) {
      ContainerManagedEntity cme = (ContainerManagedEntity) bean;
      cmpFields = HashMapFactory.make(3);
      addCMPAttributes(cme);
    }
  }

  @SuppressWarnings("unchecked")
  private void addCMPAttributes(ContainerManagedEntity cme) {
    List CMPFields = cme.getPersistentAttributes();
    for (Iterator<CMPAttribute> it = CMPFields.iterator(); it.hasNext();) {
      CMPAttribute att = (CMPAttribute) it.next();
      if (att.getType() == null) {
        System.err.println("PANIC: null type in attribute: " + att);
        continue;
      }
      FieldReference f = createFieldReference(att);
      cmpFields.put(f.getName(), f);
    }
    for (Iterator cmrField = cme.getCMRFields().iterator(); cmrField.hasNext();) {
      CMRField cmr = (CMRField) cmrField.next();
      if (cmr.getType() == null) {
        System.err.println("PANIC: null type in attribute: " + cmr);
        continue;
      }
      FieldReference f = createFieldReference(cmr);
      cmpFields.put(f.getName(), f);
    }
  }
  /**
  * Return a Set of FieldReferences, one corresponding to each
  * CMP field in this bean.
  */
  public Collection<FieldReference> getCMPFields() {
    return cmpFields.values();
  }

  /**
   * Return a Set of the bean's container managed relationship fields,
   * as FieldReference objects. 
   * @return the bean's CMRs.
   */
  @SuppressWarnings("unchecked")
  public Set<Object> getCMRFields() {
    Set<Object> result = Collections.emptySet();
    if (bean instanceof ContainerManagedEntity) {
      ContainerManagedEntity entity = (ContainerManagedEntity) bean;
      result = HashSetFactory.make(3);
      for (Iterator<CMRField> cmrField = entity.getCMRFields().iterator(); cmrField.hasNext();) {
        CMRField cmr = (CMRField) cmrField.next();
        if (cmr.getType() == null) {
          System.err.println("PANIC: null attribute type for " + cmr);
          continue;
        }
        FieldReference f = createFieldReference(cmr);
        result.add(f);
      }
    }
    return result;
  }

  /**
   * Return the Set of container created getter methods, as
   * MethodReference objects.  The container is responsible for creating
   * a getter method for each container managed field and relationship.
   * @see com.ibm.wala.j2ee.BeanMetaData#getGetterMethods()
   * @return Set of container created getter methods.
   */
  @SuppressWarnings("unchecked")
  public Map<Object, FieldReference> getGetterMethods() {
    Map<Object, FieldReference> result = Collections.emptyMap();
    if (bean instanceof ContainerManagedEntity) {
      ContainerManagedEntity cme = (ContainerManagedEntity) bean;
      result = HashMapFactory.make(3);
      for (Iterator cmpFields = cme.getPersistentAttributes().iterator(); cmpFields.hasNext();) {
        CMPAttribute attr = (CMPAttribute) cmpFields.next();
        if (attr.getType() == null) {
          System.err.println("PANIC: null type in attribute: " + attr);
          continue;
        }
        MethodReference m = createGetterReference(attr, attr.getGetterName());
        FieldReference f = createFieldReference(attr);
        result.put(m, f);
      }
      for (Iterator cmrFields = cme.getCMRFields().iterator(); cmrFields.hasNext();) {
        CMPAttribute attr = (CMPAttribute) cmrFields.next();
        if (attr.getType() == null) {
          System.err.println("PANIC: null type in attribute: " + attr);
          continue;
        }
        MethodReference m = createGetterReference(attr, attr.getGetterName());
        FieldReference f = createFieldReference(attr);
        result.put(m, f);
      }
    }
    return result;
  }
  /**
   * Return the Set of container created getter methods for CMRs, as
   * a mapping from MethodReference->FieldReference.  The container is responsible for creating
   * a getter method for each container managed relationship.
   */
  @SuppressWarnings("unchecked")
  public Map<MethodReference, FieldReference> getCMRGetters() {
    Map<MethodReference, FieldReference> result = Collections.emptyMap();
    if (bean instanceof ContainerManagedEntity) {
      ContainerManagedEntity cme = (ContainerManagedEntity) bean;
      result = HashMapFactory.make(3);
      for (Iterator cmrFields = cme.getCMRFields().iterator(); cmrFields.hasNext();) {
        CMPAttribute attr = (CMPAttribute) cmrFields.next();
        if (attr.getType() == null) {
          System.err.println("PANIC: null type in attribute: " + attr);
          continue;
        }
        MethodReference m = createGetterReference(attr, attr.getGetterName());
        FieldReference f = createFieldReference(attr);
        result.put(m, f);
      }
    }
    return result;
  }
  /**
   * Return the Set of container created setter methods, as
   * Method reference objects.  The container is responsible for creating
   * a setter method for each container managed field or relationship.
   * @see com.ibm.wala.j2ee.BeanMetaData#getSetterMethods()
   * @return Set of container created setter methods.
   */
  @SuppressWarnings("unchecked")
  public Map<Object, FieldReference> getSetterMethods() {
    Map<Object, FieldReference> result = Collections.emptyMap();
    if (bean instanceof ContainerManagedEntity) {
      ContainerManagedEntity cme = (ContainerManagedEntity) bean;
      result = HashMapFactory.make(3);
      for (Iterator cmpFields = cme.getPersistentAttributes().iterator(); cmpFields.hasNext();) {
        CMPAttribute attr = (CMPAttribute) cmpFields.next();
        if (attr.getType() == null) {
          System.err.println("PANIC: null type in attribute: " + attr);
          continue;
        }
        MethodReference m = createSetterReference(attr, attr.getSetterName());
        FieldReference f = createFieldReference(attr);
        result.put(m, f);
      }
      for (Iterator cmrFields = cme.getCMRFields().iterator(); cmrFields.hasNext();) {
        CMPAttribute attr = (CMPAttribute) cmrFields.next();
        if (attr.getType() == null) {
          System.err.println("PANIC: null type in attribute: " + attr);
          continue;
        }
        MethodReference m = createSetterReference(attr, attr.getSetterName());
        FieldReference f = createFieldReference(attr);
        result.put(m, f);
      }
    }
    return result;
  }

  /**
   * Return the Set of container created setter methods for CMRs, as
   * a mapping from MethodReference->FieldReference.  The container is responsible for creating
   * a setter method for each container managed relationship.
   */
  @SuppressWarnings("unchecked")
  public Map<MethodReference, FieldReference> getCMRSetters() {
    Map<MethodReference, FieldReference> result = Collections.emptyMap();
    if (bean instanceof ContainerManagedEntity) {
      ContainerManagedEntity cme = (ContainerManagedEntity) bean;
      result = HashMapFactory.make(3);
      for (Iterator cmrFields = cme.getCMRFields().iterator(); cmrFields.hasNext();) {
        CMPAttribute attr = (CMPAttribute) cmrFields.next();
        if (attr.getType() == null) {
          System.err.println("PANIC: null type in attribute: " + attr);
          continue;
        }
        MethodReference m = createSetterReference(attr, attr.getSetterName());
        FieldReference f = createFieldReference(attr);
        result.put(m, f);
      }
    }
    return result;
  }
  /**
   * Return the Set of container created finder methods, as
   * Method reference objects.
   */
  public Set<MethodReference> getFinders() {
    HashSet<MethodReference> result = HashSetFactory.make(5);
    Method[] methods = bean.getLocalHomeMethodsForDeployment();
    extractFinders(result, methods);
    methods = bean.getHomeMethodsForDeployment();
    extractFinders(result, methods);
    return result;
  }

  private void extractFinders(HashSet<MethodReference> result, Method[] methods) {
    for (int i = 0; i < methods.length; i++) {
      Method method = methods[i];
      if (isFinder(method)) {
        MethodReference ref = createMethodReference(method);
        result.add(ref);
      }
    }
  }

  /**
   * Method isFinder.
   * @param method
   * @return boolean
   */
  private static boolean isFinder(Method method) {
    return method.getName().indexOf("find") == 0;
  }

  /**
   * Create a field reference from a container managed field attribute.
   * @param att the container managed field attribute
   * @return FieldReference 
   */
  private FieldReference createFieldReference(CMPAttribute att) {
    if (Assertions.verifyAssertions) {
      Assertions._assert(att != null, "null attribute");
      Assertions._assert(att.getType() != null, "null attribute type");
    }
    Atom name = Atom.findOrCreateUnicodeAtom(att.getName());
    String dString = att.getType().getJavaName();
    dString = StringStuff.deployment2CanonicalTypeString(dString);
    TypeReference fieldType = TypeReference.findOrCreate(klass.getClassLoader(), TypeName.string2TypeName(dString));
    FieldReference f = FieldReference.findOrCreate(klass, name, fieldType);
    return f;
  }

  /**
   * Create a method reference from a finder
   * @param method etools method representation
   * @return MethodReference that represents the method.
   */
  private MethodReference createMethodReference(Method method) {
    return J2EEUtil.createMethodReference(method, klass.getClassLoader());
  }
  /**
   * Create a method reference from a container managed field attribute.
   * @param attr the container managed field attribute
   * @param methodName the name of the method
   * @return MethodReference that represents the method.
   */
  private MethodReference createGetterReference(CMPAttribute attr, String methodName) {
    String ret = StringStuff.deployment2CanonicalDescriptorTypeString(attr.getType().getJavaName());
    Descriptor D = Descriptor.findOrCreateUTF8("()" + ret);
    Atom name = Atom.findOrCreateUnicodeAtom(methodName);
    MethodReference m = MethodReference.findOrCreate(klass, name, D);
    return m;
  }
  /**
   * Create a method reference from a container managed field attribute.
   * @param attr the container managed field attribute
   * @param methodName the name of the method
   * @return MethodReference that represents the method.
   */
  private MethodReference createSetterReference(CMPAttribute attr, String methodName) {
    String arg = StringStuff.deployment2CanonicalDescriptorTypeString(attr.getType().getJavaName());
    Atom name = Atom.findOrCreateUnicodeAtom(methodName);
    Descriptor D = Descriptor.findOrCreateUTF8("(" + arg + ")V");
    MethodReference m = MethodReference.findOrCreate(klass, name, D);
    return m;
  }

  /**
   * @see com.ibm.wala.j2ee.BeanMetaData#getEJBClass()
   */
  public TypeReference getEJBClass() {
    return klass;
  }

  /*
   * @see com.ibm.wala.j2ee.BeanMetaData#isContainerManaged()
   */
  public boolean isContainerManaged() {
    if (bean.isContainerManagedEntity())
      return true;
    else if (bean.isSession()) {
      Session s = (Session) bean;
      if (s.getTransactionType() != null) {
        TransactionType t = s.getTransactionType();
        if (t.getValue() == TransactionType.CONTAINER) {
          return true;
        }
      }
    }
    return false;
  }
  
	/*
	 * @see com.ibm.wala.j2ee.BeanMetaData#isContainerManaged()
	 */
	public boolean isContainerManagedEntity() {
		return bean.isContainerManagedEntity();
	}

  /* (non-Javadoc)
   * @see com.ibm.wala.j2ee.BeanMetaData#isSessionBean()
   */
  public boolean isSessionBean() {
    return bean.isSession();
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.j2ee.BeanMetaData#isMessageDrivenBean()
   */
  public boolean isMessageDrivenBean() {
    return bean.isMessageDriven();
  }

  /**
   * @see com.ibm.wala.j2ee.BeanMetaData#getField(CMRField)
   */
  public FieldReference getField(CMRField field) {
    Atom name = Atom.findOrCreateUnicodeAtom(field.getName());
    return cmpFields.get(name);
  }

  /**
   * @see java.lang.Object#toString()
   */
  public String toString() {
    return klass.toString();
  }

  /**
   * @return wccm model of this bean
   */
  public EnterpriseBean getBean() {
    return bean;
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.j2ee.BeanMetaData#getHomeInterface()
   */
  public TypeReference getHomeInterface() {
    return homeInterface;
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.j2ee.BeanMetaData#getLocalHomeInterface()
   */
  public TypeReference getLocalHomeInterface() {
    return localHomeInterface;
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.j2ee.BeanMetaData#getRemoteInterface()
   */
  public TypeReference getRemoteInterface() {
    return remoteInterface;
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.j2ee.BeanMetaData#getLocalInterface()
   */
  public TypeReference getLocalInterface() {
    return localInterface;
  }

  public TypeReference getPrimaryKeyType() {
    if (bean instanceof Entity) {
      JavaClass keyKlass = ((Entity) bean).getPrimaryKey();
      String name = keyKlass.getQualifiedNameForReflection();
      return J2EEUtil.getTypeForInterface(klass.getClassLoader(), name);
    } else {
      return null;
    }
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.j2ee.BeanMetaData#isBeanManaged()
   */
  public boolean isBeanManaged() {
    return bean.isBeanManagedEntity();
  }

  /* (non-Javadoc)
   * @see com.ibm.wala.j2ee.BeanMetaData#getEJBJar()
   */
  public EJBJar getEJBJar() {
    return ejbJar;
  }
  /* (non-Javadoc)
   * @see java.lang.Object#hashCode()
   */
  public int hashCode() {
    return bean.hashCode() * 941;
  }

}
