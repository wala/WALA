/**
 * <copyright>
 * </copyright>
 *
 * $Id$
 */
package com.ibm.wala.ecore.java.scope;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.eclipse.emf.common.util.AbstractEnumerator;

/**
 * <!-- begin-user-doc -->
 * A representation of the literals of the enumeration '<em><b>EBuilt In Resource</b></em>',
 * and utility methods for working with them.
 * <!-- end-user-doc -->
 * @see com.ibm.wala.ecore.java.scope.JavaScopePackage#getEBuiltInResource()
 * @model
 * @generated
 */
public final class EBuiltInResource extends AbstractEnumerator {
  /**
   * The '<em><b>Default J2SE Libs</b></em>' literal value.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of '<em><b>Default J2SE Libs</b></em>' literal object isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @see #DEFAULT_J2SE_LIBS_LITERAL
   * @model name="DefaultJ2SELibs"
   * @generated
   * @ordered
   */
  public static final int DEFAULT_J2SE_LIBS = 0;

  /**
   * The '<em><b>Default J2EE Libs</b></em>' literal value.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of '<em><b>Default J2EE Libs</b></em>' literal object isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @see #DEFAULT_J2EE_LIBS_LITERAL
   * @model name="DefaultJ2EELibs"
   * @generated
   * @ordered
   */
  public static final int DEFAULT_J2EE_LIBS = 1;

  /**
   * The '<em><b>Primordial jar model</b></em>' literal value.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of '<em><b>Primordial jar model</b></em>' literal object isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @see #PRIMORDIAL_JAR_MODEL_LITERAL
   * @model name="primordial_jar_model"
   * @generated
   * @ordered
   */
  public static final int PRIMORDIAL_JAR_MODEL = 2;

  /**
   * The '<em><b>Extension jar model</b></em>' literal value.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of '<em><b>Extension jar model</b></em>' literal object isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @see #EXTENSION_JAR_MODEL_LITERAL
   * @model name="extension_jar_model"
   * @generated
   * @ordered
   */
  public static final int EXTENSION_JAR_MODEL = 3;

  /**
   * The '<em><b>Default J2SE Libs</b></em>' literal object.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #DEFAULT_J2SE_LIBS
   * @generated
   * @ordered
   */
  public static final EBuiltInResource DEFAULT_J2SE_LIBS_LITERAL = new EBuiltInResource(DEFAULT_J2SE_LIBS, "DefaultJ2SELibs", "DefaultJ2SELibs");

  /**
   * The '<em><b>Default J2EE Libs</b></em>' literal object.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #DEFAULT_J2EE_LIBS
   * @generated
   * @ordered
   */
  public static final EBuiltInResource DEFAULT_J2EE_LIBS_LITERAL = new EBuiltInResource(DEFAULT_J2EE_LIBS, "DefaultJ2EELibs", "DefaultJ2EELibs");

  /**
   * The '<em><b>Primordial jar model</b></em>' literal object.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #PRIMORDIAL_JAR_MODEL
   * @generated
   * @ordered
   */
  public static final EBuiltInResource PRIMORDIAL_JAR_MODEL_LITERAL = new EBuiltInResource(PRIMORDIAL_JAR_MODEL, "primordial_jar_model", "primordial_jar_model");

  /**
   * The '<em><b>Extension jar model</b></em>' literal object.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @see #EXTENSION_JAR_MODEL
   * @generated
   * @ordered
   */
  public static final EBuiltInResource EXTENSION_JAR_MODEL_LITERAL = new EBuiltInResource(EXTENSION_JAR_MODEL, "extension_jar_model", "extension_jar_model");

  /**
   * An array of all the '<em><b>EBuilt In Resource</b></em>' enumerators.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private static final EBuiltInResource[] VALUES_ARRAY =
    new EBuiltInResource[] {
      DEFAULT_J2SE_LIBS_LITERAL,
      DEFAULT_J2EE_LIBS_LITERAL,
      PRIMORDIAL_JAR_MODEL_LITERAL,
      EXTENSION_JAR_MODEL_LITERAL,
    };

  /**
   * A public read-only list of all the '<em><b>EBuilt In Resource</b></em>' enumerators.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public static final List VALUES = Collections.unmodifiableList(Arrays.asList(VALUES_ARRAY));

  /**
   * Returns the '<em><b>EBuilt In Resource</b></em>' literal with the specified literal value.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public static EBuiltInResource get(String literal) {
    for (int i = 0; i < VALUES_ARRAY.length; ++i) {
      EBuiltInResource result = VALUES_ARRAY[i];
      if (result.toString().equals(literal)) {
        return result;
      }
    }
    return null;
  }

  /**
   * Returns the '<em><b>EBuilt In Resource</b></em>' literal with the specified name.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public static EBuiltInResource getByName(String name) {
    for (int i = 0; i < VALUES_ARRAY.length; ++i) {
      EBuiltInResource result = VALUES_ARRAY[i];
      if (result.getName().equals(name)) {
        return result;
      }
    }
    return null;
  }

  /**
   * Returns the '<em><b>EBuilt In Resource</b></em>' literal with the specified integer value.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public static EBuiltInResource get(int value) {
    switch (value) {
      case DEFAULT_J2SE_LIBS: return DEFAULT_J2SE_LIBS_LITERAL;
      case DEFAULT_J2EE_LIBS: return DEFAULT_J2EE_LIBS_LITERAL;
      case PRIMORDIAL_JAR_MODEL: return PRIMORDIAL_JAR_MODEL_LITERAL;
      case EXTENSION_JAR_MODEL: return EXTENSION_JAR_MODEL_LITERAL;
    }
    return null;	
  }

  /**
   * Only this class can construct instances.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  private EBuiltInResource(int value, String name, String literal) {
    super(value, name, literal);
  }

} //EBuiltInResource
