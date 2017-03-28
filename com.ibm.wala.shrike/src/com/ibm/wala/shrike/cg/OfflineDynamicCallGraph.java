/*******************************************************************************
 * Copyright (c) 2002,2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.shrike.cg;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.Map;

import com.ibm.wala.shrikeBT.ConstantInstruction;
import com.ibm.wala.shrikeBT.Constants;
import com.ibm.wala.shrikeBT.Disassembler;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.shrikeBT.LoadInstruction;
import com.ibm.wala.shrikeBT.MethodData;
import com.ibm.wala.shrikeBT.MethodEditor;
import com.ibm.wala.shrikeBT.MethodEditor.Output;
import com.ibm.wala.shrikeBT.ReturnInstruction;
import com.ibm.wala.shrikeBT.ThrowInstruction;
import com.ibm.wala.shrikeBT.Util;
import com.ibm.wala.shrikeBT.analysis.Analyzer.FailureException;
import com.ibm.wala.shrikeBT.analysis.ClassHierarchyStore;
import com.ibm.wala.shrikeBT.analysis.Verifier;
import com.ibm.wala.shrikeBT.shrikeCT.CTUtils;
import com.ibm.wala.shrikeBT.shrikeCT.ClassInstrumenter;
import com.ibm.wala.shrikeBT.shrikeCT.OfflineInstrumenter;
import com.ibm.wala.shrikeCT.ClassReader;
import com.ibm.wala.shrikeCT.ClassWriter;
import com.ibm.wala.shrikeCT.ConstantPoolParser;
import com.ibm.wala.shrikeCT.InvalidClassFileException;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.config.FileOfClasses;
import com.ibm.wala.util.config.SetOfClasses;

/**
 * Class files are taken as input arguments (or if there are none, from standard
 * input). The methods in those files are instrumented: we insert a
 * System.err.println() at ever method call, and a System.err.println() at every
 * method entry.
 * 
 * The instrumented classes are placed in the directory "output" under the
 * current directory. Disassembled code is written to the file "report" under
 * the current directory.
 * 
 * @author CHammer
 * @author Julian Dolby (dolby@us.ibm.com)
 * @since 10/18
 */
public class OfflineDynamicCallGraph {
	private final static boolean disasm = true;
	private final static boolean verify = true;

	private static boolean patchExits = true;
	private static boolean patchCalls = false;
	
	private static Class<?> runtime = Runtime.class;
	
	private static SetOfClasses filter;
	
	private static ClassHierarchyStore cha = new ClassHierarchyStore();
	
