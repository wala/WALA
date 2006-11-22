/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.pointerAnalysis.impl;

import com.ibm.wala.ecore.java.EJavaClass;

import com.ibm.wala.ecore.java.pointerAnalysis.EArrayContents;
import com.ibm.wala.ecore.java.pointerAnalysis.PointerAnalysisPackage;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>EArray Contents</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.java.pointerAnalysis.impl.EArrayContentsImpl#getJavaClass <em>Java Class</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class EArrayContentsImpl extends EPointerImpl implements EArrayContents {
  /**
   * The cached value of the '{@link #getJavaClass() <em>Java Class</em>}' reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getJavaClass()
   * @generated
   * @ordered
   */
  protected EJavaClass javaClass = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected EArrayContentsImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected EClass eStaticClass() {
    return PointerAnalysisPackage.Literals.EARRAY_CONTENTS;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EJavaClass getJavaClass() {
    if (javaClass != null && javaClass.eIsProxy()) {
      InternalEObject oldJavaClass = (InternalEObject)javaClass;
      javaClass = (EJavaClass)eResolveProxy(oldJavaClass);
      if (javaClass != oldJavaClass) {
        if (eNotificationRequired())
          eNotify(new ENotificationImpl(this, Notification.RESOLVE, PointerAnalysisPackage.EARRAY_CONTENTS__JAVA_CLASS, oldJavaClass, javaClass));
      }
    }
    return javaClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EJavaClass basicGetJavaClass() {
    return javaClass;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setJavaClass(EJavaClass newJavaClass) {
    EJavaClass oldJavaClass = javaClass;
    javaClass = newJavaClass;
    if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, PointerAnalysisPackage.EARRAY_CONTENTS__JAVA_CLASS, oldJavaClass, javaClass));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public Object eGet(int featureID, boolean resolve, boolean coreType) {
    switch (featureID) {
      case PointerAnalysisPackage.EARRAY_CONTENTS__JAVA_CLASS:
        if (resolve) return getJavaClass();
        return basicGetJavaClass();
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
      case PointerAnalysisPackage.EARRAY_CONTENTS__JAVA_CLASS:
        setJavaClass((EJavaClass)newValue);
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
      case PointerAnalysisPackage.EARRAY_CONTENTS__JAVA_CLASS:
        setJavaClass((EJavaClass)null);
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
      case PointerAnalysisPackage.EARRAY_CONTENTS__JAVA_CLASS:
        return javaClass != null;
    }
    return super.eIsSet(featureID);
  }

} //EArrayContentsImpl