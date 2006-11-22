/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.impl;

import com.ibm.wala.ecore.common.impl.EObjectWithContainerIdImpl;

import com.ibm.wala.ecore.java.ECallSite;
import com.ibm.wala.ecore.java.EJavaMethod;
import com.ibm.wala.ecore.java.JavaPackage;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;

import org.eclipse.emf.ecore.impl.ENotificationImpl;

/**
 * <!-- begin-user-doc --> An implementation of the model object '<em><b>ECall Site</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.java.impl.ECallSiteImpl#getBytecodeIndex <em>Bytecode Index</em>}</li>
 *   <li>{@link com.ibm.wala.ecore.java.impl.ECallSiteImpl#getJavaMethod <em>Java Method</em>}</li>
 *   <li>{@link com.ibm.wala.ecore.java.impl.ECallSiteImpl#getDeclaredTarget <em>Declared Target</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class ECallSiteImpl extends EObjectWithContainerIdImpl implements ECallSite {

  /**
   * The default value of the '{@link #getBytecodeIndex() <em>Bytecode Index</em>}' attribute.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * @see #getBytecodeIndex()
   * @generated
   * @ordered
   */
  protected static final int BYTECODE_INDEX_EDEFAULT = 0;

  /**
   * The cached value of the '{@link #getBytecodeIndex() <em>Bytecode Index</em>}' attribute.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * @see #getBytecodeIndex()
   * @generated
   * @ordered
   */
  protected int bytecodeIndex = BYTECODE_INDEX_EDEFAULT;

  /**
   * The cached value of the '{@link #getJavaMethod() <em>Java Method</em>}' reference.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * @see #getJavaMethod()
   * @generated
   * @ordered
   */
  protected EJavaMethod javaMethod = null;

  /**
   * The cached value of the '{@link #getDeclaredTarget() <em>Declared Target</em>}' reference.
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * @see #getDeclaredTarget()
   * @generated
   * @ordered
   */
  protected EJavaMethod declaredTarget = null;

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * @generated
   */
  protected ECallSiteImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * @generated
   */
  protected EClass eStaticClass() {
    return JavaPackage.Literals.ECALL_SITE;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * @generated
   */
  public int getBytecodeIndex() {
    return bytecodeIndex;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * @generated
   */
  public void setBytecodeIndex(int newBytecodeIndex) {
    int oldBytecodeIndex = bytecodeIndex;
    bytecodeIndex = newBytecodeIndex;
    if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, JavaPackage.ECALL_SITE__BYTECODE_INDEX, oldBytecodeIndex, bytecodeIndex));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * @generated
   */
  public EJavaMethod getJavaMethod() {
    if (javaMethod != null && javaMethod.eIsProxy()) {
      InternalEObject oldJavaMethod = (InternalEObject)javaMethod;
      javaMethod = (EJavaMethod)eResolveProxy(oldJavaMethod);
      if (javaMethod != oldJavaMethod) {
        if (eNotificationRequired())
          eNotify(new ENotificationImpl(this, Notification.RESOLVE, JavaPackage.ECALL_SITE__JAVA_METHOD, oldJavaMethod, javaMethod));
      }
    }
    return javaMethod;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * @generated
   */
  public EJavaMethod basicGetJavaMethod() {
    return javaMethod;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * @generated
   */
  public void setJavaMethod(EJavaMethod newJavaMethod) {
    EJavaMethod oldJavaMethod = javaMethod;
    javaMethod = newJavaMethod;
    if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, JavaPackage.ECALL_SITE__JAVA_METHOD, oldJavaMethod, javaMethod));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * @generated
   */
  public EJavaMethod getDeclaredTarget() {
    if (declaredTarget != null && declaredTarget.eIsProxy()) {
      InternalEObject oldDeclaredTarget = (InternalEObject)declaredTarget;
      declaredTarget = (EJavaMethod)eResolveProxy(oldDeclaredTarget);
      if (declaredTarget != oldDeclaredTarget) {
        if (eNotificationRequired())
          eNotify(new ENotificationImpl(this, Notification.RESOLVE, JavaPackage.ECALL_SITE__DECLARED_TARGET, oldDeclaredTarget, declaredTarget));
      }
    }
    return declaredTarget;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * @generated
   */
  public EJavaMethod basicGetDeclaredTarget() {
    return declaredTarget;
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * @generated
   */
  public void setDeclaredTarget(EJavaMethod newDeclaredTarget) {
    EJavaMethod oldDeclaredTarget = declaredTarget;
    declaredTarget = newDeclaredTarget;
    if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, JavaPackage.ECALL_SITE__DECLARED_TARGET, oldDeclaredTarget, declaredTarget));
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * @generated
   */
  public Object eGet(int featureID, boolean resolve, boolean coreType) {
    switch (featureID) {
      case JavaPackage.ECALL_SITE__BYTECODE_INDEX:
        return new Integer(getBytecodeIndex());
      case JavaPackage.ECALL_SITE__JAVA_METHOD:
        if (resolve) return getJavaMethod();
        return basicGetJavaMethod();
      case JavaPackage.ECALL_SITE__DECLARED_TARGET:
        if (resolve) return getDeclaredTarget();
        return basicGetDeclaredTarget();
    }
    return super.eGet(featureID, resolve, coreType);
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * @generated
   */
  public void eSet(int featureID, Object newValue) {
    switch (featureID) {
      case JavaPackage.ECALL_SITE__BYTECODE_INDEX:
        setBytecodeIndex(((Integer)newValue).intValue());
        return;
      case JavaPackage.ECALL_SITE__JAVA_METHOD:
        setJavaMethod((EJavaMethod)newValue);
        return;
      case JavaPackage.ECALL_SITE__DECLARED_TARGET:
        setDeclaredTarget((EJavaMethod)newValue);
        return;
    }
    super.eSet(featureID, newValue);
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * @generated
   */
  public void eUnset(int featureID) {
    switch (featureID) {
      case JavaPackage.ECALL_SITE__BYTECODE_INDEX:
        setBytecodeIndex(BYTECODE_INDEX_EDEFAULT);
        return;
      case JavaPackage.ECALL_SITE__JAVA_METHOD:
        setJavaMethod((EJavaMethod)null);
        return;
      case JavaPackage.ECALL_SITE__DECLARED_TARGET:
        setDeclaredTarget((EJavaMethod)null);
        return;
    }
    super.eUnset(featureID);
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * @generated
   */
  public boolean eIsSet(int featureID) {
    switch (featureID) {
      case JavaPackage.ECALL_SITE__BYTECODE_INDEX:
        return bytecodeIndex != BYTECODE_INDEX_EDEFAULT;
      case JavaPackage.ECALL_SITE__JAVA_METHOD:
        return javaMethod != null;
      case JavaPackage.ECALL_SITE__DECLARED_TARGET:
        return declaredTarget != null;
    }
    return super.eIsSet(featureID);
  }

  /**
   * <!-- begin-user-doc --> <!-- end-user-doc -->
   * @generated
   */
  public String toString() {
    if (eIsProxy()) return super.toString();

    StringBuffer result = new StringBuffer(super.toString());
    result.append(" (bytecodeIndex: ");
    result.append(bytecodeIndex);
    result.append(')');
    return result.toString();
  }

  public boolean equals(Object obj) {
    if (getClass().equals(obj.getClass())) {
      ECallSiteImpl other = (ECallSiteImpl) obj;
      if  (bytecodeIndex != other.bytecodeIndex) {
        return false;
      }
      if (!javaMethod.equals(other.javaMethod)) {
        return false;
      }
      if (declaredTarget == null) {
        return declaredTarget == null;
      }
      return declaredTarget.equals(other.declaredTarget);
    } else {
      return false;
    }

  }

  public int hashCode() {
    int result =  bytecodeIndex + javaMethod.hashCode() * 1013;
    result = (declaredTarget == null) ? result : result + 281*declaredTarget.hashCode();
    return result;
  }

} // ECallSiteImpl
