package com.ibm.wala.cast.ir.toSource;

import static com.ibm.wala.types.TypeReference.Boolean;
import static com.ibm.wala.types.TypeReference.Char;
import static com.ibm.wala.types.TypeReference.Double;
import static com.ibm.wala.types.TypeReference.Float;
import static com.ibm.wala.types.TypeReference.Int;
import static com.ibm.wala.types.TypeReference.Long;
import static com.ibm.wala.types.TypeReference.Void;

import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.cast.ir.ssa.AssignInstruction;
import com.ibm.wala.cast.ir.ssa.AstPreInstructionVisitor;
import com.ibm.wala.cast.ir.ssa.CAstBinaryOp;
import com.ibm.wala.cast.ir.ssa.CAstUnaryOp;
import com.ibm.wala.cast.ir.ssa.analysis.LiveAnalysis;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstAnnotation;
import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstQualifier;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.CAstType;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.cast.tree.impl.CAstNodeTypeMapRecorder;
import com.ibm.wala.cast.tree.impl.CAstOperator;
import com.ibm.wala.cast.tree.visit.CAstVisitor;
import com.ibm.wala.cast.util.CAstPattern;
import com.ibm.wala.cfg.ControlFlowGraph;
import com.ibm.wala.cfg.Util;
import com.ibm.wala.cfg.cdg.ControlDependenceGraph;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.ipa.cfg.ExceptionPrunedCFG;
import com.ibm.wala.ipa.cfg.PrunedCFG;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrike.shrikeBT.IBinaryOpInstruction;
import com.ibm.wala.shrike.shrikeBT.IBinaryOpInstruction.IOperator;
import com.ibm.wala.shrike.shrikeBT.IConditionalBranchInstruction;
import com.ibm.wala.shrike.shrikeBT.IShiftInstruction;
import com.ibm.wala.shrike.shrikeBT.IUnaryOpInstruction;
import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.DefUse;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAArrayLengthInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAArrayStoreInstruction;
import com.ibm.wala.ssa.SSABinaryOpInstruction;
import com.ibm.wala.ssa.SSACFG;
import com.ibm.wala.ssa.SSACFG.BasicBlock;
import com.ibm.wala.ssa.SSACheckCastInstruction;
import com.ibm.wala.ssa.SSAComparisonInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAConversionInstruction;
import com.ibm.wala.ssa.SSAGetCaughtExceptionInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAGotoInstruction;
import com.ibm.wala.ssa.SSAInstanceofInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAInvokeInstruction;
import com.ibm.wala.ssa.SSALoadMetadataInstruction;
import com.ibm.wala.ssa.SSAMonitorInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPhiInstruction;
import com.ibm.wala.ssa.SSAPiInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.ssa.SSASwitchInstruction;
import com.ibm.wala.ssa.SSAThrowInstruction;
import com.ibm.wala.ssa.SSAUnaryOpInstruction;
import com.ibm.wala.ssa.SSAUnspecifiedExprInstruction;
import com.ibm.wala.ssa.SSAUnspecifiedInstruction;
import com.ibm.wala.ssa.SymbolTable;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Heap;
import com.ibm.wala.util.collections.IteratorUtil;
import com.ibm.wala.util.collections.NonNullSingletonIterator;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.graph.Graph;
import com.ibm.wala.util.graph.GraphSlicer;
import com.ibm.wala.util.graph.dominators.Dominators;
import com.ibm.wala.util.graph.impl.GraphInverter;
import com.ibm.wala.util.graph.impl.SlowSparseNumberedGraph;
import com.ibm.wala.util.graph.traverse.DFS;
import com.ibm.wala.util.graph.traverse.SCCIterator;
import com.ibm.wala.util.graph.traverse.Topological;
import com.ibm.wala.util.intset.BasicNaturalRelation;
import com.ibm.wala.util.intset.BitVector;
import com.ibm.wala.util.intset.IntSet;
import com.ibm.wala.util.intset.IntSetUtil;
import com.ibm.wala.util.intset.IntegerUnionFind;
import com.ibm.wala.util.intset.MutableIntSet;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UTFDataFormatException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import org.apache.commons.io.output.TeeWriter;

public abstract class ToSource {

  private final CAst ast = new CAstImpl();

  protected abstract String nameToJava(String name, boolean isTypeName);

  private static CAstPattern varDefPattern(CAstNode varName) {
    return CAstPattern.parse("DECL_STMT(VAR(\"" + varName.getValue() + "\"),**)");
  }

  private static CAstPattern varUsePattern(CAstNode varName) {
    return CAstPattern.parse("VAR(\"" + varName.getValue() + "\")");
  }

  private static CAstPattern isNot = CAstPattern.parse("UNARY_EXPR(|(OPERATOR(\"!\")||\"!\")|,*)");

  private static boolean deemedFunctional(
      SSAInstruction inst, List<SSAInstruction> regionInsts, IClassHierarchy cha) {
    if ((inst instanceof SSABinaryOpInstruction)
        || (inst instanceof SSAUnaryOpInstruction)
        || (inst instanceof SSAComparisonInstruction)
        || (inst instanceof SSAConversionInstruction)
        || (inst instanceof SSAUnspecifiedExprInstruction)) {
      return true;
    } else if (inst instanceof SSAAbstractInvokeInstruction) {
      MethodReference method = ((SSAAbstractInvokeInstruction) inst).getDeclaredTarget();
      TypeReference targetClass = method.getDeclaringClass();
      if (cha.lookupClass(targetClass) != null) {
        targetClass = cha.lookupClass(targetClass).getReference();
      }
      if ((targetClass == TypeReference.JavaLangBoolean)
          || (targetClass == TypeReference.JavaLangByte)
          || (targetClass == TypeReference.JavaLangCharacter)
          || (targetClass == TypeReference.JavaLangInteger)
          || (targetClass == TypeReference.JavaLangFloat)
          || (targetClass == TypeReference.JavaLangDouble)
          || (targetClass == TypeReference.JavaLangMath)
          || (targetClass == TypeReference.JavaLangString)
          || (targetClass == TypeReference.JavaUtilRegexPattern)) {
        return true;
      }
    } else if (inst instanceof SSAGetInstruction) {
      FieldReference read = ((SSAGetInstruction) inst).getDeclaredField();
      TypeReference cls = read.getDeclaringClass();
      if (cha.lookupClass(cls) != null) {
        cls = cha.lookupClass(cls).getReference();
      }
      if (((SSAGetInstruction) inst).isStatic() && cls == TypeReference.JavaLangSystem) {
        return true;
      }
      Set<FieldReference> written = HashSetFactory.make();
      regionInsts.forEach(
          i -> {
            if (i instanceof SSAPutInstruction) {
              written.add(((SSAPutInstruction) i).getDeclaredField());
            }
          });
      if (!written.contains(read)) {
        return true;
      }
    } else if (inst instanceof SSAArrayLoadInstruction) {
      if (regionInsts.stream().noneMatch(i -> i instanceof SSAArrayStoreInstruction)) {
        return true;
      }
    }

    return false;
  }

  protected static class TreeBuilder {
    private boolean functionalOnly = false;
    private final IClassHierarchy cha;
    private final ControlDependenceGraph<ISSABasicBlock> cdg;
    private final ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg;

    protected TreeBuilder(
        IClassHierarchy cha,
        ControlFlowGraph<SSAInstruction, ISSABasicBlock> cfg,
        ControlDependenceGraph<ISSABasicBlock> cdg) {
      this.cha = cha;
      this.cdg = cdg;
      this.cfg = cfg;
    }

    protected boolean shouldMergeIfPossible(@SuppressWarnings("unused") SSAInstruction inst) {
      return true;
    }

    protected void didMerge(@SuppressWarnings("unused") SSAInstruction inst) {}

    private void gatherInstructions(
        Set<SSAInstruction> stuff,
        IR ir,
        DefUse du,
        List<SSAInstruction> regionInsts,
        int vn,
        Heap<List<SSAInstruction>> chunks,
        SSAInstruction loopControl,
        int limit,
        IntSet unmergeableValues) {
      SSAInstruction inst = du.getDef(vn);
      if (inst != null && !shouldMergeIfPossible(inst)) {
        return;
      } else if (ir.getSymbolTable().isConstant(vn)) {
        return;
      } else if (vn <= ir.getSymbolTable().getNumberOfParameters()) {
        return;
      } else if (du.getNumberOfUses(vn) != 1) {
        if (loopControl == null) {
          return;
        } else {
          ISSABasicBlock loop = cfg.getBlockForInstruction(loopControl.iIndex());
          ISSABasicBlock me = cfg.getBlockForInstruction(inst.iIndex());
          if (loop != me && !cdg.hasEdge(loop, me)) {
            return;
          }
        }
      }

      assert inst != null;
      if (inst.iIndex() <= limit && shouldMergeIfPossible(inst)) {
        didMerge(inst);
        gatherInstructions(
            stuff, ir, du, regionInsts, inst, chunks, unmergeableValues, loopControl);
      }
    }

    private ISSABasicBlock findBlock(SSAPhiInstruction phi) {
      for (ISSABasicBlock bb : cfg) {
        Iterator<SSAPhiInstruction> bi = bb.iteratePhis();
        while (bi.hasNext()) {
          if (bi.next().equals(phi)) {
            return bb;
          }
        }
      }

      assert false : "cannot find " + phi;
      return null;
    }

    private void gatherInstructions(
        Set<SSAInstruction> stuff,
        IR ir,
        DefUse du,
        List<SSAInstruction> regionInsts,
        SSAInstruction inst,
        Heap<List<SSAInstruction>> chunks,
        IntSet unmergeableValues,
        SSAInstruction loopControl) {

      if (inst instanceof SSAPhiInstruction) {
        System.err.println("looking at PHI " + inst + " for " + regionInsts);
        boolean ok = true;
        ISSABasicBlock bb = findBlock((SSAPhiInstruction) inst);
        System.err.println("block " + bb);
        ISSABasicBlock condPred = null;
        Map<ISSABasicBlock, Object> condPredLabels = HashMapFactory.make();
        check_preds:
        {
          for (Iterator<ISSABasicBlock> pbs = cfg.getPredNodes(bb); pbs.hasNext(); ) {
            ISSABasicBlock pb = pbs.next();
            System.err.println("pred " + pb);
            for (Iterator<ISSABasicBlock> cps = cdg.getPredNodes(pb); cps.hasNext(); ) {
              ISSABasicBlock cp = cps.next();

              if (condPred == null) {
                condPred = cp;
              } else if (condPred != cp) {
                ok = false;
                break check_preds;
              }

              if (cdg.getEdgeLabels(cp, pb).size() != 1) {
                ok = false;
                break check_preds;
              }
              Object label = cdg.getEdgeLabels(cp, pb).iterator().next();
              if (condPredLabels.values().contains(label)) {
                ok = false;
                break check_preds;
              } else {
                condPredLabels.put(pb, label);
              }
            }
          }

          SSAInstruction choice = condPred.getLastInstruction();
          if (!regionInsts.contains(choice)) {
            ok = false;
            break check_preds;
          }
        }

        if (ok) {
          System.err.println(
              "found nested for " + inst + " and " + condPred + ", " + condPredLabels);
        }
      }

      if (!stuff.contains(inst) && regionInsts.contains(inst)) {

        boolean depOk = false;
        if (loopControl != null) {
          ISSABasicBlock loop = cfg.getBlockForInstruction(loopControl.iIndex());
          ISSABasicBlock me = cfg.getBlockForInstruction(inst.iIndex());
          if (loop == me || cdg.hasEdge(loop, me)) {
            System.err.println("depOK: " + loop + " " + me);
            depOk = true;
          }
        }

        if (!depOk && !deemedFunctional(inst, regionInsts)) {
          if (loopControl != null || functionalOnly) {
            if (stuff.isEmpty()) {
              stuff.add(inst);
              chunks.insert(new ArrayList<>(stuff));
            }
            return;
          } else {
            functionalOnly = true;
          }
        }

        stuff.add(inst);
        chunks.insert(new ArrayList<>(stuff));
        for (int i = inst.getNumberOfUses() - 1; i >= 0; i--) {
          if (!unmergeableValues.contains(inst.getUse(i)) && !(inst instanceof AssignInstruction)) {
            gatherInstructions(
                stuff,
                ir,
                du,
                regionInsts,
                inst.getUse(i),
                chunks,
                loopControl,
                inst.iIndex(),
                unmergeableValues);
          }
        }
      }
    }

    protected boolean deemedFunctional(SSAInstruction inst, List<SSAInstruction> regionInsts) {
      return ToSource.deemedFunctional(inst, regionInsts, cha);
    }
  }

  private static <T> Map<T, Integer> computeFinishTimes(
      Supplier<Iterator<T>> entryPoints, Graph<T> ipcfg) {
    int dfsNumber = 0;
    Map<T, Integer> dfsFinish = HashMapFactory.make();
    Iterator<T> search = DFS.iterateFinishTime(ipcfg, entryPoints.get());
    while (search.hasNext()) {
      T n = search.next();
      assert !dfsFinish.containsKey(n) : n;
      dfsFinish.put(n, dfsNumber++);
    }
    return dfsFinish;
  }

  private static <T> Map<T, Integer> computeStartTimes(
      Supplier<Iterator<T>> entryPoints, Graph<T> ipcfg) {
    int reverseDfsNumber = 0;
    Map<T, Integer> dfsStart = HashMapFactory.make();
    Iterator<T> reverseSearch = DFS.iterateDiscoverTime(ipcfg, entryPoints.get());
    while (reverseSearch.hasNext()) {
      dfsStart.put(reverseSearch.next(), reverseDfsNumber++);
    }
    return dfsStart;
  }

