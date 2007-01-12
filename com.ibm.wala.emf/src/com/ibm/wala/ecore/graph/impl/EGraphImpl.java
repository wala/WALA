/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.graph.impl;

import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.common.notify.NotificationChain;
import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.InternalEObject;
import org.eclipse.emf.ecore.impl.ENotificationImpl;
import org.eclipse.emf.ecore.impl.EObjectImpl;

import com.ibm.wala.ecore.common.ECollection;
import com.ibm.wala.ecore.common.ERelation;
import com.ibm.wala.ecore.graph.EGraph;
import com.ibm.wala.ecore.graph.GraphPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model object '<em><b>EGraph</b></em>'.
 * <!-- end-user-doc -->
 * <p>
 * The following features are implemented:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.graph.impl.EGraphImpl#getNodes <em>Nodes</em>}</li>
 *   <li>{@link com.ibm.wala.ecore.graph.impl.EGraphImpl#getEdges <em>Edges</em>}</li>
 * </ul>
 * </p>
 *
 * @generated
 */
public class EGraphImpl extends EObjectImpl implements EGraph {
  /**
   * The cached value of the '{@link #getNodes() <em>Nodes</em>}' reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getNodes()
   * @generated
   * @ordered
   */
  protected ECollection nodes = null;

  /**
   * The cached value of the '{@link #getEdges() <em>Edges</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #getEdges()
   * @generated
   * @ordered
   */
  protected ERelation edges = null;

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected EGraphImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  protected EClass eStaticClass() {
    return GraphPackage.Literals.EGRAPH;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public ECollection getNodes() {
    if (nodes != null && nodes.eIsProxy()) {
      InternalEObject oldNodes = (InternalEObject)nodes;
      nodes = (ECollection)eResolveProxy(oldNodes);
      if (nodes != oldNodes) {
        if (eNotificationRequired())
          eNotify(new ENotificationImpl(this, Notification.RESOLVE, GraphPackage.EGRAPH__NODES, oldNodes, nodes));
      }
    }
    return nodes;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public ECollection basicGetNodes() {
    return nodes;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setNodes(ECollection newNodes) {
    ECollection oldNodes = nodes;
    nodes = newNodes;
    if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, GraphPackage.EGRAPH__NODES, oldNodes, nodes));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public ERelation getEdges() {
    return edges;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public NotificationChain basicSetEdges(ERelation newEdges, NotificationChain msgs) {
    ERelation oldEdges = edges;
    edges = newEdges;
    if (eNotificationRequired()) {
      ENotificationImpl notification = new ENotificationImpl(this, Notification.SET, GraphPackage.EGRAPH__EDGES, oldEdges, newEdges);
      if (msgs == null) msgs = notification; else msgs.add(notification);
    }
    return msgs;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public void setEdges(ERelation newEdges) {
    if (newEdges != edges) {
      NotificationChain msgs = null;
      if (edges != null)
        msgs = ((InternalEObject)edges).eInverseRemove(this, EOPPOSITE_FEATURE_BASE - GraphPackage.EGRAPH__EDGES, null, msgs);
      if (newEdges != null)
        msgs = ((InternalEObject)newEdges).eInverseAdd(this, EOPPOSITE_FEATURE_BASE - GraphPackage.EGRAPH__EDGES, null, msgs);
      msgs = basicSetEdges(newEdges, msgs);
      if (msgs != null) msgs.dispatch();
    }
    else if (eNotificationRequired())
      eNotify(new ENotificationImpl(this, Notification.SET, GraphPackage.EGRAPH__EDGES, newEdges, newEdges));
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public NotificationChain eInverseRemove(InternalEObject otherEnd, int featureID, NotificationChain msgs) {
    switch (featureID) {
      case GraphPackage.EGRAPH__EDGES:
        return basicSetEdges(null, msgs);
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
      case GraphPackage.EGRAPH__NODES:
        if (resolve) return getNodes();
        return basicGetNodes();
      case GraphPackage.EGRAPH__EDGES:
        return getEdges();
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
      case GraphPackage.EGRAPH__NODES:
        setNodes((ECollection)newValue);
        return;
      case GraphPackage.EGRAPH__EDGES:
        setEdges((ERelation)newValue);
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
      case GraphPackage.EGRAPH__NODES:
        setNodes((ECollection)null);
        return;
      case GraphPackage.EGRAPH__EDGES:
        setEdges((ERelation)null);
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
      case GraphPackage.EGRAPH__NODES:
        return nodes != null;
      case GraphPackage.EGRAPH__EDGES:
        return edges != null;
    }
    return super.eIsSet(featureID);
  }

} //EGraphImpl