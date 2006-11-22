/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.common.impl;

import java.util.Collection;

import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.EObjectImpl;
import org.eclipse.emf.ecore.util.EObjectContainmentEList;
import org.eclipse.emf.ecore.util.InternalEList;

import com.ibm.wala.ecore.common.CommonPackage;
import com.ibm.wala.ecore.common.EContainer;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>EContainer</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.common.impl.EContainerImpl#getContents <em>Contents</em>}</li>
 *   <li>{@link com.ibm.wala.ecore.common.impl.EContainerImpl#getContainees <em>Containees</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class EContainerImpl extends EObjectImpl implements EContainer {
  /**
   * The cached value of the '{@link #getContainees() <em>Containees</em>}' containment reference list.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getContainees()
   * @generated
   * @ordered
   */
  protected EList containees = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected EContainerImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected EClass eStaticClass() {
    return CommonPackage.Literals.ECONTAINER;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   */
  public EList getContents() {
    return getContainees();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EList getContainees() {
    if (containees == null) {
      containees = new EObjectContainmentEList(EObject.class, this, CommonPackage.ECONTAINER__CONTAINEES);
    }
    return containees;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
    switch (featureID) {
      case CommonPackage.ECONTAINER__CONTAINEES:
        return ((InternalEList)getContainees()).basicRemove(otherEnd, msgs);
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
      case CommonPackage.ECONTAINER__CONTENTS:
        return getContents();
      case CommonPackage.ECONTAINER__CONTAINEES:
        return getContainees();
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
      case CommonPackage.ECONTAINER__CONTENTS:
        getContents().clear();
        getContents().addAll((Collection)newValue);
        return;
      case CommonPackage.ECONTAINER__CONTAINEES:
        getContainees().clear();
        getContainees().addAll((Collection)newValue);
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
      case CommonPackage.ECONTAINER__CONTENTS:
        getContents().clear();
        return;
      case CommonPackage.ECONTAINER__CONTAINEES:
        getContainees().clear();
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
      case CommonPackage.ECONTAINER__CONTENTS:
        return !getContents().isEmpty();
      case CommonPackage.ECONTAINER__CONTAINEES:
        return containees != null && !containees.isEmpty();
    }
    return super.eIsSet(featureID);
  }

} //EContainerImpl