  protected static class CodeGenerationContext implements CAstVisitor.Context {
    private final CAstEntity fakeTop =
        new CAstEntity() {

          @Override
          public int getKind() {
            // TODO Auto-generated method stub
            return 0;
          }

          @Override
          public String getName() {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public String getSignature() {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public String[] getArgumentNames() {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public CAstNode[] getArgumentDefaults() {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public int getArgumentCount() {
            // TODO Auto-generated method stub
            return 0;
          }

          @Override
          public Map<CAstNode, Collection<CAstEntity>> getAllScopedEntities() {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public Iterator<CAstEntity> getScopedEntities(CAstNode construct) {
            return EmptyIterator.instance();
          }

          @Override
          public CAstNode getAST() {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public CAstControlFlowMap getControlFlow() {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public CAstSourcePositionMap getSourceMap() {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public Position getPosition() {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public Position getNamePosition() {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public Position getPosition(int arg) {
            // TODO Auto-generated method stub
            return null;
          }

          private final CAstNodeTypeMapRecorder types = new CAstNodeTypeMapRecorder();

          @Override
          public CAstNodeTypeMapRecorder getNodeTypeMap() {
            return types;
          }

          @Override
          public Collection<CAstQualifier> getQualifiers() {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public CAstType getType() {
            // TODO Auto-generated method stub
            return null;
          }

          @Override
          public Collection<CAstAnnotation> getAnnotations() {
            // TODO Auto-generated method stub
            return null;
          }
        };

    TypeInference getTypes() {
      return types;
    }

    public IntegerUnionFind getMergePhis() {
      return mergePhis;
    }

    private final boolean isTopLevel;
    private int parentPrecedence;
    private final TypeInference types;
    private final IntegerUnionFind mergePhis;

    public CodeGenerationContext nonTopLevel() {
      if (!isTopLevel()) {
        return this;
      } else {
        return new CodeGenerationContext(this, parentPrecedence) {

          @Override
          public boolean isTopLevel() {
            return false;
          }
        };
      }
    }

    public CodeGenerationContext(
        TypeInference types, IntegerUnionFind mergePhis, boolean isTopLevel) {
      this.types = types;
      this.mergePhis = mergePhis;
      this.parentPrecedence = Integer.MAX_VALUE;
      this.isTopLevel = isTopLevel;
    }

    public CodeGenerationContext(CodeGenerationContext parent, int precedence) {
      this.types = parent.types;
      this.mergePhis = parent.mergePhis;
      this.parentPrecedence = precedence;
      this.isTopLevel = false;
    }

    @Override
    public CAstEntity top() {
      return fakeTop;
    }

    @Override
    public CAstSourcePositionMap getSourceMap() {
      // TODO Auto-generated method stub
      return null;
    }

    public boolean isTopLevel() {
      return isTopLevel;
    }
  }

  protected TreeBuilder makeTreeBuilder(
      @SuppressWarnings("unused") IR ir,
      IClassHierarchy cha,
      PrunedCFG<SSAInstruction, ISSABasicBlock> cfg,
      ControlDependenceGraph<ISSABasicBlock> cdg) {
    return new TreeBuilder(cha, cfg, cdg);
  }

  protected class RegionTreeNode {

    private final IClassHierarchy cha;
    protected final TypeInference types;
    private final Map<Set<Pair<SSAInstruction, ISSABasicBlock>>, Set<ISSABasicBlock>> regions;
    private final Map<Pair<SSAInstruction, ISSABasicBlock>, List<SSAInstruction>> linePhis;
    private final Map<Pair<SSAInstruction, ISSABasicBlock>, List<List<SSAInstruction>>>
        regionChunks;
    private final MutableIntSet mergedValues;
    protected final IntegerUnionFind mergePhis;
    private final SymbolTable ST;
    private final DefUse du;
    private final PrunedCFG<SSAInstruction, ISSABasicBlock> cfg;
    private final Set<ISSABasicBlock> loopHeaders;
    private final Set<ISSABasicBlock> loopExits;
    // The blocks that will got outside of the loop, might includes loop control
    private final Set<ISSABasicBlock> loopBreakers;
    private final Map<ISSABasicBlock, Set<ISSABasicBlock>> loopControls;
    private final Set<Set<ISSABasicBlock>> loops;
    private final SSAInstruction r;
    private final ISSABasicBlock l;
    private final ControlDependenceGraph<ISSABasicBlock> cdg;
    protected final IR ir;
    private BasicNaturalRelation livenessConflicts;
    protected final Map<Integer, String> sourceNames;
    private Graph<ISSABasicBlock> cfgNoBack;
    private Map<ISSABasicBlock, Set<ISSABasicBlock>> moveAfterWithLabel;
    private Map<ISSABasicBlock, Set<ISSABasicBlock>> skipDueToLabel;
    private Map<ISSABasicBlock, Map<ISSABasicBlock, ISSABasicBlock>> breakDueToLabel;
    protected final Map<SSAInstruction, Map<ISSABasicBlock, RegionTreeNode>> children =
        HashMapFactory.make();
    protected final Map<List<SSAInstruction>, RegionTreeNode> moveAsLoopBody =
        HashMapFactory.make();
    RegionTreeNode parent;

    protected CAstNode makeVariableName(int vn) {
      return ast.makeConstant(sourceNames.get(vn));
    }

    private Pair<BasicNaturalRelation, Iterable<SSAPhiInstruction>> orderPhisAndDetectCycles(
        Iterator<SSAPhiInstruction> blockPhis) {
      Graph<SSAPhiInstruction> G = SlowSparseNumberedGraph.make();
      blockPhis.forEachRemaining(phi -> G.addNode(phi));
      G.iterator()
          .forEachRemaining(
              p -> {
                G.iterator()
                    .forEachRemaining(
                        s -> {
                          for (int i = 0; i < s.getNumberOfUses(); i++) {
                            int d = mergePhis.find(p.getDef());
                            int u = mergePhis.find(s.getUse(i));
                            if (d == u && !G.hasEdge(s, p)) {
                              G.addEdge(s, p);
                            }
                          }
                        });
              });

      BasicNaturalRelation rename = new BasicNaturalRelation();
      System.err.println("phi scc: ------- ");
      new SCCIterator<>(G)
          .forEachRemaining(
              new Consumer<Set<SSAPhiInstruction>>() {
                private int idx = ST.getMaxValueNumber() + 1;

                @Override
                public void accept(Set<SSAPhiInstruction> t) {
                  System.err.println("phi scc: " + t);
                  if (t.size() > 1) {
                    System.err.println(
                        mergePhis.find(t.iterator().next().getDef()) + " --> " + idx);
                    rename.add(mergePhis.find(t.iterator().next().getDef()), idx++);
                  }
                }
              });

      return Pair.make(rename, Topological.makeTopologicalIter(G));
    }

    public RegionTreeNode(RegionTreeNode parent, SSAInstruction r, ISSABasicBlock l) {
      this.parent = parent;
      this.r = r;
      this.l = l;
      this.cha = parent.cha;
      this.types = parent.types;
      this.regions = parent.regions;
      this.linePhis = parent.linePhis;
      this.regionChunks = parent.regionChunks;
      this.mergedValues = parent.mergedValues;
      this.mergePhis = parent.mergePhis;
      this.ST = parent.ST;
      this.du = parent.du;
      this.cfg = parent.cfg;
      this.loopHeaders = parent.loopHeaders;
      this.loopExits = parent.loopExits;
      this.loopBreakers = parent.loopBreakers;
      this.loopControls = parent.loopControls;
      this.livenessConflicts = parent.livenessConflicts;
      this.cdg = parent.cdg;
      this.packages = parent.packages;
      this.loops = parent.loops;
      this.ir = parent.ir;
      this.sourceNames = parent.sourceNames;
      this.cfgNoBack = parent.cfgNoBack;
      this.moveAfterWithLabel = parent.moveAfterWithLabel;
      this.skipDueToLabel = parent.skipDueToLabel;
      this.breakDueToLabel = parent.breakDueToLabel;
      initChildren();
      System.err.println("added children for " + r + "," + l + ": " + children);
    }

    private boolean hasAllByIdentity(List<SSAInstruction> all, List<SSAInstruction> some) {
      return !some.stream()
          .filter(si -> !all.stream().anyMatch(ai -> ai == si))
          .findFirst()
          .isPresent();
    }

    private void removeAllByIdentity(List<SSAInstruction> all, List<SSAInstruction> some) {
      for (Iterator<SSAInstruction> insts = all.iterator(); insts.hasNext(); ) {
        SSAInstruction inst = insts.next();
        if (some.stream().anyMatch(si -> si == inst)) {
          insts.remove();
        }
      }
    }

    private int positionByIdentity(List<SSAInstruction> all, SSAInstruction item) {
      for (int i = 0; i < all.size(); i++) {
        if (all.get(i) == item) {
          return i;
        }
      }

      return -1;
    }

    private String toSourceName(int vn) {
      int root_vn = mergePhis.find(vn);
      String name = "var_" + root_vn;
      if (ir.getSymbolTable().isParameter(vn) && ir.getMethod() instanceof AstMethod) {
        return ((AstMethod) ir.getMethod()).getParameterName(vn);
      } else {
        for (int i = 1; i <= ir.getSymbolTable().getMaxValueNumber(); i++) {
          if (mergePhis.find(i) == root_vn) {
            for (Iterator<SSAInstruction> uses = du.getUses(i); uses.hasNext(); ) {
              SSAInstruction uv = uses.next();
              if (uv.iIndex() >= 0) {
                String[] names = ir.getLocalNames(uv.iIndex(), i);
                if (names != null && names.length > 0) {
                  name = names[0];
                }
              }
            }
          }
        }
        return name;
      }
    }

    private Map<Integer, String> makeNames() {
      Map<Integer, String> names = HashMapFactory.make();
      Map<String, Integer> offsets = HashMapFactory.make();
      for (int i = 1; i <= ST.getMaxValueNumber(); i++) {
        String nm = toSourceName(i);
        if (offsets.containsKey(nm)) {
          int v = offsets.get(nm);
          if (v != mergePhis.find(i)) {
            nm = nm + "_" + mergePhis.find(i);
          }
        } else {
          offsets.put(nm, mergePhis.find(i));
        }
        names.put(i, nm);
      }
      return names;
    }

    public RegionTreeNode(
        IR ir, IClassHierarchy cha, TypeInference types, SSAInstruction r, ISSABasicBlock l) {
      this.parent = null;
      this.r = r;
      this.l = l;
      this.ir = ir;
      this.cha = cha;
      this.types = types;
      this.ST = ir.getSymbolTable();

      du = new DefUse(ir);
      cfg = ExceptionPrunedCFG.makeDefiniteUncaught(ir.getControlFlowGraph());
      packages = HashMapFactory.make();
      moveAfterWithLabel = HashMapFactory.make();
      skipDueToLabel = HashMapFactory.make();
      breakDueToLabel = HashMapFactory.make();

      livenessConflicts = new BasicNaturalRelation();
      LiveAnalysis.Result liveness = LiveAnalysis.perform(ir);
      cfg.forEach(
          bb -> {
            List<BiFunction<ISSABasicBlock, Integer, Boolean>> lfs = new ArrayList<>();
            lfs.add(liveness::isLiveEntry);
            lfs.add(liveness::isLiveExit);
            for (BiFunction<ISSABasicBlock, Integer, Boolean> get : lfs) {
              for (int i = 1; i <= ir.getSymbolTable().getMaxValueNumber(); i++) {
                if (get.apply(bb, i)) {
                  for (int j = i + 1; j <= ir.getSymbolTable().getMaxValueNumber(); j++) {
                    if (get.apply(bb, j)) {
                      livenessConflicts.add(i, j);
                      livenessConflicts.add(j, i);
                    }
                  }
                }
              }
            }
            bb.iteratePhis()
                .forEachRemaining(
                    p1 -> {
                      bb.iteratePhis()
                          .forEachRemaining(
                              p2 -> {
                                if (p1 != p2) {
                                  livenessConflicts.add(p1.getDef(), p2.getDef());
                                }
                              });
                    });
          });
      List<Function<Integer, BitVector>> lfs = new ArrayList<>();
      lfs.add(liveness::getLiveBefore);
      // lfs.add(liveness::getLiveAfter);
      for (SSAInstruction inst : ir.getInstructions()) {
        if (inst != null) {
          lfs.forEach(
              f -> {
                BitVector live = f.apply(inst.iIndex());
                int lv = 0;
                while ((lv = live.nextSetBit(lv + 1)) > 0) {
                  int rv = lv + 1;
                  while ((rv = live.nextSetBit(rv + 1)) > 0) {
                    livenessConflicts.add(rv, lv);
                    livenessConflicts.add(lv, rv);
                  }
                }
              });
        }
      }
      System.err.println("liveness conflicts");
      System.err.println(livenessConflicts);
      cdg = new ControlDependenceGraph<>(cfg, true);
      System.err.println(cdg);
      IRToCAst.toCAst(ir)
          .entrySet()
          .forEach(
              e -> {
                System.err.println(e);
              });

      Graph<ISSABasicBlock> FD = Dominators.make(cfg, cfg.entry()).dominatorTree();
      for (ISSABasicBlock bb : cfg) {
        cdg.getSuccNodes(bb)
            .forEachRemaining(
                cb -> {
                  cdg.getEdgeLabels(bb, cb)
                      .forEach(
                          lb -> {
                            cdg.getSuccNodes(bb)
                                .forEachRemaining(
                                    ob -> {
                                      if (ob != cb) {
                                        Set<ISSABasicBlock> obb =
                                            DFS.getReachableNodes(cdg, Collections.singleton(ob));
                                        cdg.getEdgeLabels(bb, ob)
                                            .forEach(
                                                olb -> {
                                                  if (lb != olb) {
                                                    if (obb.contains(cb)
                                                        && DFS.getReachableNodes(
                                                                FD, Collections.singleton(bb))
                                                            .contains(cb)
                                                        && DFS.getReachableNodes(
                                                                FD, Collections.singleton(bb))
                                                            .contains(ob)) {
                                                      if (!moveAfterWithLabel.containsKey(bb)) {
                                                        moveAfterWithLabel.put(
                                                            bb, HashSetFactory.make());
                                                      }
                                                      moveAfterWithLabel.get(bb).add(cb);

                                                      obb.stream()
                                                          .filter(rnb -> cdg.hasEdge(rnb, cb))
                                                          .forEach(
                                                              rnb -> {
                                                                if (!skipDueToLabel.containsKey(
                                                                    rnb)) {
                                                                  skipDueToLabel.put(
                                                                      rnb, HashSetFactory.make());
                                                                }
                                                                skipDueToLabel.get(rnb).add(cb);
                                                              });

                                                      Iterator<ISSABasicBlock> landings =
                                                          obb.stream()
                                                              .map(
                                                                  xb ->
                                                                      IteratorUtil.streamify(
                                                                              cfg.getSuccNodes(xb))
                                                                          .filter(
                                                                              xxb ->
                                                                                  !obb.contains(
                                                                                      xxb)))
                                                              .reduce((a, b) -> Stream.concat(a, b))
                                                              .get()
                                                              .distinct()
                                                              .iterator();
                                                      if (landings.hasNext()) {
                                                        ISSABasicBlock landing = landings.next();
                                                        if (!landings.hasNext()) {
                                                          obb.stream()
                                                              .filter(
                                                                  xb ->
                                                                      cfg.getNormalSuccessors(xb)
                                                                          .contains(landing))
                                                              .forEach(
                                                                  xb -> {
                                                                    if (!breakDueToLabel
                                                                        .containsKey(xb)) {
                                                                      breakDueToLabel.put(
                                                                          xb,
                                                                          HashMapFactory.make());
                                                                    }
                                                                    breakDueToLabel
                                                                        .get(xb)
                                                                        .put(landing, bb);
                                                                  });
                                                        }
                                                      }
                                                    }
                                                  }
                                                });
                                      }
                                    });
                          });
                });
      }

      if (!moveAfterWithLabel.isEmpty()) {
        System.err.println("found move after: " + moveAfterWithLabel);
      }

      Map<ISSABasicBlock, Integer> cfgFinishTimes =
          computeFinishTimes(() -> NonNullSingletonIterator.make(cfg.entry()), cfg);
      Map<ISSABasicBlock, Integer> cfgStartTimes =
          computeStartTimes(() -> NonNullSingletonIterator.make(cfg.entry()), cfg);

      BiPredicate<ISSABasicBlock, ISSABasicBlock> isBackEdge =
          (pred, succ) ->
              cfgStartTimes.get(pred) >= cfgStartTimes.get(succ)
                  && cfgFinishTimes.get(pred) <= cfgFinishTimes.get(succ);

      loopHeaders =
          cfg.stream()
              .filter(
                  bb -> {
                    Iterator<ISSABasicBlock> ps = cfg.getPredNodes(bb);
                    while (ps.hasNext()) {
                      ISSABasicBlock pred = ps.next();
                      if (isBackEdge.test(pred, bb)) {
                        return true;
                      }
                    }
                    return false;
                  })
              .collect(Collectors.toSet());

      System.err.println("loop headers: " + loopHeaders);

      loops = HashSetFactory.make();
      loopExits = HashSetFactory.make();
      loopBreakers = HashSetFactory.make();
      loopControls = HashMapFactory.make();
      cfgNoBack = GraphSlicer.prune(cfg, (p, s) -> !isBackEdge.test(p, s));
      cfg.forEach(
          n -> {
            cfg.getPredNodes(n)
                .forEachRemaining(
                    p -> {
                      if (isBackEdge.test(p, n)) {
                        System.err.println("back:" + p + " --> " + n);

                        Set<ISSABasicBlock> forward =
                            DFS.getReachableNodes(cfgNoBack, Collections.singleton(n));
                        Set<ISSABasicBlock> backward =
                            DFS.getReachableNodes(
                                GraphInverter.invert(cfgNoBack), Collections.singleton(p));

                        Set<ISSABasicBlock> loop = HashSetFactory.make(forward);
                        loop.retainAll(backward);

                        loops.add(loop);

                        System.err.println("loop: " + loop);

                        loop.forEach(
                            bb -> {
                              IteratorUtil.streamify(cfg.getSuccNodes(bb))
                                  .filter(b -> !loop.contains(b))
                                  .forEach(sb -> loopExits.add(sb));
                            });
                        Set<ISSABasicBlock> breakers = HashSetFactory.make();
                        breakers.addAll(
                            loop.stream()
                                .filter(
                                    bb -> {
                                      System.err.println("1: " + bb);
                                      return IteratorUtil.streamify(cfg.getSuccNodes(bb))
                                          .anyMatch(
                                              b -> {
                                                return !loop.contains(b);
                                              });
                                    })
                                .filter(
                                    bb -> {
                                      System.err.println(
                                          "2: "
                                              + bb
                                              + ": "
                                              + DFS.getReachableNodes(
                                                  GraphSlicer.prune(
                                                      cfgNoBack, (pb, s) -> pb.equals(bb)),
                                                  Collections.singleton(n)));

                                      return n.equals(bb)
                                          || !DFS.getReachableNodes(
                                                  GraphSlicer.prune(
                                                      cfgNoBack, (pb, s) -> pb.equals(bb)),
                                                  Collections.singleton(n))
                                              .contains(p);
                                    })
                                .sorted(
                                    (a, b) -> {
                                      return a.getFirstInstructionIndex()
                                          - b.getFirstInstructionIndex();
                                    })
                                .collect(Collectors.toSet()));
                        assert (breakers.size() > 0);
                        // Pick the first one - the one with smallest number
                        loopControls.put(
                            breakers.stream()
                                .min(Comparator.comparing(ISSABasicBlock::getNumber))
                                .get(),
                            loop);
                        loopBreakers.addAll(breakers);
                      }
                    });
          });

      System.err.println("loop controls: " + loopControls);

      for (ISSABasicBlock b : cfg) {
        if (loopHeaders.contains(b)) {
          System.err.println("bad flow: starting " + b);
          cfg.getPredNodes(b)
              .forEachRemaining(
                  s -> {
                    System.err.println("bad flow: pred " + s);
                    if (isBackEdge.test(s, b)) {
                      System.err.println("bad flow: back edge");
                      int n = Util.whichPred(cfg, s, b);
                      b.iteratePhis()
                          .forEachRemaining(
                              phi -> {
                                System.err.println("bad flow: phi " + phi);
                                int vn = phi.getUse(n);
                                SSAInstruction def = du.getDef(vn);
                                System.err.println("bad flow: def " + def);
                                if (def instanceof SSAPhiInstruction) {
                                  System.err.println("bad flow: " + vn + " --> " + phi.getDef());
                                  for (int i = 0; i < def.getNumberOfUses(); i++) {
                                    livenessConflicts.add(def.getDef(), def.getUse(i));
                                  }
                                }
                              });
                    }
                  });
        }
      }

      mergedValues = IntSetUtil.make();
      mergePhis = new IntegerUnionFind(ST.getMaxValueNumber());
      ir.getControlFlowGraph()
          .iterator()
          .forEachRemaining(
              bb -> {
                bb.iteratePhis()
                    .forEachRemaining(
                        phi -> {
                          int def = phi.getDef();
                          for (int i = 0; i < phi.getNumberOfUses(); i++) {
                            int use = phi.getUse(i);
                            if (!ST.isConstant(use) && !livenessConflicts.contains(def, use)) {
                              IntSet currentDefConflicts = livenessConflicts.getRelated(def);
                              IntSet currentUseConflicts = livenessConflicts.getRelated(use);
                              currentDefConflicts.foreach(
                                  vn -> {
                                    livenessConflicts.add(mergePhis.find(use), mergePhis.find(vn));
                                    livenessConflicts.add(mergePhis.find(vn), mergePhis.find(use));
                                  });
                              currentUseConflicts.foreach(
                                  vn -> {
                                    livenessConflicts.add(mergePhis.find(def), mergePhis.find(vn));
                                    livenessConflicts.add(mergePhis.find(vn), mergePhis.find(def));
                                  });

                              mergePhis.union(def, use);
                              System.err.println(
                                  "merging " + def + " and " + use + " as " + mergePhis.find(def));
                              mergedValues.add(use);
                              mergedValues.add(def);
                            }
                          }
                        });
              });

      this.sourceNames = makeNames();

      regions = HashMapFactory.make();
      linePhis = HashMapFactory.make();
      cdg.forEach(
          node -> {
            if (loopControls.containsKey(node)
                || loops.stream().noneMatch(loop -> loop.contains(node))) {}

            Set<Pair<SSAInstruction, ISSABasicBlock>> regionKey = HashSetFactory.make();
            cdg.getPredNodes(node)
                .forEachRemaining(
                    p -> {
                      cdg.getEdgeLabels(p, node)
                          .forEach(
                              lbl -> {
                                if (!isBackEdge.test(p, node)) {
                                  regionKey.add(
                                      Pair.make(p.getLastInstruction(), (ISSABasicBlock) lbl));
                                }
                              });
                    });

            if (!regions.containsKey(regionKey)) {
              regions.put(regionKey, HashSetFactory.make());
            }
            regions.get(regionKey).add(node);
          });
      regions
          .entrySet()
          .forEach(
              es -> {
                System.err.println("----");
                System.err.println(es.getKey());
                System.err.println(es.getValue());
              });

      MutableIntSet unmergeableValues = IntSetUtil.make();
      regionChunks = HashMapFactory.make();
      regions
          .entrySet()
          .forEach(
              es -> {
                List<SSAInstruction> regionInsts = new ArrayList<>();
                es.getValue().stream()
                    .sorted((a, b) -> a.getLastInstructionIndex() - b.getLastInstructionIndex())
                    .filter(
                        rbb -> {
                          for (Pair<SSAInstruction, ISSABasicBlock> p : es.getKey()) {
                            ISSABasicBlock pb = cfg.getBlockForInstruction(p.fst.iIndex());
                            if (skipDueToLabel.containsKey(pb)
                                && skipDueToLabel.get(pb).contains(rbb)) {
                              return false;
                            }
                          }
                          return true;
                        })
                    .forEach(
                        bb -> {
                          bb.iterator()
                              .forEachRemaining(
                                  inst -> {
                                    if (!(inst instanceof SSAPhiInstruction)) {
                                      regionInsts.add(inst);
                                    }
                                  });
                          cfg.getSuccNodes(bb)
                              .forEachRemaining(
                                  sb -> {
                                    boolean backEdge =
                                        bb.getLastInstructionIndex() >= 0
                                            && sb.getLastInstructionIndex() >= 0
                                            && bb.getLastInstructionIndex()
                                                < sb.getFirstInstructionIndex();
                                    List<SSAInstruction> ii;
                                    if ((Util.endsWithConditionalBranch(cfg, bb)
                                            && (Util.getTakenSuccessor(cfg, bb).equals(sb)
                                                || Util.getNotTakenSuccessor(cfg, bb).equals(sb)))
                                        || (Util.endsWithSwitch(cfg, bb)
                                            && Util.getFallThruBlock(cfg, bb).equals(sb))) {

                                      Pair<SSAInstruction, ISSABasicBlock> lineKey =
                                          Pair.make(bb.getLastInstruction(), sb);
                                      if (!linePhis.containsKey(lineKey)) {
                                        linePhis.put(lineKey, new ArrayList<>());
                                      }
                                      ii = linePhis.get(lineKey);
                                    } else {
                                      ii = regionInsts;
                                    }

                                    Pair<BasicNaturalRelation, Iterable<SSAPhiInstruction>> order =
                                        orderPhisAndDetectCycles(sb.iteratePhis());
                                    System.err.println("order: " + order);

                                    List<AssignInstruction> as = new ArrayList<>();
                                    order
                                        .snd
                                        .iterator()
                                        .forEachRemaining(
                                            phi -> {
                                              System.err.println(
                                                  "phi at "
                                                      + bb
                                                      + " with "
                                                      + bb.getLastInstruction()
                                                      + " "
                                                      + cfg.getSuccNodeCount(bb));
                                              int lv = phi.getDef();
                                              int rv = phi.getUse(Util.whichPred(cfg, bb, sb));
                                              if (mergePhis.find(lv) != mergePhis.find(rv)) {
                                                int lh = lv, rh = rv;
                                                if (order.fst.anyRelated(mergePhis.find(rv))) {

                                                  int tmp =
                                                      order
                                                          .fst
                                                          .getRelated(mergePhis.find(rv))
                                                          .intIterator()
                                                          .next();
                                                  ir.getSymbolTable().ensureSymbol(tmp);
                                                  types.copyType(mergePhis.find(rv), tmp);
                                                  as.add(
                                                      0,
                                                      new AssignInstruction(
                                                          bb.getLastInstructionIndex() + 1,
                                                          tmp,
                                                          mergePhis.find(rv)));

                                                  lh = mergePhis.find(lv);
                                                  rh =
                                                      order
                                                          .fst
                                                          .getRelated(mergePhis.find(rv))
                                                          .intIterator()
                                                          .next();
                                                }
                                                types.copyType(rh, lh);
                                                AssignInstruction assign =
                                                    new AssignInstruction(
                                                        bb.getLastInstructionIndex() + 1, lh, rh);
                                                if (backEdge
                                                    && sb.getLastInstruction()
                                                        instanceof
                                                        SSAConditionalBranchInstruction) {
                                                  System.err.println(
                                                      "back edge for "
                                                          + sb.getLastInstructionIndex());
                                                  System.err.println("back edge for " + assign);
                                                  System.err.println(
                                                      "back edge for " + es.getKey());
                                                }
                                                if (Util.endsWithConditionalBranch(cfg, bb)
                                                    && (Util.getTakenSuccessor(cfg, bb).equals(sb)
                                                        || Util.getNotTakenSuccessor(cfg, bb)
                                                            .equals(bb))) {
                                                  System.err.println(
                                                      "found line phi for "
                                                          + bb
                                                          + " to "
                                                          + sb
                                                          + " for "
                                                          + rv
                                                          + " -> "
                                                          + lv);
                                                }

                                                System.err.println(
                                                    "adding " + assign + " for " + bb + " --> "
                                                        + sb);
                                                as.add(assign);
                                              }
                                            });
                                    ii.addAll(as);
                                  });
                        });

                System.err.println("insts: " + regionInsts);
                List<SSAInstruction> all = new ArrayList<>(regionInsts);
                Heap<List<SSAInstruction>> chunks =
                    computeChunks(ir, cha, unmergeableValues, regionInsts);
                System.err.println("chunks: " + chunks);
                processChunks(
                    regionInsts,
                    chunks,
                    (chunk) -> {
                      es.getKey()
                          .forEach(
                              p -> {
                                skip:
                                {
                                  for (SSAInstruction inst : chunk) {
                                    if (!(inst instanceof AssignInstruction)) {
                                      ISSABasicBlock ctlBB =
                                          cfg.getBlockForInstruction(p.fst.iIndex());
                                      ISSABasicBlock instBB =
                                          cfg.getBlockForInstruction(inst.iIndex());
                                      if (skipDueToLabel.containsKey(ctlBB)) {
                                        if (skipDueToLabel.get(ctlBB).contains(instBB)) {
                                          System.err.println("skip");
                                          break skip;
                                        }
                                      }
                                    }
                                  }

                                  if (!regionChunks.containsKey(p)) {
                                    regionChunks.put(p, new ArrayList<>());
                                  }
                                  regionChunks.get(p).add(chunk);
                                }
                              });
                    });

                es.getKey()
                    .forEach(
                        p -> {
                          if (regionChunks.containsKey(p)) {
                            orderChunk(all, regionChunks.get(p));
                          }
                        });

                assert regionInsts.isEmpty() : regionInsts + " remaining, with chunks " + chunks;
              });

      System.err.println("root region chunks: " + regionChunks);

      ir.iterateNormalInstructions()
          .forEachRemaining(
              inst -> {
                if (inst instanceof SSAGotoInstruction) {
                  ISSABasicBlock bb = cfg.getBlockForInstruction(inst.iIndex());
                  if (loopHeaders.containsAll(cfg.getNormalSuccessors(bb))) {
                    System.err.println("loop edge " + inst);
                  } else if (loopExits.containsAll(cfg.getNormalSuccessors(bb))) {
                    System.err.println("break edge " + inst);
                  }
                }
              });

      initChildren();
    }

    private void orderChunk(List<SSAInstruction> all, List<List<SSAInstruction>> list) {
      list.sort(
          (lc, rc) ->
              positionByIdentity(all, lc.iterator().next())
                  - positionByIdentity(all, rc.iterator().next()));
    }

    private Heap<List<SSAInstruction>> computeChunks(
        IR ir,
        IClassHierarchy cha,
        MutableIntSet unmergeableValues,
        List<SSAInstruction> regionInsts) {
      Heap<List<SSAInstruction>> chunks =
          new Heap<>(regionInsts.size()) {
            @Override
            protected boolean compareElements(
                List<SSAInstruction> elt1, List<SSAInstruction> elt2) {
              return elt1.size() > elt2.size()
                  ? true
                  : elt1.size() < elt2.size()
                      ? false
                      : elt1.toString().compareTo(elt2.toString()) > 0;
            }
          };
      regionInsts.forEach(
          inst -> {
            Set<SSAInstruction> insts = HashSetFactory.make();
            makeTreeBuilder(ir, cha, cfg, cdg)
                .gatherInstructions(
                    insts,
                    ir,
                    du,
                    regionInsts,
                    inst,
                    chunks,
                    unmergeableValues,
                    (inst instanceof SSAConditionalBranchInstruction)
                            && loopControls.containsKey(cfg.getBlockForInstruction(inst.iIndex()))
                            // If it's a while loop then merge instructions in test
                            // otherwise return null then the instructions will be
                            // translated
                            // into several lines and might be placed in different places
                            && isWhileLoop(inst, unmergeableValues)
                        ? inst
                        : null);
            if (insts.isEmpty()) {
              insts.add(inst);
              chunks.insert(new ArrayList<>(insts));
            }
            System.err.println("chunk for " + inst + ": " + insts);
          });
      return chunks;
    }

    void processChunks(
        List<SSAInstruction> regionInsts,
        Heap<List<SSAInstruction>> chunks,
        Consumer<List<SSAInstruction>> f) {
      SortedSet<List<SSAInstruction>> orderedChunks =
          new TreeSet<>((a, b) -> a.iterator().next().iIndex() - b.iterator().next().iIndex());
      while (chunks.size() > 0 && !regionInsts.isEmpty()) {
        List<SSAInstruction> chunk = chunks.take();
        System.err.println(
            "taking "
                + chunk.stream().map(i -> i + " " + i.iIndex()).reduce("", (a, b) -> a + ", " + b));
        if (hasAllByIdentity(regionInsts, chunk)) {
          removeAllByIdentity(regionInsts, chunk);
          System.err.println(
              "using "
                  + chunk.stream()
                      .map(i -> i + " " + i.iIndex())
                      .reduce("", (a, b) -> a + ", " + b));
          orderedChunks.add(chunk);
        }
      }

      for (List<SSAInstruction> c : orderedChunks) {
        f.accept(c);
      }
    }

    // detect the loop type, in this case only care about if it's a normal while loop
    private boolean isWhileLoop(SSAInstruction inst, IntSet unmergeableValues) {
      Set<SSAInstruction> insts = HashSetFactory.make();
      List<SSAInstruction> regionInsts = new ArrayList<>();

      // If it's dowhile, loopHeader should not contains this block
      ISSABasicBlock bb = cfg.getBlockForInstruction(inst.iIndex());
      if (!loopHeaders.contains(bb)) {
        return false;
      }

      cfg.getBlockForInstruction(inst.iIndex())
          .forEach(
              iinst -> {
                if (iinst.iIndex() >= 0) {
                  regionInsts.add(iinst);
                }
              });
      makeTreeBuilder(ir, cha, cfg, cdg)
          .gatherInstructions(
              insts,
              ir,
              du,
              regionInsts,
              inst,
              new Heap<List<SSAInstruction>>(regionInsts.size()) {

                @Override
                protected boolean compareElements(
                    List<SSAInstruction> elt1, List<SSAInstruction> elt2) {
                  return false;
                }
              },
              unmergeableValues,
              (inst instanceof SSAConditionalBranchInstruction)
                      && loopControls.containsKey(cfg.getBlockForInstruction(inst.iIndex()))
                  ? inst
                  : null);
      return insts.containsAll(regionInsts);
    }

    private void initChildren() {
      bbs()
          .forEach(
              bb -> {
                regions
                    .entrySet()
                    .forEach(
                        es -> {
                          es.getKey()
                              .forEach(
                                  k -> {
                                    System.err.println(
                                        "checking " + k.fst + " with " + bb.getLastInstruction());
                                    if (k.fst.equals(bb.getLastInstruction())) {
                                      if (!children.containsKey(k.fst)) {
                                        children.put(k.fst, HashMapFactory.make());
                                      }
                                      children.get(k.fst).put(k.snd, makeChild(k));
                                      System.err.println(
                                          "child of "
                                              + k.fst
                                              + " for "
                                              + k.snd
                                              + " is "
                                              + children.get(k.fst).get(k.snd));
                                    }
                                  });
                        });
              });
      System.err.println("children for " + this + ": " + children);
    }

    protected RegionTreeNode makeChild(Pair<SSAInstruction, ISSABasicBlock> k) {
      return new RegionTreeNode(this, k.fst, k.snd);
    }

    Iterable<ISSABasicBlock> bbs() {
      Set<Pair<SSAInstruction, ISSABasicBlock>> key = Collections.singleton(Pair.make(r, l));
      if (regions.containsKey(key)) {
        return regions.get(key);
      } else {
        return Collections.emptySet();
      }
    }

    private void indent(StringBuilder sb, int level) {
      for (int i = 0; i < level; i++) {
        sb.append("  ");
      }
    }

    /*
     * private boolean phiChunk(Set<SSAInstruction> insts) { return insts.size()
     * == 1 && insts.iterator().next() instanceof SSAPhiInstruction; }
     */

    private boolean gotoChunk(List<SSAInstruction> insts) {
      return insts.size() == 1 && insts.iterator().next() instanceof SSAGotoInstruction;
    }

    boolean controlOrderedInChunk(SSAInstruction l, SSAInstruction r, List<SSAInstruction> chunk) {
      return !(deemedFunctional(l, chunk, cha) && deemedFunctional(r, chunk, cha))
          && l.iIndex() < r.iIndex();
    }

    /*
     * boolean controlOrderedChunks( List<SSAInstruction> ls,
     * List<SSAInstruction> rs, List<SSAInstruction> insts) { for
     * (SSAInstruction l : ls) { if (!deemedFunctional(l, insts, cha)) { for
     * (SSAInstruction r : rs) { if (!deemedFunctional(r, insts, cha)) { return
     * l.iIndex() < r.iIndex(); } } } } return false; }
     */

    // Check if the given chunk contains any instruction that's part of loop control
    private boolean isInLoopControl(List<SSAInstruction> chunk) {
      return chunk.stream()
          .map(inst -> cfg.getBlockForInstruction(inst.iIndex()))
          .anyMatch(bb -> loopControls.containsKey(bb));
    }

    // There are some instructions that's part of loop control block but should be translated
    // as part of loop body. This method is trying to determine if the given chunk
    // is the case
    private boolean shouldBeLoopBody(List<SSAInstruction> chunk, Set<ISSABasicBlock> loopBlocks) {
      return isInLoopControl(chunk)
          ? (!isConditional(chunk) && !isAssignment(chunk))
          : isBeforeLoopControl(chunk, loopBlocks);
    }

    private boolean isBeforeLoopControl(
        List<SSAInstruction> chunk, Set<ISSABasicBlock> loopBlocks) {
      return chunk.stream()
          .anyMatch(
              inst ->
                  inst.iIndex() > 0
                      && loopBlocks.contains(cfg.getBlockForInstruction(inst.iIndex())));
    }

    // Check if the given chunk contains any instruction that's part of conditional branch
    private boolean isConditional(List<SSAInstruction> chunk) {
      return chunk.stream().anyMatch(inst -> inst instanceof SSAConditionalBranchInstruction);
    }

    // Check if the given chunk contains any instruction that's an assignment generated by phi node
    private boolean isAssignment(List<SSAInstruction> chunk) {
      return chunk.stream().allMatch(inst -> inst instanceof AssignInstruction);
    }

    private Set<ISSABasicBlock> getLoopBlocks(List<List<SSAInstruction>> chunks) {
      Set<ISSABasicBlock> loopBlocks = HashSetFactory.make();
      for (List<SSAInstruction> chunk : chunks) {
        for (SSAInstruction inst : chunk) {
          if (inst.iIndex() > 0) {
            ISSABasicBlock blockForInstruction = cfg.getBlockForInstruction(inst.iIndex());
            if (loopControls.containsKey(blockForInstruction)) {
              loopBlocks.addAll(loopControls.get(blockForInstruction));
            }
          }
        }
      }
      return loopBlocks;
    }

    public CAstNode toCAst() {
      CAst ast = new CAstImpl();
      List<CAstNode> elts = new ArrayList<>();
      List<CAstNode> decls = new ArrayList<>();
      List<List<SSAInstruction>> chunks = regionChunks.get(Pair.make(r, l));

      Set<ISSABasicBlock> loopBlocks = getLoopBlocks(chunks);

      chunks.forEach(
          chunkInsts -> {
            // Ignore goto chunks as well as the chunks that's part of loop control but
            // should be translated as loop body
            if (!gotoChunk(chunkInsts)) {
              if (!shouldBeLoopBody(chunkInsts, loopBlocks)) {
                Pair<CAstNode, List<CAstNode>> stuff =
                    makeToCAst(chunkInsts).processChunk(decls, packages, false);
                elts.add(stuff.fst);
                decls.addAll(stuff.snd);
              } else moveAsLoopBody.put(chunkInsts, RegionTreeNode.this);
            }
          });
      chunks.stream()
          .filter(this::gotoChunk)
          .forEach(
              c -> {
                if (!shouldBeLoopBody(c, loopBlocks)) {
                  Pair<CAstNode, List<CAstNode>> stuff =
                      makeToCAst(c).processChunk(decls, packages, false);
                  elts.add(stuff.fst);
                  decls.addAll(stuff.snd);
                } else moveAsLoopBody.put(c, RegionTreeNode.this);
              });
      decls.addAll(elts);
      return ast.makeNode(CAstNode.BLOCK_STMT, decls.toArray(new CAstNode[decls.size()]));
    }

    protected ToCAst makeToCAst(List<SSAInstruction> insts) {
      return new ToCAst(insts, new CodeGenerationContext(types, mergePhis, false));
    }

    private void toString(StringBuilder sb, int level) {
      List<List<SSAInstruction>> chunks = regionChunks.get(Pair.make(r, l));
      if (chunks == null) {
        return;
      }
      chunks.forEach(
          insts -> {
            if (!gotoChunk(insts)) {
              insts
                  .iterator()
                  .forEachRemaining(
                      i -> {
                        indent(sb, level + 1);
                        sb.append(i.toString(ST)).append("\n");
                        if (children.containsKey(i)) {
                          children
                              .get(i)
                              .entrySet()
                              .forEach(
                                  ls -> {
                                    indent(sb, level + 1);
                                    sb.append("---\n");
                                    ls.getValue().toString(sb, level + 2);
                                  });
                          indent(sb, level + 1);
                          sb.append("---\n");
                        }
                      });
            }
          });
      chunks.stream()
          .filter(this::gotoChunk)
          .forEach(
              c -> {
                indent(sb, level + 1);
                sb.append(c).append("\n");
              });
    }

    @Override
    public String toString() {
      StringBuilder sb = new StringBuilder();
      toString(sb, 0);
      return sb.toString().trim().length() == 0 ? "<empty>" : sb.toString();
    }

    protected final CAst ast = new CAstImpl();

    private final Map<String, Set<String>> packages;

    protected class ToCAst {
      private final List<SSAInstruction> chunk;
      private final CodeGenerationContext c;

      protected class Visitor implements AstPreInstructionVisitor {
        private Deque<Set<SSAInstruction>> history;
        private CAstNode node = ast.makeNode(CAstNode.EMPTY);
        private List<SSAInstruction> chunk;
        private Map<SSAInstruction, Map<ISSABasicBlock, RegionTreeNode>> children;
        private SSAInstruction root;
        protected final List<CAstNode> parentDecls;
        private final List<CAstNode> decls = new ArrayList<>();
        private final Map<String, Set<String>> packages;

        private void logHistory(SSAInstruction inst) {
          if (history != null && !history.isEmpty()) {
            history.peek().add(inst);
          }
        }

        private void startLog() {
          if (history == null) {
            history = new ArrayDeque<>();
          }
          history.push(HashSetFactory.make());
        }

        private Set<SSAInstruction> endLog() {
          assert history != null && !history.isEmpty();
          Set<SSAInstruction> h = history.pop();
          if (!history.isEmpty()) {
            history.peek().addAll(h);
          }
          return h;
        }

        public Visitor(
            SSAInstruction root,
            CodeGenerationContext c,
            List<SSAInstruction> chunk,
            List<CAstNode> parentDecls,
            Map<String, Set<String>> parentPackages,
            Map<SSAInstruction, Map<ISSABasicBlock, RegionTreeNode>> children,
            boolean extraHeaderCode) {
          this.root = root;
          this.chunk = chunk;
          this.children = children;
          this.parentDecls = parentDecls;
          this.packages = parentPackages;
          root.visit(this);
          if (root.hasDef()) {
            if (node.getKind() != CAstNode.EMPTY) {
              int def = root.getDef();
              if (extraHeaderCode
                  || mergedValues.contains(mergePhis.find(def))
                  || du.getDef(def) instanceof SSAPhiInstruction) {
                CAstNode val = node;
                node =
                    ast.makeNode(
                        CAstNode.EXPR_STMT,
                        ast.makeNode(
                            CAstNode.ASSIGN,
                            ast.makeNode(CAstNode.VAR, makeVariableName(def)),
                            val));
              } else {
                CAstNode val = node;
                CAstType type;
                try {
                  type = toSource(c.getTypes().getType(def).getTypeReference());
                } catch (IndexOutOfBoundsException e) {
                  type = toSource(TypeReference.Int);
                }
                node =
                    ast.makeNode(
                        CAstNode.DECL_STMT,
                        ast.makeNode(CAstNode.VAR, makeVariableName(def)),
                        ast.makeConstant(type),
                        val);
              }
            }
          }
        }

        private boolean checkDecls(int def, List<CAstNode> decls) {
          return decls.stream()
              .noneMatch(
                  d ->
                      varDefPattern(ast.makeConstant(sourceNames.get(mergePhis.find(def))))
                          .match(d, null));
        }

        @SuppressWarnings("unused")
        private boolean checkDecl(int def) {
          return ST.getNumberOfParameters() < def
              && checkDecls(def, decls)
              && checkDecls(def, parentDecls);
        }

        private CAstNode visit(int vn) {
          if (ST.isConstant(vn)) {
            Object value = ST.getConstantValue(vn);
            if (value instanceof String) {
              value = ((String) value).replace("\n", "\\n");
            }
            return ast.makeConstant(value);
          } else if (ST.getNumberOfParameters() >= vn) {
            if (cfg.getMethod().isStatic()) {
              return ast.makeNode(CAstNode.VAR, makeVariableName(vn));
            } else {
              if (vn == 1) {
                return ast.makeNode(CAstNode.THIS);
              } else {
                return ast.makeNode(CAstNode.VAR, makeVariableName(vn));
              }
            }
          } else {
            SSAInstruction inst = du.getDef(vn);
            if (chunk.contains(inst)) {
              logHistory(inst);
              inst.visit(this);
              if (root instanceof SSAConditionalBranchInstruction
                  && loopControls.containsKey(cfg.getBlockForInstruction(root.iIndex()))
                  && inst.hasDef()
                  && du.getNumberOfUses(vn) > 1) {

                /*
                if (checkDecl(mergePhis.find(vn))) {
                  decls.add(
                      ast.makeNode(
                          CAstNode.DECL_STMT,
                          ast.makeNode(CAstNode.VAR, makeVariableName(vn)),
                          ast.makeConstant(toSource(c.getTypes().getType(vn).getTypeReference()))));
                }
                */

                return ast.makeNode(
                    CAstNode.BLOCK_EXPR,
                    ast.makeNode(
                        CAstNode.ASSIGN, ast.makeNode(CAstNode.VAR, makeVariableName(vn)), node));
              } else {
                return node;
              }
            } else {
              return ast.makeNode(CAstNode.VAR, makeVariableName(vn));
            }
          }
        }

        private boolean inLoop(ISSABasicBlock bb) {
          return loopControls.values().stream().anyMatch(blocks -> blocks.contains(bb));
        }

        @Override
        public void visitGoto(SSAGotoInstruction inst) {
          ISSABasicBlock bb = cfg.getBlockForInstruction(inst.iIndex());
          if (inst.getTarget() >= 0) {
            ISSABasicBlock target = cfg.getBlockForInstruction(inst.getTarget());
            if (breakDueToLabel.containsKey(bb) && breakDueToLabel.get(bb).containsKey(target)) {
              System.err.println("found landing for " + breakDueToLabel.get(bb).get(target));
              node =
                  ast.makeNode(
                      CAstNode.BLOCK_STMT,
                      ast.makeNode(
                          CAstNode.BREAK,
                          ast.makeConstant(
                              "lbl_" + breakDueToLabel.get(bb).get(target).getNumber())));
              return;
            }
          }
          if (loopHeaders.containsAll(cfg.getNormalSuccessors(bb)) && inLoop(bb)) {
            node = ast.makeNode(CAstNode.CONTINUE);
          } else if (loopExits.containsAll(cfg.getNormalSuccessors(bb)) && inLoop(bb)) {
            node = ast.makeNode(CAstNode.BLOCK_STMT, ast.makeNode(CAstNode.BREAK));
          } else {
            node = ast.makeNode(CAstNode.BLOCK_STMT, ast.makeNode(CAstNode.GOTO));
          }
        }

        @Override
        public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
          CAstNode array = visit(instruction.getArrayRef());
          CAstNode index = visit(instruction.getIndex());
          CAstNode elt = ast.makeConstant(toSource(instruction.getElementType()));
          node = ast.makeNode(CAstNode.ARRAY_REF, array, elt, index);
        }

        @Override
        public void visitArrayStore(SSAArrayStoreInstruction instruction) {
          CAstNode array = visit(instruction.getArrayRef());
          CAstNode index = visit(instruction.getIndex());
          CAstNode value = visit(instruction.getValue());
          CAstNode elt = ast.makeConstant(toSource(instruction.getElementType()));
          node =
              ast.makeNode(
                  CAstNode.EXPR_STMT,
                  ast.makeNode(
                      CAstNode.ASSIGN, ast.makeNode(CAstNode.ARRAY_REF, array, elt, index), value));
        }

        @Override
        public void visitBinaryOp(SSABinaryOpInstruction instruction) {
          CAstNode left = visit(instruction.getUse(0));
          CAstNode right = visit(instruction.getUse(1));

          CAstOperator op = null;
          IOperator operator = instruction.getOperator();
          if (operator instanceof IShiftInstruction.Operator) {
            switch ((IShiftInstruction.Operator) operator) {
              case SHL:
                op = CAstOperator.OP_LSH;
                break;
              case SHR:
                op = CAstOperator.OP_RSH;
                break;
              case USHR:
                op = CAstOperator.OP_URSH;
                break;
              default:
                assert false;
                break;
            }
          }
          if (operator instanceof IBinaryOpInstruction.Operator) {
            switch ((IBinaryOpInstruction.Operator) operator) {
              case ADD:
                op = CAstOperator.OP_ADD;
                break;
              case AND:
                op = CAstOperator.OP_BIT_AND;
                break;
              case DIV:
                op = CAstOperator.OP_DIV;
                break;
              case MUL:
                op = CAstOperator.OP_MUL;
                break;
              case OR:
                op = CAstOperator.OP_BIT_OR;
                break;
              case REM:
                op = CAstOperator.OP_MOD;
                break;
              case SUB:
                op = CAstOperator.OP_SUB;
                break;
              case XOR:
                op = CAstOperator.OP_BIT_XOR;
                break;
              default:
                assert false;
                break;
            }
          } else if (operator instanceof CAstBinaryOp) {
            switch ((CAstBinaryOp) operator) {
              case CONCAT:
                op = CAstOperator.OP_CONCAT;
                break;
              case EQ:
                op = CAstOperator.OP_EQ;
                break;
              case GE:
                op = CAstOperator.OP_GE;
                break;
              case GT:
                op = CAstOperator.OP_GT;
                break;
              case LE:
                op = CAstOperator.OP_LE;
                break;
              case LT:
                op = CAstOperator.OP_LT;
                break;
              case NE:
                op = CAstOperator.OP_NE;
                break;
              case STRICT_EQ:
                op = CAstOperator.OP_EQ;
                break;
              case STRICT_NE:
                op = CAstOperator.OP_NE;
                break;
              case EXP:
                node =
                    ast.makeNode(
                        CAstNode.CALL,
                        ast.makeConstant(
                            MethodReference.findOrCreate(
                                TypeReference.JavaLangMath, "pow", "(DD)D")),
                        ast.makeConstant(true),
                        left,
                        right);
                return;
              default:
                break;
            }
          }

          node = ast.makeNode(CAstNode.BINARY_EXPR, op, left, right);
        }

        @Override
        public void visitUnaryOp(SSAUnaryOpInstruction instruction) {
          CAstNode arg = visit(instruction.getUse(0));

          CAstOperator op = null;
          IUnaryOpInstruction.IOperator opcode = instruction.getOpcode();
          if (opcode instanceof IUnaryOpInstruction.Operator) {
            switch ((IUnaryOpInstruction.Operator) opcode) {
              case NEG:
                op = CAstOperator.OP_NOT;
                break;
              default:
                assert false;
            }
          } else if (opcode instanceof CAstUnaryOp) {
            switch ((CAstUnaryOp) opcode) {
              case BITNOT:
                op = CAstOperator.OP_BITNOT;
                break;
              case MINUS:
                op = CAstOperator.OP_SUB;
                break;
              case PLUS:
                op = CAstOperator.OP_ADD;
                break;
              default:
                break;
            }
          }

          node = ast.makeNode(CAstNode.UNARY_EXPR, op, arg);
        }

        private final MethodReference iv =
            MethodReference.findOrCreate(
                TypeReference.JavaLangInteger, "valueOf", "(Ljava/lang/String;)I");

        @Override
        public void visitConversion(SSAConversionInstruction instruction) {
          CAstNode value = visit(instruction.getUse(0));
          if (instruction.getFromType() == TypeReference.JavaLangString) {
            if (instruction.getToType() == TypeReference.Int) {
              node =
                  ast.makeNode(CAstNode.CALL, ast.makeConstant(iv), ast.makeConstant(true), value);
            } else {
              assert false : instruction;
            }
          } else {
            node =
                ast.makeNode(
                    CAstNode.CAST, ast.makeConstant(toSource(instruction.getToType())), value);
          }
        }

        @Override
        public void visitComparison(SSAComparisonInstruction instruction) {
          CAstNode left = visit(instruction.getUse(0));
          CAstNode right = visit(instruction.getUse(1));

          CAstOperator op = null;
          switch (instruction.getOperator()) {
            case CMP:
              op = CAstOperator.OP_EQ;
              break;
            case CMPG:
              op = CAstOperator.OP_GT;
              break;
            case CMPL:
              op = CAstOperator.OP_LT;
              break;
            default:
              assert false;
          }

          node = ast.makeNode(CAstNode.BINARY_EXPR, op, left, right);
        }

        private CAstNode checkLinePhi(
            CAstNode block, SSAInstruction branch, ISSABasicBlock target) {
          Pair<SSAInstruction, ISSABasicBlock> key = Pair.make(branch, target);
          System.err.println(
              "checking for line phi for instruction "
                  + branch
                  + " and target "
                  + target
                  + "in "
                  + linePhis);
          if (linePhis.containsKey(key)) {
            List<SSAInstruction> insts = linePhis.get(key);
            List<CAstNode> lp = handlePhiAssignments(insts);
            if (block != null) {
              lp.add(block);
            }
            return ast.makeNode(CAstNode.BLOCK_STMT, lp.toArray(new CAstNode[lp.size()]));
          } else {
            return block;
          }
        }

        private List<CAstNode> handlePhiAssignments(List<SSAInstruction> insts) {
          List<CAstNode> lp = new ArrayList<>();
          for (SSAInstruction inst : insts) {
            assert inst instanceof AssignInstruction;
            Visitor v =
                makeToCAst(insts)
                    .makeVisitor(
                        inst, c, Collections.singletonList(inst), parentDecls, packages, false);
            lp.add(
                ast.makeNode(
                    CAstNode.EXPR_STMT,
                    ast.makeNode(
                        CAstNode.ASSIGN, v.visit(inst.getDef()), v.visit(inst.getUse(0)))));
          }
          return lp;
        }

        private ISSABasicBlock getLoopSuccessor(ISSABasicBlock controlBB) {
          assert loopControls.containsKey(controlBB);
          Set<ISSABasicBlock> loopNodes = loopControls.get(controlBB);
          Graph<ISSABasicBlock> loopGraph = GraphSlicer.prune(cfg, n -> loopNodes.contains(n));

          ISSABasicBlock loopBB = null;
          for (ISSABasicBlock nextBB : cfg.getNormalSuccessors(controlBB)) {
            if (DFS.getReachableNodes(loopGraph, Collections.singleton(nextBB))
                .contains(controlBB)) {
              assert loopBB == null;
              loopBB = nextBB;
            }
          }

          assert loopBB != null;
          return loopBB;
        }

        private CAstNode getInitialTestNode(
            SSAConditionalBranchInstruction instruction,
            CAstNode v2,
            CAstNode v1,
            BasicBlock branchBB) {
          CAstOperator castOp = null;
          IConditionalBranchInstruction.IOperator op = instruction.getOperator();
          if (op instanceof IConditionalBranchInstruction.Operator) {
            switch ((IConditionalBranchInstruction.Operator) op) {
              case EQ:
                castOp = CAstOperator.OP_EQ;
                break;
              case GE:
                castOp = CAstOperator.OP_GE;
                break;
              case GT:
                castOp = CAstOperator.OP_GT;
                break;
              case LE:
                castOp = CAstOperator.OP_LE;
                break;
              case LT:
                castOp = CAstOperator.OP_LT;
                break;
              case NE:
                castOp = CAstOperator.OP_NE;
                break;
              default:
                assert false;
                break;
            }
          }
          CAstNode test;
          test:
          {
            if (v2.getValue() instanceof Number && ((Number) v2.getValue()).equals(0)) {
              if (castOp == CAstOperator.OP_NE) {
                test = v1;
                break test;
              } else if (castOp == CAstOperator.OP_EQ) {
                if (loopControls.containsKey(branchBB)) {
                  test = v1;
                } else {
                  test = ast.makeNode(CAstNode.UNARY_EXPR, CAstOperator.OP_NOT, v1);
                }
                break test;
              }
            }
            test = ast.makeNode(CAstNode.BINARY_EXPR, castOp, v1, v2);
          }
          return test;
        }

        private CAstNode makeIfStmt(CAstNode test, CAstNode thenBranch) {
          return ast.makeNode(CAstNode.IF_STMT, stableRemoveLeadingNegation(test), thenBranch);
        }

        /**
         * Remove redundant negation from test node.
         *
         * <ul>
         *   <li>Even or zero negation count: all negation can be removed.
         *   <li>Odd count: remove negation and flip branches.
         * </ul>
         *
         * @param test The test node for the if-stmt to be created. May contain leading negation.
         * @param thenBranch The 'true' branch of the if-stmt. May be flipped with the else branch
         *     if negation count is odd.
         * @param elseBranch The 'false' branch of the if-stmt. May be flipped with the then branch
         *     if negation count is odd.
         * @return A CAstNode of type IF_STMT equivalent to (if test thenBranch elseBranch), with
         *     leading negation removed from test and possible then/else branches swapped.
         */
        private CAstNode makeIfStmt(CAstNode test, CAstNode thenBranch, CAstNode elseBranch) {
          Pair<Integer, CAstNode> countAndTest = countAndRemoveLeadingNegation(test);
          if (countAndTest.fst % 2 == 0) {
            return ast.makeNode(CAstNode.IF_STMT, countAndTest.snd, thenBranch, elseBranch);
          } else {
            return ast.makeNode(CAstNode.IF_STMT, countAndTest.snd, elseBranch, thenBranch);
          }
        }

        /**
         * Counts leading negation and removes it from the input node. Then returns a pair with this
         * information.
         *
         * @param n The input node.
         * @return A pair with first element count, and second element n, but with all leading
         *     negation removed.
         */
        private Pair<Integer, CAstNode> countAndRemoveLeadingNegation(CAstNode n) {
          int count = 0;
          CAstNode tmp = n;
          while (isLeadingNegation(tmp)) {
            count++;
            tmp = removeSingleNegation(tmp);
          }
          return Pair.make(count, tmp);
        }

        private CAstNode removeSingleNegation(CAstNode n) {
          assert isLeadingNegation(n) : "Expected node with leading negation " + n;
          return n.getChild(1);
        }

        private boolean isLeadingNegation(CAstNode n) {
          return n.getKind() == CAstNode.UNARY_EXPR
              && n.getChildCount() > 1
              && n.getChild(0) == CAstOperator.OP_NOT;
        }

        /**
         * Remove redundant negation from test node.
         *
         * <ul>
         *   <li>Even or zero negation count: all negation can be removed.
         *   <li>Odd count: remove negation and flip branches.
         * </ul>
         *
         * @param pred The node from which negation should be removed.
         * @return The input node, with pairs of leading negation removed.
         */
        private CAstNode stableRemoveLeadingNegation(CAstNode pred) {
          Pair<Integer, CAstNode> countAndPred = countAndRemoveLeadingNegation(pred);
          if (countAndPred.fst % 2 == 0) {
            return countAndPred.snd;
          } else /* odd negation count (at least one) */ {
            return ast.makeNode(CAstNode.UNARY_EXPR, CAstOperator.OP_NOT, countAndPred.snd);
          }
        }

        @Override
        public void visitConditionalBranch(SSAConditionalBranchInstruction instruction) {
          SSACFG.BasicBlock branchBB =
              (BasicBlock) cfg.getBlockForInstruction(instruction.iIndex());

          Set<SSAInstruction> testInsts;
          startLog();
          CAstNode v1 = visit(instruction.getUse(0));
          CAstNode v2 = visit(instruction.getUse(1));
          testInsts = endLog();

          CAstNode test = getInitialTestNode(instruction, v2, v1, branchBB);

          if (loopControls.containsKey(branchBB)) {
            loopType:
            {
              Set<SSAInstruction> covered = HashSetFactory.make(testInsts);
              covered.add(instruction);
              Set<SSAInstruction> bbInsts =
                  IteratorUtil.streamify(branchBB.iterateNormalInstructions())
                      .collect(Collectors.toSet());
              if (loopControls.containsKey(branchBB)
                  && loopHeaders.contains(branchBB)
                  && covered.containsAll(bbInsts)) {
                System.err.println("while loop");
                break loopType;
              }

              for (ISSABasicBlock sb : cfg.getNormalSuccessors(branchBB)) {
                if (loopHeaders.contains(thruTrampolineBlocks(sb))) {
                  System.err.println("do loop");
                  break loopType;
                }
              }

              System.err.println("ugly loop");
            }

            LoopType loopType = null;
            // determine loop type
            loopType:
            {
              Set<SSAInstruction> covered = HashSetFactory.make(testInsts);
              covered.add(instruction);
              Set<SSAInstruction> bbInsts =
                  IteratorUtil.streamify(branchBB.iterateNormalInstructions())
                      .collect(Collectors.toSet());
              if (loopControls.containsKey(branchBB)
                  && loopHeaders.contains(branchBB)
                  && covered.containsAll(bbInsts)) {
                System.err.println("while loop");
                loopType = LoopType.WHILE;
                break loopType;
              }

              for (ISSABasicBlock sb : cfg.getNormalSuccessors(branchBB)) {
                if (loopHeaders.contains(thruTrampolineBlocks(sb))) {
                  System.err.println("do loop");
                  loopType = LoopType.DOWHILE;
                  break loopType;
                }
              }

              System.err.println("ugly loop");
              loopType = LoopType.WHILETRUE;
            }

            // Find out the successor of the loop control block
            ISSABasicBlock body = getLoopSuccessor(branchBB);

            // For the 'after' block that should be moved into loop
            ISSABasicBlock loopControlElse = null;

            ISSABasicBlock nextBB = cfg.getBlockForInstruction(instruction.iIndex() + 1);
            // If successor is not the next block
            if (!nextBB.equals(body)) {
              // reverse loop condition
              test = ast.makeNode(CAstNode.UNARY_EXPR, CAstOperator.OP_NOT, test);
            }

            // add the chucks that should be part of loop body
            // they are ignored when translating CAst
            List<CAstNode> loopBlockInLoopControl = new ArrayList<>();
            if (moveAsLoopBody.size() > 0) {
              for (List<SSAInstruction> chunk : moveAsLoopBody.keySet()) {
                RegionTreeNode lr = moveAsLoopBody.get(chunk);
                List<List<SSAInstruction>> chunkBlock = new ArrayList<>();
                chunkBlock.add(chunk);
                loopBlockInLoopControl.addAll(handleBlock(chunkBlock, lr, false));
              }
            }

            // Chunks in loop body
            List<List<SSAInstruction>> loopChunks = regionChunks.get(Pair.make(instruction, body));

            List<CAstNode> loopBlock;
            if (children.containsKey(instruction) && children.get(instruction).containsKey(body)) {
              Map<ISSABasicBlock, RegionTreeNode> cc1 = children.get(instruction);
              RegionTreeNode lr = cc1.get(body);
              loopBlock = handleBlock(loopChunks, lr, false);
            } else {
              loopBlock = Collections.emptyList();
            }

            System.err.println("loop test insts: " + testInsts);
            Optional<Stream<ISSABasicBlock>> blocks =
                IteratorUtil.streamify(cdg.getPredNodes(branchBB))
                    .filter(b -> b != branchBB)
                    .map(pb -> IteratorUtil.streamify(cdg.getSuccNodes(pb)))
                    .reduce((a, b) -> Stream.concat(a, b));
            assert blocks.isPresent();
            Optional<Stream<SSAInstruction>> insts =
                blocks
                    .get()
                    .map(eb -> IteratorUtil.streamify(eb.iterator()))
                    .reduce((a, b) -> Stream.concat(a, b));
            assert insts.isPresent();
            List<SSAInstruction> depInsts = insts.get().collect(Collectors.toList());
            System.err.println("dep insts: " + depInsts);

            List<SSAInstruction> header =
                depInsts.stream()
                    .filter(
                        i ->
                            cdg.hasEdge(branchBB, cfg.getBlockForInstruction(i.iIndex()))
                                && i != instruction
                                && !testInsts.contains(i))
                    .collect(Collectors.toList());

            System.err.println("loop header insts: " + header);

            // Remove this piece of code for now because it's generating useless code for some cases
            // e.g. var_22 = System.out.
            // The code was trying to handle the case of instructions in control header but not part
            // of
            // conditional branch, but it can not work well, so that another solution is implemented
            // instead
            //            if (!header.isEmpty()) {
            //              List<CAstNode> hb = handleBlock(Collections.singletonList(header), lr,
            // true);
            //              System.err.println(decls);
            //              System.err.println(parentDecls);
            //              System.err.println(hb);
            //              hb.addAll(loopBlock);
            //              loopBlock = hb;
            //            }

            // Generate loop body per loop type
            CAstNode bodyNodes = null;
            if (LoopType.DOWHILE.equals(loopType)) {
              // if it's do while loop, use loopBlock and loopBlockInLoopControl
              loopBlockInLoopControl.addAll(loopBlock);
              bodyNodes =
                  ast.makeNode(
                      CAstNode.BLOCK_STMT,
                      loopBlockInLoopControl.toArray(new CAstNode[loopBlockInLoopControl.size()]));
            } else if (LoopType.WHILETRUE.equals(loopType)) {
              // if it's ugly loop, put test as if-statement into body

              // retrieve else statement
              Set<ISSABasicBlock> elseBlock = HashSetFactory.make(loopExits);
              elseBlock.retainAll(children.get(instruction).keySet());
              List<CAstNode> elseNodes = null;
              if (elseBlock.size() > 0) {
                loopControlElse = elseBlock.iterator().next();
              }
              if (loopControlElse != null) {
                RegionTreeNode rt = children.get(instruction).get(loopControlElse);
                List<List<SSAInstruction>> elseChunks =
                    regionChunks.get(Pair.make(instruction, loopControlElse));
                elseNodes = handleBlock(elseChunks, rt, false);
                elseNodes.add(ast.makeNode(CAstNode.BREAK));
              }

              CAstNode ifStmt =
                  makeIfStmt(
                      ast.makeNode(CAstNode.UNARY_EXPR, CAstOperator.OP_NOT, test),
                      // include the nodes in the else branch
                      elseNodes == null
                          ? ast.makeNode(CAstNode.BREAK)
                          : (elseNodes.size() == 1
                              ? elseNodes.get(0)
                              : ast.makeNode(
                                  CAstNode.BLOCK_STMT,
                                  elseNodes.toArray(new CAstNode[elseNodes.size()]))),
                      // it should be a block instead of array of AST nodes
                      ast.makeNode(
                          CAstNode.BLOCK_STMT, loopBlock.toArray(new CAstNode[loopBlock.size()])));
              if (loopBlockInLoopControl.size() == 0) {
                bodyNodes = ast.makeNode(CAstNode.BLOCK_STMT, ifStmt);
              } else {
                loopBlockInLoopControl.add(ifStmt);
                bodyNodes =
                    ast.makeNode(
                        CAstNode.BLOCK_STMT,
                        loopBlockInLoopControl.toArray(
                            new CAstNode[loopBlockInLoopControl.size()]));
              }

              test = ast.makeConstant(true);
            } else {
              // for normal while loop, use loopBlock
              bodyNodes =
                  ast.makeNode(
                      CAstNode.BLOCK_STMT, loopBlock.toArray(new CAstNode[loopBlock.size()]));
            }

            node =
                ast.makeNode(
                    CAstNode.LOOP,
                    stableRemoveLeadingNegation(test),
                    bodyNodes,
                    // reuse LOOP type but add third child as a boolean to tell if it's a do while
                    // loop
                    ast.makeConstant(LoopType.DOWHILE.equals(loopType)));

            ISSABasicBlock next = cfg.getBlockForInstruction(instruction.getTarget());
            node = checkLinePhi(node, instruction, next);

            if (children.containsKey(instruction)) {
              HashMap<ISSABasicBlock, RegionTreeNode> copy =
                  HashMapFactory.make(children.get(instruction));
              assert copy.remove(body) != null;
              if (copy.keySet().iterator().hasNext()) {
                ISSABasicBlock after = copy.keySet().iterator().next();
                assert after != null;

                // skip the case when 'after' block is moved into loop body
                if (!after.equals(loopControlElse)) {
                  List<List<SSAInstruction>> afterChunks =
                      regionChunks.get(Pair.make(instruction, after));
                  RegionTreeNode ar = children.get(instruction).get(after);
                  List<CAstNode> afterBlock = handleBlock(afterChunks, ar, false);

                  node =
                      ast.makeNode(
                          CAstNode.BLOCK_STMT,
                          node,
                          afterBlock.toArray(new CAstNode[afterBlock.size()]));
                }
              }
            }
          } else {
            assert children.containsKey(instruction)
                : "children of " + instruction + ":" + children;
            Map<ISSABasicBlock, RegionTreeNode> cc = children.get(instruction);
            List<CAstNode> takenBlock = null;

            ISSABasicBlock notTaken;
            ISSABasicBlock taken = cfg.getBlockForInstruction(instruction.getTarget());
            if (cc.containsKey(taken)) {
              Map<ISSABasicBlock, RegionTreeNode> copy = HashMapFactory.make(cc);
              assert copy.remove(taken) != null;
              notTaken = copy.keySet().iterator().next();
              List<List<SSAInstruction>> takenChunks =
                  regionChunks.get(Pair.make(instruction, taken));
              RegionTreeNode tr = cc.get(taken);
              takenBlock = handleBlock(takenChunks, tr, false);

            } else {
              assert cc.size() == 1;
              notTaken = cc.keySet().iterator().next();
            }
            assert notTaken != null;

            Pair<SSAConditionalBranchInstruction, ISSABasicBlock> notTakenKey =
                Pair.make(instruction, notTaken);
            List<List<SSAInstruction>> notTakenChunks = regionChunks.get(notTakenKey);
            RegionTreeNode fr = cc.get(notTaken);
            List<CAstNode> notTakenBlock = handleBlock(notTakenChunks, fr, false);

            // For the case where there's a need to jump out of the loop, break should be added
            // if notTakenBlock is selected (or have goto at the last?), add break
            // if takenBlock is null, add break
            // if takenBlock is not null, add break (or have goto at last?)
            if (loopBreakers.contains(branchBB)
                && !loopControls.containsKey(branchBB)
                && !loopHeaders.contains(branchBB)) {
              if (loopExits.contains(notTaken)) {
                notTakenBlock.add(ast.makeNode(CAstNode.BREAK));
              } else {
                if (takenBlock == null)
                  takenBlock = Collections.singletonList(ast.makeNode(CAstNode.BREAK));
                else takenBlock.add(ast.makeNode(CAstNode.BREAK));
              }
            }

            CAstNode notTakenStmt =
                notTakenBlock.size() == 1
                    ? notTakenBlock.iterator().next()
                    : ast.makeNode(
                        CAstNode.BLOCK_STMT,
                        notTakenBlock.toArray(new CAstNode[notTakenBlock.size()]));

            notTakenStmt = checkLinePhi(notTakenStmt, instruction, notTaken);

            CAstNode takenStmt = null;
            if (takenBlock != null) {
              takenStmt =
                  takenBlock.size() == 1
                      ? takenBlock.iterator().next()
                      : ast.makeNode(
                          CAstNode.BLOCK_STMT, takenBlock.toArray(new CAstNode[takenBlock.size()]));
            }

            takenStmt = checkLinePhi(takenStmt, instruction, taken);

            if (moveAfterWithLabel.containsKey(branchBB)) {
              assert taken != null;
              assert notTaken != null;

              List<SSAInstruction> afterInsts = new ArrayList<>();
              moveAfterWithLabel
                  .get(branchBB)
                  .forEach(
                      bb -> {
                        bb.forEach(
                            inst -> {
                              afterInsts.add(inst);
                            });
                      });

              List<CAstNode> afterCode = new ArrayList<>();
              Heap<List<SSAInstruction>> afterChunks =
                  computeChunks(ir, cha, IntSetUtil.make(), afterInsts);
              processChunks(
                  afterInsts,
                  afterChunks,
                  c -> {
                    Pair<CAstNode, List<CAstNode>> stuff =
                        makeToCAst(c).processChunk(decls, packages, false);

                    afterCode.add(stuff.fst);
                  });
              System.err.println(afterCode);

              CAstNode block;
              CAstNode[] afterCAst = afterCode.toArray(new CAstNode[afterCode.size()]);
              if (moveAfterWithLabel.get(branchBB).contains(notTaken)) {
                block =
                    ast.makeNode(
                        CAstNode.BLOCK_STMT, makeIfStmt(test, takenStmt), notTakenStmt, afterCAst);
              } else {
                block =
                    ast.makeNode(
                        CAstNode.BLOCK_STMT,
                        makeIfStmt(
                            ast.makeNode(CAstNode.UNARY_EXPR, CAstOperator.OP_NOT, test),
                            notTakenStmt),
                        takenStmt,
                        afterCAst);
              }

              node =
                  ast.makeNode(
                      CAstNode.LABEL_STMT, ast.makeConstant("lbl_" + branchBB.getNumber()), block);

            } else {

              if (takenStmt != null) {
                node = makeIfStmt(test, takenStmt, notTakenStmt);
              } else {
                node =
                    makeIfStmt(
                        ast.makeNode(CAstNode.UNARY_EXPR, CAstOperator.OP_NOT, test), notTakenStmt);
              }
            }
          }
        }

        private ISSABasicBlock thruTrampolineBlocks(ISSABasicBlock bb) {
          Iterator<SSAInstruction> insts = bb.iterator();
          if (insts.hasNext()) {
            SSAInstruction inst = insts.next();
            if (!insts.hasNext() && inst instanceof SSAGotoInstruction) {
              ISSABasicBlock nb =
                  cfg.getBlockForInstruction(((SSAGotoInstruction) inst).getTarget());
              ISSABasicBlock nnb = thruTrampolineBlocks(nb);
              if (nnb != nb) {
                return nnb;
              } else {
                return nb;
              }
            }
          }

          return bb;
        }

        private List<CAstNode> handleBlock(
            List<List<SSAInstruction>> loopChunks, RegionTreeNode lr, boolean extraHeaderCode) {

          List<Pair<CAstNode, List<CAstNode>>> normalStuff =
              handleInsts(
                  loopChunks,
                  lr,
                  x -> !(x.iterator().next() instanceof SSAGotoInstruction),
                  extraHeaderCode);

          List<Pair<CAstNode, List<CAstNode>>> gotoStuff =
              handleInsts(
                  loopChunks,
                  lr,
                  x -> x.iterator().next() instanceof SSAGotoInstruction,
                  extraHeaderCode);

          List<CAstNode> block = new ArrayList<>();
          normalStuff.forEach(p -> block.addAll(p.snd));
          gotoStuff.forEach(p -> block.addAll(p.snd));
          normalStuff.forEach(p -> block.add(p.fst));
          gotoStuff.forEach(p -> block.add(p.fst));
          System.err.println("final block: " + block);
          return block;
        }

        private List<Pair<CAstNode, List<CAstNode>>> handleInsts(
            List<List<SSAInstruction>> loopChunks,
            RegionTreeNode lr,
            Predicate<? super List<SSAInstruction>> assignFilter,
            boolean extraHeaderCode) {
          if (loopChunks == null || loopChunks.isEmpty()) {
            return Collections.emptyList();
          } else {
            return IteratorUtil.streamify(loopChunks)
                .filter(assignFilter)
                .map(c -> lr.makeToCAst(c).processChunk(parentDecls, packages, extraHeaderCode))
                .collect(Collectors.toList());
          }
        }

        @Override
        public void visitSwitch(SSASwitchInstruction instruction) {
          assert children.containsKey(instruction) : "children of " + instruction + ":" + children;

          CAstNode value = visit(instruction.getUse(0));
          List<CAstNode> switchCode = new ArrayList<>();
          Map<ISSABasicBlock, RegionTreeNode> cc = children.get(instruction);
          int[] casesAndLabels = instruction.getCasesAndLabels();
          for (int i = 0; i < casesAndLabels.length - 1; i++) {
            CAstNode caseValue = ast.makeConstant(casesAndLabels[i]);
            i++;
            ISSABasicBlock caseBlock = cfg.getBlockForInstruction(casesAndLabels[i]);
            List<List<SSAInstruction>> labelChunks =
                regionChunks.get(Pair.make(instruction, caseBlock));
            RegionTreeNode fr = cc.get(caseBlock);
            List<CAstNode> labelBlock = handleBlock(labelChunks, fr, false);
            switchCode.add(
                ast.makeNode(
                    CAstNode.LABEL_STMT,
                    caseValue,
                    ast.makeNode(
                        CAstNode.BLOCK_STMT, labelBlock.toArray(new CAstNode[labelBlock.size()]))));
          }

          assert instruction.getDefault() != -1;
          ISSABasicBlock defaultBlock = cfg.getBlockForInstruction(instruction.getDefault());
          List<List<SSAInstruction>> defaultChunks =
              regionChunks.get(Pair.make(instruction, defaultBlock));
          RegionTreeNode fr = cc.get(defaultBlock);
          List<CAstNode> defaultStuff = handleBlock(defaultChunks, fr, false);

          node =
              ast.makeNode(
                  CAstNode.SWITCH,
                  value,
                  ast.makeNode(
                      CAstNode.BLOCK_STMT, defaultStuff.toArray(new CAstNode[defaultStuff.size()])),
                  switchCode.toArray(new CAstNode[switchCode.size()]));
        }

        @Override
        public void visitReturn(SSAReturnInstruction instruction) {
          if (!instruction.returnsVoid()) {
            CAstNode arg = visit(instruction.getUse(0));
            node = ast.makeNode(CAstNode.RETURN, arg);
          }
        }

        @Override
        public void visitGet(SSAGetInstruction instruction) {
          recordPackage(instruction.getDeclaredFieldType());
          node =
              ast.makeNode(
                  CAstNode.OBJECT_REF,
                  instruction.isStatic()
                      ? ast.makeConstant(
                          instruction
                              .getDeclaredField()
                              .getDeclaringClass()
                              .getName()
                              .getClassName())
                      : visit(instruction.getRef()),
                  ast.makeConstant(instruction.getDeclaredField()));
        }

        @Override
        public void visitPut(SSAPutInstruction instruction) {
          if (instruction.isStatic()) {
            node =
                ast.makeNode(
                    CAstNode.EXPR_STMT,
                    ast.makeNode(
                        CAstNode.ASSIGN,
                        ast.makeNode(
                            CAstNode.OBJECT_REF,
                            ast.makeConstant(
                                instruction
                                    .getDeclaredField()
                                    .getDeclaringClass()
                                    .getName()
                                    .getClassName()),
                            ast.makeConstant(instruction.getDeclaredField())),
                        visit(instruction.getVal())));
          } else {
            node =
                ast.makeNode(
                    CAstNode.EXPR_STMT,
                    ast.makeNode(
                        CAstNode.ASSIGN,
                        ast.makeNode(
                            CAstNode.OBJECT_REF,
                            visit(instruction.getRef()),
                            ast.makeConstant(instruction.getDeclaredField())),
                        visit(instruction.getVal())));
          }
        }

        protected void visitAbstractInvoke(SSAAbstractInvokeInstruction inst) {
          CAstNode[] args = new CAstNode[inst.getNumberOfUses() + 2];
          for (int i = 0; i < inst.getNumberOfUses(); i++) {
            args[i + 2] = visit(inst.getUse(i));
            if (args[i + 2].getKind() == CAstNode.THIS
                && inst.getUse(i) == 1
                && !types
                    .getType(inst.getUse(i))
                    .getTypeReference()
                    .equals(inst.getDeclaredTarget().getDeclaringClass())) {
              args[i + 2] = ast.makeNode(CAstNode.SUPER);
            }
          }

          args[0] = ast.makeConstant(inst.getDeclaredTarget());

          if (inst.getCallSite().isStatic() || inst.getDeclaredTarget().isInit()) {
            recordPackage(inst.getDeclaredTarget().getDeclaringClass());
            System.err.println("looking at type " + inst.getDeclaredTarget().getDeclaringClass());
          }

          args[1] = ast.makeConstant(inst.getCallSite().isStatic());

          if (Void.equals(inst.getDeclaredResultType())) {
            node = ast.makeNode(CAstNode.EXPR_STMT, ast.makeNode(CAstNode.CALL, args));
          } else {
            recordPackage(inst.getDeclaredResultType());
            node = ast.makeNode(CAstNode.CALL, args);
          }
        }

        @Override
        public void visitInvoke(SSAInvokeInstruction instruction) {
          visitAbstractInvoke(instruction);
        }

        @Override
        public void visitNew(SSANewInstruction instruction) {
          TypeReference newType = instruction.getConcreteType();
          if (newType.isArrayType()) {
            CAstNode dims[] = new CAstNode[instruction.getNumberOfUses()];

            for (int i = 0; i < instruction.getNumberOfUses(); i++) {
              dims[i] = visit(instruction.getUse(i));
            }

            recordPackage(newType);

            node = ast.makeNode(CAstNode.NEW, ast.makeConstant(newType), dims);
          }
        }

        private void recordPackage(TypeReference newType) {
          while (newType.isArrayType()) {
            newType = newType.getArrayElementType();
          }
          String pkg = toPackage(newType);
          if (pkg != null) {
            if (!packages.containsKey(pkg)) {
              packages.put(pkg, HashSetFactory.make());
            }
            packages.get(pkg).add(toSource(newType).getName());
          }
        }

        @Override
        public void visitArrayLength(SSAArrayLengthInstruction instruction) {
          node =
              ast.makeNode(
                  CAstNode.OBJECT_REF,
                  visit(instruction.getArrayRef()),
                  ast.makeConstant("length"));
        }

        @Override
        public void visitThrow(SSAThrowInstruction instruction) {
          node = ast.makeNode(CAstNode.THROW, visit(instruction.getUse(0)));
        }

        @Override
        public void visitMonitor(SSAMonitorInstruction instruction) {
          // TODO: implement this
          //      assert false;
        }

        @Override
        public void visitCheckCast(SSACheckCastInstruction instruction) {
          node =
              ast.makeNode(
                  CAstNode.CAST,
                  ast.makeConstant(toSource(instruction.getDeclaredResultTypes()[0])),
                  visit(instruction.getVal()));
        }

        @Override
        public void visitInstanceof(SSAInstanceofInstruction instruction) {
          node =
              ast.makeNode(
                  CAstNode.INSTANCEOF,
                  ast.makeConstant(instruction.getCheckedType()),
                  visit(instruction.getRef()));
        }

        @Override
        public void visitPhi(SSAPhiInstruction instruction) {
          // assert false;
        }

        @Override
        public void visitPi(SSAPiInstruction instruction) {
          assert false;
        }

        @Override
        public void visitGetCaughtException(SSAGetCaughtExceptionInstruction instruction) {
          assert false;
        }

        @Override
        public void visitLoadMetadata(SSALoadMetadataInstruction instruction) {
          node = ast.makeConstant(instruction.getToken());
        }

        @Override
        public void visitAssign(AssignInstruction inst) {
          node = visit(inst.getUse(0));
        }

        @Override
        public <T> void visitUnspecified(SSAUnspecifiedInstruction<T> instruction) {
          node = ast.makeNode(CAstNode.PRIMITIVE, ast.makeConstant(instruction.getPayload()));
        }

        @Override
        public <T> void visitUnspecifiedExpr(SSAUnspecifiedExprInstruction<T> instruction) {
          node = ast.makeNode(CAstNode.PRIMITIVE, ast.makeConstant(instruction.getPayload()));
        }
      }

      protected Visitor makeVisitor(
          SSAInstruction root,
          CodeGenerationContext c,
          List<SSAInstruction> chunk2,
          List<CAstNode> parentDecls,
          Map<String, Set<String>> packages,
          boolean extraHeaderCode) {
        return new Visitor(root, c, chunk2, parentDecls, packages, children, extraHeaderCode);
      }

      public ToCAst(List<SSAInstruction> insts, CodeGenerationContext c) {
        this.chunk = insts;
        this.c = c;
      }

      Pair<CAstNode, List<CAstNode>> processChunk(
          List<CAstNode> parentDecls, Map<String, Set<String>> packages, boolean extraHeaderCode) {
        SSAInstruction root = chunk.iterator().next();
        Visitor x = makeVisitor(root, c, chunk, parentDecls, packages, extraHeaderCode);
        return Pair.make(x.node, x.decls);
      }
    }
  }

  protected class ToJavaVisitor extends CAstVisitor<CodeGenerationContext> {
    private final IR ir;
    private final int indent;
    private final PrintWriter out;
    protected final Map<String, Object> varTypes;

    protected ToJavaVisitor(int indent, IR ir, PrintWriter out, Map<String, Object> varTypes2) {
      this.ir = ir;
      this.out = out;
      this.indent = indent;
      this.varTypes = varTypes2;
    }

    private void indent() {
      indent(0);
    }

    private void indent(int offset) {
      for (int i = 0; i < indent - offset; i++) {
        out.print("  ");
      }
    }

    @Override
    protected boolean visitInclude(
        CAstNode n, CodeGenerationContext c, CAstVisitor<CodeGenerationContext> visitor) {
      out.println(
          "import "
              + ((String) n.getChild(0).getValue()).replace('/', '.')
              + "."
              + n.getChild(1).getValue()
              + ";");
      return true;
    }

    @Override
    protected boolean visitSwitch(
        CAstNode n, CodeGenerationContext c, CAstVisitor<CodeGenerationContext> visitor) {
      indent();
      out.print("switch (");
      visit(n.getChild(0), c, visitor);
      out.println(") {");

      ToJavaVisitor cv = makeToJavaVisitor(ir, out, indent + 1, varTypes);
      for (int i = 2; i < n.getChildCount(); i++) {
        CAstNode caseCAst = n.getChild(i);
        cv.indent();
        out.print("case ");
        cv.visit(caseCAst.getChild(0), c, cv);
        out.println(":");
        cv.visit(caseCAst.getChild(1), c, cv);
        cv.indent();
        out.println("break;");
      }

      cv.indent();
      out.println("default:");
      cv.visit(n.getChild(1), c, cv);

      indent();
      out.println("}");

      return true;
    }

    @Override
    protected boolean visitDeclStmt(
        CAstNode n, CodeGenerationContext c, CAstVisitor<CodeGenerationContext> visitor) {
      indent();
      out.print(nameToJava(((CAstType) n.getChild(1).getValue()).getName(), true) + " ");
      visit(n.getChild(0), c, visitor);
      if (n.getChildCount() > 2 && n.getChild(2).getKind() != CAstNode.EMPTY) {
        out.print(" = ");
        visit(n.getChild(2), c, visitor);
      }
      out.println(";");
      return true;
    }

    @Override
    protected boolean visitBlockStmt(
        CAstNode n, CodeGenerationContext c, CAstVisitor<CodeGenerationContext> visitor) {
      CodeGenerationContext cc = c.nonTopLevel();
      try (ByteArrayOutputStream b = new ByteArrayOutputStream()) {
        try (PrintWriter bw = new PrintWriter(b)) {

          int len = b.toString().trim().length();
          int count = 0;
          ToJavaVisitor cv = makeToJavaVisitor(ir, bw, indent, varTypes);
          for (CAstNode child : n.getChildren()) {
            if (child.getKind() != CAstNode.EMPTY) {
              cv.visit(child, cc, cv);
              bw.flush();
              int ln = b.toString().trim().length();
              if (ln > len) count++;
              len = ln;
            }
          }

          String javaText = b.toString();

          String trimmedCode = javaText.trim();
          if (trimmedCode.length() > 0) {
            if (count > 1 || c.isTopLevel()) {
              indent(1);
              out.println("{");
            }
            if (count <= 1 && !c.isTopLevel()) {
              indent(trimmedCode.startsWith("{") ? 1 : 0);
              out.println(trimmedCode);
            } else {
              out.print(javaText);
            }
            if (count > 1 || c.isTopLevel()) {
              indent(1);
              out.println("}");
            }
          }
        }
      } catch (IOException e) {
        assert false : e;
      }
      return true;
    }

    @Override
    protected boolean visitConstant(
        CAstNode n, CodeGenerationContext c, CAstVisitor<CodeGenerationContext> visitor) {
      Object v = n.getValue();
      if (v instanceof FieldReference) {
        out.print(nameToJava(((FieldReference) v).getName().toString(), false));
      } else if (v instanceof Character) {
        out.print("'" + v + "'");
      } else if (v instanceof String) {
        String s = (String) v;
        if (s.startsWith("\"") && s.endsWith("\"")) {
          out.print(s);
        } else {
          out.print('"' + s + '"');
        }
      } else {
        out.print(v);
      }
      return true;
    }

    @Override
    protected boolean visitVar(
        CAstNode n, CodeGenerationContext c, CAstVisitor<CodeGenerationContext> visitor) {
      String nm = n.getChild(0).getValue().toString();
      out.print(nameToJava(nm, false));
      return true;
    }

    @Override
    protected boolean visitThis(
        CAstNode n, CodeGenerationContext c, CAstVisitor<CodeGenerationContext> visitor) {
      out.print("this");
      return true;
    }

    @Override
    protected boolean visitSuper(
        CAstNode n, CodeGenerationContext c, CAstVisitor<CodeGenerationContext> visitor) {
      out.print("super");
      return true;
    }

    @Override
    public boolean visitAssign(
        CAstNode n, CodeGenerationContext c, CAstVisitor<CodeGenerationContext> visitor) {
      visit(n.getChild(0), c, visitor);
      out.print(" = ");
      visit(n.getChild(1), c, visitor);
      return true;
    }

    @Override
    protected boolean visitCall(
        CAstNode n, CodeGenerationContext c, CAstVisitor<CodeGenerationContext> visitor) {
      MethodReference target = (MethodReference) n.getChild(0).getValue();
      boolean isStatic = ((Boolean) n.getChild(1).getValue()).booleanValue();
      if ("<init>".equals(target.getName().toString())) {
        assert !isStatic;
        Atom type = target.getDeclaringClass().getName().getClassName();
        visit(n.getChild(2), c, this);
        if (n.getChild(2).getKind() != CAstNode.THIS && n.getChild(2).getKind() != CAstNode.SUPER) {
          out.print(" = new " + nameToJava(type.toString(), true));
        }
        out.print("(");
        for (int i = 3; i < n.getChildCount(); i++) {
          if (i != 3) {
            out.print(", ");
          }
          visit(n.getChild(i), c, visitor);
        }
      } else if (isStatic) {
        Atom type = target.getDeclaringClass().getName().getClassName();
        Atom name = target.getName();
        out.print(
            nameToJava(type.toString(), true) + "." + nameToJava(name.toString(), false) + "(");
        for (int i = 2; i < n.getChildCount(); i++) {
          if (i != 2) {
            out.print(", ");
          }
          visit(n.getChild(i), c, visitor);
        }
      } else {
        if (n.getChild(2).getKind() != CAstNode.THIS) {
          visit(n.getChild(2), c, this);
          out.print(".");
        }
        out.print(target.getName() + "(");
        for (int i = 3; i < n.getChildCount(); i++) {
          if (i != 3) {
            out.print(", ");
          }
          visit(n.getChild(i), c, visitor);
        }
      }
      out.print(")");
      return true;
    }

    @Override
    protected boolean visitBlockExpr(
        CAstNode n, CodeGenerationContext c, CAstVisitor<CodeGenerationContext> visitor) {
      assert n.getChildCount() == 1;
      out.print("(");
      visit(n.getChild(0), c, visitor);
      out.print(")");
      return true;
    }

    @Override
    protected boolean visitPrimitive(
        CAstNode n, CodeGenerationContext c, CAstVisitor<CodeGenerationContext> visitor) {
      indent();
      out.print("System.err.println(\"");
      out.print(n.getChild(0).getValue());
      out.println("\");");
      return true;
    }

    @Override
    protected boolean visitExprStmt(
        CAstNode n, CodeGenerationContext c, CAstVisitor<CodeGenerationContext> visitor) {
      indent();
      visit(n.getChild(0), c, visitor);
      out.println(";");
      return true;
    }

    @Override
    protected boolean visitLabelStmt(
        CAstNode n, CodeGenerationContext c, CAstVisitor<CodeGenerationContext> visitor) {
      indent();
      out.println(n.getChild(0).getValue() + ":");
      ToJavaVisitor lv = makeToJavaVisitor(ir, out, indent + 1, varTypes);
      visit(n.getChild(1), c, lv);
      return true;
    }

    @Override
    protected boolean visitLoop(
        CAstNode n, CodeGenerationContext c, CAstVisitor<CodeGenerationContext> visitor) {
      ToJavaVisitor cv = makeToJavaVisitor(ir, out, indent + 1, varTypes);
      indent();

      // If it's do while loop, then genarate do{}while();
      // otherwise keep what's be done already, that's while(){};
      if (n.getChildCount() > 2 && n.getChild(2).getValue().equals(java.lang.Boolean.TRUE)) {
        out.println("do ");
        cv.visit(n.getChild(1), c, cv);
        indent();
        out.print("while (");
        cv.visit(n.getChild(0), c, visitor);
        out.println(");");
        return true;
      } else {
        out.print("while (");
        cv.visit(n.getChild(0), c, visitor);
        out.println(")");
        cv.visit(n.getChild(1), c, cv);
        return true;
      }
    }

    @Override
    protected boolean visitIfStmt(
        CAstNode n, CodeGenerationContext c, CAstVisitor<CodeGenerationContext> visitor) {

      String thenJavaText = "";
      String elseJavaText = "";

      try (ByteArrayOutputStream b = new ByteArrayOutputStream()) {
        try (PrintWriter bw = new PrintWriter(b)) {

          ToJavaVisitor cthen = makeToJavaVisitor(ir, bw, indent + 1, varTypes);
          cthen.visit(n.getChild(1), c, cthen);

          bw.flush();
          thenJavaText = b.toString();

          if (n.getChildCount() > 2) {
            try (ByteArrayOutputStream eb = new ByteArrayOutputStream()) {
              try (PrintWriter ebw = new PrintWriter(eb)) {

                ToJavaVisitor celse = makeToJavaVisitor(ir, ebw, indent + 1, varTypes);
                celse.visit(n.getChild(2), c, celse);

                ebw.flush();
                elseJavaText = eb.toString();
              }
            }
          }
        }
      } catch (IOException e) {
        assert false : e;
      }

      boolean reverse = CAstPattern.match(isNot, n.getChild(0)) != null;

      indent();
      if (thenJavaText.length() > 0) {
        if (elseJavaText.length() > 0) {
          ToJavaVisitor cif = makeToJavaVisitor(ir, out, indent + 1, varTypes);
          out.print("if (");
          cif.visit(reverse ? n.getChild(0).getChild(1) : n.getChild(0), c, cif);
          out.println(")");
          out.print(reverse ? elseJavaText : thenJavaText);
          indent();
          out.println("else");
          out.print(reverse ? thenJavaText : elseJavaText);

        } else {
          ToJavaVisitor cif = makeToJavaVisitor(ir, out, indent + 1, varTypes);
          out.print("if (");
          cif.visit(n.getChild(0), c, cif);
          out.println(")");
          out.print(thenJavaText);
        }
      } else {
        if (elseJavaText.length() > 0) {
          ToJavaVisitor cif = makeToJavaVisitor(ir, out, indent + 1, varTypes);
          out.print("if (");
          cif.visit(
              reverse
                  ? n.getChild(0).getChild(1)
                  : ast.makeNode(CAstNode.UNARY_EXPR, CAstOperator.OP_NOT, n.getChild(0)),
              c,
              cif);
          out.println(")");
          out.print(elseJavaText);

        } else if (!isPrimitiveExpr(n.getChild(0))) {
          ToJavaVisitor cif = makeToJavaVisitor(ir, out, indent + 1, varTypes);
          cif.visit(n.getChild(0), c, cif);
          out.println(";");
        }
      }

      return true;
    }

    private boolean isPrimitiveExpr(CAstNode child) {
      switch (child.getKind()) {
        case CAstNode.BINARY_EXPR:
        case CAstNode.UNARY_EXPR:
        case CAstNode.CONSTANT:
          {
            return true;
          }
        default:
          {
            return false;
          }
      }
    }

    @Override
    public boolean visitNode(
        CAstNode n, CodeGenerationContext c, CAstVisitor<CodeGenerationContext> visitor) {
      return true;
    }

    @Override
    protected boolean doVisit(
        CAstNode n, CodeGenerationContext context, CAstVisitor<CodeGenerationContext> visitor) {
      switch (n.getKind()) {
        case CAstNode.BREAK:
          {
            indent();
            String lbl = n.getChildCount() > 0 ? " " + n.getChild(0).getValue() : "";
            out.println("break" + lbl + ";");
            return true;
          }
        case CAstNode.CONTINUE:
          {
            indent();
            out.println("continue;");
            return true;
          }
        default:
          break;
      }
      return true;
    }

    @Override
    protected boolean visitReturn(
        CAstNode n, CodeGenerationContext c, CAstVisitor<CodeGenerationContext> visitor) {
      indent();
      out.print("return");
      if (n.getChildCount() > 0) {
        out.print(" ");
        visit(n.getChild(0), c, this);
      }
      out.println(";");
      return true;
    }

    protected int precedence(String operator) {
      switch (operator) {
        case "**":
          return 0;
        case "!":
        case "~":
          return 1;
        case "%":
        case "*":
        case "/":
          return 3;
        case "+":
        case "-":
          return 5;
        case "<<":
        case ">>":
        case ">>>":
          return 7;
        case "==":
        case "<":
        case ">":
        case "<=":
        case ">=":
        case "!=":
          return 9;
        case "&":
          return 11;
        case "^":
          return 13;
        case "|":
          return 15;
        case "&&":
          return 17;
        case "||":
          return 19;
        default:
          assert false : "unknown " + operator;
          return -1;
      }
    }

    @Override
    protected boolean visitBinaryExpr(
        CAstNode n, CodeGenerationContext c, CAstVisitor<CodeGenerationContext> visitor) {
      int myPrecedence = precedence(n.getChild(0).getValue().toString());
      CodeGenerationContext cc = new CodeGenerationContext(c, myPrecedence);
      boolean parens = c.parentPrecedence < myPrecedence;
      if (parens) out.print("(");
      visit(n.getChild(1), cc, this);
      out.print(" " + n.getChild(0).getValue() + " ");
      visit(n.getChild(2), cc, this);
      if (parens) out.print(")");
      return true;
    }

    @Override
    protected boolean visitUnaryExpr(
        CAstNode n, CodeGenerationContext c, CAstVisitor<CodeGenerationContext> visitor) {
      int myPrecedence = precedence(n.getChild(0).getValue().toString());
      CodeGenerationContext cc = new CodeGenerationContext(c, myPrecedence);
      boolean parens = c.parentPrecedence < myPrecedence;
      if (parens) out.print("(");
      out.print(n.getChild(0).getValue() + " ");
      visit(n.getChild(1), cc, this);
      if (parens) out.print(")");
      return true;
    }

    @Override
    protected boolean visitCast(
        CAstNode n, CodeGenerationContext c, CAstVisitor<CodeGenerationContext> visitor) {
      out.print("(" + nameToJava(((CAstType) n.getChild(0).getValue()).getName(), true) + ") ");
      visit(n.getChild(1), c, visitor);
      return true;
    }

    @Override
    protected boolean visitArrayRef(
        CAstNode n, CodeGenerationContext c, CAstVisitor<CodeGenerationContext> visitor) {
      visit(n.getChild(0), c, visitor);
      out.print("[");
      visit(n.getChild(2), c, visitor);
      out.print("]");
      return true;
    }

    @Override
    protected boolean visitObjectRef(
        CAstNode n, CodeGenerationContext c, CAstVisitor<CodeGenerationContext> visitor) {
      if (n.getChild(0).getKind() != CAstNode.THIS) {
        visit(n.getChild(0), c, visitor);
        out.print(".");
      }
      if (n.getChild(1).getValue() instanceof String) {
        out.print(nameToJava(n.getChild(1).getValue().toString(), false));
      } else {
        visit(n.getChild(1), c, visitor);
      }
      return true;
    }

    @Override
    protected boolean visitNew(
        CAstNode n, CodeGenerationContext c, CAstVisitor<CodeGenerationContext> visitor) {
      TypeReference type = (TypeReference) n.getChild(0).getValue();
      if (type.isArrayType()) {
        TypeReference eltType = type.getInnermostElementType();
        out.print("new " + nameToJava(toSource(eltType).toString(), true));
        for (int i = 1; i < n.getChildCount(); i++) {
          out.print("[");
          visit(n.getChild(i), c, visitor);
          out.print("]");
        }
        return true;
      } else {
        return super.visitNew(n, c, visitor);
      }
    }
  }

  public abstract Set<File> toJava(
      IClassHierarchy cha,
      File outDir,
      Predicate<IClass> filter,
      Map<MethodReference, String> codeRecorder);

  @SuppressWarnings("resource")
  public void toJava(
      IR ir,
      IClassHierarchy cha,
      TypeInference types,
      PrintWriter out,
      Consumer<CAstNode> includes,
      int level,
      Map<MethodReference, String> codeRecorder) {
    PrunedCFG<SSAInstruction, ISSABasicBlock> cfg =
        ExceptionPrunedCFG.makeDefiniteUncaught(ir.getControlFlowGraph());
    System.err.println("IR:\n" + ir);

    Dominators<ISSABasicBlock> FD = Dominators.make(cfg, ir.getControlFlowGraph().entry());
    System.err.println("forward dominators: " + FD.dominatorTree());
    Dominators<ISSABasicBlock> RD =
        Dominators.make(GraphInverter.invert(cfg), ir.getControlFlowGraph().exit());
    System.err.println("reverse dominators: " + RD.dominatorTree());

    RegionTreeNode root = makeTreeNode(ir, cha, types, cfg);

    System.err.println("tree");
    System.err.println(root);
    CAstNode ast = root.toCAst();
    System.err.println(ast);

    Map<String, Object> varTypes = HashMapFactory.make();
    CAstPattern decls = CAstPattern.parse("DECL_STMT(VAR(<name>*),<type>*,**)");
    CAstPattern.findAll(decls, ast)
        .forEach(
            s -> {
              varTypes.put(
                  (String) ((CAstNode) s.get("name")).getValue(),
                  ((CAstNode) s.get("type")).getValue());
            });

    MutableIntSet done = IntSetUtil.make();

    IMethod m = ir.getMethod();
    if (m.isClinit()) {
      if (ast.getKind() != CAstNode.BLOCK_STMT || ast.getChildCount() == 1) {
        out.println("  static {");
      } else {
        out.println("  static");
      }

    } else {
      out.print("  public ");
      if (m.isStatic()) {
        out.print("static ");
      }
      if (m.isInit()) {
        out.println(m.getDeclaringClass().getName().getClassName() + "(");
      } else {
        out.print(
            toSource(m.getReturnType()).getName()
                + " "
                + nameToJava(m.getName().toString(), false)
                + "(");
      }
      for (int i = 0; i < m.getReference().getNumberOfParameters(); i++) {
        done.add(root.mergePhis.find(i + (m.isStatic() ? 1 : 2)));
        if (i != 0) {
          out.print(", ");
        }

        out.print(
            nameToJava(toSource(m.getReference().getParameterType(i)).getName(), true)
                + " "
                + nameToJava(
                    root.toSourceName(root.mergePhis.find(i + (m.isStatic() ? 1 : 2))), false));
      }
      out.print(") ");
      try {
        TypeReference[] exceptions = m.getDeclaredExceptions();
        if (exceptions != null && exceptions.length > 0) {
          boolean first = true;
          for (TypeReference e : exceptions) {
            if (first) {
              first = false;
              out.print(" throws ");
            } else {
              out.print(", ");
            }
            out.print(e.getName().getClassName());
          }
        }
      } catch (InvalidClassFileException e) {
        assert false : e;
      }
    }

    CAst cast = new CAstImpl();

    root.packages.forEach(
        (p, cs) -> {
          cs.forEach(
              c -> {
                includes.accept(
                    cast.makeNode(
                        CAstNode.INCLUDE,
                        cast.makeConstant(p.replace('/', '.')),
                        cast.makeConstant(c)));
              });
        });

    List<CAstNode> inits = new ArrayList<>();

    assert ast.getKind() == CAstNode.BLOCK_STMT;

    boolean hasExplicitCtorCall =
        ir.getMethod().isInit()
            && ast.getChildCount() > 0
            && ast.getChild(0).getKind() == CAstNode.EXPR_STMT
            && ast.getChild(0).getChild(0).getKind() == CAstNode.CALL
            && ast.getChild(0).getChild(0).getChildCount() > 0
            && (ast.getChild(0).getChild(0).getChild(2).getKind() == CAstNode.THIS
                || ast.getChild(0).getChild(0).getChild(2).getKind() == CAstNode.SUPER);

    System.err.println("looking at " + ast);

    if (hasExplicitCtorCall) {
      inits.add(ast.getChild(0));
    }

    for (int vn = ir.getSymbolTable().getNumberOfParameters() + 1;
        vn <= ir.getSymbolTable().getMaxValueNumber();
        vn++) {
      CAstNode srcName = cast.makeConstant(root.sourceNames.get(vn));
      if (!done.contains(root.mergePhis.find(vn))
          && !CAstPattern.findAll(varUsePattern(srcName), ast).isEmpty()
          && CAstPattern.findAll(varDefPattern(srcName), ast).isEmpty()) {
        System.err.println("found " + vn);
        done.add(root.mergePhis.find(vn));
        inits.add(
            cast.makeNode(
                CAstNode.DECL_STMT,
                cast.makeNode(CAstNode.VAR, srcName),
                cast.makeConstant(toSource(types.getType(vn).getTypeReference()))));
      }
    }

    for (int i = hasExplicitCtorCall ? 1 : 0; i < ast.getChildCount(); i++) {
      inits.add(ast.getChild(i));
    }

    ast = cast.makeNode(CAstNode.BLOCK_STMT, inits);

    try (StringWriter sw = new StringWriter()) {
      PrintWriter pw = out;
      if (codeRecorder != null) {
        pw = new PrintWriter(new TeeWriter(out, sw));
      }
      ToJavaVisitor toJava = makeToJavaVisitor(ir, pw, level, varTypes);
      toJava.visit(ast, new CodeGenerationContext(types, root.mergePhis, true), toJava);
      if (codeRecorder != null) {
        codeRecorder.put(ir.getMethod().getReference(), sw.getBuffer().toString());
      }
    } catch (IOException e) {
      assert false : e;
    }

    ByteArrayOutputStream b;
    try (PrintWriter o = new PrintWriter(b = new ByteArrayOutputStream())) {
      CAstPattern.findAll(
              CAstPattern.parse("|(EXPR_STMT(<expr>*)||DECL_STMT(VAR(<v>*),*,<expr>*))|"), ast)
          .forEach(
              s -> {
                CAstNode e = (CAstNode) s.get("expr");
                System.err.println("expr:: " + e);
                ToJavaVisitor ev = makeToJavaVisitor(ir, o, level, varTypes);
                try {
                  if (s.containsKey("v")) {
                    CAstNode v = (CAstNode) s.get("v");
                    ev.visit(v, new CodeGenerationContext(types, root.mergePhis, true), ev);
                    o.print(" = ");
                  }
                  ev.visit(e, new CodeGenerationContext(types, root.mergePhis, true), ev);
                  o.print("\n");
                  o.flush();
                } catch (Throwable e1) {
                  assert false : e1;
                }
              });
    }
    System.err.println(b.toString());

    if (m.isClinit() && (ast.getKind() != CAstNode.BLOCK_STMT || ast.getChildCount() == 1)) {
      out.println("  }");
    }
  }

  protected ToJavaVisitor makeToJavaVisitor(
      IR ir, PrintWriter out, int level, Map<String, Object> varTypes) {
    ToJavaVisitor toJava = new ToJavaVisitor(level, ir, out, varTypes);
    return toJava;
  }

  protected RegionTreeNode makeTreeNode(
      IR ir,
      IClassHierarchy cha,
      TypeInference types,
      PrunedCFG<SSAInstruction, ISSABasicBlock> cfg) {
    RegionTreeNode root =
        new RegionTreeNode(ir, cha, types, cfg.getNode(1).getLastInstruction(), cfg.getNode(2));
    return root;
  }

  protected static String toPackage(TypeReference type) {
    if (type.isArrayType()) {
      return toPackage(type.getArrayElementType());
    } else if (type.isReferenceType()) {
      try {
        if (type.getName().getPackage() != null) {
          return type.getName().getPackage().toUnicodeString();
        } else {
          return null;
        }
      } catch (UTFDataFormatException e) {
        assert false : e;
        return type.getName().getPackage().toString();
      }
    } else {
      return null;
    }
  }

  protected static CAstType toSource(TypeReference type) {
    if (type.isArrayType()) {
      return new CAstType.Array() {

        @Override
        public Collection<CAstType> getSupertypes() {
          return null;
        }

        @Override
        public String getName() {
          return toSource(type.getArrayElementType()).getName() + "[]";
        }

        @Override
        public int getNumDimensions() {
          CAstType elt = toSource(type.getArrayElementType());
          if (elt instanceof CAstType.Array) {
            return 1 + ((CAstType.Array) elt).getNumDimensions();
          } else {
            return 1;
          }
        }

        @Override
        public CAstType getElementType() {
          return toSource(type.getArrayElementType());
        }

        @Override
        public String toString() {
          return getName();
        }
      };
    } else if (type.isPrimitiveType()) {
      return new CAstType.Primitive() {

        @Override
        public Collection<CAstType> getSupertypes() {
          return null;
        }

        @Override
        public String getName() {
          if (Boolean.equals(type)) {
            return "boolean";
          } else if (Int.equals(type)) {
            return "int";
          } else if (Long.equals(type)) {
            return "long";
          } else if (Float.equals(type)) {
            return "float";
          } else if (Double.equals(type)) {
            return "double";
          } else if (Char.equals(type)) {
            return "char";
          } else if (Void.equals(type)) {
            return "void";
          } else {
            assert false : type;
            return null;
          }
        }

        @Override
        public String toString() {
          return getName();
        }
      };
    } else if (type.isClassType()) {
      return new CAstType.Class() {

        @Override
        public Collection<CAstType> getSupertypes() {
          // TODO Auto-generated method stub
          return null;
        }

        @Override
        public String getName() {
          return type.getName().getClassName().toString();
        }

        @Override
        public boolean isInterface() {
          // TODO Auto-generated method stub
          return false;
        }

        @Override
        public Collection<CAstQualifier> getQualifiers() {
          // TODO Auto-generated method stub
          return null;
        }

        @Override
        public String toString() {
          return getName();
        }
      };
    } else {
      assert false;
      return null;
    }
  }
}
