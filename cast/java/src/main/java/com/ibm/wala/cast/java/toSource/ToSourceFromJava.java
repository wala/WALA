package com.ibm.wala.cast.java.toSource;

import com.ibm.wala.analysis.typeInference.TypeInference;
import com.ibm.wala.cast.ir.ssa.AstIRFactory;
import com.ibm.wala.cast.ir.toSource.ToSource;
import com.ibm.wala.cast.java.analysis.typeInference.AstJavaTypeInference;
import com.ibm.wala.cast.java.ssa.AstJavaInstructionVisitor;
import com.ibm.wala.cast.java.ssa.AstJavaInvokeInstruction;
import com.ibm.wala.cast.java.ssa.EnclosingObjectReference;
import com.ibm.wala.cast.loader.AstClass;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.classLoader.IClass;
import com.ibm.wala.classLoader.IField;
import com.ibm.wala.classLoader.IMethod;
import com.ibm.wala.ipa.callgraph.impl.Everywhere;
import com.ibm.wala.ipa.cfg.PrunedCFG;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;
import com.ibm.wala.ssa.IR;
import com.ibm.wala.ssa.IRFactory;
import com.ibm.wala.ssa.ISSABasicBlock;
import com.ibm.wala.ssa.SSAInstruction;
import com.ibm.wala.ssa.SSAOptions;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;

public class ToSourceFromJava extends ToSource {

  @Override
  protected String nameToJava(String name, boolean isType) {
    if (isType) {
      if (name.matches("^[0-9].*$")) {
        name = "_" + name;
      }
      return name.replace('$', '.');
    } else {
      return name;
    }
  }

  protected class JavaRegionTreeNode extends RegionTreeNode {

    public JavaRegionTreeNode(
        IR ir, IClassHierarchy cha, TypeInference types, SSAInstruction r, ISSABasicBlock l) {
      super(ir, cha, types, r, l);
    }

    public JavaRegionTreeNode(RegionTreeNode parent, SSAInstruction r, ISSABasicBlock l) {
      super(parent, r, l);
    }

    @Override
    protected RegionTreeNode makeChild(Pair<SSAInstruction, ISSABasicBlock> k) {
      return new JavaRegionTreeNode(this, k.fst, k.snd);
    }

    @Override
    protected ToCAst makeToCAst(List<SSAInstruction> c) {
      return new ToCAst(
          c, new CodeGenerationContext(types, mergePhis, false, super.positionRecorder)) {

        class JavaVisitor
            extends com.ibm.wala.cast.ir.toSource.ToSource.RegionTreeNode.ToCAst.Visitor
            implements AstJavaInstructionVisitor {

          public JavaVisitor(
              SSAInstruction root,
              CodeGenerationContext c,
              List<SSAInstruction> chunk,
              List<CAstNode> parentDecls,
              Map<String, Set<String>> packages,
              Map<SSAInstruction, Map<ISSABasicBlock, RegionTreeNode>> children,
              boolean extraHeaderCode) {
            super(root, c, chunk, parentDecls, packages, children, extraHeaderCode);
          }

          @Override
          public void visitJavaInvoke(AstJavaInvokeInstruction instruction) {
            visitAbstractInvoke(instruction);
          }

          @Override
          public void visitEnclosingObjectReference(EnclosingObjectReference inst) {
            // TODO Auto-generated method stub

          }
        }

        @Override
        protected Visitor makeVisitor(
            SSAInstruction root,
            CodeGenerationContext c,
            List<SSAInstruction> chunk,
            List<CAstNode> parentDecls,
            Map<String, Set<String>> packages,
            boolean extraHeaderCode) {
          return new JavaVisitor(root, c, chunk, parentDecls, packages, children, extraHeaderCode);
        }
      };
    }
  }

  @Override
  protected RegionTreeNode makeTreeNode(
      IR ir,
      IClassHierarchy cha,
      TypeInference types,
      PrunedCFG<SSAInstruction, ISSABasicBlock> cfg) {
    return new JavaRegionTreeNode(
        ir, cha, types, cfg.getNode(1).getLastInstruction(), cfg.getNode(2));
  }

