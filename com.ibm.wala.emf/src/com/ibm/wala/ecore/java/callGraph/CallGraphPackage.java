/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.callGraph;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

import com.ibm.wala.ecore.graph.GraphPackage;

/**
 * <!-- begin-user-doc -->
 * The <b>Package</b> for the model.
 * It contains accessors for the meta objects to represent
 * <ul>
 *   <li>each class,</li>
 *   <li>each feature of each class,</li>
 *   <li>each enum,</li>
 *   <li>and each data type</li>
 * </ul>
 * <!-- end-user-doc -->
 * @see com.ibm.wala.ecore.java.callGraph.CallGraphFactory
 * @model kind="package"
 * @generated
 */
public interface CallGraphPackage extends EPackage {
  /**
   * The package name.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  String eNAME = "callGraph";

  /**
   * The package namespace URI.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  String eNS_URI = "http:///com/ibm/wala/wala.ecore.java.callGraph";

  /**
   * The package namespace name.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  String eNS_PREFIX = "com.ibm.wala.java.callGraph";

  /**
   * The singleton instance of the package.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  CallGraphPackage eINSTANCE = com.ibm.wala.ecore.java.callGraph.impl.CallGraphPackageImpl.init();

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.java.callGraph.impl.ECallGraphImpl <em>ECall Graph</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.java.callGraph.impl.ECallGraphImpl
   * @see com.ibm.wala.ecore.java.callGraph.impl.CallGraphPackageImpl#getECallGraph()
   * @generated
   */
  int ECALL_GRAPH = 0;

  /**
   * The feature id for the '<em><b>Nodes</b></em>' reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ECALL_GRAPH__NODES = GraphPackage.EGRAPH__NODES;

  /**
   * The feature id for the '<em><b>Edges</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ECALL_GRAPH__EDGES = GraphPackage.EGRAPH__EDGES;

  /**
   * The feature id for the '<em><b>Entrypoints</b></em>' reference list.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ECALL_GRAPH__ENTRYPOINTS = GraphPackage.EGRAPH_FEATURE_COUNT + 0;

  /**
   * The number of structural features of the '<em>ECall Graph</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ECALL_GRAPH_FEATURE_COUNT = GraphPackage.EGRAPH_FEATURE_COUNT + 1;


  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.java.callGraph.ECallGraph <em>ECall Graph</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>ECall Graph</em>'.
   * @see com.ibm.wala.ecore.java.callGraph.ECallGraph
   * @generated
   */
  EClass getECallGraph();

  /**
   * Returns the meta object for the reference list '{@link com.ibm.wala.ecore.java.callGraph.ECallGraph#getEntrypoints <em>Entrypoints</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the reference list '<em>Entrypoints</em>'.
   * @see com.ibm.wala.ecore.java.callGraph.ECallGraph#getEntrypoints()
   * @see #getECallGraph()
   * @generated
   */
  EReference getECallGraph_Entrypoints();

  /**
   * Returns the factory that creates the instances of the model.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the factory that creates the instances of the model.
   * @generated
   */
  CallGraphFactory getCallGraphFactory();

  /**
   * <!-- begin-user-doc -->
   * Defines literals for the meta objects that represent
   * <ul>
   *   <li>each class,</li>
   *   <li>each feature of each class,</li>
   *   <li>each enum,</li>
   *   <li>and each data type</li>
   * </ul>
   * <!-- end-user-doc -->
   * @generated
   */
  interface Literals {
    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.java.callGraph.impl.ECallGraphImpl <em>ECall Graph</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.java.callGraph.impl.ECallGraphImpl
     * @see com.ibm.wala.ecore.java.callGraph.impl.CallGraphPackageImpl#getECallGraph()
     * @generated
     */
    EClass ECALL_GRAPH = eINSTANCE.getECallGraph();

    /**
     * The meta object literal for the '<em><b>Entrypoints</b></em>' reference list feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EReference ECALL_GRAPH__ENTRYPOINTS = eINSTANCE.getECallGraph_Entrypoints();

  }

} //CallGraphPackage
