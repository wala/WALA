/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.graph.impl;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;
import org.eclipse.emf.ecore.impl.EFactoryImpl;
import org.eclipse.emf.ecore.plugin.EcorePlugin;

import com.ibm.wala.annotations.Internal;
import com.ibm.wala.ecore.graph.EGraph;
import com.ibm.wala.ecore.graph.ETree;
import com.ibm.wala.ecore.graph.GraphFactory;
import com.ibm.wala.ecore.graph.GraphPackage;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Factory</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class GraphFactoryImpl extends EFactoryImpl implements GraphFactory {
  /**
   * Creates the default factory implementation.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public static GraphFactory init() {
    try {
      GraphFactory theGraphFactory = (GraphFactory)EPackage.Registry.INSTANCE.getEFactory("http:///com/ibm/wala/wala.ecore.graph"); 
      if (theGraphFactory != null) {
        return theGraphFactory;
      }
    }
    catch (Exception exception) {
      EcorePlugin.INSTANCE.log(exception);
    }
    return new GraphFactoryImpl();
  }

  /**
   * Creates an instance of the factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Internal
  public GraphFactoryImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EObject create(EClass eClass) {
    switch (eClass.getClassifierID()) {
      case GraphPackage.EGRAPH: return createEGraph();
      case GraphPackage.ETREE: return createETree();
      default:
        throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier");
    }
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EGraph createEGraph() {
    EGraphImpl eGraph = new EGraphImpl();
    return eGraph;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public ETree createETree() {
    ETreeImpl eTree = new ETreeImpl();
    return eTree;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public GraphPackage getGraphPackage() {
    return (GraphPackage)getEPackage();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @deprecated
   * @generated
   */
  public static GraphPackage getPackage() {
    return GraphPackage.eINSTANCE;
  }

} //GraphFactoryImpl
