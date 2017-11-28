package com.ibm.wala.shrikeCT;

import static com.ibm.wala.shrikeCT.StackMapTableWriter.writeUByte;
import static com.ibm.wala.shrikeCT.StackMapTableWriter.writeUShort;

import java.io.IOException;
import java.io.OutputStream;

import com.ibm.wala.shrikeBT.analysis.Analyzer;

public class StackMapConstants {

  interface StackMapType {
      void write(OutputStream s, ClassWriter writer) throws IOException;
      
      int size();
      
      boolean isObject();
  }
    
  public enum Item implements StackMapType {
    ITEM_Top(0),
    ITEM_Integer(1),
    ITEM_Float(2),
    ITEM_Double(3) {
      @Override
      public int size() {
        return 2;
      }
    },
    ITEM_Long(4) {
      @Override
      public int size() {
        return 2;
      }
    },
    ITEM_Null(5),
    ITEM_UninitializedThis(6),
    ITEM_Object(7) {
      @Override
      public boolean isObject() {
        return true;
      }
    },
    ITEM_Uninitalized(8) {
      @Override
      public boolean isObject() {
        return true;
      }
    };

    private final byte code;

    Item(int code) {
      this.code = (byte)code;
    }

    @Override
    public boolean isObject() {
      return false;
    }
    
    @Override
    public int size() {
      return 1;
    }
    
    @Override
    public void write(OutputStream s, ClassWriter writer) throws IOException {
      writeUByte(s, code);
    }
  }

  public static class UninitializedType implements StackMapType {
    private final String type;
    private final int offset;
    
    public UninitializedType(String type) {
      assert type.startsWith("#");
      this.type = Analyzer.stripSharp(type);
      this.offset = Integer.parseInt(type.substring(1, type.lastIndexOf('#')));
    }

    @Override
    public void write(OutputStream s, ClassWriter writer) throws IOException {
      Item.ITEM_Uninitalized.write(s, writer);
      writeUShort(s, offset);
    }

    @Override
    public int size() {
      return Item.ITEM_Uninitalized.size();
    }
    
    @Override
    public boolean isObject() {
      return true;
    }
    
    @Override
    public String toString() {
      return "uninit:" + type;
    }
  }
  
  public static class ObjectType implements StackMapType {
    private final String type;
     
    ObjectType(ClassReader cr, int typeIndex) throws IllegalArgumentException, InvalidClassFileException {
      this(cr.getCP().getCPString(typeIndex));
    }
    
    ObjectType(String type) {
      this.type = type;
    }

    @Override
    public int size() {
      return Item.ITEM_Object.size();
    }
    
    @Override
    public boolean isObject() {
      return true;
    }

    @Override
    public String toString() {
      return "obj:" + type;
    }
    
    @Override
    public void write(OutputStream s, ClassWriter writer) throws IOException {
      Item.ITEM_Object.write(s, writer);
      if ("L;".equals(type)) {
        writeUShort(s, writer.addCPClass("java/lang/Object"));        
      } else if (type.startsWith("L")) {
        writeUShort(s, writer.addCPClass(type.substring(1, type.length()-1)));
      } else {
        writeUShort(s, writer.addCPClass(type));
      }
    }
  }

  public static Item items[] = {
      Item.ITEM_Top,
      Item.ITEM_Integer,
      Item.ITEM_Float,
      Item.ITEM_Double,
      Item.ITEM_Long,
      Item.ITEM_Null,
      Item.ITEM_UninitializedThis,
      Item.ITEM_Object,
      Item.ITEM_Uninitalized
  };
  
  public static class StackMapFrame {

    public StackMapFrame(StackMapFrame frame, int newOffset) {
      this(frame.frameType, newOffset, frame.localTypes, frame.stackTypes);
    }

    public StackMapFrame(int frameType, int offset, StackMapType[] localTypes, StackMapType[] stackTypes) {
      this.frameType = frameType;
      this.offset = offset;
      this.localTypes = localTypes;
      this.stackTypes = stackTypes;
    }
    
    private final int frameType;
    private final int offset;
    private final StackMapType[] localTypes;    
    private final StackMapType[] stackTypes;
    
    public int getFrameType() {
      return frameType;
    }

    public int getOffset() {
      return offset;
    }

    public StackMapType[] getLocalTypes() {
      return localTypes;
    }

    public StackMapType[] getStackTypes() {
      return stackTypes;
    }

    @Override
    public String toString() {
      StringBuffer sb = new StringBuffer();
      sb.append("frame type: ").append(frameType).append("\n");
      sb.append("  offset: ").append(offset).append("\n");
      
      sb.append("  locals\n");
      for (StackMapType localType : localTypes) {
        sb.append("  ").append(localType).append("\n");
      }

      sb.append("  stack\n");
      for (StackMapType stackType : stackTypes) {
        sb.append("  ").append(stackType).append("\n");
      }

      return sb.toString();
    }
    
    public void write(OutputStream out, ClassWriter writer) throws IOException {
      // frame type
      writeUByte(out, frameType);
      
      // offset delta
      writeUShort(out, offset);
      
      // locals
      if (localTypes != null) {
        writeUShort(out, localTypes.length);          
        for(StackMapType type : localTypes) {
          type.write(out, writer);
        } 
      } else {
        writeUShort(out, 0);          
      }
      
      // stack
      if (stackTypes != null) {
        writeUShort(out, stackTypes.length);          
        for(int j = stackTypes.length; j > 0; ) {
          stackTypes[--j].write(out, writer);
        }  
      } else {
        writeUShort(out, 0);          
      }
    }
  }
}
