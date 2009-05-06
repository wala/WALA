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
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.eclipse.jst.j2ee.ejb.EJBRelationshipRole;

import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MemberReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;

/**
 * Interface to data from deployment descriptors.
 */
public interface DeploymentMetaData {

  /**
   * Return the BeanMetaData describing a particular bean, or null if the type does not correspond to an EJB.
   */
  BeanMetaData getBeanMetaData(TypeReference type);

  Set<FieldReference> getAllCMPFields();

  /**
   * Return a Set of the container managed relationship (cmr) fields.
   * 
   * @return Set of container managed relationship fields.
   */
  Set<Object> getAllCMRFields();

  /**
   * Is a class an EJB remote interface?
   */
  boolean isRemoteInterface(TypeReference t);

  /**
   * Is a class an EJB home interface?
   */
  boolean isHomeInterface(TypeReference t);

  /**
   * Is a class an EJB local interface?
   */
  boolean isLocalInterface(TypeReference t);

  /**
   * Is a class an EJB local home interface?
   */
  boolean isLocalHomeInterface(TypeReference t);

  /**
   * Is a class an EJB interface (any flavor)?
   */
  boolean isEJBInterface(TypeReference t);

  /**
   * Is type an MDB?
   */
  boolean isMessageDriven(TypeReference type);

  /**
   * Return the entity bean implementation corresponding to the interface t
   * 
   * @param t the home or remote interface for a bean
   * @return the BeanMetaData, or null if not found.
   */
  BeanMetaData getBeanForInterface(TypeReference t);

  /**
   * Return true if the class is container managed.
   * 
   * @return true if the class is container managed.
   */
  boolean isContainerManaged(TypeReference t);

  TypeReference getCMPType(TypeReference typeReference);

  boolean isCMPGetter(MemberReference mr);

  FieldReference getCMPField(MemberReference mr);

  boolean isCMPSetter(MemberReference mr);

  /**
   * Return the Set of MethodReferences corresponding to EJB finder methods.
   */
  Collection<MethodReference> getAllFinders();

  /**
   * Return the Set of methods corresponding to EJB CMR getter methods, as a mapping from MethodReference -> FieldReference
   */
  Map<MethodReference, FieldReference> getAllCMRGetters();

  /**
   * Given a field that is populated by a CMR, return the descriptor of the Bean type that the field will point to.
   */
  BeanMetaData getCMRBean(FieldReference field);

  /**
   * @param method a finder
   * @return the type representing the bean that is returned by this finder
   */
  TypeReference getFinderBeanType(MemberReference method);

  /**
   * @return true iff ref is finder method
   */
  boolean isFinder(MemberReference ref);

  /**
   * @return Iterator<BeanMetaData> of all entity beans available
   */
  Iterator<BeanMetaData> iterateEntities();

  /**
   * @return Iterator<BeanMetaData> of all session beans available
   */
  Iterator<BeanMetaData> iterateSessions();

  /**
   * @return Iterator<BeanMetaData> of all message-driven beans available
   */
  Iterator<BeanMetaData> iterateMDBs();

  /**
   * @return true iff method is a getter for a CMR.
   */
  boolean isCMRGetter(MemberReference method);

  /**
   * @return true iff method is a setter for a CMR.
   */
  boolean isCMRSetter(MemberReference method);

  /**
   * @param field a field that represents a CMR
   * @return the corresponding field on the opposite role of the CMR
   */
  FieldReference getOppositeField(FieldReference field);

  /**
   * @param field a field that represents a CMR
   * @return the governing EJBRelationshipRole
   */
  EJBRelationshipRole getCMRRole(FieldReference field);

}
