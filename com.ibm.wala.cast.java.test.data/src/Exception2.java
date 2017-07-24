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
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

public final class Exception2 {

  @SuppressWarnings("resource")
  public static void main(String[] args) {
    Exception2 e2= new Exception2();
    FileInputStream fis = null;
    FileOutputStream fos = null;

    try {
      fis = new FileInputStream( args[0] ); 
      fos = new FileOutputStream( args[1] );

      int data;
      while ( (data = fis.read()) != -1 ) {
	fos.write(data);
      }
    } catch (FileNotFoundException e) {
      System.err.println( "File not found" ); 
      // whatever
    } catch (IOException e) {
      System.err.print( "I/O problem " ); 
      System.err.println( e.getMessage() ); 
    } finally {
      if (fis != null) {
	try {
	  fis.close();
	} catch (IOException e) {
	  System.exit(-1);
	}
      }
      if (fos != null) {
	try {
	  fos.close();
	} catch (IOException e) {
	  System.exit(-1);
	}
      }
    }
  }
}
