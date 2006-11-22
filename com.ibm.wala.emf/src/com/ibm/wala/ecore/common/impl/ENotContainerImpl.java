/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.common.impl;

import com.ibm.wala.ecore.common.CommonPackage;
import com.ibm.wala.ecore.common.ENotContainer;

import java.util.Collection;

import org.eclipse.emf.common.util.EList;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;

import org.eclipse.emf.ecore.impl.EObjectImpl;

import org.eclipse.emf.ecore.util.EObjectResolvingEList;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>ENot Container</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.common.impl.ENotContainerImpl#getContents <em>Contents</em>}</li>
 *   <li>{@link com.ibm.wala.ecore.common.impl.ENotContainerImpl#getElements <em>Elements</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class ENotContainerImpl extends EObjectImpl implements ENotContainer {
  /**
   * The cached value of the '{@link #getElements() <em>Elements</em>}' reference list.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getElements()
   * @generated
   * @ordered
   */
  protected EList elements = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected ENotContainerImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected EClass eStaticClass() {
    return CommonPackage.Literals.ENOT_CONTAINER;
  }

  /**
   */
  public EList getContents() {
    return getElements();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EList getElements() {
    if (elements == null) {
      elements = new EObjectResolvingEList(EObject.class, this, CommonPackage.ENOT_CONTAINER__ELEMENTS);
    }
    return elements;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public Object eGet(int featureID, boolean resolve, boolean coreType) {
    switch (featureID) {
      case CommonPackage.ENOT_CONTAINER__CONTENTS:
        return getContents();
      case CommonPackage.ENOT_CONTAINER__ELEMENTS:
        return getElements();
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
      case CommonPackage.ENOT_CONTAINER__CONTENTS:
        getContents().clear();
        getContents().addAll((Collection)newValue);
        return;
      case CommonPackage.ENOT_CONTAINER__ELEMENTS:
        getElements().clear();
        getElements().addAll((Collection)newValue);
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
      case CommonPackage.ENOT_CONTAINER__CONTENTS:
        getContents().clear();
        return;
      case CommonPackage.ENOT_CONTAINER__ELEMENTS:
        getElements().clear();
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
      case CommonPackage.ENOT_CONTAINER__CONTENTS:
        return !getContents().isEmpty();
      case CommonPackage.ENOT_CONTAINER__ELEMENTS:
        return elements != null && !elements.isEmpty();
    }
    return super.eIsSet(featureID);
  }

} //ENotContainerImpl