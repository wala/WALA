/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.scope.impl;

import java.util.Collection;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;
import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;

import com.ibm.wala.ecore.java.scope.EClassLoader;
import com.ibm.wala.ecore.java.scope.EModule;
import com.ibm.wala.ecore.java.scope.JavaScopePackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>EClass Loader</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.java.scope.impl.EClassLoaderImpl#getModules <em>Modules</em>}</li>
 *   <li>{@link com.ibm.wala.ecore.java.scope.impl.EClassLoaderImpl#getLoaderName <em>Loader Name</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class EClassLoaderImpl extends EObjectImpl implements EClassLoader {
  /**
   * The cached value of the '{@link #getModules() <em>Modules</em>}' containment reference list.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getModules()
   * @generated
   * @ordered
   */
  protected EList modules = null;

  /**
   * The default value of the '{@link #getLoaderName() <em>Loader Name</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getLoaderName()
   * @generated
   * @ordered
   */
  protected static final String LOADER_NAME_EDEFAULT = null;

  /**
   * The cached value of the '{@link #getLoaderName() <em>Loader Name</em>}' attribute.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getLoaderName()
   * @generated
   * @ordered
   */
  protected String loaderName = LOADER_NAME_EDEFAULT;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected EClassLoaderImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected EClass eStaticClass() {
    return JavaScopePackage.Literals.ECLASS_LOADER;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EList getModules() {
    if (modules == null) {
      modules = new EObjectContainmentEList(EModule.class, this, JavaScopePackage.ECLASS_LOADER__MODULES);
    }
    return modules;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public String getLoaderName() {
    return loaderName;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setLoaderName(String newLoaderName) {
    String oldLoaderName = loaderName;
    loaderName = newLoaderName;
    if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, JavaScopePackage.ECLASS_LOADER__LOADER_NAME, oldLoaderName, loaderName));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
    switch (featureID) {
      case JavaScopePackage.ECLASS_LOADER__MODULES:
        return ((InternalEList)getModules()).basicRemove(otherEnd, msgs);
    }
    return super.eInverseRemove(otherEnd, featureID, msgs);
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public Object eGet(int featureID, boolean resolve, boolean coreType) {
    switch (featureID) {
      case JavaScopePackage.ECLASS_LOADER__MODULES:
        return getModules();
      case JavaScopePackage.ECLASS_LOADER__LOADER_NAME:
        return getLoaderName();
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
      case JavaScopePackage.ECLASS_LOADER__MODULES:
        getModules().clear();
        getModules().addAll((Collection)newValue);
        return;
      case JavaScopePackage.ECLASS_LOADER__LOADER_NAME:
        setLoaderName((String)newValue);
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
      case JavaScopePackage.ECLASS_LOADER__MODULES:
        getModules().clear();
        return;
      case JavaScopePackage.ECLASS_LOADER__LOADER_NAME:
        setLoaderName(LOADER_NAME_EDEFAULT);
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
      case JavaScopePackage.ECLASS_LOADER__MODULES:
        return modules != null && !modules.isEmpty();
      case JavaScopePackage.ECLASS_LOADER__LOADER_NAME:
        return LOADER_NAME_EDEFAULT == null ? loaderName != null : !LOADER_NAME_EDEFAULT.equals(loaderName);
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
    result.append(" (loaderName: ");
    result.append(loaderName);
    result.append(')');
    return result.toString();
  }

} //EClassLoaderImpl