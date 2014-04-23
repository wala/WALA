/*******************************************************************************
 * Copyright (c) 2002,2006 IBM Corporation.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package com.ibm.wala.shrikeBT;

/**
 * A ConstantInstruction pushes some constant value onto the stack.
 */
public abstract class ConstantInstruction extends Instruction {

  public static class ClassToken {
    private final String typeName;

    ClassToken(String typeName) {
      this.typeName = typeName;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((typeName == null) ? 0 : typeName.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      ClassToken other = (ClassToken) obj;
      if (typeName == null) {
        if (other.typeName != null)
          return false;
      } else if (!typeName.equals(other.typeName))
        return false;
      return true;
    }

    public String getTypeName() {
      return typeName;
    }

  }

  public static class ReferenceToken {
    private final String className;
    private final String elementName;
    private final String descriptor;
    
    public ReferenceToken(String className, String elementName, String descriptor) {
      super();
      this.className = className;
      this.elementName = elementName;
      this.descriptor = descriptor;
    }

    @Override
    public int hashCode() {
      final int prime = 31;
      int result = 1;
      result = prime * result + ((className == null) ? 0 : className.hashCode());
      result = prime * result + ((descriptor == null) ? 0 : descriptor.hashCode());
      result = prime * result + ((elementName == null) ? 0 : elementName.hashCode());
      return result;
    }

    @Override
    public boolean equals(Object obj) {
      if (this == obj)
        return true;
      if (obj == null)
        return false;
      if (getClass() != obj.getClass())
        return false;
      ReferenceToken other = (ReferenceToken) obj;
      if (className == null) {
        if (other.className != null)
          return false;
      } else if (!className.equals(other.className))
        return false;
      if (descriptor == null) {
        if (other.descriptor != null)
          return false;
      } else if (!descriptor.equals(other.descriptor))
        return false;
      if (elementName == null) {
        if (other.elementName != null)
          return false;
      } else if (!elementName.equals(other.elementName))
        return false;
      return true;
    }
  }
  
  public ConstantInstruction(short opcode) {
    super(opcode);
  }

  ConstantPoolReader getLazyConstantPool() {
    return null;
  }

  int getCPIndex() {
    return 0;
  }

  final static class ConstNull extends ConstantInstruction {
    protected ConstNull() {
      super(OP_aconst_null);
    }

    private final static ConstNull preallocated = new ConstNull();

    static ConstNull makeInternal() {
      return preallocated;
    }

    @Override
    public Object getValue() {
      return null;
    }

    @Override
    public String getType() {
      return TYPE_null;
    }
  }

  static class ConstInt extends ConstantInstruction {
    protected int value;

    private final static ConstInt[] preallocated = preallocate();

    protected ConstInt(short opcode, int value) {
      super(opcode);
      this.value = value;
    }

    private static ConstInt[] preallocate() {
      ConstInt[] r = new ConstInt[256];
      for (int i = 0; i < r.length; i++) {
        r[i] = new ConstInt(OP_bipush, i - 128);
      }
      for (int i = -1; i <= 5; i++) {
        r[i + 128] = new ConstInt((short) (i - (-1) + OP_iconst_m1), i);
      }
      return r;
    }

    static ConstInt makeInternal(int i) {
      if (((byte) i) == i) {
        return preallocated[i + 128];
      } else if (((short) i) == i) {
        return new ConstInt(OP_sipush, i);
      } else {
        return new ConstInt(OP_ldc_w, i);
      }
    }

    @Override
    final public Object getValue() {
      return Integer.valueOf(getIntValue());
    }

    @Override
    final public String getType() {
      return TYPE_int;
    }

    public int getIntValue() {
      return value;
    }
  }

  final static class LazyInt extends ConstInt {
    final private ConstantPoolReader cp;

    final private int index;

    private boolean isSet;

    protected LazyInt(short opcode, ConstantPoolReader cp, int index) {
      super(opcode, 0);
      this.cp = cp;
      this.index = index;
      this.isSet = false;
    }

    @Override
    public int getIntValue() {
      if (!isSet) {
        value = cp.getConstantPoolInteger(index);
        isSet = true;
      }
      return value;
    }

    @Override
    public ConstantPoolReader getLazyConstantPool() {
      return cp;
    }

    @Override
    public int getCPIndex() {
      return index;
    }
  }

  static class ConstLong extends ConstantInstruction {
    protected long value;

    private final static ConstLong[] preallocated = preallocate();

