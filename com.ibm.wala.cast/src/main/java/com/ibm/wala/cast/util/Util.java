package com.ibm.wala.cast.util;

import com.ibm.wala.cast.loader.CAstAbstractLoader;
import com.ibm.wala.classLoader.IClassLoader;
import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.core.util.warnings.Warning;
import com.ibm.wala.ipa.cha.IClassHierarchy;
import com.ibm.wala.util.WalaException;
import java.util.Iterator;

public class Util {

  public static void checkForFrontEndErrors(IClassHierarchy cha) throws WalaException {
    StringBuilder message = null;
    for (IClassLoader loader : cha.getLoaders()) {
      if (loader instanceof CAstAbstractLoader) {
        Iterator<ModuleEntry> errors = ((CAstAbstractLoader) loader).getModulesWithParseErrors();
        if (errors.hasNext()) {
          if (message == null) {
            message = new StringBuilder("front end errors:\n");
          }
          while (errors.hasNext()) {
            ModuleEntry errorModule = errors.next();
            for (Warning w : ((CAstAbstractLoader) loader).getMessages(errorModule)) {
              message.append("error in ").append(errorModule.getName()).append(":\n");
              message.append(w.toString()).append('\n');
            }
          }
        }
        // clear out the errors to free some memory
        ((CAstAbstractLoader) loader).clearMessages();
      }
    }
    if (message != null) {
      message.append("end of front end errors\n");
      throw new WalaException(String.valueOf(message));
    }
  }
}
