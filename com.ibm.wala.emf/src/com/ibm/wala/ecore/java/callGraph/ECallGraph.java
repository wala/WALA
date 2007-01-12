/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.callGraph;

import org.eclipse.emf.common.util.EList;

import com.ibm.wala.ecore.graph.EGraph;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>ECall Graph</b></em>'.
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link com.ibm.wala.ecore.java.callGraph.ECallGraph#getEntrypoints <em>Entrypoints</em>}</li>
 * </ul>
 * </p>
 *
 * @see com.ibm.wala.ecore.java.callGraph.CallGraphPackage#getECallGraph()
 * @model
 * @generated
 */
public interface ECallGraph extends EGraph {
  /**
   * Returns the value of the '<em><b>Entrypoints</b></em>' reference list.
   * The list contents are of type {@link com.ibm.wala.ecore.java.EJavaMethod}.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Entrypoints</em>' reference list isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Entrypoints</em>' reference list.
   * @see com.ibm.wala.ecore.java.callGraph.CallGraphPackage#getECallGraph_Entrypoints()
   * @model type="com.ibm.wala.ecore.java.EJavaMethod"
   * @generated
   */
  EList getEntrypoints();

} // ECallGraph