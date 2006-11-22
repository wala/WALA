/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.scope;

import org.eclipse.emf.ecore.EFactory;

/**
 * <!-- begin-user-doc -->
 * The <b>Factory</b> for the model.
 * It provides a create method for each non-abstract class of the model.
 * <!-- end-user-doc -->
 * @see com.ibm.wala.ecore.java.scope.JavaScopePackage
 * @generated
 */
public interface JavaScopeFactory extends EFactory {
  /**
   * The singleton instance of the factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  JavaScopeFactory eINSTANCE = com.ibm.wala.ecore.java.scope.impl.JavaScopeFactoryImpl.init();

  /**
   * Returns a new object of class '<em>EJava Analysis Scope</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>EJava Analysis Scope</em>'.
   * @generated
   */
  EJavaAnalysisScope createEJavaAnalysisScope();

  /**
   * Returns a new object of class '<em>EClass Loader</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>EClass Loader</em>'.
   * @generated
   */
  EClassLoader createEClassLoader();

  /**
   * Returns a new object of class '<em>EBuilt In Module</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>EBuilt In Module</em>'.
   * @generated
   */
  EBuiltInModule createEBuiltInModule();

  /**
   * Returns a new object of class '<em>EJar File</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>EJar File</em>'.
   * @generated
   */
  EJarFile createEJarFile();

  /**
   * Returns a new object of class '<em>EFile</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>EFile</em>'.
   * @generated
   */
  EFile createEFile();

  /**
   * Returns a new object of class '<em>EClass File</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>EClass File</em>'.
   * @generated
   */
  EClassFile createEClassFile();

  /**
   * Returns a new object of class '<em>ESource File</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>ESource File</em>'.
   * @generated
   */
  ESourceFile createESourceFile();

  /**
   * Returns a new object of class '<em>EClasspath</em>'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return a new object of class '<em>EClasspath</em>'.
   * @generated
   */
  EClasspath createEClasspath();

  /**
   * Returns the package supported by this factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the package supported by this factory.
   * @generated
   */
  JavaScopePackage getJavaScopePackage();

} //JavaScopeFactory
