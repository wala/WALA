/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.pointerAnalysis;

import com.ibm.wala.ecore.java.EJavaMethod;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>EReturn Value Pointer</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.java.pointerAnalysis.EReturnValuePointer#isIsExceptionalReturnValue <em>Is Exceptional Return Value</em>}</li>
 *   <li>{@link com.ibm.wala.ecore.java.pointerAnalysis.EReturnValuePointer#getJavaMethod <em>Java Method</em>}</li>
 * </ul>
 * </p>
 *
 * @see com.ibm.wala.ecore.java.pointerAnalysis.PointerAnalysisPackage#getEReturnValuePointer()
 * @model
 * @generated
 */
public interface EReturnValuePointer extends EPointer {
  /**
   * Returns the value of the '<em><b>Is Exceptional Return Value</b></em>' attribute.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Is Exceptional Return Value</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Is Exceptional Return Value</em>' attribute.
   * @see #setIsExceptionalReturnValue(boolean)
   * @see com.ibm.wala.ecore.java.pointerAnalysis.PointerAnalysisPackage#getEReturnValuePointer_IsExceptionalReturnValue()
   * @model
   * @generated
   */
  boolean isIsExceptionalReturnValue();

  /**
   * Sets the value of the '{@link com.ibm.wala.ecore.java.pointerAnalysis.EReturnValuePointer#isIsExceptionalReturnValue <em>Is Exceptional Return Value</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Is Exceptional Return Value</em>' attribute.
   * @see #isIsExceptionalReturnValue()
   * @generated
   */
  void setIsExceptionalReturnValue(boolean value);

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
   * @see com.ibm.wala.ecore.java.pointerAnalysis.PointerAnalysisPackage#getEReturnValuePointer_JavaMethod()
   * @model required="true"
   * @generated
   */
  EJavaMethod getJavaMethod();

  /**
   * Sets the value of the '{@link com.ibm.wala.ecore.java.pointerAnalysis.EReturnValuePointer#getJavaMethod <em>Java Method</em>}' reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Java Method</em>' reference.
   * @see #getJavaMethod()
   * @generated
   */
  void setJavaMethod(EJavaMethod value);

} // EReturnValuePointer