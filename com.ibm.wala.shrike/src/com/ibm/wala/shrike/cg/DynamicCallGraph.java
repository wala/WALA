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
import java.io.FileWriter;
import java.io.Writer;
import java.util.regex.Pattern;

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
import com.ibm.wala.shrikeBT.analysis.Verifier;
import com.ibm.wala.shrikeBT.shrikeCT.ClassInstrumenter;
import com.ibm.wala.shrikeBT.shrikeCT.OfflineInstrumenter;
import com.ibm.wala.shrikeCT.ClassReader;
import com.ibm.wala.shrikeCT.ClassWriter;

/**
 * Class files are taken as input arguments (or if there are none, from standard
 * input). The methods in those files are instrumented: we insert a
 * System.err.println() at ever method call, and a System.err.println() at every
 * method entry.
 * 
 * In Unix, I run it like this: java -cp ~/dev/shrike/shrike
 * com.ibm.dynHLRace.shrikeInstrumentor test.jar -o output.jar
 * 
 * The instrumented classes are placed in the directory "output" under the
 * current directory. Disassembled code is written to the file "report" under
 * the current directory.
 * 
 * @author CHammer
 * @author Julian Dolby (dolby@us.ibm.com)
 * @since 10/18
 */
public class DynamicCallGraph {
	private final static boolean disasm = true;
	private final static boolean verify = true;

	private static OfflineInstrumenter instrumenter;

	private static Class<?> runtime = Runtime.class;
	
	private static Pattern filter;
	
	public static void main(String[] args) throws Exception {
	  instrumenter = new OfflineInstrumenter();

	  Writer w = new BufferedWriter(new FileWriter("report", false));

	  args = instrumenter.parseStandardArgs(args);
			
	  for(int i = 0; i < args.length - 1; i++) {
	    if ("--runtime".equals(args[i])) {
	      runtime = Class.forName(args[i+1]);
	    } else if ("--filter".equals(args[i])) {
	      filter = Pattern.compile(args[i+1]);
	    }
	  }
	  
	  instrumenter.setPassUnmodifiedClasses(true);
	  instrumenter.beginTraversal();
	  ClassInstrumenter ci;
	  while ((ci = instrumenter.nextClass()) != null) {
	    doClass(ci, w);
	  }
	  instrumenter.close();
	}

	private static void doClass(final ClassInstrumenter ci, Writer w) throws Exception {
		final String className = ci.getReader().getName();
    if (filter != null && ! filter.matcher(className).find()) {
      return;
    }
		w.write("Class: " + className + "\n");
		w.flush();

		ClassReader r = ci.getReader();
		for (int m = 0; m < ci.getReader().getMethodCount(); m++) {
			final MethodData d = ci.visitMethod(m);

			// d could be null, e.g., if the method is abstract or native
			if (d != null) {
				w.write("Instrumenting " + ci.getReader().getMethodName(m) + " " + ci.getReader().getMethodType(m) + ":\n");
				w.flush();

				if (disasm) {
					w.write("Initial ShrikeBT code:\n");
					(new Disassembler(d)).disassembleTo(w);
					w.flush();
				}

				if (verify) {
					Verifier v = new Verifier(d);
					v.verify();
				}

				final MethodEditor me = new MethodEditor(d);
				me.beginPass();
				
				final String method = r.getName().replace('/', '.').concat(".").concat(r.getMethodName(m).concat(r.getMethodType(m)));
				final boolean isConstructor = method.contains("<init>");
				final boolean nonStatic = !java.lang.reflect.Modifier.isStatic(r.getMethodAccessFlags(m));
				me.insertAtStart(new MethodEditor.Patch() {
				  @Override
          public void emitTo(MethodEditor.Output w) {
				    w.emit(ConstantInstruction.makeString(method));
				    if (nonStatic && !isConstructor)
				      w.emit(LoadInstruction.make(Constants.TYPE_Object, 0)); //load this
				    else
				      w.emit(ConstantInstruction.make(Constants.TYPE_null, null));
				    w.emit(Util.makeInvoke(runtime, "execution", new Class[] {String.class, Object.class}));
				  }
				});

				me.visitInstructions(new MethodEditor.Visitor() {
					@Override
					public void visitReturn(ReturnInstruction instruction) {
						insertBefore(new MethodEditor.Patch() {
							@Override
              public void emitTo(MethodEditor.Output w) {
								w.emit(ConstantInstruction.makeString(method));
								if (nonStatic)
									w.emit(LoadInstruction.make(Constants.TYPE_Object, 0)); //load this
								else
									w.emit(ConstantInstruction.make(Constants.TYPE_null, null));
								w.emit(ConstantInstruction.make(0)); // false
								w.emit(Util.makeInvoke(runtime, "termination", new Class[] {String.class, Object.class, boolean.class}));
							}
						});
					}
					
					@Override
					public void visitThrow(ThrowInstruction instruction) {
						insertBefore(new MethodEditor.Patch() {
							@Override
              public void emitTo(Output w) {
								w.emit(ConstantInstruction.makeString(method));
								if (nonStatic)
									w.emit(LoadInstruction.make(Constants.TYPE_Object, 0)); //load this
								else
									w.emit(ConstantInstruction.make(Constants.TYPE_null, null));
								w.emit(ConstantInstruction.make(1)); // true
								w.emit(Util.makeInvoke(runtime, "termination", new Class[] {String.class, Object.class, boolean.class}));
							}
						});
					}
					
					@Override
					public void visitInvoke(IInvokeInstruction inv) {
						final String callee = Util.makeClass(inv.getClassType()) + "." + inv.getMethodName() + inv.getMethodSignature();
						addInstructionExceptionHandler(/*"java.lang.Throwable"*/null, new MethodEditor.Patch() {
							@Override
              public void emitTo(MethodEditor.Output w) {
								w.emit(ConstantInstruction.makeString(callee));
								w.emit(Util.makeInvoke(runtime, "pop", new Class[] {String.class}));
								w.emit(ThrowInstruction.make(true));
							}
						});
						insertBefore(new MethodEditor.Patch() {
							@Override
              public void emitTo(MethodEditor.Output w) {
								w.emit(ConstantInstruction.makeString(callee));
								// target unknown
								w.emit(ConstantInstruction.make(Constants.TYPE_null, null));
								w.emit(Util.makeInvoke(runtime, "addToCallStack", new Class[] {String.class, Object.class}));
							}
						});
						insertAfter(new MethodEditor.Patch() {
							@Override
              public void emitTo(MethodEditor.Output w) {
								w.emit(ConstantInstruction.makeString(callee));
								w.emit(Util.makeInvoke(runtime, "pop", new Class[] {String.class}));
							}
						});
					}
				});

				
				// this updates the data d
				me.applyPatches();

				if (disasm) {
					w.write("Final ShrikeBT code:\n");
					(new Disassembler(d)).disassembleTo(w);
					w.flush();
				}

				if (verify) {
					Verifier v = new Verifier(d);
					v.verify();
				}
			}
		}

		if (ci.isChanged()) {
			ClassWriter cw = ci.emitClass();
			instrumenter.outputModifiedClass(ci, cw);
		}
	}

}
