package com.ibm.wala.dalvik.analysis.typeInference;

import com.ibm.wala.analysis.typeInference.TypeAbstraction;
import com.ibm.wala.analysis.typeInference.TypeVariable;

public class DalvikTypeVariable extends TypeVariable {
	private final boolean isIntZeroConstant;

	public DalvikTypeVariable(TypeAbstraction type, boolean isIntZeroConstant) {
		super(type);
		this.isIntZeroConstant = isIntZeroConstant;
	}

	public DalvikTypeVariable(TypeAbstraction type) {
		this(type, false);
	}
	public boolean isIntZeroConstant() {
		return isIntZeroConstant;
	}
}
