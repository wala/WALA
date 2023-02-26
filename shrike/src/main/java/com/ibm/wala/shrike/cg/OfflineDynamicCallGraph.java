/*
 * Copyright (c) 2002,2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package com.ibm.wala.shrike.cg;

import com.ibm.wala.shrike.shrikeBT.ConstantInstruction;
import com.ibm.wala.shrike.shrikeBT.Constants;
import com.ibm.wala.shrike.shrikeBT.Disassembler;
import com.ibm.wala.shrike.shrikeBT.IInvokeInstruction;
import com.ibm.wala.shrike.shrikeBT.IInvokeInstruction.Dispatch;
import com.ibm.wala.shrike.shrikeBT.InvokeDynamicInstruction;
import com.ibm.wala.shrike.shrikeBT.InvokeInstruction;
import com.ibm.wala.shrike.shrikeBT.LoadInstruction;
import com.ibm.wala.shrike.shrikeBT.MethodData;
import com.ibm.wala.shrike.shrikeBT.MethodEditor;
import com.ibm.wala.shrike.shrikeBT.MethodEditor.Output;
import com.ibm.wala.shrike.shrikeBT.ReturnInstruction;
import com.ibm.wala.shrike.shrikeBT.ThrowInstruction;
import com.ibm.wala.shrike.shrikeBT.Util;
import com.ibm.wala.shrike.shrikeBT.analysis.Analyzer.FailureException;
import com.ibm.wala.shrike.shrikeBT.analysis.ClassHierarchyStore;
import com.ibm.wala.shrike.shrikeBT.analysis.Verifier;
import com.ibm.wala.shrike.shrikeBT.shrikeCT.CTUtils;
import com.ibm.wala.shrike.shrikeBT.shrikeCT.ClassInstrumenter;
import com.ibm.wala.shrike.shrikeBT.shrikeCT.OfflineInstrumenter;
import com.ibm.wala.shrike.shrikeCT.ClassReader;
import com.ibm.wala.shrike.shrikeCT.ClassWriter;
import com.ibm.wala.shrike.shrikeCT.ConstantPoolParser;
import com.ibm.wala.shrike.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.config.SetOfClasses;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

/**
 * Class files are taken as input arguments (or if there are none, from standard input). The methods
 * in those files are instrumented: we insert a System.err.println() at ever method call, and a
 * System.err.println() at every method entry.
 *
 * <p>The instrumented classes are placed in the directory "output" under the current directory.
 * Disassembled code is written to the file "report" under the current directory.
 *
 * @author CHammer
 * @author Julian Dolby (dolby@us.ibm.com)
 * @since 10/18
 */
public class OfflineDynamicCallGraph {
  private static class AddTracingToInvokes extends MethodEditor.Visitor {
    @Override
    public void visitInvoke(IInvokeInstruction inv) {
      final String calleeClass = inv.getClassType();
      final String calleeMethod = inv.getMethodName() + inv.getMethodSignature();
      addInstructionExceptionHandler(
          /*"java.lang.Throwable"*/ null,
          new MethodEditor.Patch() {
            @Override
            public void emitTo(MethodEditor.Output w) {
              w.emit(Util.makeInvoke(runtime, "pop", new Class[] {}));
              w.emit(ThrowInstruction.make(true));
            }
          });
      insertBefore(
          new MethodEditor.Patch() {
            @Override
            public void emitTo(MethodEditor.Output w) {
              w.emit(ConstantInstruction.makeString(calleeClass));
              w.emit(ConstantInstruction.makeString(calleeMethod));
              // target unknown
              w.emit(Util.makeGet(runtime, "NULL_TAG"));
              // w.emit(ConstantInstruction.make(Constants.TYPE_null, null));
              w.emit(
                  Util.makeInvoke(
                      runtime,
                      "addToCallStack",
                      new Class[] {String.class, String.class, Object.class}));
            }
          });
      insertAfter(
          new MethodEditor.Patch() {
            @Override
            public void emitTo(MethodEditor.Output w) {
              w.emit(Util.makeInvoke(runtime, "pop", new Class[] {}));
            }
          });
    }
  }

  private static final boolean disasm = true;
  private static final boolean verify = true;

