/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.impl;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;

import com.ibm.wala.ecore.common.impl.EObjectWithContainerIdImpl;
import com.ibm.wala.ecore.java.EJavaClass;
import com.ibm.wala.ecore.java.EJavaMethod;
import com.ibm.wala.ecore.java.JavaPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>EJava Method</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.java.impl.EJavaMethodImpl#getMethodName <em>Method Name</em>}</li>
 *   <li>{@link com.ibm.wala.ecore.java.impl.EJavaMethodImpl#getDescriptor <em>Descriptor</em>}</li>
 *   <li>{@link com.ibm.wala.ecore.java.impl.EJavaMethodImpl#getJavaClass <em>Java Class</em>}</li>
 *   <li>{@link com.ibm.wala.ecore.java.impl.EJavaMethodImpl#getSignature <em>Signature</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class EJavaMethodImpl extends EObjectWithContainerIdImpl implements EJavaMethod {


  /**
   * The default value of the '{@link #getMethodName() <em>Method Name</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getMethodName()
   * @generated
   * @ordered
   */
  protected static final String METHOD_NAME_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getMethodName() <em>Method Name</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getMethodName()
   * @generated
   * @ordered
   */
  protected String methodName = METHOD_NAME_EDEFAULT;

  /**
   * The default value of the '{@link #getDescriptor() <em>Descriptor</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getDescriptor()
   * @generated
   * @ordered
   */
  protected static final String DESCRIPTOR_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getDescriptor() <em>Descriptor</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getDescriptor()
   * @generated
   * @ordered
   */
  protected String descriptor = DESCRIPTOR_EDEFAULT;

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
   * The default value of the '{@link #getSignature() <em>Signature</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getSignature()
   * @generated
   * @ordered
   */
  protected static final String SIGNATURE_EDEFAULT = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected EJavaMethodImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected EClass eStaticClass() {
    return JavaPackage.Literals.EJAVA_METHOD;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public String getMethodName() {
    return methodName;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setMethodName(String newMethodName) {
    String oldMethodName = methodName;
    methodName = newMethodName;
    if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, JavaPackage.EJAVA_METHOD__METHOD_NAME, oldMethodName, methodName));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public String getDescriptor() {
    return descriptor;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setDescriptor(String newDescriptor) {
    String oldDescriptor = descriptor;
    descriptor = newDescriptor;
    if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, JavaPackage.EJAVA_METHOD__DESCRIPTOR, oldDescriptor, descriptor));
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
          eNotify(new ENotificationImpl(this, Notification.RESOLVE, JavaPackage.EJAVA_METHOD__JAVA_CLASS, oldJavaClass, javaClass));
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
      eNotify(new ENotificationImpl(this, Notification.SET, JavaPackage.EJAVA_METHOD__JAVA_CLASS, oldJavaClass, javaClass));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public String getSignature() {
    // TODO: implement this method to return the 'Signature' attribute
    // Ensure that you remove @generated or mark it @generated NOT
    throw new UnsupportedOperationException();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public boolean isClinit() {
    // TODO: implement this method
    // Ensure that you remove @generated or mark it @generated NOT
    throw new UnsupportedOperationException();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public Object eGet(int featureID, boolean resolve, boolean coreType) {
    switch (featureID) {
      case JavaPackage.EJAVA_METHOD__METHOD_NAME:
        return getMethodName();
      case JavaPackage.EJAVA_METHOD__DESCRIPTOR:
        return getDescriptor();
      case JavaPackage.EJAVA_METHOD__JAVA_CLASS:
        if (resolve) return getJavaClass();
        return basicGetJavaClass();
      case JavaPackage.EJAVA_METHOD__SIGNATURE:
        return getSignature();
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
      case JavaPackage.EJAVA_METHOD__METHOD_NAME:
        setMethodName((String)newValue);
        return;
      case JavaPackage.EJAVA_METHOD__DESCRIPTOR:
        setDescriptor((String)newValue);
        return;
      case JavaPackage.EJAVA_METHOD__JAVA_CLASS:
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
      case JavaPackage.EJAVA_METHOD__METHOD_NAME:
        setMethodName(METHOD_NAME_EDEFAULT);
        return;
      case JavaPackage.EJAVA_METHOD__DESCRIPTOR:
        setDescriptor(DESCRIPTOR_EDEFAULT);
        return;
      case JavaPackage.EJAVA_METHOD__JAVA_CLASS:
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
      case JavaPackage.EJAVA_METHOD__METHOD_NAME:
        return METHOD_NAME_EDEFAULT == null ? methodName != null : !METHOD_NAME_EDEFAULT.equals(methodName);
      case JavaPackage.EJAVA_METHOD__DESCRIPTOR:
        return DESCRIPTOR_EDEFAULT == null ? descriptor != null : !DESCRIPTOR_EDEFAULT.equals(descriptor);
      case JavaPackage.EJAVA_METHOD__JAVA_CLASS:
        return javaClass != null;
      case JavaPackage.EJAVA_METHOD__SIGNATURE:
        return SIGNATURE_EDEFAULT == null ? getSignature() != null : !SIGNATURE_EDEFAULT.equals(getSignature());
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
    result.append(" (methodName: ");
    result.append(methodName);
    result.append(", descriptor: ");
    result.append(descriptor);
    result.append(')');
    return result.toString();
  }

  public boolean equals(Object obj) {
    if (getClass().equals(obj.getClass())) {
      EJavaMethodImpl other = (EJavaMethodImpl)obj;
      return descriptor.equals(other.descriptor) && javaClass.equals(other.javaClass) && methodName.equals(other.methodName);
    } else {
      return false;
    }
  }

  public int hashCode() {
    return descriptor.hashCode() + 157 * javaClass.hashCode() + 1439 * methodName.hashCode();
  }

} //EJavaMethodImpl