    protected ConstLong(short opcode, long value) {
      super(opcode);
      this.value = value;
    }

    private static ConstLong[] preallocate() {
      ConstLong[] r = { new ConstLong(OP_lconst_0, 0), new ConstLong(OP_lconst_1, 1) };
      return r;
    }

    static ConstLong makeInternal(long v) {
      if (v == 0 || v == 1) {
        return preallocated[(int) v];
      } else {
        return new ConstLong(OP_ldc2_w, v);
      }
    }

    @Override
    final public Object getValue() {
      return Long.valueOf(getLongValue());
    }

    @Override
    final public String getType() {
      return TYPE_long;
    }

    public long getLongValue() {
      return value;
    }
  }

  final static class LazyLong extends ConstLong {
    final private ConstantPoolReader cp;

    final private int index;

    private boolean isSet;

    protected LazyLong(short opcode, ConstantPoolReader cp, int index) {
      super(opcode, 0);
      this.cp = cp;
      this.index = index;
      this.isSet = false;
    }

    @Override
    public long getLongValue() {
      if (!isSet) {
        value = cp.getConstantPoolLong(index);
        isSet = true;
      }
      return value;
    }

    @Override
    public ConstantPoolReader getLazyConstantPool() {
      return cp;
    }

    @Override
    public int getCPIndex() {
      return index;
    }
  }

  static class ConstFloat extends ConstantInstruction {
    protected float value;

    private final static ConstFloat[] preallocated = preallocate();

    protected ConstFloat(short opcode, float value) {
      super(opcode);
      this.value = value;
    }

    private static ConstFloat[] preallocate() {
      ConstFloat[] r = { new ConstFloat(OP_fconst_0, 0), new ConstFloat(OP_fconst_1, 1), new ConstFloat(OP_fconst_2, 2) };
      return r;
    }

    static ConstFloat makeInternal(float v) {
      if (v == 0.0 || v == 1.0 || v == 2.0) {
        return preallocated[(int) v];
      } else {
        return new ConstFloat(OP_ldc_w, v);
      }
    }

    @Override
    final public Object getValue() {
      return new Float(getFloatValue());
    }

    @Override
    final public String getType() {
      return TYPE_float;
    }

    public float getFloatValue() {
      return value;
    }
  }

  final static class LazyFloat extends ConstFloat {
    final private ConstantPoolReader cp;

    final private int index;

    private boolean isSet;

    protected LazyFloat(short opcode, ConstantPoolReader cp, int index) {
      super(opcode, 0.0f);
      this.cp = cp;
      this.index = index;
      this.isSet = false;
    }

    @Override
    public float getFloatValue() {
      if (!isSet) {
        value = cp.getConstantPoolFloat(index);
        isSet = true;
      }
      return value;
    }

    @Override
    public ConstantPoolReader getLazyConstantPool() {
      return cp;
    }

    @Override
    public int getCPIndex() {
      return index;
    }
  }

  static class ConstDouble extends ConstantInstruction {
    protected double value;

    private final static ConstDouble[] preallocated = preallocate();

    protected ConstDouble(short opcode, double value) {
      super(opcode);
      this.value = value;
    }

    private static ConstDouble[] preallocate() {
      ConstDouble[] r = { new ConstDouble(OP_dconst_0, 0), new ConstDouble(OP_dconst_1, 1) };
      return r;
    }

    static ConstDouble makeInternal(double v) {
      if (v == 0.0 || v == 1.0) {
        return preallocated[(int) v];
      } else {
        return new ConstDouble(OP_ldc2_w, v);
      }
    }

    @Override
    final public Object getValue() {
      return new Double(getDoubleValue());
    }

    @Override
    final public String getType() {
      return TYPE_double;
    }

    public double getDoubleValue() {
      return value;
    }
  }

  final static class LazyDouble extends ConstDouble {
    final private ConstantPoolReader cp;

    final private int index;

    private boolean isSet;

    protected LazyDouble(short opcode, ConstantPoolReader cp, int index) {
      super(opcode, 0.0);
      this.cp = cp;
      this.index = index;
      this.isSet = false;
    }

    @Override
    public double getDoubleValue() {
      if (!isSet) {
        value = cp.getConstantPoolDouble(index);
        isSet = true;
      }
      return value;
    }

    @Override
    public ConstantPoolReader getLazyConstantPool() {
      return cp;
    }

    @Override
    public int getCPIndex() {
      return index;
    }
  }