	public static void main(String[] args) throws IOException, ClassNotFoundException, InvalidClassFileException, FailureException {
	  OfflineInstrumenter instrumenter;
	  ClassInstrumenter ci;
	  try (final Writer w = new BufferedWriter(new FileWriter("report", false))) {

	    for(int i = 0; i < args.length - 1; i++) {
	      if ("--runtime".equals(args[i])) {
		runtime = Class.forName(args[i+1]);
	      } else if ("--exclusions".equals(args[i])) {
		filter = new FileOfClasses(new FileInputStream(args[i+1]));
	      } else if ("--dont-patch-exits".equals(args[i])) {
		patchExits = false;
	      } else if ("--patch-calls".equals(args[i])) {
		patchCalls = true;
	      } else if ("--rt-jar".equals(args[i])) {
		System.err.println("using " + args[i+1] + " as stdlib");
		OfflineInstrumenter libReader = new OfflineInstrumenter();
		libReader.addInputJar(new File(args[i+1]));
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

	static ClassWriter doClass(final ClassInstrumenter ci, Writer w) throws InvalidClassFileException, IOException, FailureException {
		final String className = ci.getReader().getName();
    if (filter != null && filter.contains(className)) {
      return null;
    }
    
    if (disasm) {
      w.write("Class: " + className + "\n");
      w.flush();
    }
    
		final ClassReader r = ci.getReader();
		
		for (int m = 0; m < ci.getReader().getMethodCount(); m++) {
			final MethodData d = ci.visitMethod(m);

			// d could be null, e.g., if the method is abstract or native
			if (d != null) {
		    if (filter != null && filter.contains(className + "." + ci.getReader().getMethodName(m))) {
		      return null;
		    }

				if (disasm) {
	        w.write("Instrumenting " + ci.getReader().getMethodName(m) + " " + ci.getReader().getMethodType(m) + ":\n");
					w.write("Initial ShrikeBT code:\n");
					(new Disassembler(d)).disassembleTo(w);
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

				me.insertAtStart(new MethodEditor.Patch() {
				  @Override
          public void emitTo(MethodEditor.Output w) {
            w.emit(ConstantInstruction.makeString(theClass));
				    w.emit(ConstantInstruction.makeString(theMethod));
				    if (nonStatic && !isConstructor)
				      w.emit(LoadInstruction.make(Constants.TYPE_Object, 0)); //load this
				    else
              w.emit(Util.makeGet(runtime, "NULL_TAG"));
				      // w.emit(ConstantInstruction.make(Constants.TYPE_null, null));
				    w.emit(Util.makeInvoke(runtime, "execution", new Class[] {String.class, String.class, Object.class}));
				  }
				});

				if (patchExits) {
				  me.addMethodExceptionHandler(null, new MethodEditor.Patch() { 
				    @Override
				    public void emitTo(Output w) {
				      w.emit(ConstantInstruction.makeString(theClass));
				      w.emit(ConstantInstruction.makeString(theMethod));
				      //if (nonStatic)
				      //  w.emit(LoadInstruction.make(Constants.TYPE_Object, 0)); //load this
				      //else
				      w.emit(Util.makeGet(runtime, "NULL_TAG"));
				      // w.emit(ConstantInstruction.make(Constants.TYPE_null, null));
				      w.emit(ConstantInstruction.make(1)); // true
				      w.emit(Util.makeInvoke(runtime, "termination", new Class[] {String.class, String.class, Object.class, boolean.class}));
				      w.emit(ThrowInstruction.make(false));
				    }
				  });

				  me.visitInstructions(new MethodEditor.Visitor() {
				    @Override
				    public void visitReturn(ReturnInstruction instruction) {
				      insertBefore(new MethodEditor.Patch() {
				        @Override
				        public void emitTo(MethodEditor.Output w) {
				          w.emit(ConstantInstruction.makeString(theClass));
				          w.emit(ConstantInstruction.makeString(theMethod));
				          if (nonStatic)
				            w.emit(LoadInstruction.make(Constants.TYPE_Object, 0)); //load this
				          else
				            w.emit(Util.makeGet(runtime, "NULL_TAG"));
				          // w.emit(ConstantInstruction.make(Constants.TYPE, null));
				          w.emit(ConstantInstruction.make(0)); // false
				          w.emit(Util.makeInvoke(runtime, "termination", new Class[] {String.class, String.class, Object.class, boolean.class}));
				        }
				      });
				    }
				  });
				}

				if (patchCalls) {
				  me.visitInstructions(new MethodEditor.Visitor() {

				    @Override
				    public void visitInvoke(IInvokeInstruction inv) {
				      final String calleeClass = inv.getClassType();
				      final String calleeMethod = inv.getMethodName() + inv.getMethodSignature();
				      addInstructionExceptionHandler(/*"java.lang.Throwable"*/null, new MethodEditor.Patch() {
				        @Override
				        public void emitTo(MethodEditor.Output w) {
				          w.emit(ConstantInstruction.makeString(calleeClass));
				          w.emit(ConstantInstruction.makeString(calleeMethod));
				          w.emit(Util.makeInvoke(runtime, "pop", new Class[] {String.class, String.class}));
				          w.emit(ThrowInstruction.make(true));
				        }
				      });
				      insertBefore(new MethodEditor.Patch() {
				        @Override
				        public void emitTo(MethodEditor.Output w) {
				          w.emit(ConstantInstruction.makeString(calleeClass));
				          w.emit(ConstantInstruction.makeString(calleeMethod));
				          // target unknown
				          w.emit(Util.makeGet(runtime, "NULL_TAG"));
				          // w.emit(ConstantInstruction.make(Constants.TYPE_null, null));
				          w.emit(Util.makeInvoke(runtime, "addToCallStack", new Class[] {String.class, String.class, Object.class}));
				        }
				      });
				      insertAfter(new MethodEditor.Patch() {
				        @Override
				        public void emitTo(MethodEditor.Output w) {
				          w.emit(ConstantInstruction.makeString(calleeClass));
				          w.emit(ConstantInstruction.makeString(calleeMethod));
				          w.emit(Util.makeInvoke(runtime, "pop", new Class[] {String.class, String.class}));
				        }
				      });
				    }
				  });
				}
				
				// this updates the data d
				me.applyPatches();

				if (disasm) {
					w.write("Final ShrikeBT code:\n");
					(new Disassembler(d)).disassembleTo(w);
					w.flush();
				}

				if (verify) {
					Verifier v = new Verifier(d);
          // v.setClassHierarchy(cha);
					v.verify();
				}
			}
		}

		if (ci.isChanged()) {
		  ClassWriter cw = new ClassWriter() {
		    private final Map<Object, Integer> entries = HashMapFactory.make();
		    
		    {
		      ConstantPoolParser p = r.getCP();
		      for(int i = 1; i < p.getItemCount(); i++) {
		        switch (p.getItemType(i)) {
		        case CONSTANT_Integer:
		          entries.put(new Integer(p.getCPInt(i)), i);
		          break;
		        case CONSTANT_Long:
              entries.put(new Long(p.getCPLong(i)), i);
              break;
		        case CONSTANT_Float:
              entries.put(new Float(p.getCPFloat(i)), i);
              break;
		        case CONSTANT_Double:
              entries.put(new Double(p.getCPDouble(i)), i);
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
		        case CONSTANT_MethodHandle:
		        case CONSTANT_MethodType:
		        case CONSTANT_InvokeDynamic:
		        }
		      }
		    }
		    
		    private int findExistingEntry(Object o) {
		      if (entries.containsKey(o)) {
		        return entries.get(o);
		      } else {
		        return -1;
		      }
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
			return cw;
		
		} else {
		  return null;
		}
	}

}
