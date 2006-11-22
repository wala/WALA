/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.scope.impl;

import com.ibm.wala.ecore.java.scope.*;

import org.eclipse.emf.ecore.EClass;
import org.eclipse.emf.ecore.EDataType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EPackage;

import org.eclipse.emf.ecore.impl.EFactoryImpl;

import org.eclipse.emf.ecore.plugin.EcorePlugin;

/**
 * <!-- begin-user-doc -->
 * An implementation of the model <b>Factory</b>.
 * <!-- end-user-doc -->
 * @generated
 */
public class JavaScopeFactoryImpl extends EFactoryImpl implements JavaScopeFactory {
  /**
   * Creates the default factory implementation.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public static JavaScopeFactory init() {
    try {
      JavaScopeFactory theJavaScopeFactory = (JavaScopeFactory)EPackage.Registry.INSTANCE.getEFactory("http:///com/ibm/wala/wala.ecore.java.scope"); 
      if (theJavaScopeFactory != null) {
        return theJavaScopeFactory;
      }
    }
    catch (Exception exception) {
      EcorePlugin.INSTANCE.log(exception);
    }
    return new JavaScopeFactoryImpl();
  }

  /**
   * Creates an instance of the factory.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public JavaScopeFactoryImpl() {
    super();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EObject create(EClass eClass) {
    switch (eClass.getClassifierID()) {
      case JavaScopePackage.EJAVA_ANALYSIS_SCOPE: return createEJavaAnalysisScope();
      case JavaScopePackage.ECLASS_LOADER: return createEClassLoader();
      case JavaScopePackage.EBUILT_IN_MODULE: return createEBuiltInModule();
      case JavaScopePackage.EJAR_FILE: return createEJarFile();
      case JavaScopePackage.EFILE: return createEFile();
      case JavaScopePackage.ECLASS_FILE: return createEClassFile();
      case JavaScopePackage.ESOURCE_FILE: return createESourceFile();
      case JavaScopePackage.ECLASSPATH: return createEClasspath();
      default:
        throw new IllegalArgumentException("The class '" + eClass.getName() + "' is not a valid classifier");
    }
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public Object createFromString(EDataType eDataType, String initialValue) {
    switch (eDataType.getClassifierID()) {
      case JavaScopePackage.EBUILT_IN_RESOURCE:
        return createEBuiltInResourceFromString(eDataType, initialValue);
      case JavaScopePackage.ESTANDARD_CLASS_LOADER:
        return createEStandardClassLoaderFromString(eDataType, initialValue);
      default:
        throw new IllegalArgumentException("The datatype '" + eDataType.getName() + "' is not a valid classifier");
    }
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public String convertToString(EDataType eDataType, Object instanceValue) {
    switch (eDataType.getClassifierID()) {
      case JavaScopePackage.EBUILT_IN_RESOURCE:
        return convertEBuiltInResourceToString(eDataType, instanceValue);
      case JavaScopePackage.ESTANDARD_CLASS_LOADER:
        return convertEStandardClassLoaderToString(eDataType, instanceValue);
      default:
        throw new IllegalArgumentException("The datatype '" + eDataType.getName() + "' is not a valid classifier");
    }
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EJavaAnalysisScope createEJavaAnalysisScope() {
    EJavaAnalysisScopeImpl eJavaAnalysisScope = new EJavaAnalysisScopeImpl();
    return eJavaAnalysisScope;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClassLoader createEClassLoader() {
    EClassLoaderImpl eClassLoader = new EClassLoaderImpl();
    return eClassLoader;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EBuiltInModule createEBuiltInModule() {
    EBuiltInModuleImpl eBuiltInModule = new EBuiltInModuleImpl();
    return eBuiltInModule;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EJarFile createEJarFile() {
    EJarFileImpl eJarFile = new EJarFileImpl();
    return eJarFile;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EFile createEFile() {
    EFileImpl eFile = new EFileImpl();
    return eFile;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClassFile createEClassFile() {
    EClassFileImpl eClassFile = new EClassFileImpl();
    return eClassFile;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public ESourceFile createESourceFile() {
    ESourceFileImpl eSourceFile = new ESourceFileImpl();
    return eSourceFile;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EClasspath createEClasspath() {
    EClasspathImpl eClasspath = new EClasspathImpl();
    return eClasspath;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EBuiltInResource createEBuiltInResourceFromString(EDataType eDataType, String initialValue) {
    EBuiltInResource result = EBuiltInResource.get(initialValue);
    if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
    return result;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public String convertEBuiltInResourceToString(EDataType eDataType, Object instanceValue) {
    return instanceValue == null ? null : instanceValue.toString();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public EStandardClassLoader createEStandardClassLoaderFromString(EDataType eDataType, String initialValue) {
    EStandardClassLoader result = EStandardClassLoader.get(initialValue);
    if (result == null) throw new IllegalArgumentException("The value '" + initialValue + "' is not a valid enumerator of '" + eDataType.getName() + "'");
    return result;
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public String convertEStandardClassLoaderToString(EDataType eDataType, Object instanceValue) {
    return instanceValue == null ? null : instanceValue.toString();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public JavaScopePackage getJavaScopePackage() {
    return (JavaScopePackage)getEPackage();
  }

  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @deprecated
   * @generated
   */
  public static JavaScopePackage getPackage() {
    return JavaScopePackage.eINSTANCE;
  }

} //JavaScopeFactoryImpl
