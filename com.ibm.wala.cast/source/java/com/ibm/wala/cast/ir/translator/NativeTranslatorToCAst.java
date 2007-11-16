package com.ibm.wala.cast.ir.translator;

import com.ibm.wala.cast.ir.translator.NativeBridge;
import com.ibm.wala.cast.tree.*;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.impl.*;
import com.ibm.wala.cast.tree.visit.*;
import com.ibm.wala.cast.util.*;
import com.ibm.wala.util.collections.*;
import com.ibm.wala.util.debug.*;

import java.io.*;
import java.net.*;
import java.util.*;

public abstract class NativeTranslatorToCAst extends NativeBridge {

  protected abstract class NativeEntity implements CAstEntity {
    private Position sourcePosition;
    
    private final Map scopedEntities = 
      new HashMap();

    public Map getAllScopedEntities() {
      return scopedEntities;
    }

    public String getSignature() {
      Assertions.UNREACHABLE();
      return null;
    }

    public void setPosition(Position pos) {
      sourcePosition = pos;
    }

    public Position getPosition() {
      return sourcePosition;
    }

    public Iterator getScopedEntities(CAstNode construct) {
      if (scopedEntities.containsKey(construct)) {
	return ((Set)scopedEntities.get(construct)).iterator();
      } else {
	return EmptyIterator.instance();
      }
    }

    private void addScopedEntity(CAstNode construct, CAstEntity child) {
      if (!scopedEntities.containsKey(construct)) {
	scopedEntities.put(construct, new HashSet(1));
      }

      ((Set)scopedEntities.get(construct)).add( child );
    }
  }

  protected abstract class NativeCodeEntity extends NativeEntity {
    protected final CAstSourcePositionRecorder src =       
      new CAstSourcePositionRecorder();
    protected final CAstControlFlowRecorder cfg = 
      new CAstControlFlowRecorder(src);
    protected final CAstNodeTypeMapRecorder types =
      new CAstNodeTypeMapRecorder();

    protected final CAstType type;

    protected CAstNode Ast;

    protected NativeCodeEntity(CAstType type) {
      this.type = type;
    }

    public CAstNode getAST() {
      return Ast;
    }

    public CAstType getType() {
      return type;
    }

    public CAstControlFlowMap getControlFlow() {
      return cfg;
    }

    public CAstSourcePositionMap getSourceMap() {
      return src;
    }

    public CAstNodeTypeMap getNodeTypeMap() {
      return types;
    }

    public void setGotoTarget(CAstNode from, CAstNode to) {
      setLabelledGotoTarget(from, to, null);
    }

    public void setLabelledGotoTarget(CAstNode from, CAstNode to, Object label) {
      if (! cfg.isMapped(from)) {
	cfg.map(from, from);
      }
      if (! cfg.isMapped(to)) {
	cfg.map(to, to);
      }
      cfg.add(from, to, label);
    }

    public void setNodePosition(CAstNode n, Position pos) {
      src.setPosition(n, pos);
    }

    public void setNodeType(CAstNode n, CAstType type) {
      types.add(n, type);
    }
  }

  protected abstract class NativeDataEntity extends NativeEntity {
    public CAstNode getAST() {
      return null;
    }

    public CAstControlFlowMap getControlFlow() {
      return null;
    }

    public CAstSourcePositionMap getSourceMap() {
      return null;
    }

    public CAstNodeTypeMap getNodeTypeMap() {
      return null;
    }

    public String[] getArgumentNames() {
      return new String[0];
    }

    public CAstNode[] getArgumentDefaults() {
      return new CAstNode[0];
    }

    public int getArgumentCount() {
      return 0;
    }
  }

  protected class NativeFieldEntity extends NativeDataEntity {
    private final String name;
    private final Set modifiers;
    private final CAstEntity declaringClass;

    public NativeFieldEntity(String name, Set modifiers, boolean isStatic, CAstEntity declaringClass) {
      this.name = name;
      this.declaringClass = declaringClass;

      this.modifiers = new HashSet();
      if (modifiers != null) {
	this.modifiers.addAll(modifiers);
      }
      if (isStatic) {
	this.modifiers.add(CAstQualifier.STATIC);
      }
    }

    public String toString() {
      return "field " + name + " of " + declaringClass.getName();
    }

    public int getKind() {
      return FIELD_ENTITY;
    }

    public String getName() {
      return name;
    }

    public CAstType getType() {
      Assertions.UNREACHABLE();
      return null;
    }

    public Collection getQualifiers() {
      return modifiers;
    }
  }

  protected class NativeClassEntity extends NativeDataEntity {
    private final CAstType.Class type;

    public NativeClassEntity(CAstType.Class type) {
      this.type = type;
    }

    public String toString() {
      return "class " + type.getName();
    }

    public int getKind() {
      return TYPE_ENTITY;
    }

    public String getName() {
      return type.getName();
    }

    public CAstType getType() {
      return type;
    }

    public Collection getQualifiers() {
      return type.getQualifiers();
    }
  };
     
  protected class NativeScriptEntity extends NativeCodeEntity {
    private final File file;

    public NativeScriptEntity(File file, CAstType type) {
      super(type);
      this.file = file;
    }

    public NativeScriptEntity(String file, CAstType type) {
      this(new File(file), type);
    }

    public int getKind() {
      return SCRIPT_ENTITY;
    }

    public String getName() {
      return "script " + file.getName();
    }

    public String toString() {
      return "script " + file.getName();
    }

    public String[] getArgumentNames() {
      return new String[]{ "script object" };
    }

    public CAstNode[] getArgumentDefaults() {
      return new CAstNode[0];
    }

    public int getArgumentCount() {
      return 1;
    }

    public Collection getQualifiers() {
      return Collections.EMPTY_SET;
    }
 
    public String getFileName() {
      return file.getAbsolutePath();
    }
  };

  protected final URL sourceURL;

  protected final String sourceFileName;

  protected NativeTranslatorToCAst(CAst Ast,
				   URL sourceURL,
				   String sourceFileName) 
  {
    super(Ast);
    this.sourceURL = sourceURL;
    this.sourceFileName = sourceFileName;
  }

  private String getLocalFile() {
    return sourceFileName;
  }

  private String getFile() {
    return sourceURL.getFile();
  }

  private Position makeLocation(final int fl, 
				final int fc, 
				final int ll,
				final int lc) 
  {
    return new AbstractSourcePosition() {
      public int getFirstLine() { return fl; }
      public int getLastLine() { return ll; }
      public int getFirstCol() { return fc; }
      public int getLastCol() { return lc; }
      public URL getURL() { return sourceURL; }
      public InputStream getInputStream() throws IOException { 
        return new FileInputStream(sourceFileName);
      }
      public String toString() {
        String urlString = sourceURL.toString();
	if (urlString.lastIndexOf( File.separator ) == -1)
	  return "["+fl+":"+fc+"]->["+ll+":"+lc+"]";
	else
	  return urlString.substring(urlString.lastIndexOf(File.separator)+1)+"@["+fl+":"+fc+"]->["+ll+":"+lc+"]";
      }
    };
  }

  public abstract CAstEntity translateToCAst();

}
