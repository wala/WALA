/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java;

import com.ibm.wala.ecore.common.EObjectWithContainerId;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>EJava Class</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.java.EJavaClass#getClassName <em>Class Name</em>}</li>
 *   <li>{@link com.ibm.wala.ecore.java.EJavaClass#getLoader <em>Loader</em>}</li>
 * </ul>
 * </p>
 *
 * @see com.ibm.wala.ecore.java.JavaPackage#getEJavaClass()
 * @model
 * @generated
 */
public interface EJavaClass extends EObjectWithContainerId {
  /**
   * Returns the value of the '<em><b>Class Name</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Class Name</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Class Name</em>' attribute.
   * @see #setClassName(String)
   * @see com.ibm.wala.ecore.java.JavaPackage#getEJavaClass_ClassName()
   * @model required="true"
   * @generated
   */
  String getClassName();

  /**
   * Sets the value of the '{@link com.ibm.wala.ecore.java.EJavaClass#getClassName <em>Class Name</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Class Name</em>' attribute.
   * @see #getClassName()
   * @generated
   */
  void setClassName(String value);

  /**
   * Returns the value of the '<em><b>Loader</b></em>' attribute.
   * The literals are from the enumeration {@link com.ibm.wala.ecore.java.EClassLoaderName}.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Loader</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Loader</em>' attribute.
   * @see com.ibm.wala.ecore.java.EClassLoaderName
   * @see #setLoader(EClassLoaderName)
   * @see com.ibm.wala.ecore.java.JavaPackage#getEJavaClass_Loader()
   * @model required="true"
   * @generated
   */
  EClassLoaderName getLoader();

  /**
   * Sets the value of the '{@link com.ibm.wala.ecore.java.EJavaClass#getLoader <em>Loader</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Loader</em>' attribute.
   * @see com.ibm.wala.ecore.java.EClassLoaderName
   * @see #getLoader()
   * @generated
   */
  void setLoader(EClassLoaderName value);

} // EJavaClass