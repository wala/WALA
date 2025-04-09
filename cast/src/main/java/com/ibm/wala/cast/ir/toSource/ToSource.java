package com.ibm.wala.cast.ir.toSource;

import static com.ibm.wala.types.TypeReference.Boolean;
import static com.ibm.wala.types.TypeReference.Char;
import static com.ibm.wala.types.TypeReference.Double;
import static com.ibm.wala.types.TypeReference.Float;
import static com.ibm.wala.types.TypeReference.Int;
import static com.ibm.wala.types.TypeReference.Long;
import static com.ibm.wala.types.TypeReference.Void;

import com.ibm.wala.analysis.typeInference.TypeAbstraction;
import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.cast.ir.ssa.AssignInstruction;
import com.ibm.wala.cast.ir.ssa.AstPreInstructionVisitor;
import com.ibm.wala.cast.ir.ssa.CAstBinaryOp;
import com.ibm.wala.cast.ir.ssa.CAstUnaryOp;
import com.ibm.wala.cast.ir.ssa.SSAUnspecifiedConditionalExprInstruction;
import com.ibm.wala.cast.ir.ssa.analysis.LiveAnalysis;
import com.ibm.wala.cast.loader.AstMethod;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstAnnotation;
import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstNodeTypeMap;
import com.ibm.wala.cast.tree.CAstQualifier;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.CAstType;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.cast.tree.impl.CAstNodeTypeMapRecorder;
import com.ibm.wala.cast.tree.impl.CAstOperator;
import com.ibm.wala.cast.tree.impl.CAstSourcePositionRecorder;
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
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import org.apache.commons.io.output.TeeWriter;

public abstract class ToSource {

  protected static boolean DEBUG = false;

  private final CAst ast = new CAstImpl();

