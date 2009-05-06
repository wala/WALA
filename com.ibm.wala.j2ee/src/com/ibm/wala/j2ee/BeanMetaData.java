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
import java.util.Map;
import java.util.Set;

import org.eclipse.jst.j2ee.ejb.CMRField;
import org.eclipse.jst.j2ee.ejb.EJBJar;
import org.eclipse.jst.j2ee.ejb.EnterpriseBean;

import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;

/**
 * Deployment descriptor data for a single EJB.
 * 
 * TODO: this currently can represent either a session or entity bean. Introduce classes to distinguish between them.
 */
public interface BeanMetaData {

  /**
   * Return a Set of {@link FieldReference}, one corresponding to each CMP field in this bean.
   */
  Collection<FieldReference> getCMPFields();

  /**
   * Return a Set of container managed relationship (cmr) fields, as FieldReference objects.
   */
  Set<Object> getCMRFields();

  /**
   * Return a Map of the container created getter methods, as MethodReference objects, and the field that the method references.
   * 
   * @return Map of container created getter methods and field reference.
   */
  Map<Object, FieldReference> getGetterMethods();

  /**
   * Return a Map of the container created setter methods, as MethodReference objects, and the field that the method references.
   * 
   * @return Map of container created setter methods and field reference.
   */
  Map<Object, FieldReference> getSetterMethods();

  /**
   * Return a Map of container created getter methods for CMRs, as a mapping from MethodReference->FieldReference. The container is
   * responsible for creating a getter method for each container managed relationship.
   */
  Map<MethodReference, FieldReference> getCMRGetters();

  /**
   * Return a Map of container created setter methods for CMRs, as a mapping from MethodReference->FieldReference. The container is
   * responsible for creating a setter method for each container managed relationship.
   */
  Map<MethodReference, FieldReference> getCMRSetters();

  /**
   * Return the type of the EJB class for this entity bean
   */
  TypeReference getEJBClass();

  /**
   * Return true if the bean is container managed.
   * 
   * @return true if bean is container managed.
   */
  boolean isContainerManaged();

  /**
   * Return true if the bean is a container managed entity.
   * 
   * @return true if bean is a container managed entity.
   */
  boolean isContainerManagedEntity();

  /**
   * Return true if the bean uses BMP
   * 
   * @return true if bean uses BMP
   */
  boolean isBeanManaged();

  /**
   * Return true if the bean is a session bean.
   * 
   * @return true if bean is a session bean.
   */
  boolean isSessionBean();

  /**
   * Return true if the bean is a message-driven
   * 
   * @return true if bean is a message-driven
   */
  boolean isMessageDrivenBean();

  /**
   * Return the Set of container created finder methods, as Method reference objects.
   * 
   * @return Set of container created setter methods.
   */
  public Set<MethodReference> getFinders();

  /**
   * Method getField.
   * 
   * @param firstField
   * @return FieldReference
   */
  FieldReference getField(CMRField firstField);

  /**
   * @return WCCM representation of this bean.
   */
  public EnterpriseBean getBean();

  /**
   * @return WCCM representation of this bean's container
   */
  EJBJar getEJBJar();

  /**
   * @return TypeReference representing this entity's home interface
   */
  TypeReference getHomeInterface();

  /**
   * @return TypeReference representing this entity's local home interface
   */
  TypeReference getLocalHomeInterface();

  /**
   * @return TypeReference representing this entity's remote interface
   */
  TypeReference getRemoteInterface();

  /**
   * @return TypeReference representing this entity's local interface
   */
  TypeReference getLocalInterface();

  /**
   * @return TypeReference representing this entity's primary key type.
   */
  TypeReference getPrimaryKeyType();

}