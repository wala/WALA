/*
 * Copyright (c) 2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 */
package messageFormatTest;

import java.text.MessageFormat;

public class MessageFormatBench {
  public static void main(String[] args) {
    Object[] testArgs = {3L, "MyDisk"};
    MessageFormat form = new MessageFormat("The disk \"{1}\" contains {0} file(s).");
    MessageFormat form2 = (MessageFormat) form.clone();
    System.out.println(form2.format(testArgs));
  }
}
