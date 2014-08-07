package com.ibm.wala.shrikeCT;

import com.ibm.wala.shrikeCT.ClassReader.AttrIterator;
import com.ibm.wala.shrikeCT.ConstantPoolParser.ReferenceToken;

public class BootstrapMethodsReader extends AttributeReader {

  public interface BootstrapMethod {
    int invokeType();
    String methodClass();
    String methodName();
    String methodType();
    Object callArgument(int i);
    int callArgumentKind(int i);
  }
  
  private BootstrapMethod entries[];
  
  protected BootstrapMethodsReader(AttrIterator attr) throws InvalidClassFileException {
    super(attr, "BootstrapMethods");
    readBootstrapEntries();
  }

  private void readBootstrapEntries() throws InvalidClassFileException {
    final ConstantPoolParser cp = cr.getCP();

    entries = new BootstrapMethod[cr.getUShort(attr + 6)];
    int base = 8;
    for(int i = 0; i < entries.length; i++) {
      final int methodHandleOffset = cr.getUShort(attr + base);
      final int argsBase = attr + base + 4;
      
      final int argumentCount = cr.getUShort(attr + base + 2);
      entries[i] = new BootstrapMethod() {
        private final int invokeType = cp.getCPHandleKind(methodHandleOffset);
        private final String methodClass = cp.getCPHandleClass(methodHandleOffset);
        private final String methodName = cp.getCPHandleName(methodHandleOffset);
        private final String methodType = cp.getCPHandleType(methodHandleOffset);
        
        @Override
        public String toString() {
          return methodClass + ":" + methodName + methodType;
        }
        
        @Override
        public int invokeType() {
          return invokeType;
        }

        @Override
        public String methodClass() {
          return methodClass;
        }

        @Override
        public String methodName() {
          return methodName;
        }

        @Override
        public String methodType() {
          return methodType;
        }

        @Override
        public int callArgumentKind(int i) {
          assert 0 <= i && i < argumentCount;
          int index = argsBase + (2*i);
          return cp.getItemType(cr.getUShort(index));
        }
        
        @Override
        public Object callArgument(int i) {
          try {
            int index = cr.getUShort(argsBase + (2*i));
            int t = callArgumentKind(i);
            switch (t) {
            case ClassConstants.CONSTANT_Utf8:
              return cp.getCPUtf8(index);
            case ClassConstants.CONSTANT_Class:
              return cp.getCPClass(index);
            case ClassConstants.CONSTANT_String:
              return cp.getCPString(index);
            case ClassConstants.CONSTANT_Integer:
              return cp.getCPInt(index);
            case ClassConstants.CONSTANT_Float:
              return cp.getCPFloat(index);
            case ClassConstants.CONSTANT_Double:
              return cp.getCPDouble(index);
            case ClassConstants.CONSTANT_Long:
              return cp.getCPLong(index);
            case ClassConstants.CONSTANT_MethodHandle:
              String className = cp.getCPHandleClass(index);
              String eltName = cp.getCPHandleName(index);
              String eltDesc = cp.getCPHandleType(index);
              return new ReferenceToken(className, eltName, eltDesc);
            case ClassConstants.CONSTANT_MethodType:
              return cp.getCPMethodType(index);
            default:
              assert false : "invalid type " + t;
            }            
          } catch (IllegalArgumentException e) {
            assert false : e;
          } catch (InvalidClassFileException e) {
            assert false : e;
          }
          return null;
        }
      };
      
      base += (argumentCount*2) + 4;
    }
  }

  public int count() {
    return entries.length;
  }
  
  public BootstrapMethod getEntry(int i) {
    return entries[i];
  }
}