  private static boolean patchExits = true;
  private static boolean patchCalls = true;
  private static final boolean extractCalls = true;
  private static boolean extractDynamicCalls = false;
  private static boolean extractConstructors = true;

  private static Class<?> runtime = Runtime.class;

  private static SetOfClasses filter;

  private static final ClassHierarchyStore cha = new ClassHierarchyStore();

  public static void main(String[] args)
      throws IOException, ClassNotFoundException, InvalidClassFileException, FailureException {
    OfflineInstrumenter instrumenter;
    ClassInstrumenter ci;
    try (final Writer w = new BufferedWriter(new FileWriter("build/report", false))) {

      for (int i = 0; i < args.length; i++) {
        if ("--runtime".equals(args[i])) {
          runtime = Class.forName(args[i + 1]);
        } else if ("--exclusions".equals(args[i])) {
          try (FileInputStream input = new FileInputStream(args[i + 1])) {
            filter = new FileOfClasses(input);
          }
        } else if ("--dont-patch-exits".equals(args[i])) {
          patchExits = false;
        } else if ("--patch-calls".equals(args[i])) {
          patchCalls = true;
        } else if ("--extract-dynamic-calls".equals(args[i])) {
          extractDynamicCalls = true;
        } else if ("--extract-constructors".equals(args[i])) {
          extractConstructors = true;
        } else if ("--rt-jar".equals(args[i])) {
          System.err.println("using " + args[i + 1] + " as stdlib");
          OfflineInstrumenter libReader = new OfflineInstrumenter();
          libReader.addInputJar(new File(args[i + 1]));
          while ((ci = libReader.nextClass()) != null) {
            CTUtils.addClassToHierarchy(cha, ci.getReader());
          }
        }
      }

      instrumenter = new OfflineInstrumenter();
      args = instrumenter.parseStandardArgs(args);

      instrumenter.setPassUnmodifiedClasses(true);

      instrumenter.beginTraversal();
      while ((ci = instrumenter.nextClass()) != null) {
        CTUtils.addClassToHierarchy(cha, ci.getReader());
      }

      instrumenter.setClassHierarchyProvider(cha);

      instrumenter.beginTraversal();
      while ((ci = instrumenter.nextClass()) != null) {
        ClassWriter cw = doClass(ci, w);
        if (cw != null) {
          instrumenter.outputModifiedClass(ci, cw);
        }
      }
    }

    instrumenter.close();
  }

