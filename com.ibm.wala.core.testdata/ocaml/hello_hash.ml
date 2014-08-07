(*******************************************************************************
 * Copyright (c) 2014 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 ******************************************************************************)

let method_hash = Hashtbl.create 1000;;

let print_hello arg = print_string (arg ^ "!\n");;

let bar arg = 
	print_string "before calling print_hello in bar\n";
	print_hello arg;
	print_string "after calling print_hello in bar\n";;

let foo arg  = 
	print_string "before calling bar in foo\n";
	bar arg;
	print_string "after calling bar in foo\n";;

Hashtbl.add method_hash "var" (foo);;
(Hashtbl.find method_hash "var") "Hello World";;
