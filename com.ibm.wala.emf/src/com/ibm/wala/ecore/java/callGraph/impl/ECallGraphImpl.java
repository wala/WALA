/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.callGraph.impl;

import java.util.Collection;

import org.eclipse.emf.common.util.EList;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.util.EObjectResolvingEList;

import com.ibm.wala.ecore.graph.impl.EGraphImpl;
import com.ibm.wala.ecore.java.EJavaMethod;
import com.ibm.wala.ecore.java.callGraph.CallGraphPackage;
import com.ibm.wala.ecore.java.callGraph.ECallGraph;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>ECall Graph</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.java.callGraph.impl.ECallGraphImpl#getEntrypoints <em>Entrypoints</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class ECallGraphImpl extends EGraphImpl implements ECallGraph {
  /**
   * The cached value of the '{@link #getEntrypoints() <em>Entrypoints</em>}' reference list.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getEntrypoints()
   * @generated
   * @ordered
   */
  protected EList entrypoints = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected ECallGraphImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected EClass eStaticClass() {
    return CallGraphPackage.Literals.ECALL_GRAPH;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EList getEntrypoints() {
    if (entrypoints == null) {
      entrypoints = new EObjectResolvingEList(EJavaMethod.class, this, CallGraphPackage.ECALL_GRAPH__ENTRYPOINTS);
    }
    return entrypoints;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public Object eGet(int featureID, boolean resolve, boolean coreType) {
    switch (featureID) {
      case CallGraphPackage.ECALL_GRAPH__ENTRYPOINTS:
        return getEntrypoints();
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
      case CallGraphPackage.ECALL_GRAPH__ENTRYPOINTS:
        getEntrypoints().clear();
        getEntrypoints().addAll((Collection)newValue);
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
      case CallGraphPackage.ECALL_GRAPH__ENTRYPOINTS:
        getEntrypoints().clear();
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
      case CallGraphPackage.ECALL_GRAPH__ENTRYPOINTS:
        return entrypoints != null && !entrypoints.isEmpty();
    }
    return super.eIsSet(featureID);
  }

} //ECallGraphImpl