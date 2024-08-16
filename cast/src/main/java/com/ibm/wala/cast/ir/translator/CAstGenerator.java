package com.ibm.wala.cast.ir.translator;

import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.classLoader.ModuleEntry;
import java.util.Set;

public interface CAstGenerator {

  Set<CAstEntity> translate(Set<ModuleEntry> src);
}
