/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.impl;

import com.ibm.wala.ecore.common.impl.EObjectWithContainerIdImpl;

import com.ibm.wala.ecore.java.EClassLoaderName;
import com.ibm.wala.ecore.java.EJavaClass;
import com.ibm.wala.ecore.java.JavaPackage;

import org.eclipse.emf.common.notify.Notification;

import org.eclipse.emf.ecore.EClass;

import org.eclipse.emf.ecore.impl.ENotificationImpl;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>EJava Class</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.java.impl.EJavaClassImpl#getClassName <em>Class Name</em>}</li>
 *   <li>{@link com.ibm.wala.ecore.java.impl.EJavaClassImpl#getLoader <em>Loader</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class EJavaClassImpl extends EObjectWithContainerIdImpl implements EJavaClass {
  /**
   * The default value of the '{@link #getClassName() <em>Class Name</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getClassName()
   * @generated
   * @ordered
   */
  protected static final String CLASS_NAME_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getClassName() <em>Class Name</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getClassName()
   * @generated
   * @ordered
   */
  protected String className = CLASS_NAME_EDEFAULT;

  /**
   * The default value of the '{@link #getLoader() <em>Loader</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getLoader()
   * @generated
   * @ordered
   */
  protected static final EClassLoaderName LOADER_EDEFAULT = EClassLoaderName.APPLICATION_LITERAL;

  /**
   * The cached value of the '{@link #getLoader() <em>Loader</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getLoader()
   * @generated
   * @ordered
   */
  protected EClassLoaderName loader = LOADER_EDEFAULT;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected EJavaClassImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected EClass eStaticClass() {
    return JavaPackage.Literals.EJAVA_CLASS;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public String getClassName() {
    return className;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setClassName(String newClassName) {
    String oldClassName = className;
    className = newClassName;
    if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, JavaPackage.EJAVA_CLASS__CLASS_NAME, oldClassName, className));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClassLoaderName getLoader() {
    return loader;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setLoader(EClassLoaderName newLoader) {
    EClassLoaderName oldLoader = loader;
    loader = newLoader == null ? LOADER_EDEFAULT : newLoader;
    if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, JavaPackage.EJAVA_CLASS__LOADER, oldLoader, loader));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public Object eGet(int featureID, boolean resolve, boolean coreType) {
    switch (featureID) {
      case JavaPackage.EJAVA_CLASS__CLASS_NAME:
        return getClassName();
      case JavaPackage.EJAVA_CLASS__LOADER:
        return getLoader();
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
      case JavaPackage.EJAVA_CLASS__CLASS_NAME:
        setClassName((String)newValue);
        return;
      case JavaPackage.EJAVA_CLASS__LOADER:
        setLoader((EClassLoaderName)newValue);
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
      case JavaPackage.EJAVA_CLASS__CLASS_NAME:
        setClassName(CLASS_NAME_EDEFAULT);
        return;
      case JavaPackage.EJAVA_CLASS__LOADER:
        setLoader(LOADER_EDEFAULT);
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
      case JavaPackage.EJAVA_CLASS__CLASS_NAME:
        return CLASS_NAME_EDEFAULT == null ? className != null : !CLASS_NAME_EDEFAULT.equals(className);
      case JavaPackage.EJAVA_CLASS__LOADER:
        return loader != LOADER_EDEFAULT;
    }
    return super.eIsSet(featureID);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   */
  public String toString() {
    return className;
  }

  public boolean equals(Object obj) {
   if (getClass().equals(obj.getClass())) {
     EJavaClassImpl other = (EJavaClassImpl)obj;
     return loader.equals(other.loader) && className.equals(other.className);
   } else {
     return false;
   }
  }

  public int hashCode() {
    return loader.hashCode() + 93 * className.hashCode();
  }

} //EJavaClassImpl