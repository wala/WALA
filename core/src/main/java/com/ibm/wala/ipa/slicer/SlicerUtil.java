package com.ibm.wala.ipa.slicer;

import com.ibm.wala.ipa.callgraph.AnalysisScope;
import com.ibm.wala.ipa.callgraph.CGNode;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.SSAAbstractInvokeInstruction;
import com.ibm.wala.ssa.SSAAbstractThrowInstruction;
import com.ibm.wala.ssa.SSAArrayLoadInstruction;
import com.ibm.wala.ssa.SSAConditionalBranchInstruction;
import com.ibm.wala.ssa.SSAGetInstruction;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSANewInstruction;
import com.ibm.wala.ssa.SSAPutInstruction;
import com.ibm.wala.ssa.SSAReturnInstruction;
import com.ibm.wala.util.collections.Iterator2Iterable;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.intset.IntSet;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.util.Collection;

/** utility methods for working with slices and slice {@link Statement}s */
public class SlicerUtil {

  private SlicerUtil() {}

  /**
   * Find call to method in CGNode
   *
   * @param n the node
   * @param methodName name of called method
   * @return Statement calling the method
   * @throws com.ibm.wala.util.debug.UnimplementedError if no such statement found
   */
  public static Statement findCallTo(CGNode n, String methodName) {
    IR ir = n.getIR();
    for (SSAInstruction s : Iterator2Iterable.make(ir.iterateAllInstructions())) {
      if (s instanceof SSAAbstractInvokeInstruction) {
        SSAAbstractInvokeInstruction call = (SSAAbstractInvokeInstruction) s;
        if (call.getCallSite().getDeclaredTarget().getName().toString().equals(methodName)) {
          IntSet indices = ir.getCallInstructionIndices(call.getCallSite());
          Assertions.productionAssertion(
              indices.size() == 1, "expected 1 but got " + indices.size());
          return new NormalStatement(n, indices.intIterator().next());
        }
      }
    }
    Assertions.UNREACHABLE("failed to find call to " + methodName + " in " + n);
    return null;
  }

  /**
   * Find the first {@link SSANewInstruction} in a node
   *
   * @param n the node
   * @return Statement corresponding to first new instruction
   * @throws com.ibm.wala.util.debug.UnimplementedError if no new instruction is found
   */
  public static Statement findFirstAllocation(CGNode n) {
    IR ir = n.getIR();
    for (int i = 0; i < ir.getInstructions().length; i++) {
      SSAInstruction s = ir.getInstructions()[i];
      if (s instanceof SSANewInstruction) {
        return new NormalStatement(n, i);
      }
    }
    Assertions.UNREACHABLE("failed to find allocation in " + n);
    return null;
  }

  public static void dumpSlice(Collection<Statement> slice) {
    dumpSlice(slice, new PrintWriter(System.err));
  }

  public static void dumpSlice(Collection<Statement> slice, PrintWriter w) {
    w.println("SLICE:\n");
    int i = 1;
    for (Statement s : slice) {
      String line = i++ + "   " + s;
      w.println(line);
      w.flush();
    }
  }

  public static void dumpSliceToFile(Collection<Statement> slice, String fileName)
      throws FileNotFoundException {
    File f = new File(fileName);
    FileOutputStream fo = new FileOutputStream(f);
    try (final PrintWriter w = new PrintWriter(fo)) {
      dumpSlice(slice, w);
    }
  }

  public static int countAllocations(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        NormalStatement ns = (NormalStatement) s;
        if (ns.getInstruction() instanceof SSANewInstruction) {
          count++;
        }
      }
    }
    return count;
  }

  public static int countApplicationAllocations(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        NormalStatement ns = (NormalStatement) s;
        if (ns.getInstruction() instanceof SSANewInstruction) {
          AnalysisScope scope = s.getNode().getClassHierarchy().getScope();
          if (scope.isApplicationLoader(
              s.getNode().getMethod().getDeclaringClass().getClassLoader())) {
            count++;
          }
        }
      }
    }
    return count;
  }

  public static int countThrows(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        NormalStatement ns = (NormalStatement) s;
        if (ns.getInstruction() instanceof SSAAbstractThrowInstruction) {
          count++;
        }
      }
    }
    return count;
  }

  public static int countAloads(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        NormalStatement ns = (NormalStatement) s;
        if (ns.getInstruction() instanceof SSAArrayLoadInstruction) {
          count++;
        }
      }
    }
    return count;
  }

  public static int countNormals(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        count++;
      }
    }
    return count;
  }

  public static int countApplicationNormals(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        AnalysisScope scope = s.getNode().getClassHierarchy().getScope();
        if (scope.isApplicationLoader(
            s.getNode().getMethod().getDeclaringClass().getClassLoader())) {
          count++;
        }
      }
    }
    return count;
  }

  public static int countConditionals(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        NormalStatement ns = (NormalStatement) s;
        if (ns.getInstruction() instanceof SSAConditionalBranchInstruction) {
          count++;
        }
      }
    }
    return count;
  }

  public static int countInvokes(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        NormalStatement ns = (NormalStatement) s;
        if (ns.getInstruction() instanceof SSAAbstractInvokeInstruction) {
          count++;
        }
      }
    }
    return count;
  }

  public static int countPutfields(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        NormalStatement ns = (NormalStatement) s;
        if (ns.getInstruction() instanceof SSAPutInstruction) {
          SSAPutInstruction p = (SSAPutInstruction) ns.getInstruction();
          if (!p.isStatic()) {
            count++;
          }
        }
      }
    }
    return count;
  }

  public static int countReturns(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        NormalStatement ns = (NormalStatement) s;
        if (ns.getInstruction() instanceof SSAReturnInstruction) {
          count++;
        }
      }
    }
    return count;
  }

  public static int countGetfields(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        NormalStatement ns = (NormalStatement) s;
        if (ns.getInstruction() instanceof SSAGetInstruction) {
          SSAGetInstruction p = (SSAGetInstruction) ns.getInstruction();
          if (!p.isStatic()) {
            count++;
          }
        }
      }
    }
    return count;
  }

  public static int countPutstatics(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        NormalStatement ns = (NormalStatement) s;
        if (ns.getInstruction() instanceof SSAPutInstruction) {
          SSAPutInstruction p = (SSAPutInstruction) ns.getInstruction();
          if (p.isStatic()) {
            count++;
          }
        }
      }
    }
    return count;
  }

  public static int countGetstatics(Collection<Statement> slice) {
    int count = 0;
    for (Statement s : slice) {
      if (s.getKind().equals(Statement.Kind.NORMAL)) {
        NormalStatement ns = (NormalStatement) s;
        if (ns.getInstruction() instanceof SSAGetInstruction) {
          SSAGetInstruction p = (SSAGetInstruction) ns.getInstruction();
          if (p.isStatic()) {
            count++;
          }
        }
      }
    }
    return count;
  }
}
