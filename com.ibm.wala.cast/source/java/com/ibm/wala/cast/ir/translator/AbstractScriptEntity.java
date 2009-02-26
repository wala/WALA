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

  public int getKind() {
    return SCRIPT_ENTITY;
  }

  protected File getFile() {
    return file;
  }

  public String getName() {
    return "script " + file.getName();
  }

  public String toString() {
    return "script " + file.getName();
  }

  public String[] getArgumentNames() {
    return new String[] { "script object" };
  }

  public CAstNode[] getArgumentDefaults() {
    return new CAstNode[0];
  }

  public int getArgumentCount() {
    return 1;
  }

  public Collection<CAstQualifier> getQualifiers() {
    return Collections.emptySet();
  }

  public String getFileName() {
    return file.getAbsolutePath();
  }
}