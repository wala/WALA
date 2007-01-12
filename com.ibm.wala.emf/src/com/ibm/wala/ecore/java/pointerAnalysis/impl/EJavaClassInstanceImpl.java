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

import com.ibm.wala.ecore.java.EJavaClass;
import com.ibm.wala.ecore.java.pointerAnalysis.EJavaClassInstance;
import com.ibm.wala.ecore.java.pointerAnalysis.PointerAnalysisPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>EJava Class Instance</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.java.pointerAnalysis.impl.EJavaClassInstanceImpl#getJavaClass <em>Java Class</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class EJavaClassInstanceImpl extends EInstanceImpl implements EJavaClassInstance {
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
  protected EJavaClassInstanceImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected EClass eStaticClass() {
    return PointerAnalysisPackage.Literals.EJAVA_CLASS_INSTANCE;
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
          eNotify(new ENotificationImpl(this, Notification.RESOLVE, PointerAnalysisPackage.EJAVA_CLASS_INSTANCE__JAVA_CLASS, oldJavaClass, javaClass));
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
      eNotify(new ENotificationImpl(this, Notification.SET, PointerAnalysisPackage.EJAVA_CLASS_INSTANCE__JAVA_CLASS, oldJavaClass, javaClass));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public Object eGet(int featureID, boolean resolve, boolean coreType) {
    switch (featureID) {
      case PointerAnalysisPackage.EJAVA_CLASS_INSTANCE__JAVA_CLASS:
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
      case PointerAnalysisPackage.EJAVA_CLASS_INSTANCE__JAVA_CLASS:
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
      case PointerAnalysisPackage.EJAVA_CLASS_INSTANCE__JAVA_CLASS:
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
      case PointerAnalysisPackage.EJAVA_CLASS_INSTANCE__JAVA_CLASS:
        return javaClass != null;
    }
    return super.eIsSet(featureID);
  }

} //EJavaClassInstanceImpl