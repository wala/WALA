/******************************************************************************
 * Copyright (c) 2009 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
package com.ibm.wala.cast.loader;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cast.ir.translator.AstTranslator;
import com.ibm.wala.cast.ir.translator.AstTranslator.AstLexicalInformation;
import com.ibm.wala.cast.ir.translator.AstTranslator.WalkContext;
import com.ibm.wala.cast.ir.translator.TranslatorToCAst;
import com.ibm.wala.cast.ir.translator.TranslatorToIR;
import com.ibm.wala.cast.loader.AstMethod.Retranslatable;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstQualifier;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.cast.types.AstMethodReference;
import com.ibm.wala.cast.util.CAstPrinter;
import com.ibm.wala.cfg.AbstractCFG;
import com.ibm.wala.cfg.IBasicBlock;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.Module;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.classLoader.SourceFileModule;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.io.TemporaryFile;
import com.ibm.wala.util.warnings.Warning;

/**
 * abstract class loader that performs CAst and IR generation for relevant
 * entities in a list of {@link Module}s. Subclasses provide the CAst / IR
 * translators appropriate for the language.
 * 
 */
public abstract class CAstAbstractModuleLoader extends CAstAbstractLoader {

  private static final boolean DEBUG = false;

  public CAstAbstractModuleLoader(IClassHierarchy cha, IClassLoader parent) {
    super(cha, parent);
  }

  public CAstAbstractModuleLoader(IClassHierarchy cha) {
    this(cha, null);
  }

  /**
   * create the appropriate CAst translator for the language and source module
   */
  protected abstract TranslatorToCAst getTranslatorToCAst(CAst ast, ModuleEntry M) throws IOException;

  /**
   * should IR be generated for entity?
   */
  protected abstract boolean shouldTranslate(CAstEntity entity);

  /**
   * create the appropriate IR translator for the language
   */
  protected abstract TranslatorToIR initTranslator();

  protected File getLocalFile(SourceModule M) throws IOException {
    if (M instanceof SourceFileModule) {
      return ((SourceFileModule) M).getFile();
    } else {
      File f = File.createTempFile("module", ".txt");
      f.deleteOnExit();
      TemporaryFile.streamToFile(f, M.getInputStream());
      return f;
    }
  }

  /**
   * subclasses should override to perform actions after CAst and IR have been
   * generated. by default, do nothing
   */
  protected void finishTranslation() {

  }

  @Override
  public void init(final List<Module> modules) {

    final CAst ast = new CAstImpl();

    // convert everything to CAst
    final Set<Pair<CAstEntity, ModuleEntry>> topLevelEntities = new LinkedHashSet<>();
    for (Module module : modules) {
      translateModuleToCAst(module, ast, topLevelEntities);
    }

    // generate IR as needed
    final TranslatorToIR xlatorToIR = initTranslator();

    for (Pair<CAstEntity, ModuleEntry> p : topLevelEntities) {
      if (shouldTranslate(p.fst)) {
        xlatorToIR.translate(p.fst, p.snd);
      }
    }

    if (DEBUG) {
      for (TypeName tn : types.keySet()) {
        try {
          System.err.println(("found type " + tn + " : " + types.get(tn) + " < " + types.get(tn).getSuperclass()));
        } catch (Exception e) {
          System.err.println(e);
        }
      }
    }

    finishTranslation();
  }

  /**
   * translate moduleEntry to CAst and store result in topLevelEntities
   * 
   * @param ast
   * @param topLevelEntities
   */
  private void translateModuleEntryToCAst(ModuleEntry moduleEntry, CAst ast, Set<Pair<CAstEntity, ModuleEntry>> topLevelEntities) {
    try {
      if (moduleEntry.isModuleFile()) {
        // nested module
        translateModuleToCAst(moduleEntry.asModule(), ast, topLevelEntities);
      } else {
        TranslatorToCAst xlatorToCAst = getTranslatorToCAst(ast, moduleEntry);

        CAstEntity fileEntity = null;
        try {
          fileEntity = xlatorToCAst.translateToCAst();
        
          if (DEBUG) {
            CAstPrinter.printTo(fileEntity, new PrintWriter(System.err));
          }
          topLevelEntities.add(Pair.make(fileEntity, moduleEntry));

        } catch (TranslatorToCAst.Error e) {
          addMessage(moduleEntry, e.warning);
        }
      }
    } catch (final IOException e) {
      addMessage(moduleEntry, new Warning(Warning.SEVERE) {
        @Override
        public String getMsg() {
          return "I/O issue: " + e.getMessage();
        }
      });
    } catch (final RuntimeException e) {
      final ByteArrayOutputStream s = new ByteArrayOutputStream();
      PrintStream ps = new PrintStream(s);
      e.printStackTrace(ps);
      addMessage(moduleEntry, new Warning(Warning.SEVERE) {
        @Override
        public String getMsg() {
          return "Parsing issue: " + new String(s.toByteArray());
        }
      });
    }
  }

