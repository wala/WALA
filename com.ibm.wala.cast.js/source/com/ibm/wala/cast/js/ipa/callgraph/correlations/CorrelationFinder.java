/******************************************************************************
 * Copyright (c) 2011 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/

package com.ibm.wala.cast.js.ipa.callgraph.correlations;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.ibm.wala.cast.ipa.callgraph.CAstAnalysisScope;
import com.ibm.wala.cast.ir.ssa.AbstractReflectiveGet;
import com.ibm.wala.cast.ir.ssa.AbstractReflectivePut;
import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.ir.translator.TranslatorToCAst.Error;
import com.ibm.wala.cast.js.html.DefaultSourceExtractor;
import com.ibm.wala.cast.js.html.WebUtil;
import com.ibm.wala.cast.js.ipa.callgraph.JSCallGraphUtil;
import com.ibm.wala.cast.js.loader.JavaScriptLoader;
import com.ibm.wala.cast.js.loader.JavaScriptLoaderFactory;
import com.ibm.wala.cast.js.translator.JavaScriptTranslatorFactory;
import com.ibm.wala.cast.js.util.Util;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.loader.AstMethod.LexicalInformation;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.classLoader.SourceModule;
import com.ibm.wala.classLoader.SourceURLModule;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cha.ClassHierarchyException;
import com.ibm.wala.ipa.cha.ClassHierarchyFactory;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrikeBT.IBinaryOpInstruction.IOperator;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.util.WalaException;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.collections.ObjectArrayMapping;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.intset.BitVectorIntSet;
import com.ibm.wala.util.intset.IntIterator;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.MutableIntSet;
import com.ibm.wala.util.intset.OrdinalSetMapping;
import com.ibm.wala.util.io.FileProvider;

/**
 * Helper class for identifying correlated read/write pairs.
 * 
 * @author mschaefer
 *
 */
public class CorrelationFinder {
  private final static boolean TRACK_ESCAPES = true;
  private final static boolean IGNORE_NUMERIC_INDICES = false;
  
  private final JavaScriptTranslatorFactory translatorFactory;
  
  public CorrelationFinder(JavaScriptTranslatorFactory translatorFactory) {
    this.translatorFactory = translatorFactory;
  }

