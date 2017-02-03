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
public class Breaks {

  private static class Ref {
    String[] classes;

    String[] getClasses() {
      return classes;
    }

    private Ref(String[] classes) {
      this.classes = classes;
    }
  }

  private void testBreakFromIf(String objectClass, Ref reference) {
    objectClassCheck:
    if (objectClass != null) {
      String[] classes = reference.getClasses();
      int size = classes.length;
      for (int i = 0; i < size; i++) {    
	if (classes[i] == objectClass)
	  break objectClassCheck;
      }
      return;
    }
    if (objectClass == null) {
      reference.classes = null;
    }
  }

  public static void main(String[] args) {
    (new Breaks()).testBreakFromIf("whatever", new Ref(args));
  }

}
