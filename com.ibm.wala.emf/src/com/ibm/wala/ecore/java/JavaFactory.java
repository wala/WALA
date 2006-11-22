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
   * Returns a new object of class '<em>EJava Method</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>EJava Method</em>'.
   * @generated
   */
  EJavaMethod createEJavaMethod();

  /**
   * Returns a new object of class '<em>ECall Site</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>ECall Site</em>'.
   * @generated
   */
  ECallSite createECallSite();

  /**
   * Returns a new object of class '<em>EClass Hierarchy</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>EClass Hierarchy</em>'.
   * @generated
   */
  EClassHierarchy createEClassHierarchy();

  /**
   * Returns a new object of class '<em>EInterface Hierarchy</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>EInterface Hierarchy</em>'.
   * @generated
   */
  EInterfaceHierarchy createEInterfaceHierarchy();

  /**
   * Returns a new object of class '<em>EType Hierarchy</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>EType Hierarchy</em>'.
   * @generated
   */
  ETypeHierarchy createETypeHierarchy();

  /**
   * Returns the package supported by this factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the package supported by this factory.
   * @generated
   */
  JavaPackage getJavaPackage();

} //JavaFactory