  @SuppressWarnings("unused")
  public static CorrelationSummary findCorrelatedAccesses(IMethod method, IR ir) {
    AstMethod astMethod = (AstMethod)method;
    DefUse du = new DefUse(ir);
    OrdinalSetMapping<SSAInstruction> instrIndices = new ObjectArrayMapping<>(ir.getInstructions());
    CorrelationSummary summary = new CorrelationSummary(method, instrIndices);

    // collect all dynamic property writes in the method
    LinkedList<AbstractReflectivePut> puts = new LinkedList<>();
    for(SSAInstruction inst : Iterator2Iterable.make(ir.iterateNormalInstructions()))
      if(inst instanceof AbstractReflectivePut)
        puts.addFirst((AbstractReflectivePut)inst);

    SSAInstruction insts[] = ir.getInstructions();
    instrs: for(int ii = 0; ii < insts.length; ii++) {
      SSAInstruction inst = insts[ii];
      if(inst instanceof AbstractReflectiveGet) {
        AbstractReflectiveGet get = (AbstractReflectiveGet)inst;
        int index = get.getMemberRef();

        if(ir.getSymbolTable().isConstant(index))
          continue;

        if(ir.getSymbolTable().isParameter(index))
          continue;

        // try to determine what "index" is called at the source level
        String indexName = getSourceLevelName(ir, ii, index);
        if(indexName == null)
          continue instrs;          

        // check that "index" is not accessed in an inner function
        LexicalInformation lexicalInfo = astMethod.lexicalInfo();
        if (lexicalInfo.getExposedNames() != null) {
          for(Pair<String,String> n : lexicalInfo.getExposedNames()) {
            if (n.fst.equals(indexName) && lexicalInfo.getScopingName().equals(n.snd))
              continue instrs;             
          }
        }

        // if "index" is a numeric variable, it is not worth extracting
        if(IGNORE_NUMERIC_INDICES && mustBeNumeric(ir, du, index))
          continue instrs;

        // set of SSA variables into which the value read by 'get' may flow
        MutableIntSet reached = new BitVectorIntSet();
        reached.add(get.getDef());
        // saturate reached by following def-use chains through phi instructions and across function calls
        LinkedList<Integer> worklist = new LinkedList<>();
        MutableIntSet done = new BitVectorIntSet();
        worklist.add(get.getDef());
        while(!worklist.isEmpty()) {
          Integer i = worklist.pop();
          done.add(i);
          for(SSAInstruction inst2 : Iterator2Iterable.make(du.getUses(i))) {
            int i2 = instrIndices.getMappedIndex(inst2);
            if(inst2 instanceof SSAPhiInstruction) {
              int def = inst2.getDef();
              if(reached.add(def) && !done.contains(def))
                worklist.add(def);
            } else if(inst2 instanceof SSAAbstractInvokeInstruction) {
              int def = inst2.getDef();
              if(reached.add(def) && !done.contains(def))
                worklist.add(def);
              // if the index also flows into this invocation, record an escape correlation
              if(TRACK_ESCAPES) {
                for(int j=0;j<inst2.getNumberOfUses();++j) {
                  if(inst2.getUse(j) == index) {
                    summary.addCorrelation(new EscapeCorrelation(get, (SSAAbstractInvokeInstruction)inst2, indexName, getSourceLevelNames(ir, i2, reached)));
                    break;
                  }
                }
              }
            }
          }
        }
        // now find property writes with the same index whose RHS is in 'reached'
        for(AbstractReflectivePut put : puts)
          if(put.getMemberRef() == index && reached.contains(put.getValue()))
            summary.addCorrelation(new ReadWriteCorrelation(get, put, indexName, getSourceLevelNames(ir, ii, reached)));
      }
    }
     
    return summary;
  }

  // tries to determine which source level variable an SSA variable corresponds to
  // if it does not correspond to any variable, or to more than one, null is returned
  private static String getSourceLevelName(IR ir, int index, int vn) {
    String indexName = null;
    String[] sourceNamesForValues = ir.getLocalNames(index, vn);
    
    if(sourceNamesForValues == null)
      return null;
    
    for(String candidateName : sourceNamesForValues) {
      if(indexName != null) {
        indexName = null;
        break;
      }
      if(!candidateName.contains(" ")) // ignore internal names
        indexName = candidateName;
    }
    return indexName;
  }
  
  private static Set<String> getSourceLevelNames(IR ir, int index, IntSet vs) {
    Set<String> res = new HashSet<>();
    for(IntIterator iter=vs.intIterator();iter.hasNext();) {
      String name = getSourceLevelName(ir, index, iter.next());
      if(name != null)
        res.add(name);
    }
    return res;
  }

  // checks whether the given SSA variable must always be assigned a numeric value
  private static boolean mustBeNumeric(IR ir, DefUse du, int v) {
    LinkedList<Integer> worklist = new LinkedList<>();
    MutableIntSet done = new BitVectorIntSet();
    worklist.add(v);
    while(!worklist.isEmpty()) {
      int i = worklist.pop();
      done.add(i);
      if(ir.getSymbolTable().isConstant(i) && ir.getSymbolTable().getConstantValue(i) instanceof Number)
        continue;
      SSAInstruction inst2 = du.getDef(i);
      if(inst2 instanceof SSAPhiInstruction) {
        for(int j=0;j<inst2.getNumberOfUses();++j) {
          int use = inst2.getUse(j);
          if(!done.contains(use))
            worklist.add(use);
        }
      } else if(inst2 instanceof SSABinaryOpInstruction) {
        IOperator operator = ((SSABinaryOpInstruction)inst2).getOperator();
        // if it is an ADD, both operands have to be provably numeric
        if(operator == IBinaryOpInstruction.Operator.ADD) {
          for(int j=0;j<inst2.getNumberOfUses();++j) {
            int use = inst2.getUse(j);
            if(!done.contains(use))
              worklist.add(use);
          }              
        }
        // otherwise the result is definitely numeric
      } else {
        // found a definition that doesn't look numeric
        return false;
      }
    }
    // found no non-numeric definitions
    return true;
  }