  static ClassWriter doClass(final ClassInstrumenter ci, Writer w)
      throws InvalidClassFileException, IOException, FailureException {
    final String className = ci.getReader().getName();
    if (filter != null && filter.contains(className)) {
      return null;
    }

    if (disasm) {
      w.write("Class: " + className + '\n');
      w.flush();
    }

    final ClassReader r = ci.getReader();

    final Map<Object, MethodData> methods = HashMapFactory.make();

    for (int m = 0; m < ci.getReader().getMethodCount(); m++) {
      final MethodData d = ci.visitMethod(m);

      // d could be null, e.g., if the method is abstract or native
      if (d != null) {
        if (filter != null && filter.contains(className + '.' + ci.getReader().getMethodName(m))) {
          return null;
        }

        if (disasm) {
          w.write(
              "Instrumenting "
                  + ci.getReader().getMethodName(m)
                  + ' '
                  + ci.getReader().getMethodType(m)
                  + ":\n");
          w.write("Initial ShrikeBT code:\n");
          new Disassembler(d).disassembleTo(w);
          w.flush();
        }

        if (verify) {
          Verifier v = new Verifier(d);
          // v.setClassHierarchy(cha);
          v.verify();
        }

        final MethodEditor me = new MethodEditor(d);
        me.beginPass();

        final String theClass = r.getName();
        final String theMethod = r.getMethodName(m).concat(r.getMethodType(m));
        final boolean isConstructor = theMethod.contains("<init>");
        final boolean nonStatic = !java.lang.reflect.Modifier.isStatic(r.getMethodAccessFlags(m));

        if (patchExits) {
          me.addMethodExceptionHandler(
              null,
              new MethodEditor.Patch() {
                @Override
                public void emitTo(Output w) {
                  w.emit(ConstantInstruction.makeString(theClass));
                  w.emit(ConstantInstruction.makeString(theMethod));
                  // if (nonStatic)
                  //  w.emit(LoadInstruction.make(Constants.TYPE_Object, 0)); //load this
                  // else
                  w.emit(Util.makeGet(runtime, "NULL_TAG"));
                  // w.emit(ConstantInstruction.make(Constants.TYPE_null, null));
                  w.emit(ConstantInstruction.make(1)); // true
                  w.emit(
                      Util.makeInvoke(
                          runtime,
                          "termination",
                          new Class[] {String.class, String.class, Object.class, boolean.class}));
                  w.emit(ThrowInstruction.make(false));
                }
              });

          me.visitInstructions(
              new MethodEditor.Visitor() {
                @Override
                public void visitReturn(ReturnInstruction instruction) {
                  insertBefore(
                      new MethodEditor.Patch() {
                        @Override
                        public void emitTo(MethodEditor.Output w) {
                          w.emit(ConstantInstruction.makeString(theClass));
                          w.emit(ConstantInstruction.makeString(theMethod));
                          if (nonStatic)
                            w.emit(LoadInstruction.make(Constants.TYPE_Object, 0)); // load this
                          else w.emit(Util.makeGet(runtime, "NULL_TAG"));
                          // w.emit(ConstantInstruction.make(Constants.TYPE, null));
                          w.emit(ConstantInstruction.make(0)); // false
                          w.emit(
                              Util.makeInvoke(
                                  runtime,
                                  "termination",
                                  new Class[] {
                                    String.class, String.class, Object.class, boolean.class
                                  }));
                        }
                      });
                }
              });
        }

        if (patchCalls) {
          if (extractCalls) {
            me.visitInstructions(
                new AddTracingToInvokes() {
                  @Override
                  public void visitInvoke(final IInvokeInstruction inv) {
                    if ((!extractConstructors && inv.getMethodName().equals("<init>"))
                        || (r.getAccessFlags() & Constants.ACC_INTERFACE) != 0
                        || (!extractDynamicCalls && inv instanceof InvokeDynamicInstruction)) {
                      super.visitInvoke(inv);
                    } else {
                      this.replaceWith(
                          new MethodEditor.Patch() {
                            @Override
                            public void emitTo(final Output w) {
                              final String methodSignature =
                                  inv.getInvocationCode().hasImplicitThis()
                                          && !(inv instanceof InvokeDynamicInstruction)
                                      ? '('
                                          + inv.getClassType()
                                          + inv.getMethodSignature().substring(1)
                                      : inv.getMethodSignature();
                              Object key;
                              if (inv instanceof InvokeDynamicInstruction) {
                                key = inv;
                              } else {
                                key =
                                    Pair.make(
                                        inv.getClassType(),
                                        Pair.make(inv.getMethodName(), methodSignature));
                              }

                              if (!methods.containsKey(key)) {
                                MethodData trampoline =
                                    ci.createEmptyMethodData(
                                        "$shrike$trampoline$" + methods.size(),
                                        methodSignature,
                                        Constants.ACC_STATIC | Constants.ACC_PRIVATE);
                                methods.put(key, trampoline);
                                MethodEditor me = new MethodEditor(trampoline);
                                me.beginPass();
                                me.insertAtStart(
                                    new MethodEditor.Patch() {
                                      private String hackType(String type) {
                                        if ("B".equals(type)
                                            || "C".equals(type)
                                            || "S".equals(type)
                                            || "Z".equals(type)) {
                                          return "I";
                                        } else {
                                          return type;
                                        }
                                      }

                                      @Override
                                      public void emitTo(MethodEditor.Output w) {
                                        String[] types = Util.getParamsTypes(null, methodSignature);
                                        for (int i = 0, local = 0; i < types.length; i++) {
                                          String type = hackType(types[i]);
                                          w.emit(LoadInstruction.make(type, local));
                                          if ("J".equals(type) || "D".equals(type)) {
                                            local += 2;
                                          } else {
                                            local++;
                                          }
                                        }
                                        Dispatch mode = (Dispatch) inv.getInvocationCode();
                                        if (inv instanceof InvokeDynamicInstruction) {
                                          InvokeDynamicInstruction inst =
                                              new InvokeDynamicInstruction(
                                                  ((InvokeDynamicInstruction) inv).getOpcode(),
                                                  ((InvokeDynamicInstruction) inv).getBootstrap(),
                                                  inv.getMethodName(),
                                                  inv.getMethodSignature());
                                          w.emit(inst);
                                        } else {
                                          InvokeInstruction inst =
                                              InvokeInstruction.make(
                                                  inv.getMethodSignature(),
                                                  inv.getClassType(),
                                                  inv.getMethodName(),
                                                  mode);
                                          w.emit(inst);
                                        }
                                        // w.emit(ReturnInstruction.make(hackType(inv.getMethodSignature().substring(inv.getMethodSignature().indexOf(")")+1))));
                                      }
                                    });

                                me.applyPatches();
                                me.endPass();

                                me.beginPass();
                                me.visitInstructions(new AddTracingToInvokes());
                                me.applyPatches();
                                me.endPass();

                                if (verify) {
                                  Verifier v = new Verifier(trampoline);
                                  // v.setClassHierarchy(cha);
                                  try {
                                    v.verify();
                                  } catch (FailureException e) {
                                    throw new RuntimeException(e);
                                  }
                                }
                              }

                              MethodData mt = methods.get(key);
                              w.emit(
                                  InvokeInstruction.make(
                                      mt.getSignature(),
                                      mt.getClassType(),
                                      mt.getName(),
                                      Dispatch.STATIC));
                            }
                          });
                    }
                  }
                });
          } else {
            me.visitInstructions(new AddTracingToInvokes());
          }
        }

        me.insertAtStart(
            new MethodEditor.Patch() {
              @Override
              public void emitTo(MethodEditor.Output w) {
                w.emit(ConstantInstruction.makeString(theClass));
                w.emit(ConstantInstruction.makeString(theMethod));
                if (nonStatic && !isConstructor)
                  w.emit(LoadInstruction.make(Constants.TYPE_Object, 0)); // load this
                else w.emit(Util.makeGet(runtime, "NULL_TAG"));
                // w.emit(ConstantInstruction.make(Constants.TYPE_null, null));
                w.emit(
                    Util.makeInvoke(
                        runtime,
                        "execution",
                        new Class[] {String.class, String.class, Object.class}));
              }
            });

        // this updates the data d
        me.applyPatches();

        me.endPass();

        if (disasm) {
          w.write("Final ShrikeBT code:\n");
          new Disassembler(d).disassembleTo(w);
          w.flush();
        }

        if (verify && !extractConstructors) {
          Verifier v = new Verifier(d);
          // v.setClassHierarchy(cha);
          v.verify();
        }
      }
    }

    if (ci.isChanged()) {
      ClassWriter cw =
          new ClassWriter() {
            private final Map<Object, Integer> entries = HashMapFactory.make();

            {
              ConstantPoolParser p = r.getCP();
              for (int i = 1; i < p.getItemCount(); i++) {
                final byte itemType = p.getItemType(i);
                switch (itemType) {
                  case CONSTANT_Integer:
                    entries.put(p.getCPInt(i), i);
                    break;
                  case CONSTANT_Long:
                    entries.put(p.getCPLong(i), i);
                    break;
                  case CONSTANT_Float:
                    entries.put(p.getCPFloat(i), i);
                    break;
                  case CONSTANT_Double:
                    entries.put(p.getCPDouble(i), i);
                    break;
                  case CONSTANT_Utf8:
                    entries.put(p.getCPUtf8(i), i);
                    break;
                  case CONSTANT_String:
                    entries.put(new CWStringItem(p.getCPString(i), CONSTANT_String), i);
                    break;
                  case CONSTANT_Class:
                    entries.put(new CWStringItem(p.getCPClass(i), CONSTANT_Class), i);
                    break;
                  default:
                    // do nothing
                }
              }
            }

            private int findExistingEntry(Object o) {
              return entries.getOrDefault(o, -1);
            }

            @Override
            protected int addCPEntry(Object o, int size) {
              int entry = findExistingEntry(o);
              if (entry != -1) {
                return entry;
              } else {
                return super.addCPEntry(o, size);
              }
            }
          };

      ci.emitClass(cw);

      if (patchCalls && extractCalls) {
        for (MethodData trampoline : methods.values()) {
          CTUtils.compileAndAddMethodToClassWriter(trampoline, cw, null);
        }
      }

      return cw;

    } else {
      return null;
    }
  }
}
