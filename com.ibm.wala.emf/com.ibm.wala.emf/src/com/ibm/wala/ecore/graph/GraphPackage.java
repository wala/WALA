/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.graph;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.EReference;

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
 * @see com.ibm.wala.ecore.graph.GraphFactory
 * @model kind="package"
 * @generated
 */
public interface GraphPackage extends EPackage {
  /**
   * The package name.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  String eNAME = "graph";

  /**
   * The package namespace URI.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  String eNS_URI = "http:///com/ibm/wala/wala.ecore.graph";

  /**
   * The package namespace name.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  String eNS_PREFIX = "com.ibm.wala.graph";

  /**
   * The singleton instance of the package.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  GraphPackage eINSTANCE = com.ibm.wala.ecore.graph.impl.GraphPackageImpl.init();

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.graph.impl.EGraphImpl <em>EGraph</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.graph.impl.EGraphImpl
   * @see com.ibm.wala.ecore.graph.impl.GraphPackageImpl#getEGraph()
   * @generated
   */
  int EGRAPH = 0;

  /**
   * The feature id for the '<em><b>Nodes</b></em>' reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EGRAPH__NODES = 0;

  /**
   * The feature id for the '<em><b>Edges</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EGRAPH__EDGES = 1;

  /**
   * The number of structural features of the '<em>EGraph</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int EGRAPH_FEATURE_COUNT = 2;

  /**
   * The meta object id for the '{@link com.ibm.wala.ecore.graph.impl.ETreeImpl <em>ETree</em>}' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see com.ibm.wala.ecore.graph.impl.ETreeImpl
   * @see com.ibm.wala.ecore.graph.impl.GraphPackageImpl#getETree()
   * @generated
   */
  int ETREE = 1;

  /**
   * The feature id for the '<em><b>Nodes</b></em>' reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ETREE__NODES = EGRAPH__NODES;

  /**
   * The feature id for the '<em><b>Edges</b></em>' containment reference.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ETREE__EDGES = EGRAPH__EDGES;

  /**
   * The number of structural features of the '<em>ETree</em>' class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   * @ordered
   */
  int ETREE_FEATURE_COUNT = EGRAPH_FEATURE_COUNT + 0;


  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.graph.EGraph <em>EGraph</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>EGraph</em>'.
   * @see com.ibm.wala.ecore.graph.EGraph
   * @generated
   */
  EClass getEGraph();

  /**
   * Returns the meta object for the reference '{@link com.ibm.wala.ecore.graph.EGraph#getNodes <em>Nodes</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the reference '<em>Nodes</em>'.
   * @see com.ibm.wala.ecore.graph.EGraph#getNodes()
   * @see #getEGraph()
   * @generated
   */
  EReference getEGraph_Nodes();

  /**
   * Returns the meta object for the containment reference '{@link com.ibm.wala.ecore.graph.EGraph#getEdges <em>Edges</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for the containment reference '<em>Edges</em>'.
   * @see com.ibm.wala.ecore.graph.EGraph#getEdges()
   * @see #getEGraph()
   * @generated
   */
  EReference getEGraph_Edges();

  /**
   * Returns the meta object for class '{@link com.ibm.wala.ecore.graph.ETree <em>ETree</em>}'.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the meta object for class '<em>ETree</em>'.
   * @see com.ibm.wala.ecore.graph.ETree
   * @generated
   */
  EClass getETree();

  /**
   * Returns the factory that creates the instances of the model.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @return the factory that creates the instances of the model.
   * @generated
   */
  GraphFactory getGraphFactory();

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
     * The meta object literal for the '{@link com.ibm.wala.ecore.graph.impl.EGraphImpl <em>EGraph</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.graph.impl.EGraphImpl
     * @see com.ibm.wala.ecore.graph.impl.GraphPackageImpl#getEGraph()
     * @generated
     */
    EClass EGRAPH = eINSTANCE.getEGraph();

    /**
     * The meta object literal for the '<em><b>Nodes</b></em>' reference feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EReference EGRAPH__NODES = eINSTANCE.getEGraph_Nodes();

    /**
     * The meta object literal for the '<em><b>Edges</b></em>' containment reference feature.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @generated
     */
    EReference EGRAPH__EDGES = eINSTANCE.getEGraph_Edges();

    /**
     * The meta object literal for the '{@link com.ibm.wala.ecore.graph.impl.ETreeImpl <em>ETree</em>}' class.
     * <!-- begin-user-doc -->
     * <!-- end-user-doc -->
     * @see com.ibm.wala.ecore.graph.impl.ETreeImpl
     * @see com.ibm.wala.ecore.graph.impl.GraphPackageImpl#getETree()
     * @generated
     */
    EClass ETREE = eINSTANCE.getETree();

  }

} //GraphPackage
