/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.common;

import org.eclipse.emf.ecore.EFactory;

/**
 * <!-- begin-user-doc -->
 * The <b>Factory</b> for the model.
 * It provides a create method for each non-abstract class of the model.
 * <!-- end-user-doc -->
 * @see com.ibm.wala.ecore.common.CommonPackage
 * @generated
 */
public interface CommonFactory extends EFactory {
  /**
   * The singleton instance of the factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  CommonFactory eINSTANCE = com.ibm.wala.ecore.common.impl.CommonFactoryImpl.init();

  /**
   * Returns a new object of class '<em>EPair</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>EPair</em>'.
   * @generated
   */
  EPair createEPair();

  /**
   * Returns a new object of class '<em>ERelation</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>ERelation</em>'.
   * @generated
   */
  ERelation createERelation();

  /**
   * Returns a new object of class '<em>EContainer</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>EContainer</em>'.
   * @generated
   */
  EContainer createEContainer();

  /**
   * Returns a new object of class '<em>ENot Container</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>ENot Container</em>'.
   * @generated
   */
  ENotContainer createENotContainer();

  /**
   * Returns a new object of class '<em>EString Holder</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>EString Holder</em>'.
   * @generated
   */
  EStringHolder createEStringHolder();

  /**
   * Returns a new object of class '<em>EObject With Container Id</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>EObject With Container Id</em>'.
   * @generated
   */
  EObjectWithContainerId createEObjectWithContainerId();

  /**
   * Returns the package supported by this factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the package supported by this factory.
   * @generated
   */
  CommonPackage getCommonPackage();

} //CommonFactory
