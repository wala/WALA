/******************************************************************************
 * Copyright (c) 2002 - 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

/**
 * 
 */
package com.ibm.wala.cast.ir.translator;

import java.io.File;
import java.util.Collection;
import java.util.Collections;

import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstQualifier;
import com.ibm.wala.cast.tree.CAstType;

public class AbstractScriptEntity extends AbstractCodeEntity {
  private final File file;

  public AbstractScriptEntity(File file, CAstType type) {
    super(type);
    this.file = file;
  }

  public AbstractScriptEntity(String file, CAstType type) {
    this(new File(file), type);
  }

  @Override
  public int getKind() {
    return SCRIPT_ENTITY;
  }

  protected File getFile() {
    return file;
  }

  @Override
  public String getName() {
    return "script " + file.getName();
  }

  @Override
  public String toString() {
    return "script " + file.getName();
  }

  @Override
  public String[] getArgumentNames() {
    return new String[] { "script object" };
  }

  @Override
  public CAstNode[] getArgumentDefaults() {
    return new CAstNode[0];
  }

  @Override
  public int getArgumentCount() {
    return 1;
  }

  @Override
  public Collection<CAstQualifier> getQualifiers() {
    return Collections.emptySet();
  }

  public String getFileName() {
    return file.getAbsolutePath();
  }
}
