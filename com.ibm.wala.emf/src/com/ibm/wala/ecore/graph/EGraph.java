/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.graph;

import com.ibm.wala.ecore.common.ECollection;
import com.ibm.wala.ecore.common.ERelation;

import org.eclipse.emf.ecore.EObject;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>EGraph</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.graph.EGraph#getNodes <em>Nodes</em>}</li>
 *   <li>{@link com.ibm.wala.ecore.graph.EGraph#getEdges <em>Edges</em>}</li>
 * </ul>
 * </p>
 *
 * @see com.ibm.wala.ecore.graph.GraphPackage#getEGraph()
 * @model
 * @generated
 */
public interface EGraph extends EObject {
  /**
   * Returns the value of the '<em><b>Nodes</b></em>' reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Nodes</em>' reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Nodes</em>' reference.
   * @see #setNodes(ECollection)
   * @see com.ibm.wala.ecore.graph.GraphPackage#getEGraph_Nodes()
   * @model required="true"
   * @generated
   */
  ECollection getNodes();

  /**
   * Sets the value of the '{@link com.ibm.wala.ecore.graph.EGraph#getNodes <em>Nodes</em>}' reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Nodes</em>' reference.
   * @see #getNodes()
   * @generated
   */
  void setNodes(ECollection value);

  /**
   * Returns the value of the '<em><b>Edges</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Edges</em>' containment reference isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Edges</em>' containment reference.
   * @see #setEdges(ERelation)
   * @see com.ibm.wala.ecore.graph.GraphPackage#getEGraph_Edges()
   * @model containment="true" required="true"
   * @generated
   */
  ERelation getEdges();

  /**
   * Sets the value of the '{@link com.ibm.wala.ecore.graph.EGraph#getEdges <em>Edges</em>}' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @param value the new value of the '<em>Edges</em>' containment reference.
   * @see #getEdges()
   * @generated
   */
  void setEdges(ERelation value);

} // EGraph