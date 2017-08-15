/*******************************************************************************
 * Copyright (c) 2009 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package cornerCases;

import java.io.BufferedInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class TryFinally {
  
  @SuppressWarnings("resource")
  public void test1(InputStream i1, InputStream i2) throws IOException {
    BufferedInputStream in = new BufferedInputStream(i1);
    try {
      FileOutputStream zipOut = new FileOutputStream("someFile.txt");
      try {
        zipOut.write(0);
      } finally {
        zipOut.close();
      }
    } finally {
      in.close();
    }
  }
}
