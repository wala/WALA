/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java;

import org.eclipse.emf.ecore.EFactory;

/**
 * <!-- begin-user-doc -->
 * The <b>Factory</b> for the model.
 * It provides a create method for each non-abstract class of the model.
 * <!-- end-user-doc -->
 * @see com.ibm.wala.ecore.java.JavaPackage
 * @generated
 */
public interface JavaFactory extends EFactory {
  /**
   * The singleton instance of the factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  JavaFactory eINSTANCE = com.ibm.wala.ecore.java.impl.JavaFactoryImpl.init();

  /**
   * Returns a new object of class '<em>EJava Class</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>EJava Class</em>'.
   * @generated
   */
  EJavaClass createEJavaClass();

  /**
   * Returns the package supported by this factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the package supported by this factory.
   * @generated
   */
  JavaPackage getJavaPackage();

} //JavaFactory
