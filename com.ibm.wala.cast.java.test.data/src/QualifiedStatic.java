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
public class QualifiedStatic {
    @SuppressWarnings("unused")
    public static void main(String[] args) {
	QualifiedStatic qs= new QualifiedStatic();
	FooQ fq= new FooQ();
	int x = FooQ.value;
    }
}
class FooQ {
    static int value= 0;
}
