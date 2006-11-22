/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.j2ee.scope;

import org.eclipse.emf.ecore.EFactory;

/**
 * <!-- begin-user-doc -->
 * The <b>Factory</b> for the model.
 * It provides a create method for each non-abstract class of the model.
 * <!-- end-user-doc -->
 * @see com.ibm.wala.ecore.j2ee.scope.J2EEScopePackage
 * @generated
 */
public interface J2EEScopeFactory extends EFactory {
  /**
   * The singleton instance of the factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  J2EEScopeFactory eINSTANCE = com.ibm.wala.ecore.j2ee.scope.impl.J2EEScopeFactoryImpl.init();

  /**
   * Returns a new object of class '<em>EJ2EE Analysis Scope</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>EJ2EE Analysis Scope</em>'.
   * @generated
   */
  EJ2EEAnalysisScope createEJ2EEAnalysisScope();

  /**
   * Returns a new object of class '<em>EEar File</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>EEar File</em>'.
   * @generated
   */
  EEarFile createEEarFile();

  /**
   * Returns a new object of class '<em>EWar File</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>EWar File</em>'.
   * @generated
   */
  EWarFile createEWarFile();

  /**
   * Returns the package supported by this factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the package supported by this factory.
   * @generated
   */
  J2EEScopePackage getJ2EEScopePackage();

} //J2EEScopeFactory
