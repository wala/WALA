/******************************************************************************
 * Copyright (c) 2002 - 2008 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
interface IntConstant {
	int getConstant();
}

public class InnerClassLexicalReads {
	
	/*
	 * CAst Instructions:
	 * 2   v3 = new <Source,LInnerClassLexicalReads/makeIntConstant(I)LIntConstant;/<anonymous subtype of IntConstant>$9$9>@2[9:9] -> [13:3]
	 * 3   invokespecial < Source, LInnerClassLexicalReads/makeIntConstant(I)LIntConstant;/<anonymous subtype of IntConstant>$9$9, <init>()V > v3 @3 exception:v5[9:9] -> [13:3]
	 * 4   return v3                                [9:2] -> [13:4]
	 */
	public static IntConstant makeIntConstant(int x) {
		final int y = x * x;
		return new IntConstant() {
//			CAst CONSTRUCTOR Instructions:
//				1   invokespecial < Source, Ljava/lang/Object, <init>()V > v1 @1 exception:v3[20:9] -> [32:3]
			
			/*
			 * CAst Instructions:
			 * 0   v2:com.ibm.wala.ssa.SymbolTable$1@16b18b6 = lexical:y@LInnerClassLexicalReads/makeIntConstant(I)LIntConstant;
			 * 1   return v2:com.ibm.wala.ssa.SymbolTable$1@16b18b6[11:4] -> [11:13]
			 */
			
      public int getConstant() {
				return y;
			}
		};
	}


	/*
	 * CAst Instructions:
	 * 1   v2:com.ibm.wala.ssa.SymbolTable$1@4272b2 = invokestatic < Source, LInnerClassLexicalReads, makeIntConstant(I)LIntConstant; > v3:#123 @1 exception:v4[17:19] -> [17:39]
	 * 2   v7 = getstatic < Source, Ljava/lang/System, out, <Source,Ljava/io/PrintStream> >[18:2] -> [18:12]
	 * 3   v8 = invokeinterface < Source, LIntConstant, getConstant()I > v2:com.ibm.wala.ssa.SymbolTable$1@4272b2 @3 exception:v9[18:21] -> [18:37]
	 * 4   invokevirtual < Source, Ljava/io/PrintStream, println(I)V > v7,v8 @4 exception:v10[18:2] -> [18:38]
	 */
	public static void main(String args[]) {
		@SuppressWarnings("unused")
		InnerClassLexicalReads ignored = new InnerClassLexicalReads(); // call this just to make <init> reachable (test checks for unreachable methods)
		int foo = 5;
		int haha = foo * foo;
		IntConstant ic = makeIntConstant(haha);
		System.out.println(ic.getConstant());
		int x = ic.getConstant();
		System.out.println(x);
	}
}
