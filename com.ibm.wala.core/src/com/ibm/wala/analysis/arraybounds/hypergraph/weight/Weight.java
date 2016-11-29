package com.ibm.wala.analysis.arraybounds.hypergraph.weight;

import com.ibm.wala.analysis.arraybounds.hypergraph.weight.NormalOrder;
import com.ibm.wala.analysis.arraybounds.hypergraph.weight.ReverseOrder;
import com.ibm.wala.analysis.arraybounds.hypergraph.weight.Weight;

/**
 * A weight may be not set, a number or unlimited, note that the meaning of
 * unlimited is given by the chosen order (see {@link NormalOrder} and
 * {@link ReverseOrder}).
 *
 * @author Stephan Gocht {@code <stephan@gobro.de>}
 */
public class Weight {
	public enum Type {
		NUMBER, NOT_SET, UNLIMITED
	}

	public static final Weight UNLIMITED = new Weight(Type.UNLIMITED, 0);
	public static final Weight NOT_SET = new Weight(Type.NOT_SET, 0);

	public static final Weight ZERO = new Weight(Type.NUMBER, 0);

	private final Type type;
	private final int number;

	public Weight(int number) {
		this.type = Type.NUMBER;
		this.number = number;
	}

	public Weight(Type type, int number) {
		super();
		this.type = type;
		this.number = number;
	}

	/**
	 * Returns this + other. If this is not Number this will be returned, if
	 * other is not number other will be returned
	 *
	 * @param other
	 * @return this + other
	 */
	public Weight add(Weight other) {
		Weight result = null;
		if (this.getType() == Type.NUMBER) {
			if (other.getType() == Type.NUMBER) {
				result = new Weight(Type.NUMBER, this.getNumber()
						+ other.getNumber());
			} else {
				result = other;
			}
		} else {
			result = this;
		}

		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj) {
			return true;
		}
		if (obj == null) {
			return false;
		}
		if (this.getClass() != obj.getClass()) {
			return false;
		}
		final Weight other = (Weight) obj;
		if (this.number != other.number) {
			return false;
		}
		if (this.type != other.type) {
			return false;
		}
		return true;
	}

	public int getNumber() {
		return this.number;
	}

	public Type getType() {
		return this.type;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + this.number;
		result = prime * result
				+ ((this.type == null) ? 0 : this.type.hashCode());
		return result;
	}

	@Override
	public String toString() {
		if (this.type == Type.NUMBER) {
			return Integer.toString(this.number);
		} else {
			if (this.type == Type.NOT_SET) {
				return "NOT_SET";
			} else if (this.type == Type.UNLIMITED) {
				return "UNLIMITED";
			} else {
				return "Type: " + this.type;
			}
		}
	}
}
