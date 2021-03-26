/*
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.cast.tree;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;

/**
 * The assumption is that abstract syntax trees pertain to particular programming language
 * constructs, such as classes, methods, programs and the like. Thus, the expectation is that users
 * of CAst will typically be communicating such entities, and this interface is meant to give them a
 * mechanism to do this.
 *
 * <p>The set of kinds that are currently in this file is not meant to be exhaustive, and should be
 * extended as needed for any new languages that come along.
 *
 * @author Julian Dolby (dolby@us.ibm.com)
 */
public interface CAstEntity {

  /** This entity is a function. Children: in JavaScript, FUNCTION_ENTITY's; in Java, none. */
  public static int FUNCTION_ENTITY = 1;

  /**
   * This entity is a program script for a scripting language. Children: in JavaScript,
   * FUNCTION_ENTITY's(?); doesn't occur in Java.
   */
  public static int SCRIPT_ENTITY = 2;

  /**
   * This entity is a type in an object-oriented language. Children: typically, immediately enclosed
   * FIELD_ENTITY's, FUNCTION_ENTITY's, and TYPE_ENTITY's.
   */
  public static int TYPE_ENTITY = 3;

  /** This entity is a field in an object-oriented language. Children: usually, none */
  public static int FIELD_ENTITY = 4;

  /** This entity is a source file (i.e. a compilation unit). */
  public static int FILE_ENTITY = 5;

  /** This entity represents a rule in a logic language. */
  public static int RULE_ENTITY = 6;

  /**
   * This entity is a macro. A macro is a code body that only makes sense when expanded in the
   * context of another code body.
   */
  public static int MACRO_ENTITY = 7;

  /** This entity represents a global varible */
  public static int GLOBAL_ENTITY = 8;

  /**
   * Languages that introduce new kinds of CAstEntity should use this number as the base of integers
   * chosen to denote the new entity types.
   */
  public static final int SUB_LANGUAGE_BASE = 100;

  /**
   * What kind of entity is this? The answer should be one of the constants in this file. This has
   * no meaning to the CAPA AST interfaces, but should be meaningful to a given producer and
   * consumer of an entity.
   */
  int getKind();

  /**
   * Some programming language constructs have names. This should be it, if appropriate, and null
   * otherwise.
   */
  String getName();

  /**
   * Some programming language constructs have signatures, which are like names but usually have
   * some detail to distinguish the construct from others with the same name. Signatures often
   * denote typing information as well, but this is not required. This method should return a
   * signature if appropriate, and null otherwise.
   */
  String getSignature();

  /**
   * Some programming language constructs have named arguments. This should be their names, if
   * appropriate. Otherwise, please return an array of size 0, since null can be a pain.
   */
  String[] getArgumentNames();

  /**
   * Some programming language constructs allow arguments to have default values. This should be
   * those defaults, one per named argument above. Otherwise, please return an array of size 0,
   * since null can be a pain.
   */
  CAstNode[] getArgumentDefaults();

  /**
   * Some programming language constructs have a specific number of arguments. This should be that
   * number, if appropriate, and 0 otherwise.
   */
  int getArgumentCount();

  /**
   * Some programming language constructs have a lexical structure. This should be those constructs
   * that are directly inside the current one. The result of this method is a map from source
   * construct to the set of entities induced by that construct. Entities induced by no particular
   * construct are mapped by the null key.
   */
  Map<CAstNode, Collection<CAstEntity>> getAllScopedEntities();

  /**
   * Some programming language constructs have a lexical structure. This should be those constructs
   * that are directly inside the current one. The result of this method is the scoped entities
   * induced by the construct `construct' (i.e. a node of the AST returned by
   *
   * <p>Enclosed entities not induced by a specific AST node are mapped by the construct 'null'.
   */
  Iterator<CAstEntity> getScopedEntities(CAstNode construct);

  /** The CAPA AST of this entity. */
  CAstNode getAST();

  /** The control flow map for the CAPA AST of this entity. */
  CAstControlFlowMap getControlFlow();

  /** The map of CAstNodes to source positions for the CAPA AST of this entity. */
  CAstSourcePositionMap getSourceMap();

  /** The source position of this entity. */
  CAstSourcePositionMap.Position getPosition();

  /** The source position of the token denoting this entity's name. */
  CAstSourcePositionMap.Position getNamePosition();

  /** The source position of argument 'arg' this entity, if any; */
  CAstSourcePositionMap.Position getPosition(int arg);

  /**
   * The map from CAstNodes to types. Valid for nodes that have an explicitly declared type (e.g.
   * local vars).
   */
  CAstNodeTypeMap getNodeTypeMap();

  /**
   * Returns an Iterator over the qualifiers of the given entity, if it has any, e.g., "final",
   * "private".
   */
  Collection<CAstQualifier> getQualifiers();

  /** The CAst type of this entity. */
  CAstType getType();

  /** Returns the set of any annotations this entity may have */
  Collection<CAstAnnotation> getAnnotations();

  /** Allow finding original entity after rewrites */
  default CAstEntity getOriginal() {
    return this;
  }
}
