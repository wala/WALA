/******************************************************************************
 * Copyright (c) 2002 - 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *****************************************************************************/
import java.io.FileReader;

import org.mozilla.javascript.CompilerEnvirons;
import org.mozilla.javascript.Parser;
import org.mozilla.javascript.ScriptOrFnNode;
import org.mozilla.javascript.tools.ToolErrorReporter;

class dumpRhinoParse {

    public static void main(String[] args) {
	try {
	    ToolErrorReporter reporter = new ToolErrorReporter(true);
	    CompilerEnvirons compilerEnv = new CompilerEnvirons();
	    compilerEnv.setErrorReporter(reporter);
	    
	    Parser P = new Parser(compilerEnv, compilerEnv.getErrorReporter());
	    
	    ScriptOrFnNode N = P.parse(new FileReader(args[0]), args[0], 1);
	    System.out.println( N.toStringTree(N) );
	    System.out.println( P.getEncodedSource() );
	}
	
	catch (Exception e) {
	    System.err.println( e.getMessage() );
	}
    }

}


	
  
