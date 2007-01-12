/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.pointerAnalysis.impl;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;

import com.ibm.wala.ecore.java.EJavaMethod;
import com.ibm.wala.ecore.java.pointerAnalysis.ELocalPointer;
import com.ibm.wala.ecore.java.pointerAnalysis.PointerAnalysisPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>ELocal Pointer</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.java.pointerAnalysis.impl.ELocalPointerImpl#getValueNumber <em>Value Number</em>}</li>
 *   <li>{@link com.ibm.wala.ecore.java.pointerAnalysis.impl.ELocalPointerImpl#getJavaMethod <em>Java Method</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class ELocalPointerImpl extends EPointerImpl implements ELocalPointer {
  /**
   * The default value of the '{@link #getValueNumber() <em>Value Number</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getValueNumber()
   * @generated
   * @ordered
   */
  protected static final int VALUE_NUMBER_EDEFAULT = 0;

  /**
   * The cached value of the '{@link #getValueNumber() <em>Value Number</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getValueNumber()
   * @generated
   * @ordered
   */
  protected int valueNumber = VALUE_NUMBER_EDEFAULT;

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
  protected ELocalPointerImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected EClass eStaticClass() {
    return PointerAnalysisPackage.Literals.ELOCAL_POINTER;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public int getValueNumber() {
    return valueNumber;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setValueNumber(int newValueNumber) {
    int oldValueNumber = valueNumber;
    valueNumber = newValueNumber;
    if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PointerAnalysisPackage.ELOCAL_POINTER__VALUE_NUMBER, oldValueNumber, valueNumber));
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
          eNotify(new ENotificationImpl(this, Notification.RESOLVE, PointerAnalysisPackage.ELOCAL_POINTER__JAVA_METHOD, oldJavaMethod, javaMethod));
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
      eNotify(new ENotificationImpl(this, Notification.SET, PointerAnalysisPackage.ELOCAL_POINTER__JAVA_METHOD, oldJavaMethod, javaMethod));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public Object eGet(int featureID, boolean resolve, boolean coreType) {
    switch (featureID) {
      case PointerAnalysisPackage.ELOCAL_POINTER__VALUE_NUMBER:
        return new Integer(getValueNumber());
      case PointerAnalysisPackage.ELOCAL_POINTER__JAVA_METHOD:
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
      case PointerAnalysisPackage.ELOCAL_POINTER__VALUE_NUMBER:
        setValueNumber(((Integer)newValue).intValue());
        return;
      case PointerAnalysisPackage.ELOCAL_POINTER__JAVA_METHOD:
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
      case PointerAnalysisPackage.ELOCAL_POINTER__VALUE_NUMBER:
        setValueNumber(VALUE_NUMBER_EDEFAULT);
        return;
      case PointerAnalysisPackage.ELOCAL_POINTER__JAVA_METHOD:
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
      case PointerAnalysisPackage.ELOCAL_POINTER__VALUE_NUMBER:
        return valueNumber != VALUE_NUMBER_EDEFAULT;
      case PointerAnalysisPackage.ELOCAL_POINTER__JAVA_METHOD:
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
    result.append(" (valueNumber: ");
    result.append(valueNumber);
    result.append(')');
    return result.toString();
  }

} //ELocalPointerImpl