  /**
   * translate all relevant entities in the module to CAst, storing the results
   * in topLevelEntities
   */
  private void translateModuleToCAst(Module module, CAst ast, Set<Pair<CAstEntity, ModuleEntry>> topLevelEntities) {
    for (ModuleEntry me : Iterator2Iterable.make(module.getEntries())) {
      translateModuleEntryToCAst(me, ast, topLevelEntities);
    }
  }

  public class DynamicCodeBody extends AstFunctionClass {
    private final WalkContext translationContext;
    private final CAstEntity entity;
    
    public DynamicCodeBody(TypeReference codeName, TypeReference parent, IClassLoader loader,
        CAstSourcePositionMap.Position sourcePosition, CAstEntity entity, WalkContext context) {
      super(codeName, parent, loader, sourcePosition);
      types.put(codeName.getName(), this);
      this.translationContext = context;
      this.entity = entity;
    }

    @Override
    public IClassHierarchy getClassHierarchy() {
      return cha;
    }
    
    public IMethod setCodeBody(DynamicMethodObject codeBody) {
      this.functionBody = codeBody;
      codeBody.entity = entity;
      codeBody.translationContext = translationContext;
      return codeBody;
    }
    
    @Override
    public Collection<Annotation> getAnnotations() {
      return Collections.emptySet();
    }
  }

  public class DynamicMethodObject extends AstMethod implements Retranslatable {
    private WalkContext translationContext;
    private CAstEntity entity;

    public DynamicMethodObject(IClass cls, Collection<CAstQualifier> qualifiers, AbstractCFG<?, ?> cfg, SymbolTable symtab, boolean hasCatchBlock,
        Map<IBasicBlock<SSAInstruction>, TypeReference[]> caughtTypes, boolean hasMonitorOp, AstLexicalInformation lexicalInfo, DebuggingInformation debugInfo) {
      super(cls, qualifiers, cfg, symtab, AstMethodReference.fnReference(cls.getReference()), hasCatchBlock, caughtTypes,
          hasMonitorOp, lexicalInfo, debugInfo, null);

      // force creation of these constants by calling the getter methods
      symtab.getNullConstant();
    }

    
    @Override
    public CAstEntity getEntity() {
      return entity;
    }


    @Override
    public void retranslate(AstTranslator xlator) {
      xlator.translate(entity, translationContext);
    }

    @Override
    public IClassHierarchy getClassHierarchy() {
      return cha;
    }

    @Override
    public String toString() {
      return "<Code body of " + cls + ">";
    }

    @Override
    public TypeReference[] getDeclaredExceptions() {
      return null;
    }

    @Override
    public LexicalParent[] getParents() {
      if (lexicalInfo() == null)
        return new LexicalParent[0];

      final String[] parents = lexicalInfo().getScopingParents();

      if (parents == null)
        return new LexicalParent[0];

      LexicalParent result[] = new LexicalParent[parents.length];

      for (int i = 0; i < parents.length; i++) {
        final int hack = i;
        final AstMethod method = (AstMethod) lookupClass(parents[i], cha).getMethod(AstMethodReference.fnSelector);
        result[i] = new LexicalParent() {
          @Override
          public String getName() {
            return parents[hack];
          }

          @Override
          public AstMethod getMethod() {
            return method;
          }
        };

        if (AstTranslator.DEBUG_LEXICAL) {
          System.err.println(("parent " + result[i].getName() + " is " + result[i].getMethod()));
        }
      }

      return result;
    }

    @Override
    public String getLocalVariableName(int bcIndex, int localNumber) {
      return null;
    }

    @Override
    public boolean hasLocalVariableTable() {
      return false;
    }

    public int getMaxLocals() {
      Assertions.UNREACHABLE();
      return -1;
    }

    public int getMaxStackHeight() {
      Assertions.UNREACHABLE();
      return -1;
    }

    @Override
    public TypeReference getParameterType(int i) {
      if (i == 0) {
        return getDeclaringClass().getReference();
      } else {
        return getDeclaringClass().getClassLoader().getLanguage().getRootType();
      }
    }
  }

  public class CoreClass extends AstDynamicPropertyClass {
    private final TypeName superName;
    
      public CoreClass(TypeName name, TypeName superName, IClassLoader loader, CAstSourcePositionMap.Position sourcePosition) {
        super(sourcePosition, name, loader, (short) 0, Collections.emptyMap(), CAstAbstractModuleLoader.this.getLanguage().getRootType());
        types.put(name, this);
        this.superName = superName;
      }

      @Override
      public IClassHierarchy getClassHierarchy() {
        return cha;
      }

      @Override
      public String toString() {
        return "Core[" + getReference().getName().toString().substring(1) + "]";
      }

      @Override
      public Collection<IClass> getDirectInterfaces() {
        return Collections.emptySet();
      }

      @Override
      public IClass getSuperclass() {
        return superName==null? null: types.get(superName);
      }
      
      @Override
      public Collection<Annotation> getAnnotations() {
        return Collections.emptySet();
      }
    }

}