  @SuppressWarnings("unused")
  private void printCorrelatedAccesses(URL url) throws IOException, ClassHierarchyException {
    printCorrelatedAccesses(findCorrelatedAccesses(url));
  }

  private void printCorrelatedAccesses(Map<IMethod, CorrelationSummary> summaries) {
    List<Pair<Position, String>> correlations = new ArrayList<>();
    for(CorrelationSummary summary : summaries.values())
      correlations.addAll(summary.pp());

    Collections.sort(correlations, new Comparator<Pair<Position, String>>() {
      @Override
      @SuppressWarnings("unchecked")
      public int compare(Pair<Position, String> o1, Pair<Position, String> o2) {
        return o1.fst.compareTo(o2.fst);
      }		
    });
    int i = 0;
    for(Pair<Position, String> p : correlations)
      System.out.println((i++) + " -- " + p.fst + ": " + p.snd);
  }

  public Map<IMethod, CorrelationSummary> findCorrelatedAccesses(URL url) throws IOException, ClassHierarchyException {
    Set<? extends SourceModule> scripts = null;
    if(url.getPath().endsWith(".js")) {
      scripts = Collections.singleton(new SourceURLModule(url));
    } else {
      JavaScriptLoader.addBootstrapFile(WebUtil.preamble);
      try {
        scripts = WebUtil.extractScriptFromHTML(url, DefaultSourceExtractor.factory).fst;
      } catch (Error e) {
        e.printStackTrace();
        assert false : e.warning;
      }
    }
    Map<IMethod, CorrelationSummary> summaries = findCorrelatedAccesses(scripts);
    return summaries;
  }

  public Map<IMethod, CorrelationSummary> findCorrelatedAccesses(Collection<? extends SourceModule> scripts) throws IOException,
      ClassHierarchyException {
    return findCorrelatedAccesses(scripts.toArray(new SourceModule[scripts.size()]));
  }

  public Map<IMethod, CorrelationSummary> findCorrelatedAccesses(SourceModule[] scripts_array) throws IOException,
      ClassHierarchyException {
    JSCallGraphUtil.setTranslatorFactory(translatorFactory);
    JavaScriptLoaderFactory loaders = JSCallGraphUtil.makeLoaders(null);
    CAstAnalysisScope scope = new CAstAnalysisScope(scripts_array, loaders, Collections.singleton(JavaScriptLoader.JS));
    IClassHierarchy cha = ClassHierarchyFactory.make(scope, loaders, JavaScriptLoader.JS);
    try {
      Util.checkForFrontEndErrors(cha);
    } catch (WalaException e) {
      return Collections.emptyMap();
    }
    IRFactory<IMethod> factory = AstIRFactory.makeDefaultFactory();

    Map<IMethod, CorrelationSummary> correlations = HashMapFactory.make();
    for(IClass klass : cha) {
      for(IMethod method : klass.getAllMethods()) {
        IR ir = factory.makeIR(method, Everywhere.EVERYWHERE, SSAOptions.defaultOptions());
        CorrelationSummary summary = findCorrelatedAccesses(method, ir);
        if(!summary.getCorrelations().isEmpty())
          correlations.put(method, summary);
      }
    }
    
    return correlations;
  }

  @SuppressWarnings("unused")
  private URL toUrl(String src) throws MalformedURLException {
    // first try interpreting as local file name, if that doesn't work just assume it's a URL
    try {
      File f = (new FileProvider()).getFileFromClassLoader(src, this.getClass().getClassLoader());
      URL url = f.toURI().toURL();
      return url;
    } catch(FileNotFoundException fnfe) {
      return new URL(src);
    }
  }
}
