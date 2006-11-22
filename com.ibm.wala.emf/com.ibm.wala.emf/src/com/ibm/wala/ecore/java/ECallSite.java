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
 * A representation of the model object '<em><b>ECall Site</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.java.ECallSite#getBytecodeIndex <em>Bytecode Index</em>}</li>
 *   <li>{@link com.ibm.wala.ecore.java.ECallSite#getJavaMethod <em>Java Method</em>}</li>
 *   <li>{@link com.ibm.wala.ecore.java.ECallSite#getDeclaredTarget <em>Declared Target</em>}</li>
 * </ul>
 * </p>
 *
 * @see com.ibm.wala.ecore.java.JavaPackage#getECallSite()
 * @model
 * @generated
 */
public interface ECallSite extends EObjectWithContainerId {
  /**
   * Returns the value of the '<em><b>Bytecode Index</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Bytecode Index</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Bytecode Index</em>' attribute.
   * @see #setBytecodeIndex(int)
   * @see com.ibm.wala.ecore.java.JavaPackage#getECallSite_BytecodeIndex()
   * @model
   * @generated
   */
  int getBytecodeIndex();

  /**
   * Sets the value of the '{@link com.ibm.wala.ecore.java.ECallSite#getBytecodeIndex <em>Bytecode Index</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Bytecode Index</em>' attribute.
   * @see #getBytecodeIndex()
   * @generated
   */
  void setBytecodeIndex(int value);

  /**
   * Returns the value of the '<em><b>Java Method</b></em>' reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Java Method</em>' reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Java Method</em>' reference.
   * @see #setJavaMethod(EJavaMethod)
   * @see com.ibm.wala.ecore.java.JavaPackage#getECallSite_JavaMethod()
   * @model required="true"
   * @generated
   */
  EJavaMethod getJavaMethod();

  /**
   * Sets the value of the '{@link com.ibm.wala.ecore.java.ECallSite#getJavaMethod <em>Java Method</em>}' reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Java Method</em>' reference.
   * @see #getJavaMethod()
   * @generated
   */
  void setJavaMethod(EJavaMethod value);

  /**
   * Returns the value of the '<em><b>Declared Target</b></em>' reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Declared Target</em>' reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Declared Target</em>' reference.
   * @see #setDeclaredTarget(EJavaMethod)
   * @see com.ibm.wala.ecore.java.JavaPackage#getECallSite_DeclaredTarget()
   * @model required="true"
   * @generated
   */
  EJavaMethod getDeclaredTarget();

  /**
   * Sets the value of the '{@link com.ibm.wala.ecore.java.ECallSite#getDeclaredTarget <em>Declared Target</em>}' reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Declared Target</em>' reference.
   * @see #getDeclaredTarget()
   * @generated
   */
  void setDeclaredTarget(EJavaMethod value);

} // ECallSite