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
 * A representation of the model object '<em><b>EClass Loader</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.java.scope.EClassLoader#getModules <em>Modules</em>}</li>
 *   <li>{@link com.ibm.wala.ecore.java.scope.EClassLoader#getLoaderName <em>Loader Name</em>}</li>
 * </ul>
 * </p>
 *
 * @see com.ibm.wala.ecore.java.scope.JavaScopePackage#getEClassLoader()
 * @model
 * @generated
 */
public interface EClassLoader extends EObject {
  /**
   * Returns the value of the '<em><b>Modules</b></em>' containment reference list.
   * The list contents are of type {@link com.ibm.wala.ecore.java.scope.EModule}.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Modules</em>' containment reference list isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Modules</em>' containment reference list.
   * @see com.ibm.wala.ecore.java.scope.JavaScopePackage#getEClassLoader_Modules()
   * @model type="com.ibm.wala.ecore.java.scope.EModule" containment="true" required="true"
   * @generated
   */
  EList getModules();

  /**
   * Returns the value of the '<em><b>Loader Name</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Loader Name</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Loader Name</em>' attribute.
   * @see #setLoaderName(String)
   * @see com.ibm.wala.ecore.java.scope.JavaScopePackage#getEClassLoader_LoaderName()
   * @model
   * @generated
   */
  String getLoaderName();

  /**
   * Sets the value of the '{@link com.ibm.wala.ecore.java.scope.EClassLoader#getLoaderName <em>Loader Name</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Loader Name</em>' attribute.
   * @see #getLoaderName()
   * @generated
   */
  void setLoaderName(String value);

} // EClassLoader