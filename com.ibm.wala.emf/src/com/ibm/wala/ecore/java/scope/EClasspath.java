/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.scope;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>EClasspath</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.java.scope.EClasspath#getString <em>String</em>}</li>
 * </ul>
 * </p>
 *
 * @see com.ibm.wala.ecore.java.scope.JavaScopePackage#getEClasspath()
 * @model
 * @generated
 */
public interface EClasspath extends EObject {
  /**
   * Returns the value of the '<em><b>String</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>String</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>String</em>' attribute.
   * @see #setString(String)
   * @see com.ibm.wala.ecore.java.scope.JavaScopePackage#getEClasspath_String()
   * @model required="true"
   * @generated
   */
  String getString();

  /**
   * Sets the value of the '{@link com.ibm.wala.ecore.java.scope.EClasspath#getString <em>String</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>String</em>' attribute.
   * @see #getString()
   * @generated
   */
  void setString(String value);

} // EClasspath