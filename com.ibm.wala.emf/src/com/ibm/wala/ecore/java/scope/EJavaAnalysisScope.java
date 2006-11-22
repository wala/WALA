/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.scope;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>EJava Analysis Scope</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.java.scope.EJavaAnalysisScope#getLoaders <em>Loaders</em>}</li>
 *   <li>{@link com.ibm.wala.ecore.java.scope.EJavaAnalysisScope#getExclusionFileName <em>Exclusion File Name</em>}</li>
 * </ul>
 * </p>
 *
 * @see com.ibm.wala.ecore.java.scope.JavaScopePackage#getEJavaAnalysisScope()
 * @model
 * @generated
 */
public interface EJavaAnalysisScope extends EObject {
  /**
   * Returns the value of the '<em><b>Loaders</b></em>' containment reference list.
   * The list contents are of type {@link com.ibm.wala.ecore.java.scope.EClassLoader}.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Loaders</em>' containment reference list isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Loaders</em>' containment reference list.
   * @see com.ibm.wala.ecore.java.scope.JavaScopePackage#getEJavaAnalysisScope_Loaders()
   * @model type="com.ibm.wala.ecore.java.scope.EClassLoader" containment="true" required="true"
   * @generated
   */
  EList getLoaders();

  /**
   * Returns the value of the '<em><b>Exclusion File Name</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Exclusion File Name</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Exclusion File Name</em>' attribute.
   * @see #setExclusionFileName(String)
   * @see com.ibm.wala.ecore.java.scope.JavaScopePackage#getEJavaAnalysisScope_ExclusionFileName()
   * @model
   * @generated
   */
  String getExclusionFileName();

  /**
   * Sets the value of the '{@link com.ibm.wala.ecore.java.scope.EJavaAnalysisScope#getExclusionFileName <em>Exclusion File Name</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Exclusion File Name</em>' attribute.
   * @see #getExclusionFileName()
   * @generated
   */
  void setExclusionFileName(String value);

} // EJavaAnalysisScope