/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.pointerAnalysis.impl;

import com.ibm.wala.ecore.java.EJavaMethod;

import com.ibm.wala.ecore.java.pointerAnalysis.EReturnValuePointer;
import com.ibm.wala.ecore.java.pointerAnalysis.PointerAnalysisPackage;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>EReturn Value Pointer</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.java.pointerAnalysis.impl.EReturnValuePointerImpl#isIsExceptionalReturnValue <em>Is Exceptional Return Value</em>}</li>
 *   <li>{@link com.ibm.wala.ecore.java.pointerAnalysis.impl.EReturnValuePointerImpl#getJavaMethod <em>Java Method</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class EReturnValuePointerImpl extends EPointerImpl implements EReturnValuePointer {
  /**
   * The default value of the '{@link #isIsExceptionalReturnValue() <em>Is Exceptional Return Value</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #isIsExceptionalReturnValue()
   * @generated
   * @ordered
   */
  protected static final boolean IS_EXCEPTIONAL_RETURN_VALUE_EDEFAULT = false;

  /**
   * The cached value of the '{@link #isIsExceptionalReturnValue() <em>Is Exceptional Return Value</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #isIsExceptionalReturnValue()
   * @generated
   * @ordered
   */
  protected boolean isExceptionalReturnValue = IS_EXCEPTIONAL_RETURN_VALUE_EDEFAULT;

  /**
   * The cached value of the '{@link #getJavaMethod() <em>Java Method</em>}' reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getJavaMethod()
   * @generated
   * @ordered
   */
  protected EJavaMethod javaMethod = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected EReturnValuePointerImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected EClass eStaticClass() {
    return PointerAnalysisPackage.Literals.ERETURN_VALUE_POINTER;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public boolean isIsExceptionalReturnValue() {
    return isExceptionalReturnValue;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setIsExceptionalReturnValue(boolean newIsExceptionalReturnValue) {
    boolean oldIsExceptionalReturnValue = isExceptionalReturnValue;
    isExceptionalReturnValue = newIsExceptionalReturnValue;
    if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PointerAnalysisPackage.ERETURN_VALUE_POINTER__IS_EXCEPTIONAL_RETURN_VALUE, oldIsExceptionalReturnValue, isExceptionalReturnValue));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EJavaMethod getJavaMethod() {
    if (javaMethod != null && javaMethod.eIsProxy()) {
      InternalEObject oldJavaMethod = (InternalEObject)javaMethod;
      javaMethod = (EJavaMethod)eResolveProxy(oldJavaMethod);
      if (javaMethod != oldJavaMethod) {
        if (eNotificationRequired())
          eNotify(new ENotificationImpl(this, Notification.RESOLVE, PointerAnalysisPackage.ERETURN_VALUE_POINTER__JAVA_METHOD, oldJavaMethod, javaMethod));
      }
    }
    return javaMethod;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EJavaMethod basicGetJavaMethod() {
    return javaMethod;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setJavaMethod(EJavaMethod newJavaMethod) {
    EJavaMethod oldJavaMethod = javaMethod;
    javaMethod = newJavaMethod;
    if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PointerAnalysisPackage.ERETURN_VALUE_POINTER__JAVA_METHOD, oldJavaMethod, javaMethod));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public Object eGet(int featureID, boolean resolve, boolean coreType) {
    switch (featureID) {
      case PointerAnalysisPackage.ERETURN_VALUE_POINTER__IS_EXCEPTIONAL_RETURN_VALUE:
        return isIsExceptionalReturnValue() ? Boolean.TRUE : Boolean.FALSE;
      case PointerAnalysisPackage.ERETURN_VALUE_POINTER__JAVA_METHOD:
        if (resolve) return getJavaMethod();
        return basicGetJavaMethod();
    }
    return super.eGet(featureID, resolve, coreType);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void eSet(int featureID, Object newValue) {
    switch (featureID) {
      case PointerAnalysisPackage.ERETURN_VALUE_POINTER__IS_EXCEPTIONAL_RETURN_VALUE:
        setIsExceptionalReturnValue(((Boolean)newValue).booleanValue());
        return;
      case PointerAnalysisPackage.ERETURN_VALUE_POINTER__JAVA_METHOD:
        setJavaMethod((EJavaMethod)newValue);
        return;
    }
    super.eSet(featureID, newValue);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void eUnset(int featureID) {
    switch (featureID) {
      case PointerAnalysisPackage.ERETURN_VALUE_POINTER__IS_EXCEPTIONAL_RETURN_VALUE:
        setIsExceptionalReturnValue(IS_EXCEPTIONAL_RETURN_VALUE_EDEFAULT);
        return;
      case PointerAnalysisPackage.ERETURN_VALUE_POINTER__JAVA_METHOD:
        setJavaMethod((EJavaMethod)null);
        return;
    }
    super.eUnset(featureID);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public boolean eIsSet(int featureID) {
    switch (featureID) {
      case PointerAnalysisPackage.ERETURN_VALUE_POINTER__IS_EXCEPTIONAL_RETURN_VALUE:
        return isExceptionalReturnValue != IS_EXCEPTIONAL_RETURN_VALUE_EDEFAULT;
      case PointerAnalysisPackage.ERETURN_VALUE_POINTER__JAVA_METHOD:
        return javaMethod != null;
    }
    return super.eIsSet(featureID);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public String toString() {
    if (eIsProxy()) return super.toString();

    StringBuffer result = new StringBuffer(super.toString());
    result.append(" (isExceptionalReturnValue: ");
    result.append(isExceptionalReturnValue);
    result.append(')');
    return result.toString();
  }

} //EReturnValuePointerImpl