  public static final String CT_LOOP_JUMP_VAR_NAME = "RESTART-LOOP";
  public static final String CT_LOOP_BREAK_VAR_NAME = "TERMINATE-LOOP";

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
        if (DEBUG) System.err.println("looking at PHI " + inst + " for " + regionInsts);
        boolean ok = true;
        ISSABasicBlock bb = findBlock((SSAPhiInstruction) inst);
        if (DEBUG) System.err.println("block " + bb);
        ISSABasicBlock condPred = null;
        Map<ISSABasicBlock, Object> condPredLabels = HashMapFactory.make();
        check_preds:
        {
          for (Iterator<ISSABasicBlock> pbs = cfg.getPredNodes(bb); pbs.hasNext(); ) {
            ISSABasicBlock pb = pbs.next();
            if (DEBUG) System.err.println("pred " + pb);
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
          if (DEBUG)
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
            if (DEBUG) System.err.println("depOK: " + loop + " " + me);
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
            return positionRecorder;
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
    private final CAstSourcePositionRecorder positionRecorder;

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
        TypeInference types,
        IntegerUnionFind mergePhis,
        boolean isTopLevel,
        CAstSourcePositionRecorder positionRecorder) {
      this.types = types;
      this.mergePhis = mergePhis;
      this.parentPrecedence = Integer.MAX_VALUE;
      this.isTopLevel = isTopLevel;
      this.positionRecorder = positionRecorder;
    }

    public CodeGenerationContext(CodeGenerationContext parent, int precedence) {
      this.types = parent.types;
      this.mergePhis = parent.mergePhis;
      this.parentPrecedence = precedence;
      this.isTopLevel = false;
      this.positionRecorder = parent.positionRecorder;
    }

    @Override
    public CAstEntity top() {
      return fakeTop;
    }

    @Override
    public CAstSourcePositionMap getSourceMap() {
      return positionRecorder;
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
    // The key is loop header basic block while the value is the loop object in IR
    private final Map<ISSABasicBlock, Loop> loops;
    // The key is loop breaker and the value is the list of loops that'll be jumped over
    private final Map<ISSABasicBlock, List<Loop>> jumpToTop;
    // The key is loop breaker and the value is the list of loops that'll be jumped over
    private final Map<ISSABasicBlock, List<Loop>> jumpToOutside;
    // The key is loop breaker and the value is the list of loops that'll be jumped over
    private final Map<ISSABasicBlock, List<Loop>> sharedLoopControl;
    // The key is loop breaker and the value is the list of loops that'll jump to its parent header
    private final Map<ISSABasicBlock, List<Loop>> returnToParentHeader;
    // The key is loop breaker in inner most loop and the value is the list of loops that'll jump
    // over includes top loop but not inner most loop
    private final Map<ISSABasicBlock, List<Loop>> returnToOutsideTail;
    private final SSAInstruction r;
    private final ISSABasicBlock l;
    private final ControlDependenceGraph<ISSABasicBlock> cdg;
    protected final IR ir;
    private BasicNaturalRelation livenessConflicts;
    protected final Map<Integer, String> sourceNames;
    private Graph<ISSABasicBlock> cfgNoBack;
    protected final Map<SSAInstruction, Map<ISSABasicBlock, RegionTreeNode>> children =
        HashMapFactory.make();
    protected final CAstSourcePositionRecorder positionRecorder;
    RegionTreeNode parent;

    protected CAstNode makeVariableName(int vn) {
      return ast.makeConstant(sourceNames.get(vn));
    }

    protected CAstSourcePositionRecorder getPositionRecorder() {
      return this.positionRecorder;
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
      if (DEBUG) System.err.println("phi scc: ------- ");
      new SCCIterator<>(G)
          .forEachRemaining(
              new Consumer<Set<SSAPhiInstruction>>() {
                private int idx = ST.getMaxValueNumber() + 1;

                @Override
                public void accept(Set<SSAPhiInstruction> t) {
                  if (DEBUG) System.err.println("phi scc: " + t);
                  if (t.size() > 1) {
                    if (DEBUG)
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
      this.livenessConflicts = parent.livenessConflicts;
      this.cdg = parent.cdg;
      this.packages = parent.packages;
      this.loops = parent.loops;
      this.jumpToTop = parent.jumpToTop;
      this.jumpToOutside = parent.jumpToOutside;
      this.sharedLoopControl = parent.sharedLoopControl;
      this.returnToParentHeader = parent.returnToParentHeader;
      this.returnToOutsideTail = parent.returnToOutsideTail;
      this.ir = parent.ir;
      this.sourceNames = parent.sourceNames;
      this.positionRecorder = parent.positionRecorder;
      this.cfgNoBack = parent.cfgNoBack;
      initChildren();
      if (DEBUG) System.err.println("added children for " + r + "," + l + ": " + children);
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
      this.positionRecorder = new CAstSourcePositionRecorder();

      du = new DefUse(ir);
      cfg = ExceptionPrunedCFG.makeDefiniteUncaught(ir.getControlFlowGraph());
      packages = HashMapFactory.make();

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
      if (DEBUG) {
        System.err.println("liveness conflicts");
        System.err.println(livenessConflicts);
      }
      cdg = new ControlDependenceGraph<>(cfg, true);
      if (DEBUG) {
        System.err.println(cdg);
        IRToCAst.toCAst(ir, DEBUG)
            .entrySet()
            .forEach(
                e -> {
                  System.err.println(e);
                });
      }

      Map<ISSABasicBlock, Integer> cfgFinishTimes =
          computeFinishTimes(() -> NonNullSingletonIterator.make(cfg.entry()), cfg);
      Map<ISSABasicBlock, Integer> cfgStartTimes =
          computeStartTimes(() -> NonNullSingletonIterator.make(cfg.entry()), cfg);

      BiPredicate<ISSABasicBlock, ISSABasicBlock> isBackEdge =
          (pred, succ) ->
              cfgStartTimes.get(pred) >= cfgStartTimes.get(succ)
                  && cfgFinishTimes.get(pred) <= cfgFinishTimes.get(succ);

      Set<ISSABasicBlock> loopHeaders =
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

      if (DEBUG) System.err.println("loop headers: " + loopHeaders);

      loops = HashMapFactory.make();
      loopHeaders.forEach((header) -> loops.put(header, new Loop(header)));

      cfgNoBack = GraphSlicer.prune(cfg, (p, s) -> !isBackEdge.test(p, s));
      cfg.forEach(
          n -> {
            cfg.getPredNodes(n)
                .forEachRemaining(
                    p -> {
                      if (isBackEdge.test(p, n)) {
                        if (DEBUG) System.err.println("back:" + p + " --> " + n);

                        LoopPart part = new LoopPart();

                        Set<Pair<ISSABasicBlock, ISSABasicBlock>> loopBreakers =
                            HashSetFactory.make();

                        Set<ISSABasicBlock> forward =
                            DFS.getReachableNodes(cfgNoBack, Collections.singleton(n));
                        Set<ISSABasicBlock> backward =
                            DFS.getReachableNodes(
                                GraphInverter.invert(cfgNoBack), Collections.singleton(p));

                        Set<ISSABasicBlock> allBlocks = HashSetFactory.make(forward);
                        allBlocks.retainAll(backward);

                        part.setAllBlocks(allBlocks);
                        part.setLoopHeader(
                            allBlocks.stream()
                                .min(Comparator.comparing(ISSABasicBlock::getNumber))
                                .get());

                        if (DEBUG) System.err.println("loop: " + allBlocks);

                        Set<ISSABasicBlock> breakers = HashSetFactory.make();
                        breakers.addAll(
                            allBlocks.stream()
                                .filter(
                                    bb -> {
                                      if (DEBUG) System.err.println("1: " + bb);
                                      return IteratorUtil.streamify(cfg.getSuccNodes(bb))
                                          .anyMatch(
                                              b -> {
                                                return !allBlocks.contains(b);
                                              });
                                    })
                                .filter(
                                    bb -> {
                                      if (DEBUG)
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

                        breakers.forEach(
                            bb -> {
                              IteratorUtil.streamify(cfg.getSuccNodes(bb))
                                  .filter(b -> !allBlocks.contains(b))
                                  .forEach(sb -> loopBreakers.add(Pair.make(bb, sb)));
                            });
                        part.setLoopBreakers(loopBreakers);

                        assert (breakers.size() > 0);

                        // Pick the first one - the one with smallest number
                        part.setLoopControl(
                            breakers.stream()
                                .min(Comparator.comparing(ISSABasicBlock::getNumber))
                                .get());

                        assert loops.containsKey(part.getLoopHeader());
                        loops.get(part.getLoopHeader()).addLoopPart(part);
                      }
                    });
          });

      // figure out nested loops
      // handle nested loop
      List<HashMap<ISSABasicBlock, List<Loop>>> loopRelation =
          LoopHelper.updateLoopRelationship(cfg, loops, DEBUG);
      assert (loopRelation.size() == 5);
      jumpToTop = loopRelation.get(0);
      jumpToOutside = loopRelation.get(1);
      sharedLoopControl = loopRelation.get(2);
      returnToParentHeader = loopRelation.get(3);
      returnToOutsideTail = loopRelation.get(4);

      if (DEBUG) {
        System.err.println(
            "loop controls: "
                + loops.values().stream()
                    .map(loop -> loop.getLoopControl())
                    .collect(Collectors.toList()));
        System.err.println(
            "loops: "
                + loops.values().stream()
                    .map(loop -> loop.toString())
                    .collect(Collectors.toList()));
      }

      for (ISSABasicBlock b : cfg) {
        if (loopHeaders.contains(b)) {
          if (DEBUG) System.err.println("bad flow: starting " + b);
          cfg.getPredNodes(b)
              .forEachRemaining(
                  s -> {
                    if (DEBUG) System.err.println("bad flow: pred " + s);
                    if (isBackEdge.test(s, b)) {
                      if (DEBUG) System.err.println("bad flow: back edge");
                      int n = Util.whichPred(cfg, s, b);
                      b.iteratePhis()
                          .forEachRemaining(
                              phi -> {
                                if (DEBUG) System.err.println("bad flow: phi " + phi);
                                int vn = phi.getUse(n);
                                SSAInstruction def = du.getDef(vn);
                                if (DEBUG) System.err.println("bad flow: def " + def);
                                if (def instanceof SSAPhiInstruction) {
                                  if (DEBUG)
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
                                    // in some case -1 will not be counted as a value of the set but
                                    // foreach will visit it
                                    if (vn != -1) {
                                      livenessConflicts.add(
                                          mergePhis.find(def), mergePhis.find(vn));
                                      livenessConflicts.add(
                                          mergePhis.find(vn), mergePhis.find(def));
                                    } else {
                                      if (DEBUG)
                                        System.err.println("should not be here>>>>>>>>>>>>>>");
                                    }
                                  });

                              mergePhis.union(def, use);
                              if (DEBUG)
                                System.err.println(
                                    "merging "
                                        + def
                                        + " and "
                                        + use
                                        + " as "
                                        + mergePhis.find(def));
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
      if (DEBUG)
        regions
            .entrySet()
            .forEach(
                es -> {
                  System.err.println("--region--");
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

                                    if (DEBUG) System.err.println("order: " + order);

                                    List<AssignInstruction> as = new ArrayList<>();
                                    order
                                        .snd
                                        .iterator()
                                        .forEachRemaining(
                                            phi -> {
                                              if (DEBUG)
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
                                                if (DEBUG) {
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
                                                }
                                                as.add(assign);
                                              }
                                            });
                                    ii.addAll(as);
                                  });
                        });

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
                if (DEBUG) System.err.println("insts: " + regionInsts);
                List<SSAInstruction> all = new ArrayList<>(regionInsts);
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
                              // If it's a while loop then merge instructions in test
                              // otherwise return null then the instructions will be
                              // translated
                              // into several lines and might be placed in different places
                              LoopHelper.shouldMergeTest(cfg, ST, inst, loops, jumpToTop)
                                  ? inst
                                  : null);
                      if (insts.isEmpty()) {
                        insts.add(inst);
                        chunks.insert(new ArrayList<>(insts));
                      }
                      if (DEBUG) System.err.println("chunk for " + inst + ": " + insts);
                    });
                if (DEBUG) System.err.println("chunks: " + chunks);
                while (chunks.size() > 0 && !regionInsts.isEmpty()) {
                  List<SSAInstruction> chunk = chunks.take();
                  if (DEBUG)
                    System.err.println(
                        "taking "
                            + chunk.stream()
                                .map(i -> i + " " + i.iIndex())
                                .reduce("", (a, b) -> a + ", " + b));
                  if (hasAllByIdentity(regionInsts, chunk)) {
                    removeAllByIdentity(regionInsts, chunk);
                    if (DEBUG)
                      System.err.println(
                          "using "
                              + chunk.stream()
                                  .map(i -> i + " " + i.iIndex())
                                  .reduce("", (a, b) -> a + ", " + b));
                    es.getKey()
                        .forEach(
                            p -> {
                              if (!regionChunks.containsKey(p)) {
                                regionChunks.put(p, new ArrayList<>());
                              }
                              regionChunks.get(p).add(chunk);
                              //                              }
                            });
                  }
                }

                es.getKey()
                    .forEach(
                        p -> {
                          if (regionChunks.containsKey(p)) {
                            regionChunks
                                .get(p)
                                .sort(
                                    (lc, rc) ->
                                        positionByIdentity(all, lc.iterator().next())
                                            - positionByIdentity(all, rc.iterator().next()));
                          }
                        });

                assert regionInsts.isEmpty() : regionInsts + " remaining, with chunks " + chunks;
              });

      if (DEBUG) {
        System.err.println("root region chunks: " + regionChunks);

        ir.iterateNormalInstructions()
            .forEachRemaining(
                inst -> {
                  if (inst instanceof SSAGotoInstruction) {
                    ISSABasicBlock bb = cfg.getBlockForInstruction(inst.iIndex());
                    if (loopHeaders.containsAll(cfg.getNormalSuccessors(bb))) {
                      System.err.println("loop edge " + inst);
                    } else if (loops.values().stream()
                        .map(loop -> loop.getLoopExits())
                        .flatMap(Collection::stream)
                        .collect(Collectors.toSet())
                        .containsAll(cfg.getNormalSuccessors(bb))) {
                      System.err.println("break edge " + inst);
                    }
                  }
                });
      }

      initChildren();
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
                                    if (DEBUG)
                                      System.err.println(
                                          "checking " + k.fst + " with " + bb.getLastInstruction());
                                    if (k.fst.equals(bb.getLastInstruction())) {
                                      if (!children.containsKey(k.fst)) {
                                        children.put(k.fst, HashMapFactory.make());
                                      }
                                      children.get(k.fst).put(k.snd, makeChild(k));
                                      if (DEBUG)
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
      if (DEBUG) System.err.println("children for " + this + ": " + children);
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

    public CAstEntity toCAstEntity(List<Loop> loops) {
      // To make it clear, the work flow for nested loops will be
      // toCAst(null) -> createLoop(empty) -> toLoopCAst(loops) -> toCAst(loops)
      //                                                        -> createLoop(loops)
      CAstNode root = toCAst(loops);
      return new CAstEntity() {
        @Override
        public int getKind() {
          return CAstNode.CAST;
        }

        @Override
        public String getName() {
          return "";
        }

        @Override
        public String getSignature() {
          return "";
        }

        @Override
        public String[] getArgumentNames() {
          return new String[0];
        }

        @Override
        public CAstNode[] getArgumentDefaults() {
          return new CAstNode[0];
        }

        @Override
        public int getArgumentCount() {
          return 0;
        }

        @Override
        public Map<CAstNode, Collection<CAstEntity>> getAllScopedEntities() {
          return Collections.emptyMap();
        }

        @Override
        public Iterator<CAstEntity> getScopedEntities(CAstNode construct) {
          throw new RuntimeException("Unimplemented!");
        }

        @Override
        public CAstNode getAST() {
          return root;
        }

        @Override
        public CAstControlFlowMap getControlFlow() {
          throw new RuntimeException("Unimplemented!");
        }

        @Override
        public CAstSourcePositionMap getSourceMap() {
          return positionRecorder;
        }

        @Override
        public Position getPosition() {
          throw new RuntimeException("Unimplemented!");
        }

        @Override
        public Position getNamePosition() {
          throw new RuntimeException("Unimplemented!");
        }

        @Override
        public Position getPosition(int arg) {
          throw new RuntimeException("Unimplemented!");
        }

        @Override
        public CAstNodeTypeMap getNodeTypeMap() {
          throw new RuntimeException("Unimplemented!");
        }

        @Override
        public Collection<CAstQualifier> getQualifiers() {
          return Collections.emptyList();
        }

        @Override
        public CAstType getType() {
          throw new RuntimeException("Unimplemented!");
        }

        @Override
        public Collection<CAstAnnotation> getAnnotations() {
          return Collections.emptyList();
        }
      };
    }

    // generate the whole region tree node into CAstNode
    // parentLoops might be null
    public CAstNode toCAst(List<Loop> currentLoops) {
      List<CAstNode> elts = new ArrayList<>();
      List<CAstNode> decls = new ArrayList<>();
      List<List<SSAInstruction>> chunks = regionChunks.get(Pair.make(r, l));

      // translate the chunks that's not gotos
      createLoop(
          cfg, chunks, currentLoops == null ? new ArrayList<>() : currentLoops, decls, elts, false);

      // translate gotos
      chunks.stream()
          .filter(cs -> LoopHelper.gotoChunk(cs))
          .forEach(
              c -> {
                Pair<CAstNode, List<CAstNode>> stuff =
                    makeToCAst(c).processChunk(decls, packages, currentLoops);
                elts.add(stuff.fst);
                decls.addAll(stuff.snd);
              });
      decls.addAll(elts);
      return ast.makeNode(CAstNode.BLOCK_STMT, decls.toArray(new CAstNode[decls.size()]));
    }

    public Pair<CAstNode, List<CAstNode>> toLoopCAst(
        List<List<SSAInstruction>> chunks,
        List<CAstNode> decls,
        List<Loop> currentLoops,
        List<CAstNode> elts) {
      assert (currentLoops != null && currentLoops.size() > 0);

      Loop currentLoop = currentLoops.get(currentLoops.size() - 1);

      // usually loop control is the last chunk passed in from the caller
      assert (chunks != null && chunks.size() > 0);
      List<SSAInstruction> condChunk =
          chunks.stream()
              .filter(
                  chunk ->
                      LoopHelper.isConditional(chunk)
                          // TODO: this is based on the assumption that the first conditional will
                          // be the loop control
                          && LoopHelper.isLoopControl(cfg, chunk, currentLoop))
              .findFirst()
              .orElse(null);
      assert condChunk != null;

      // create nodes before loop control
      createLoop(cfg, chunks, currentLoops, decls, elts, true);

      // find out the initial loop type
      LoopType loopType = LoopHelper.getLoopType(cfg, ST, currentLoop, jumpToTop);

      List<SSAInstruction> condChunkWithoutConditional =
          condChunk.stream()
              .filter(inst -> !(inst instanceof SSAConditionalBranchInstruction))
              .collect(Collectors.toList());
      SSAInstruction instruction =
          condChunk.stream()
              .filter(inst -> (inst instanceof SSAConditionalBranchInstruction))
              .findFirst()
              .get();

      // grab phrase name from instruction if applicable
      String thenPhrase = null;
      String elsePhrase = null;
      SSAInstruction inst = du.getDef(instruction.getUse(0));
      if (inst instanceof SSAUnspecifiedConditionalExprInstruction) {
        thenPhrase = ((SSAUnspecifiedConditionalExprInstruction<?>) inst).getThenPhrase();
        elsePhrase = ((SSAUnspecifiedConditionalExprInstruction<?>) inst).getElsePhrase();
      }

      // find out test
      CAstNode test;
      if (condChunkWithoutConditional.size() > 0) {
        test =
            makeToCAst(condChunkWithoutConditional).processChunk(decls, packages, currentLoops).fst;
        if (CAstNode.DECL_STMT == test.getKind()) {
          test = test.getChild(test.getChildCount() - 1);

          SSAInstruction defNode =
              du.getDef(
                  condChunkWithoutConditional.get(condChunkWithoutConditional.size() - 1).getDef());
          for (int i = 0; i < defNode.getNumberOfUses(); i++) {
            SSAInstruction useNode = du.getDef(defNode.getUse(i));
            if (useNode instanceof SSAPhiInstruction) {
              // an assignment is needed
              if (test.getChildCount() > 2 && CAstNode.BINARY_EXPR == test.getChild(1).getKind()) {
                test =
                    ast.makeNode(
                        CAstNode.BINARY_EXPR,
                        test.getChild(0),
                        ast.makeNode(
                            CAstNode.BLOCK_EXPR,
                            ast.makeNode(
                                CAstNode.ASSIGN, test.getChild(1).getChild(2), test.getChild(1))),
                        test.getChild(2));
              }
            }
          }
        }
      } else {
        test = ast.makeConstant(true);
      }

      // find the block which should be loop body
      ISSABasicBlock body =
          LoopHelper.getLoopSuccessor(cfg, currentLoop.getLoopControl(), currentLoop);
      List<CAstNode> nodesBeforeControl = new ArrayList<>();
      // the loop body as a node
      CAstNode bodyNode = null;

      // For the 'after' block that should be moved into loop, which should be the else branch of
      // loop control
      ISSABasicBlock after = null;
      if (children.get(instruction).size() > 1) {
        HashMap<ISSABasicBlock, RegionTreeNode> copy =
            HashMapFactory.make(children.get(instruction));
        assert copy.remove(body) != null;
        after = copy.keySet().iterator().next();
      }
      List<CAstNode> afterNodes = new ArrayList<>();

      // If successor is not the next block
      if (body.getNumber() != (currentLoop.getLoopControl().getNumber() + 1)) {
        // reverse loop condition
        test = ast.makeNode(CAstNode.UNARY_EXPR, CAstOperator.OP_NOT, test);
      }

      // add the CAstNodes that's already generated
      if (elts != null && elts.size() > 0) {
        // pass all nodes into loop body
        nodesBeforeControl.addAll(elts);
        elts.clear();
      }

      // generate loop body that's after control
      CAstNode condSuccessor = null;
      // if current loop was jumped by it's loop control, then no need to generate loop body because
      // it should be part of nested loop
      if (sharedLoopControl.containsKey(currentLoop.getLoopControl())
          && sharedLoopControl.get(currentLoop.getLoopControl()).contains(currentLoop)) {
        condSuccessor = ast.makeNode(CAstNode.EMPTY);
      } else {
        // translate loop body after conditional
        RegionTreeNode lr = children.get(instruction).get(body);
        condSuccessor = lr.toCAst(currentLoops);
      }

      if (after != null
          && !(sharedLoopControl.containsKey(currentLoop.getLoopControl())
              && sharedLoopControl.get(currentLoop.getLoopControl()).get(0).equals(currentLoop))) {
        RegionTreeNode rt = children.get(instruction).get(after);
        afterNodes.addAll(rt.toCAst(currentLoops).getChildren());
      }

      if (LoopType.DOWHILE.equals(loopType)) {
        // if it's do while loop, use nodesBeforeControl and condSuccessor as whole loop body
        nodesBeforeControl.addAll(condSuccessor.getChildren());
        bodyNode =
            ast.makeNode(
                CAstNode.BLOCK_STMT,
                nodesBeforeControl.toArray(new CAstNode[nodesBeforeControl.size()]));
      } else if (LoopType.WHILETRUE.equals(loopType)) {
        // if it's flexible loop, put test as if-statement into body

        // if there are loop jump and no condSuccessor and elseNodes are empty, do not need to
        // generate if statement
        if (CAstNode.EMPTY == condSuccessor.getKind()
            && afterNodes.size() < 1
            // check if it's top loop
            && (jumpToTop.keySet().stream()
                    .anyMatch(
                        breaker -> currentLoop.containsNestedLoop(jumpToTop.get(breaker).get(0)))
                //                || sharedLoopControl.keySet().stream()
                //                    .anyMatch(breaker ->
                // currentLoop.equals(sharedLoopControl.get(breaker).get(0)))
                || returnToParentHeader.keySet().stream()
                    .anyMatch(
                        breaker ->
                            currentLoop.containsNestedLoop(
                                returnToParentHeader.get(breaker).get(0))))) {
          // use nodesBeforeControl directly
          bodyNode =
              ast.makeNode(
                  CAstNode.BLOCK_STMT,
                  nodesBeforeControl.toArray(new CAstNode[nodesBeforeControl.size()]));

          test = ast.makeConstant(true);
        } else if (nodesBeforeControl.isEmpty()
            && CAstHelper.containsOnlyGotoAndBreak(afterNodes)) {
          // if if-statement is the only thing in body, test should not change
          // loop body should be the elements in condSuccessor, it will become a while loop

          bodyNode = ast.makeNode(CAstNode.BLOCK_STMT, condSuccessor.getChildren());
          loopType = LoopType.WHILE;
        } else {
          if (afterNodes.size() > 0) {
            if (afterNodes.get(afterNodes.size() - 1).getKind() == CAstNode.BLOCK_STMT
                && afterNodes.get(afterNodes.size() - 1).getChild(0).getKind() == CAstNode.BREAK) {
              if (DEBUG)
                System.err.println(
                    "afterNodes is end with break, no need to add break"); // TODO: need it for a
              // while to see when
              // to add break
            } else {
              if (DEBUG)
                System.err.println(
                    "afterNodes is having nodes and not end with break, need to add break"); // TODO:
              // need it
              // for a
              // while
              // to see
              // when to
              // add
              // break
              afterNodes.add(ast.makeNode(CAstNode.BREAK));
            }
          } else afterNodes.add(ast.makeNode(CAstNode.BLOCK_STMT, ast.makeNode(CAstNode.BREAK)));

          // if a if-stmt will be created, try to restore conditional statement's phrases
          List<CAstNode> condSuccessorChildren = new ArrayList<>();
          condSuccessorChildren.addAll(condSuccessor.getChildren());
          if (thenPhrase != null && thenPhrase.length() > 0) {
            if (CAstHelper.isLeadingNegation(test)) afterNodes.add(0, ast.makeConstant(thenPhrase));
            else condSuccessorChildren.add(0, ast.makeConstant(thenPhrase));
          }
          if (elsePhrase != null && elsePhrase.length() > 0) {
            if (CAstHelper.isLeadingNegation(test))
              condSuccessorChildren.add(0, ast.makeConstant(elsePhrase));
            else afterNodes.add(0, ast.makeConstant(elsePhrase));
          }
          List<CAstNode> ifStmt =
              CAstHelper.makeIfStmt(
                  ast.makeNode(CAstNode.UNARY_EXPR, CAstOperator.OP_NOT, test),
                  // include the nodes in the else branch
                  afterNodes.size() < 1
                      ? ast.makeNode(CAstNode.BREAK)
                      : (afterNodes.size() == 1
                          ? afterNodes.get(0)
                          : ast.makeNode(
                              CAstNode.BLOCK_STMT,
                              afterNodes.toArray(new CAstNode[afterNodes.size()]))),
                  // it should be a block instead of array of AST nodes
                  ast.makeNode(CAstNode.BLOCK_STMT, condSuccessorChildren),
                  true);
          nodesBeforeControl.addAll(ifStmt);
          bodyNode =
              ast.makeNode(
                  CAstNode.BLOCK_STMT,
                  nodesBeforeControl.toArray(new CAstNode[nodesBeforeControl.size()]));

          test = ast.makeConstant(true);
          afterNodes.clear(); // avoid duplication;
        }
      } else if (LoopType.FOR.equals(loopType)) {
        assert (condSuccessor.getChildCount() > 1);

        List<CAstNode> forConditions = new ArrayList<>();
        // Find out the assignment that should be moved into for() statement
        if (nodesBeforeControl.size() > 0
            && CAstNode.EXPR_STMT == nodesBeforeControl.get(0).getKind()
            && nodesBeforeControl.get(0).getChildCount() == 1) {
          forConditions.add(nodesBeforeControl.remove(0).getChild(0));
        } else {
          forConditions.add(ast.makeNode(CAstNode.EMPTY));
        }
        // the test
        forConditions.add(test);
        // The incremental which originally second last of loop body
        // It should not be EXPR_STMT to avoid indent and comma
        nodesBeforeControl.addAll(condSuccessor.getChildren());
        // Looking for  assignment from test like
        //        EXPR_STMT
        //        ASSIGN
        //          VAR
        //            "tmp_37"
        //          BINARY_EXPR
        //            "+"
        //            VAR
        //              "tmp_37"
        //            "1"
        CAstNode assignNode = null;
        Object varName = CAstHelper.findVariableNameFromTest(test);
        if (varName != null) {
          assignNode = CAstHelper.seekAssignmentAndRemove(nodesBeforeControl, varName.toString());
        }

        if (assignNode != null) {
          assignNode = assignNode.getChild(0);
          forConditions.add(assignNode);
        } else {
          forConditions.add(ast.makeNode(CAstNode.EMPTY));
        }

        // form a new test as block statement, this will help to tell it's a for loop
        test = ast.makeNode(CAstNode.BLOCK_STMT, forConditions);

        bodyNode =
            ast.makeNode(
                CAstNode.BLOCK_STMT,
                nodesBeforeControl.toArray(new CAstNode[nodesBeforeControl.size()]));
      } else {
        // for normal while loop, use loopBlock
        bodyNode =
            ast.makeNode(
                CAstNode.BLOCK_STMT,
                condSuccessor.getChildren().toArray(new CAstNode[condSuccessor.getChildCount()]));
      }

      Pair<CAstNode, CAstNode> newNodeByJump =
          CAstHelper.generateInnerLoopJumpToHeaderOrTail(
              jumpToTop,
              returnToParentHeader,
              jumpToOutside,
              returnToOutsideTail,
              currentLoop,
              bodyNode,
              CT_LOOP_JUMP_VAR_NAME,
              CT_LOOP_BREAK_VAR_NAME,
              loopType,
              test);
      if (!test.equals(newNodeByJump.fst)) {
        // there's only one case of changing test, which will lead to change loop type
        test = newNodeByJump.fst;
        loopType = LoopType.DOWHILE;
      }
      bodyNode = newNodeByJump.snd;

      CAstNode loopNode =
          ast.makeNode(
              CAstNode.LOOP,
              CAstHelper.stableRemoveLeadingNegation(test),
              bodyNode,
              // reuse LOOP type but add third child as a boolean to tell if it's a do while
              // loop
              ast.makeConstant(LoopType.DOWHILE.equals(loopType)));

      // for the case where conditional statement will become test of a loop, force it to be a
      // whiletrue loop
      if (CAstHelper.isLeadingNegation(test)
          && CAstHelper.isConditionalStatement(CAstHelper.removeSingleNegation(test))
          && ((thenPhrase != null && thenPhrase.length() > 0)
              || (elsePhrase != null && elsePhrase.length() > 0))) {
        // force to while true loop for conditional statements
        loopType = LoopType.WHILETRUE;

        // restore phrase names
        List<CAstNode> phrase1 = new ArrayList<>();
        if (afterNodes.size() < 1) {
          phrase1.add(ast.makeNode(CAstNode.BREAK));
        } else phrase1.addAll(afterNodes);
        if (thenPhrase != null && thenPhrase.length() > 0) {
          phrase1.add(0, ast.makeConstant(thenPhrase));
        }

        List<CAstNode> phrase2 = new ArrayList<>();
        if (bodyNode.getKind() == CAstNode.BLOCK_STMT) {
          phrase2.addAll(bodyNode.getChildren());
        } else {
          phrase2.add(bodyNode);
        }

        List<CAstNode> loopBody = new ArrayList<>();

        loopBody.add(
            ast.makeNode(
                CAstNode.IF_STMT,
                CAstHelper.removeSingleNegation(test),
                ast.makeNode(CAstNode.BLOCK_STMT, phrase1)));
        loopBody.addAll(phrase2);

        afterNodes.clear(); // avoid duplication;

        loopNode =
            ast.makeNode(
                CAstNode.LOOP,
                ast.makeConstant(true),
                ast.makeNode(CAstNode.BLOCK_STMT, loopBody),
                // reuse LOOP type but add third child as a boolean to tell if it's a do while
                // loop
                ast.makeConstant(false));
      }

      ISSABasicBlock next =
          cfg.getBlockForInstruction(((SSAConditionalBranchInstruction) instruction).getTarget());
      loopNode = checkLinePhi(loopNode, instruction, next, decls);

      // skip the case when 'after' block is moved into loop body
      if (!afterNodes.isEmpty()) {
        loopNode =
            ast.makeNode(
                CAstNode.BLOCK_STMT, loopNode, afterNodes.toArray(new CAstNode[afterNodes.size()]));

      } else {

        // still need to wrap into a block
        loopNode = ast.makeNode(CAstNode.BLOCK_STMT, loopNode);
      }

      chunks.stream()
          .filter(cs -> LoopHelper.gotoChunk(cs))
          .forEach(
              c -> {
                Pair<CAstNode, List<CAstNode>> stuff =
                    makeToCAst(c).processChunk(decls, packages, currentLoops);
                elts.add(stuff.fst);
                decls.addAll(stuff.snd);
              });
      return Pair.make(loopNode, decls);
    }

    private CAstNode checkLinePhi(
        CAstNode block, SSAInstruction branch, ISSABasicBlock target, List<CAstNode> parentDecls) {
      CAst ast = new CAstImpl();
      Pair<SSAInstruction, ISSABasicBlock> key = Pair.make(branch, target);
      if (DEBUG)
        System.err.println(
            "checking for line phi for instruction "
                + branch
                + " and target "
                + target
                + "in "
                + linePhis);
      if (linePhis.containsKey(key)) {
        List<SSAInstruction> insts = linePhis.get(key);
        List<CAstNode> lp = handlePhiAssignments(insts, parentDecls);
        if (block != null) {
          lp.add(block);
        }
        return ast.makeNode(CAstNode.BLOCK_STMT, lp.toArray(new CAstNode[lp.size()]));
      } else {
        return block;
      }
    }

    private List<CAstNode> handlePhiAssignments(
        List<SSAInstruction> insts, List<CAstNode> parentDecls) {
      List<CAstNode> lp = new ArrayList<>();
      for (SSAInstruction inst : insts) {
        assert inst instanceof AssignInstruction;
        ToCAst.Visitor v =
            makeToCAst(insts)
                .makeVisitor(
                    inst,
                    new CodeGenerationContext(types, mergePhis, false, this.positionRecorder),
                    Collections.singletonList(inst),
                    parentDecls,
                    packages,
                    false);
        lp.add(
            ast.makeNode(
                CAstNode.EXPR_STMT,
                ast.makeNode(CAstNode.ASSIGN, v.visit(inst.getDef()), v.visit(inst.getUse(0)))));
      }
      return lp;
    }

    private void processLoopChunks(
        List<List<SSAInstruction>> loopChunks,
        List<Loop> currentLoops,
        List<CAstNode> decls,
        List<CAstNode> elts) {
      assert (loopChunks.size() > 0);
      // those chunks might belongs to different loops
      int startIndex = 0;
      int endIndex = 1;
      Loop loopByChunk =
          LoopHelper.findLoopByChunk(cfg, loopChunks.get(startIndex), loops, currentLoops);
      while (endIndex < loopChunks.size()) {
        Loop nextLoopByChunk =
            LoopHelper.findLoopByChunk(cfg, loopChunks.get(endIndex), loops, currentLoops);
        if ((loopByChunk == null && nextLoopByChunk == null)
            || (loopByChunk != null && loopByChunk.equals(nextLoopByChunk))) {
          // they belongs to same loop
          endIndex++;
        } else {
          // they belongs to different loops
          // parse previous loop
          // create loop list
          List<Loop> passLoops = new ArrayList<>();
          if (currentLoops.size() > 0
              && currentLoops.get(currentLoops.size() - 1).containsNestedLoop(loopByChunk)) {
            // if they are nested then pass the loop hierarchy
            passLoops.addAll(currentLoops);
          }
          if (!passLoops.contains(loopByChunk))
            passLoops.add(loopByChunk); // TODO: will the loopByChunk=null?

          Pair<CAstNode, List<CAstNode>> stuff =
              toLoopCAst(
                  loopChunks.subList(startIndex, endIndex), decls, passLoops, new ArrayList<>());
          elts.addAll(stuff.fst.getChildren());

          loopByChunk = nextLoopByChunk;
          startIndex = endIndex;
          endIndex = endIndex++;
        }
      }

      // parse last loop
      // create loop list
      List<Loop> passLoops = new ArrayList<>();
      if (currentLoops.size() > 0
          && currentLoops.get(currentLoops.size() - 1).containsNestedLoop(loopByChunk)) {
        // if they are nested then pass the loop hierarchy
        passLoops.addAll(currentLoops);
      }
      if (!passLoops.contains(loopByChunk))
        passLoops.add(loopByChunk); // TODO: will the loopByChunk=null?

      Pair<CAstNode, List<CAstNode>> stuff =
          toLoopCAst(
              loopChunks.subList(startIndex, loopChunks.size()),
              decls,
              passLoops,
              new ArrayList<>());
      elts.addAll(stuff.fst.getChildren());

      loopChunks.clear();
    }

    private void createLoop(
        PrunedCFG<SSAInstruction, ISSABasicBlock> cfg,
        List<List<SSAInstruction>> chunks,
        List<Loop> currentLoops,
        List<CAstNode> decls,
        List<CAstNode> elts,
        boolean verifyConditional) {

      List<List<SSAInstruction>> loopChunks = new ArrayList<>();
      chunks.forEach(
          chunkInsts -> {
            // Ignore goto chunks for now
            if (!LoopHelper.gotoChunk(chunkInsts)) {
              if (LoopHelper.shouldMoveAsLoopBody(
                  cfg, ST, chunkInsts, loops, currentLoops, jumpToTop)) {
                // move to loop chunks
                loopChunks.add(chunkInsts);
              } else {
                if (loopChunks.size() > 0
                    // In nested loop, the assignment might be part of outside loop,
                    // that should be translated as a normal chunk and at that time loopChunks might
                    // not be empty
                    && LoopHelper.isConditional(loopChunks.get(loopChunks.size() - 1))) {
                  // loopChunks might contain the chunks belongs to different loops
                  processLoopChunks(loopChunks, currentLoops, decls, elts);
                }

                // For the call comes from toCAst, the body should always be called, otherwise, skip
                // loop control
                if (!(verifyConditional
                    && (LoopHelper.isConditional(chunks.get(chunks.size() - 1))
                        && chunkInsts.equals(chunks.get(chunks.size() - 1))))) {
                  Pair<CAstNode, List<CAstNode>> stuff =
                      makeToCAst(chunkInsts).processChunk(decls, packages, currentLoops);
                  elts.add(stuff.fst);
                  decls.addAll(stuff.snd);
                }
              }
            }
          });

      // there's a case loopChunks are the last few chunks in the list, then parse it
      if (loopChunks.size() > 0) {
        processLoopChunks(loopChunks, currentLoops, decls, elts);
      }
    }

    protected ToCAst makeToCAst(List<SSAInstruction> insts) {
      return new ToCAst(
          insts, new CodeGenerationContext(types, mergePhis, false, this.positionRecorder));
    }

    private void toString(StringBuilder sb, int level) {
      List<List<SSAInstruction>> chunks = regionChunks.get(Pair.make(r, l));
      if (chunks == null) {
        return;
      }
      chunks.forEach(
          insts -> {
            if (!LoopHelper.gotoChunk(insts)) {
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
          .filter(cs -> LoopHelper.gotoChunk(cs))
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
        private List<Loop> currentLoops = new ArrayList<>();
        private final CAstSourcePositionRecorder positionRecorder;

        private void logHistory(SSAInstruction inst) {
          if (history != null && !history.isEmpty()) {
            history.peek().add(inst);
          }
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
          this.positionRecorder = c.positionRecorder;
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

        public Visitor(
            SSAInstruction root,
            CodeGenerationContext c,
            List<SSAInstruction> chunk,
            List<CAstNode> parentDecls,
            Map<String, Set<String>> parentPackages,
            Map<SSAInstruction, Map<ISSABasicBlock, RegionTreeNode>> children,
            List<Loop> currentLoops) {
          if (currentLoops != null) this.currentLoops = currentLoops;
          this.root = root;
          this.chunk = chunk;
          this.children = children;
          this.parentDecls = parentDecls;
          this.packages = parentPackages;
          this.positionRecorder = c.positionRecorder;
          root.visit(this);
          if (root.hasDef()) {
            if (node.getKind() != CAstNode.EMPTY) {
              int def = root.getDef();
              if (mergedValues.contains(mergePhis.find(def))
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
                  TypeAbstraction typeAbs = c.getTypes().getType(def);
                  if (typeAbs.equals(TypeAbstraction.TOP)) {
                    type = toSource(TypeReference.Int);
                  } else {
                    type = toSource(typeAbs.getTypeReference());
                  }
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

        /**
         * A very stateful method with the following pre-conditions & effects.
         *
         * <ul>
         *   <li>PRE: this.ir.getMethod() returns and instance of AstMethod with valid position info
         *       for the given iIndex
         *   <li>EFFECTS: Adds position information for current value of this.node using position
         *       info from this.ir.getMethod()
         * </ul>
         */
        private CAstNode markPosition(CAstNode node, int iIndex) {
          assert (ir.getMethod() instanceof AstMethod)
              : "Expected AstMethod containing source position information";
          AstMethod m = (AstMethod) ir.getMethod();
          Position pos = m.getSourcePosition(iIndex);
          if (pos != null) {
            positionRecorder.setPosition(node, pos);
          }
          return node;
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
                  && LoopHelper.isLoopControl(
                      cfg, root, loops) // TODO: should check within the given loop
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

        @Override
        public void visitGoto(SSAGotoInstruction inst) {
          Loop loop = currentLoops.size() > 0 ? currentLoops.get(currentLoops.size() - 1) : null;
          ISSABasicBlock bb = cfg.getBlockForInstruction(inst.iIndex());

          if (loop != null
              && loop.getLoopHeader().equals(cfg.getNormalSuccessors(bb).iterator().next())
              && !loop.isLastBlock(bb)) {
            // if there are more than one loop part, only last one should not generate CONTINUE
            node = ast.makeNode(CAstNode.CONTINUE);
          } else if (loop != null && loop.getLoopExits().containsAll(cfg.getNormalSuccessors(bb))) {
            node = ast.makeNode(CAstNode.BLOCK_STMT, ast.makeNode(CAstNode.BREAK));
          } else if (CAstHelper.needsExitParagraph(inst)) {
            // if it's a jump from middle to the end
            node = CAstHelper.createExitParagraph();
          } else node = ast.makeNode(CAstNode.BLOCK_STMT, ast.makeNode(CAstNode.GOTO));
          markPosition(node, inst.iIndex());
        }

        @Override
        public void visitArrayLoad(SSAArrayLoadInstruction instruction) {
          CAstNode array = visit(instruction.getArrayRef());
          CAstNode index = visit(instruction.getIndex());
          CAstNode elt = ast.makeConstant(toSource(instruction.getElementType()));
          node = ast.makeNode(CAstNode.ARRAY_REF, array, elt, index);
          markPosition(node, instruction.iIndex());
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
          markPosition(node, instruction.iIndex());
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
                markPosition(node, instruction.iIndex());
                return;
              default:
                break;
            }
          }

          node = ast.makeNode(CAstNode.BINARY_EXPR, op, left, right);
          markPosition(node, instruction.iIndex());
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
          markPosition(node, instruction.iIndex());
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
            markPosition(node, instruction.iIndex());
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
          markPosition(node, instruction.iIndex());
        }

        private CAstNode checkLinePhi(
            CAstNode block, SSAInstruction branch, ISSABasicBlock target) {
          Pair<SSAInstruction, ISSABasicBlock> key = Pair.make(branch, target);
          if (DEBUG)
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

        private CAstNode getInitialTestNode(
            SSAConditionalBranchInstruction instruction,
            CAstNode v2,
            CAstNode v1,
            BasicBlock branchBB) {
          Loop loop = currentLoops.size() > 0 ? currentLoops.get(currentLoops.size() - 1) : null;
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
            if (v2.getValue() instanceof Number && v2.getValue().equals(0)) {
              if (castOp == CAstOperator.OP_NE) {
                if (loop != null && loop.getLoopControl().equals(branchBB)) {
                  test = v1;
                } else {
                  test = ast.makeNode(CAstNode.UNARY_EXPR, CAstOperator.OP_NOT, v1);
                }
                break test;
              } else if (castOp == CAstOperator.OP_EQ) {
                if (loop != null && loop.getLoopControl().equals(branchBB)) {
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

        @Override
        public void visitConditionalBranch(SSAConditionalBranchInstruction instruction) {
          Loop loop = currentLoops.size() > 0 ? currentLoops.get(currentLoops.size() - 1) : null;
          assert children.containsKey(instruction) : "children of " + instruction + ":" + children;
          Map<ISSABasicBlock, RegionTreeNode> cc = children.get(instruction);

          SSACFG.BasicBlock branchBB =
              (BasicBlock) cfg.getBlockForInstruction(instruction.iIndex());

          // grab phrase names from instruction if applicable
          String thenPhrase = null;
          String elsePhrase = null;
          SSAInstruction inst = du.getDef(instruction.getUse(0));
          if (inst instanceof SSAUnspecifiedConditionalExprInstruction) {
            thenPhrase = ((SSAUnspecifiedConditionalExprInstruction<?>) inst).getThenPhrase();
            elsePhrase = ((SSAUnspecifiedConditionalExprInstruction<?>) inst).getElsePhrase();
          }

          CAstNode v1 = visit(instruction.getUse(0));
          CAstNode v2 = visit(instruction.getUse(1));

          CAstNode test = getInitialTestNode(instruction, v2, v1, branchBB);

          List<CAstNode> takenBlock = null;

          ISSABasicBlock notTaken;
          ISSABasicBlock taken = cfg.getBlockForInstruction(instruction.getTarget());
          if (cc.containsKey(taken)) {
            HashMap<ISSABasicBlock, RegionTreeNode> copy = HashMapFactory.make(cc);
            assert copy.remove(taken) != null;
            notTaken = copy.keySet().iterator().next();
            List<List<SSAInstruction>> takenChunks =
                regionChunks.get(Pair.make(instruction, taken));
            RegionTreeNode tr = cc.get(taken);
            takenBlock = handleBlock(takenChunks, tr, currentLoops);

          } else {

            assert cc.size() == 1;
            notTaken = cc.keySet().iterator().next();
          }
          assert notTaken != null;

          Pair<SSAConditionalBranchInstruction, ISSABasicBlock> notTakenKey =
              Pair.make(instruction, notTaken);
          List<List<SSAInstruction>> notTakenChunks = regionChunks.get(notTakenKey);
          RegionTreeNode fr = cc.get(notTaken);
          List<CAstNode> notTakenBlock = handleBlock(notTakenChunks, fr, currentLoops);

          if (loop != null
              && loop.getLoopBreakers().contains(branchBB)
              && !loop.getLoopHeader().equals(branchBB)) {
            if (loop.getLoopExits().contains(notTaken)) {
              if (notTakenBlock.get(notTakenBlock.size() - 1).getKind() == CAstNode.BLOCK_STMT
                  && notTakenBlock.get(notTakenBlock.size() - 1).getChild(0).getKind()
                      == CAstNode.BREAK) {
                if (DEBUG)
                  System.err.println(
                      " notTakenBlock is end with break, no need to add break"); // TODO: need it
                // for
                // a while to see
                // when to add break
              } else {
                if (DEBUG)
                  System.err.println(
                      "notTakenBlock is having nodes and not end with break, need to add break"); // TODO: need it for a while to see when to add break
                boolean useReturn = cfg.getNormalSuccessors(notTaken).contains(cfg.exit()) && cfg.getNormalSuccessors(notTaken).size() == 1;
                notTakenBlock.add(ast.makeNode(useReturn? CAstNode.RETURN: CAstNode.BREAK));
              }

              CAstHelper.generateInnerLoopJumpToHeaderOrTailTrue(
                  jumpToTop,
                  returnToParentHeader,
                  jumpToOutside,
                  returnToOutsideTail,
                  branchBB,
                  loop,
                  notTakenBlock,
                  CT_LOOP_JUMP_VAR_NAME,
                  CT_LOOP_BREAK_VAR_NAME,
                  DEBUG);
            } else {
              if (takenBlock.get(takenBlock.size() - 1).getKind() == CAstNode.BLOCK_STMT
                  && takenBlock.get(takenBlock.size() - 1).getChild(0).getKind()
                      == CAstNode.BREAK) {
                if (DEBUG)
                  System.err.println(
                      " takenBlock is end with break, no need to add break"); // TODO: need it for
                // a while to see
                // when to add break
              } else {
                if (DEBUG)
                  System.err.println(
                      "takenBlock is having nodes and not end with break, need to add break"); // TODO: need it for a while to see when to add break
                takenBlock.add(ast.makeNode(CAstNode.BREAK));
              }

              CAstHelper.generateInnerLoopJumpToHeaderOrTailTrue(
                  jumpToTop,
                  returnToParentHeader,
                  jumpToOutside,
                  returnToOutsideTail,
                  branchBB,
                  loop,
                  takenBlock,
                  CT_LOOP_JUMP_VAR_NAME,
                  CT_LOOP_BREAK_VAR_NAME,
                  DEBUG);
            }
          } else {
            Optional<ISSABasicBlock> innerMostLoopBreaker =
                returnToOutsideTail.keySet().stream()
                    .filter(
                        breaker ->
                            // If it is the correct inner most loop
                            loop.getLoopBreakers().contains(breaker)
                                && loop.getLoopExitrByBreaker(breaker).equals(branchBB)
                                && returnToOutsideTail
                                    .get(breaker)
                                    .get(returnToOutsideTail.get(breaker).size() - 1)
                                    .containsNestedLoop(loop))
                    .findFirst();
            if (innerMostLoopBreaker.isPresent()) {
              assert takenBlock != null && notTakenBlock != null;

              Loop topLoop = returnToOutsideTail.get(innerMostLoopBreaker.get()).get(0);
              // TODO: more than 3 layers are not tested yet
              if (DEBUG)
                System.err.println(
                    "This is the case to set jump to header and tail to be True in different blocks");

              CAstNode setJumpTrue =
                  ast.makeNode(
                      CAstNode.EXPR_STMT,
                      ast.makeNode(
                          CAstNode.ASSIGN,
                          ast.makeNode(CAstNode.VAR, ast.makeConstant(CT_LOOP_JUMP_VAR_NAME)),
                          ast.makeConstant(1)));

              CAstNode setBreakTrue =
                  ast.makeNode(
                      CAstNode.EXPR_STMT,
                      ast.makeNode(
                          CAstNode.ASSIGN,
                          ast.makeNode(CAstNode.VAR, ast.makeConstant(CT_LOOP_BREAK_VAR_NAME)),
                          ast.makeConstant(1)));

              if (LoopHelper.gotoHeader(cfg, topLoop, taken)) {
                takenBlock.add(setJumpTrue);
                notTakenBlock.add(setBreakTrue);
              } else {
                takenBlock.add(setBreakTrue);
                notTakenBlock.add(setJumpTrue);
              }
            }
          }

          // restore phrase names if applicable
          if (thenPhrase != null && thenPhrase.length() > 0) {
            if (CAstHelper.isLeadingNegation(test))
              notTakenBlock.add(0, ast.makeConstant(thenPhrase));
            else takenBlock.add(0, ast.makeConstant(thenPhrase));
          }
          if (elsePhrase != null && elsePhrase.length() > 0) {
            if (CAstHelper.isLeadingNegation(test)) takenBlock.add(0, ast.makeConstant(elsePhrase));
            else notTakenBlock.add(0, ast.makeConstant(elsePhrase));
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

          if (takenStmt != null) {
            List<CAstNode> ifStmt =
                CAstHelper.makeIfStmt(test, takenStmt, notTakenStmt, loop != null);
            if (ifStmt.size() == 1) node = ifStmt.get(0);
            else {
              node = ast.makeNode(CAstNode.BLOCK_STMT, ifStmt);
            }
          } else {
            node =
                CAstHelper.makeIfStmt(
                    ast.makeNode(CAstNode.UNARY_EXPR, CAstOperator.OP_NOT, test), notTakenStmt);
          }

          markPosition(node, instruction.iIndex());
        }

        private List<CAstNode> handleBlock(
            List<List<SSAInstruction>> loopChunks, RegionTreeNode lr, List<Loop> currentLoops) {
          List<CAstNode> elts = new ArrayList<>();
          lr.createLoop(
              cfg,
              loopChunks,
              currentLoops == null ? new ArrayList<>() : currentLoops,
              decls,
              elts,
              false);

          // translate gotos
          loopChunks.stream()
              .filter(cs -> LoopHelper.gotoChunk(cs))
              .forEach(
                  c -> {
                    Pair<CAstNode, List<CAstNode>> stuff =
                        lr.makeToCAst(c).processChunk(decls, packages, currentLoops);
                    elts.add(stuff.fst);
                    decls.addAll(stuff.snd);
                  });

          if (DEBUG) System.err.println("final block: " + elts);
          return elts;
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
            List<CAstNode> labelBlock = handleBlock(labelChunks, fr, currentLoops);
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
          List<CAstNode> defaultStuff = handleBlock(defaultChunks, fr, currentLoops);

          node =
              ast.makeNode(
                  CAstNode.SWITCH,
                  value,
                  ast.makeNode(
                      CAstNode.BLOCK_STMT, defaultStuff.toArray(new CAstNode[defaultStuff.size()])),
                  switchCode.toArray(new CAstNode[switchCode.size()]));
          markPosition(node, instruction.iIndex());
        }

        @Override
        public void visitReturn(SSAReturnInstruction instruction) {
          if (!instruction.returnsVoid()) {
            CAstNode arg = visit(instruction.getUse(0));
            node = ast.makeNode(CAstNode.RETURN, arg);
            markPosition(node, instruction.iIndex());
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
          markPosition(node, instruction.iIndex());
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
          markPosition(node, instruction.iIndex());
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
            if (DEBUG)
              System.err.println("looking at type " + inst.getDeclaredTarget().getDeclaringClass());
          }

          args[1] = ast.makeConstant(inst.getCallSite().isStatic());

          if (Void.equals(inst.getDeclaredResultType())) {
            node = ast.makeNode(CAstNode.EXPR_STMT, ast.makeNode(CAstNode.CALL, args));
          } else {
            recordPackage(inst.getDeclaredResultType());
            node = ast.makeNode(CAstNode.CALL, args);
          }
          markPosition(node, inst.iIndex());
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
            markPosition(node, instruction.iIndex());
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
          markPosition(node, instruction.iIndex());
        }

        @Override
        public void visitThrow(SSAThrowInstruction instruction) {
          node = ast.makeNode(CAstNode.THROW, visit(instruction.getUse(0)));
          markPosition(node, instruction.iIndex());
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
          markPosition(node, instruction.iIndex());
        }

        @Override
        public void visitInstanceof(SSAInstanceofInstruction instruction) {
          node =
              ast.makeNode(
                  CAstNode.INSTANCEOF,
                  ast.makeConstant(instruction.getCheckedType()),
                  visit(instruction.getRef()));
          markPosition(node, instruction.iIndex());
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
          markPosition(node, instruction.iIndex());
        }

        @Override
        public void visitAssign(AssignInstruction inst) {
          node = visit(inst.getUse(0));
          markPosition(node, inst.iIndex());
        }

        @Override
        public <T> void visitUnspecified(SSAUnspecifiedInstruction<T> instruction) {
          node = ast.makeNode(CAstNode.PRIMITIVE, ast.makeConstant(instruction.getPayload()));
          markPosition(node, instruction.iIndex());
        }

        @Override
        public <T> void visitUnspecifiedExpr(SSAUnspecifiedExprInstruction<T> instruction) {
          node = ast.makeNode(CAstNode.PRIMITIVE, ast.makeConstant(instruction.getPayload()));
          markPosition(node, instruction.iIndex());
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

      protected Visitor makeVisitor(
          SSAInstruction root,
          CodeGenerationContext c,
          List<SSAInstruction> chunk2,
          List<CAstNode> parentDecls,
          Map<String, Set<String>> packages,
          List<Loop> currentLoops) {
        return new Visitor(root, c, chunk2, parentDecls, packages, children, currentLoops);
      }

      public ToCAst(List<SSAInstruction> insts, CodeGenerationContext c) {
        this.chunk = insts;
        this.c = c;
      }

      Pair<CAstNode, List<CAstNode>> processChunk(
          List<CAstNode> parentDecls, Map<String, Set<String>> packages, List<Loop> currentLoops) {
        SSAInstruction root = chunk.iterator().next();
        Visitor x = makeVisitor(root, c, chunk, parentDecls, packages, currentLoops);
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

      assert (n.getChildCount() > 0);

      if (CAstNode.BLOCK_STMT == n.getChild(0).getKind()) {
        // If it's for loop, then generate for(;;){};
        out.print("for (");
        cv.visit(n.getChild(0).getChild(0), c, visitor);
        out.print("; ");
        cv.visit(n.getChild(0).getChild(1), c, visitor);
        out.print("; ");
        cv.visit(n.getChild(0).getChild(2), c, visitor);
        out.println(")");
        cv.visit(n.getChild(1), c, cv);
        return true;
      } else if (n.getChildCount() > 2 && n.getChild(2).getValue().equals(java.lang.Boolean.TRUE)) {
        // If it's do loop, then generate do{}while();
        out.println("do ");
        cv.visit(n.getChild(1), c, cv);
        indent();
        out.print("while (");
        cv.visit(n.getChild(0), c, visitor);
        out.println(");");
        return true;
      } else {
        // otherwise keep what's been done already, that's while(){};
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

        } else {
          ToJavaVisitor cif = makeToJavaVisitor(ir, out, indent + 1, varTypes);
          cif.visit(n.getChild(0), c, cif);
          out.println(";");
        }
      }

      return true;
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
            //            String lbl = n.getChildCount() > 0 ? " " + n.getChild(0).getValue() : "";
            //            out.println("break" + lbl + ";");
            out.println("break;");
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
    if (DEBUG) System.err.println("IR:\n" + ir);

    RegionTreeNode root = makeTreeNode(ir, cha, types, cfg);

    if (DEBUG) {
      System.err.println("tree");
      System.err.println(root);
    }
    CAstEntity entity = root.toCAstEntity(null);
    CAstNode ast = entity.getAST();
    if (DEBUG) System.err.println(ast);

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

    if (DEBUG) System.err.println("looking at " + ast);

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
        if (DEBUG) System.err.println("found " + vn);
        done.add(root.mergePhis.find(vn));
        inits.add(
            cast.makeNode(
                CAstNode.DECL_STMT,
                cast.makeNode(CAstNode.VAR, srcName),
                cast.makeConstant(toSource(types.getType(vn).getTypeReference()))));
      }
    }

    // search and decide if jump should be defined
    if (CAstHelper.hasVarAssigned(ast, CT_LOOP_JUMP_VAR_NAME)) {
      inits.add(
          cast.makeNode(
              CAstNode.DECL_STMT,
              cast.makeNode(CAstNode.VAR, cast.makeConstant(CT_LOOP_JUMP_VAR_NAME)),
              cast.makeConstant(toSource(TypeReference.Int))));
    }
    // search and decide if test should be defined
    if (CAstHelper.hasVarAssigned(ast, CT_LOOP_BREAK_VAR_NAME)) {
      inits.add(
          cast.makeNode(
              CAstNode.DECL_STMT,
              cast.makeNode(CAstNode.VAR, cast.makeConstant(CT_LOOP_BREAK_VAR_NAME)),
              cast.makeConstant(toSource(TypeReference.Int))));
    }

    for (int i = hasExplicitCtorCall ? 1 : 0; i < ast.getChildCount(); i++) {
      inits.add(ast.getChild(i));
    }

    ast = cast.makeNode(CAstNode.BLOCK_STMT, inits);

    try (StringWriter sw = new StringWriter()) {
      if (codeRecorder != null) {
        try (PrintWriter pw = new PrintWriter(new TeeWriter(out, sw))) {
          ToJavaVisitor toJava = makeToJavaVisitor(ir, pw, level, varTypes);
          toJava.visit(
              ast,
              new CodeGenerationContext(types, root.mergePhis, true, root.getPositionRecorder()),
              toJava);
          codeRecorder.put(ir.getMethod().getReference(), sw.getBuffer().toString());
          pw.close();
        }
      } else {
        ToJavaVisitor toJava = makeToJavaVisitor(ir, out, level, varTypes);
        toJava.visit(
            ast,
            new CodeGenerationContext(types, root.mergePhis, true, root.getPositionRecorder()),
            toJava);
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
                if (DEBUG) System.err.println("expr:: " + e);
                ToJavaVisitor ev = makeToJavaVisitor(ir, o, level, varTypes);
                try {
                  if (s.containsKey("v")) {
                    CAstNode v = (CAstNode) s.get("v");
                    ev.visit(
                        v,
                        new CodeGenerationContext(
                            types, root.mergePhis, true, root.getPositionRecorder()),
                        ev);
                    o.print(" = ");
                  }
                  ev.visit(
                      e,
                      new CodeGenerationContext(
                          types, root.mergePhis, true, root.getPositionRecorder()),
                      ev);
                  o.print("\n");
                  o.flush();
                } catch (Throwable e1) {
                  assert false : e1;
                }
              });
    }
    if (DEBUG) System.err.println(b.toString());

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