  @Override
  public Set<File> toJava(
      IClassHierarchy cha,
      File outDir,
      Predicate<IClass> filter,
      Map<MethodReference, String> codeRecorder) {
    IRFactory<IMethod> irs = AstIRFactory.makeDefaultFactory();

    Map<IMethod, IR> code = HashMapFactory.make();
    cha.forEach(
        n -> {
          if (filter.test(n)) {
            n.getDeclaredMethods()
                .forEach(
                    m -> {
                      if (!m.isAbstract()) {
                        code.put(
                            m, irs.makeIR(m, Everywhere.EVERYWHERE, SSAOptions.defaultOptions()));
                      }
                    });
          }
        });

    Set<File> files = HashSetFactory.make();
    for (IClass cls : cha) {
      if (cls instanceof AstClass) {
        String clsName = nameToJava(cls.getName().toString().substring(1), true);
        File f = new File(outDir, clsName + ".java");
        File dir = f.getParentFile();
        if (!dir.exists()) {
          dir.mkdirs();
        }
        Set<Pair<String, String>> seen = HashSetFactory.make();
        try (PrintWriter all = new PrintWriter(new FileWriter(f))) {
          try (ByteArrayOutputStream bs = new ByteArrayOutputStream()) {
            try (PrintWriter out = new PrintWriter(bs)) {
              String cn;
              if (clsName.contains("/")) {
                cn = clsName.substring(clsName.lastIndexOf('/') + 1);
                all.println(
                    "package "
                        + clsName.substring(0, clsName.lastIndexOf('/')).replace('/', '.')
                        + ";");
              } else {
                cn = clsName;
              }
              out.println("public class " + nameToJava(cn, true) + " {");
              for (IField fr : cls.getDeclaredStaticFields()) {
                out.println(
                    "  static "
                        + nameToJava(toSource(fr.getFieldTypeReference()).getName(), true)
                        + " "
                        + nameToJava(fr.getName().toString(), false)
                        + ";");
                Pair<String, String> key =
                    Pair.make(
                        fr.getFieldTypeReference()
                            .getName()
                            .getPackage()
                            .toString()
                            .replace('/', '.'),
                        fr.getFieldTypeReference().getName().getClassName().toString());
                if (!seen.contains(key)) {
                  seen.add(key);
                  String importStr = "import " + key.fst + "." + nameToJava(key.snd, true);
                  all.println(importStr + (importStr.endsWith(";") ? "" : ";"));
                }
              }
              for (IField fr : cls.getDeclaredInstanceFields()) {
                out.println(
                    nameToJava(toSource(fr.getFieldTypeReference()).getName(), true)
                        + " "
                        + nameToJava(fr.getName().toString(), false)
                        + ";");
              }
              for (IMethod m : cls.getDeclaredMethods()) {
                if (code.containsKey(m)) {
                  Consumer<TypeReference> checkImport =
                      (e) -> {
                        if (e.isReferenceType() && e.getName().getPackage() != null) {
                          Pair<String, String> key =
                              Pair.make(
                                  e.getName().getPackage().toString().replace('/', '.'),
                                  nameToJava(e.getName().getClassName().toString(), true));
                          if (!seen.contains(key)) {
                            seen.add(key);
                            all.println("import " + key.fst + "." + key.snd + ";");
                          }
                        }
                      };
                  for (int i = 0; i < m.getNumberOfParameters(); i++) {
                    checkImport.accept(m.getParameterType(i));
                  }
                  if (m.getDeclaredExceptions() != null) {
                    for (TypeReference e : m.getDeclaredExceptions()) {
                      checkImport.accept(e);
                    }
                  }

                  IR ir = code.get(m);
                  AstJavaTypeInference types = new AstJavaTypeInference(ir, true);
                  types.solve();
                  System.err.println(types);
                  toJava(
                      ir,
                      cha,
                      types,
                      out,
                      (i) -> {
                        Pair<String, String> key =
                            Pair.make(
                                (String) i.getChild(0).getValue(),
                                (String) i.getChild(1).getValue());
                        if (!seen.contains(key)) {
                          seen.add(key);
                          String importStr = "import " + key.fst + "." + nameToJava(key.snd, true);
                          all.println(importStr + (importStr.endsWith(";") ? "" : ";"));
                        }
                      },
                      2,
                      codeRecorder);
                }
              }
              out.println("}");
              out.close();
            }
            all.print(new String(bs.toByteArray()));
          }

          files.add(f);
        } catch (IOException | UnsupportedOperationException | InvalidClassFileException e) {
          assert false : e;
        }
      }
    }
    return files;
  }
}