  static class ConstString extends ConstantInstruction {
    protected String value;

    protected ConstString(short opcode, String value) {
      super(opcode);
      this.value = value;
    }

    static ConstString makeInternal(String v) {
      return new ConstString(OP_ldc_w, v);
    }

    @Override
    public Object getValue() {
      return value;
    }

    @Override
    final public String getType() {
      return TYPE_String;
    }
  }

  final static class LazyString extends ConstString {
    final private ConstantPoolReader cp;

    final private int index;

    protected LazyString(short opcode, ConstantPoolReader cp, int index) {
      super(opcode, null);
      this.cp = cp;
      this.index = index;
    }

    @Override
    public Object getValue() {
      if (value == null) {
        value = cp.getConstantPoolString(index);
      }
      return value;
    }

    @Override
    public ConstantPoolReader getLazyConstantPool() {
      return cp;
    }

    @Override
    public int getCPIndex() {
      return index;
    }
  }

  static class ConstClass extends ConstantInstruction {
    protected String typeName;

    protected ConstClass(short opcode, String typeName) {
      super(opcode);
      this.typeName = typeName;
    }

    static ConstClass makeInternal(String v) {
      return new ConstClass(OP_ldc_w, v);
    }

    @Override
    public Object getValue() {
      return new ClassToken(typeName);
    }

    @Override
    final public String getType() {
      return TYPE_Class;
    }

    @Override
    public boolean isPEI() {
      // load of a class constant may trigger a ClassNotFoundException
      return true;
    }
  }

  final static class LazyClass extends ConstClass {
    final private ConstantPoolReader cp;

    final private int index;

    protected LazyClass(short opcode, ConstantPoolReader cp, int index) {
      super(opcode, null);
      this.cp = cp;
      this.index = index;
    }

    @Override
    public Object getValue() {
      if (typeName == null) {
        typeName = cp.getConstantPoolClassType(index);
      }
      return new ClassToken(typeName);
    }

    @Override
    public ConstantPoolReader getLazyConstantPool() {
      return cp;
    }

    @Override
    public int getCPIndex() {
      return index;
    }
  }

  static class ConstMethodType extends ConstantInstruction {
    protected String descriptor;
    
    ConstMethodType(short opcode, String descriptor) {
      super(opcode);
      this.descriptor = descriptor;
    }

    @Override
    public Object getValue() {
      return descriptor;
    }

    @Override
    public String getType() {
      return TYPE_String;
    }
  }
  
  static class LazyMethodType extends ConstMethodType {
    final private ConstantPoolReader cp;

    final private int index;

    LazyMethodType(short opcode, ConstantPoolReader cp, int index) {
      super(opcode, null);
      this.cp = cp;
      this.index = index;
    }
    
    @Override
    public Object getValue() {
      if (descriptor == null) {
        descriptor = cp.getConstantPoolMethodType(index);
      }
      return descriptor;
    }

    @Override
    public ConstantPoolReader getLazyConstantPool() {
      return cp;
    }

    @Override
    public int getCPIndex() {
      return index;
    }
  }
  
  static class ConstMethodHandle extends ConstantInstruction {
    protected Object value;
    
    public ConstMethodHandle(short opcode, Object value) {
      super(opcode);
      this.value = value;
    }

    @Override
    public Object getValue() {
      return value;
    }

    @Override
    public String getType() {
      return TYPE_MethodHandle;
    }
    
  }
  
  static class LazyMethodHandle extends ConstMethodHandle {
    final private ConstantPoolReader cp;

    final private int index;

    LazyMethodHandle(short opcode, ConstantPoolReader cp, int index) {
      super(opcode, null);
      this.cp = cp;
      this.index = index;
    }
    
    @Override
    public Object getValue() {
      if (value == null) {
        String className = cp.getConstantPoolHandleClassType(getCPIndex());
        String eltName = cp.getConstantPoolHandleName(getCPIndex());
        String eltDesc = cp.getConstantPoolHandleType(getCPIndex());
        value = new ReferenceToken(className, eltName, eltDesc);
      }
      return value;
    }

    @Override
    public ConstantPoolReader getLazyConstantPool() {
      return cp;
    }

    @Override
    public int getCPIndex() {
      return index;
    }
  }
  
  /**
   * @return the constant value pushed: an Integer, a Long, a Float, a Double, a String, or null
   */
  public abstract Object getValue();

  /**
   * @return the type of the value pushed
   */
  public abstract String getType();

