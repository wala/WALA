/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.pointerAnalysis;

import com.ibm.wala.ecore.java.EJavaClass;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>EJava Class Instance</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.java.pointerAnalysis.EJavaClassInstance#getJavaClass <em>Java Class</em>}</li>
 * </ul>
 * </p>
 *
 * @see com.ibm.wala.ecore.java.pointerAnalysis.PointerAnalysisPackage#getEJavaClassInstance()
 * @model
 * @generated
 */
public interface EJavaClassInstance extends EInstance {
  /**
   * Returns the value of the '<em><b>Java Class</b></em>' reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Java Class</em>' reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Java Class</em>' reference.
   * @see #setJavaClass(EJavaClass)
   * @see com.ibm.wala.ecore.java.pointerAnalysis.PointerAnalysisPackage#getEJavaClassInstance_JavaClass()
   * @model required="true"
   * @generated
   */
  EJavaClass getJavaClass();

  /**
   * Sets the value of the '{@link com.ibm.wala.ecore.java.pointerAnalysis.EJavaClassInstance#getJavaClass <em>Java Class</em>}' reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Java Class</em>' reference.
   * @see #getJavaClass()
   * @generated
   */
  void setJavaClass(EJavaClass value);

} // EJavaClassInstance