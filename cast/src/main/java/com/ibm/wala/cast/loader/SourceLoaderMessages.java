package com.ibm.wala.cast.loader;

import com.ibm.wala.classLoader.ModuleEntry;
import com.ibm.wala.core.util.warnings.Warning;
import com.ibm.wala.util.collections.HashSetFactory;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

public interface SourceLoaderMessages {

  Map<ModuleEntry, Set<Warning>> getErrors();

  public default Set<Warning> messagesFor(ModuleEntry module) {
    if (!getErrors().containsKey(module)) {
      getErrors().put(module, HashSetFactory.make());
    }
    return getErrors().get(module);
  }

  public default void addMessages(ModuleEntry module, Set<Warning> message) {
    messagesFor(module).addAll(message);
  }

  public default void addMessage(ModuleEntry module, Warning message) {
    messagesFor(module).add(message);
  }

  public default Iterator<ModuleEntry> getMessages(final byte severity) {
    return getErrors().entrySet().stream()
        .filter(entry -> entry.getValue().stream().anyMatch(w -> w.getLevel() == severity))
        .map(Map.Entry::getKey)
        .iterator();
  }

  public default Iterator<ModuleEntry> getModulesWithParseErrors() {
    return getMessages(Warning.SEVERE);
  }

  public default Iterator<ModuleEntry> getModulesWithWarnings() {
    return getMessages(Warning.MILD);
  }

  public default Set<Warning> getMessages(ModuleEntry m) {
    return getErrors().get(m);
  }

  public default void clearMessages() {
    getErrors().clear();
  }
}