  public static ConstantInstruction make(String type, Object constant) throws IllegalArgumentException {
    if (type == null && constant != null) {
      throw new IllegalArgumentException("(type == null) and (constant != null)");
    }
    if (constant == null) {
      return ConstNull.makeInternal();
    } else if (type.equals(TYPE_String)) {
      return makeString((String) constant);
    } else if (type.equals(TYPE_Class)) {
      return makeClass((String) constant);
    } else {
      try {
        switch (Util.getTypeIndex(type)) {
        case TYPE_int_index:
          return make(((Number) constant).intValue());
        case TYPE_long_index:
          return make(((Number) constant).longValue());
        case TYPE_float_index:
          return make(((Number) constant).floatValue());
        case TYPE_double_index:
          return make(((Number) constant).doubleValue());
        default:
          throw new IllegalArgumentException("Invalid type for constant: " + type);
        }
      } catch (ClassCastException e) {
        throw new IllegalArgumentException(e);
      }
    }
  }

  public static ConstantInstruction make(int i) {
    return ConstInt.makeInternal(i);
  }

  public static ConstantInstruction make(long l) {
    return ConstLong.makeInternal(l);
  }

  public static ConstantInstruction make(float f) {
    return ConstFloat.makeInternal(f);
  }

  public static ConstantInstruction make(double d) {
    return ConstDouble.makeInternal(d);
  }

  public static ConstantInstruction makeString(String s) {
    return s == null ? (ConstantInstruction) ConstNull.makeInternal() : (ConstantInstruction) ConstString.makeInternal(s);
  }

  public static ConstantInstruction makeClass(String s) {
    return ConstClass.makeInternal(s);
  }

  static ConstantInstruction make(ConstantPoolReader cp, int index) {
    switch (cp.getConstantPoolItemType(index)) {
    case CONSTANT_Integer:
      return new LazyInt(OP_ldc_w, cp, index);
    case CONSTANT_Long:
      return new LazyLong(OP_ldc2_w, cp, index);
    case CONSTANT_Float:
      return new LazyFloat(OP_ldc_w, cp, index);
    case CONSTANT_Double:
      return new LazyDouble(OP_ldc2_w, cp, index);
    case CONSTANT_String:
      return new LazyString(OP_ldc_w, cp, index);
    case CONSTANT_Class:
      return new LazyClass(OP_ldc_w, cp, index);
    case CONSTANT_MethodHandle:
      return new LazyMethodHandle(OP_ldc_w, cp, index);
    case CONSTANT_MethodType:
      return new LazyMethodType(OP_ldc_w, cp, index);
    default:
      return null;
    }
  }

  @Override
  final public boolean equals(Object o) {
    if (o instanceof ConstantInstruction) {
      ConstantInstruction i = (ConstantInstruction) o;
      if (!i.getType().equals(getType())) {
        return false;
      }
      if (i.getValue() == null) {
        if (getValue() == null) {
          return true;
        } else {
          return false;
        }
      } else {
        if (getValue() == null) {
          return false;
        } else {
          return i.getValue().equals(getValue());
        }
      }
    } else {
      return false;
    }
  }

  @Override
  final public String getPushedType(String[] types) {
    return getType();
  }

  @Override
  final public byte getPushedWordSize() {
    return Util.getWordSize(getType());
  }

  @Override
  final public int hashCode() {
    int v = getValue() == null ? 0 : getValue().hashCode();
    return getType().hashCode() + 14411 * v;
  }

  @Override
  final public void visit(IInstruction.Visitor v) throws NullPointerException {
    v.visitConstant(this);
  }

  private static String quote(Object o) {
    if (o instanceof String) {
      String s = (String) o;
      StringBuffer buf = new StringBuffer("\"");
      int len = s.length();
      for (int i = 0; i < len; i++) {
        char ch = s.charAt(i);
        switch (ch) {
        case '"':
          buf.append('\\');
          buf.append(ch);
          break;
        case '\n':
          buf.append("\\\n");
          break;
        case '\t':
          buf.append("\\\t");
          break;
        default:
          buf.append(ch);
        }
      }
      buf.append("\"");
      return buf.toString();
    } else if (o == null) {
      return "null";
    } else {
      return o.toString();
    }
  }

  @Override
  final public String toString() {
    return "Constant(" + getType() + "," + quote(getValue()) + ")";
  }

  @Override
  public boolean isPEI() {
    return false;
  }
}
