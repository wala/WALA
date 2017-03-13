package com.ibm.wala.shrikeCT;

import static com.ibm.wala.shrikeBT.Constants.TYPE_double;
import static com.ibm.wala.shrikeBT.Constants.TYPE_float;
import static com.ibm.wala.shrikeBT.Constants.TYPE_int;
import static com.ibm.wala.shrikeBT.Constants.TYPE_long;
import static com.ibm.wala.shrikeBT.Constants.TYPE_null;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import com.ibm.wala.shrikeBT.Compiler.Output;
import com.ibm.wala.shrikeBT.GotoInstruction;
import com.ibm.wala.shrikeBT.IInstruction;
import com.ibm.wala.shrikeBT.MethodData;
import com.ibm.wala.shrikeBT.analysis.Analyzer;
import com.ibm.wala.shrikeBT.analysis.Analyzer.FailureException;
import com.ibm.wala.shrikeBT.analysis.ClassHierarchyProvider;
import com.ibm.wala.shrikeBT.analysis.Verifier;
import com.ibm.wala.shrikeCT.ClassWriter.Element;
import com.ibm.wala.shrikeCT.StackMapConstants.Item;
import com.ibm.wala.shrikeCT.StackMapConstants.ObjectType;
import com.ibm.wala.shrikeCT.StackMapConstants.StackMapFrame;
import com.ibm.wala.shrikeCT.StackMapConstants.StackMapType;
import com.ibm.wala.shrikeCT.StackMapConstants.UninitializedType;
import com.ibm.wala.util.collections.HashMapFactory;

public class StackMapTableWriter extends Element {
  private final byte[] data;
    
  public StackMapTableWriter(ClassWriter writer, List<StackMapFrame> frames) throws IOException {
    this.data = serialize(writer, frames);
  }
  
  private byte[] serialize(ClassWriter writer, List<StackMapFrame> frames) throws IOException {
    ByteArrayOutputStream data = new ByteArrayOutputStream();
    
    for(StackMapFrame frame : frames) {
      frame.write(data, writer);
    }
    
    ByteArrayOutputStream bs = new ByteArrayOutputStream();
    writeUShort(bs, writer.addCPUtf8("StackMapTable"));
    writeInt(bs, data.size() + 2);
    writeUShort(bs, frames.size());
    data.writeTo(bs);
    
    return bs.toByteArray();

  }
  
  public StackMapTableWriter(ClassWriter writer, MethodData method, Output output, ClassHierarchyProvider cha, String[][] vars) throws FailureException, IOException {
    this(writer, stackMapTable(writer, method, output, cha, vars, null));
  }

  public StackMapTableWriter(ClassWriter writer, MethodData method, Output output, ClassHierarchyProvider cha, String[][] vars, List<StackMapFrame> reuseFrames) throws FailureException, IOException {
    this(writer, stackMapTable(writer, method, output, cha, vars, reuseFrames));
  }

  private static List<StackMapFrame> remapStackFrames(List<StackMapFrame> sm, int[] newBytecodesToOldBytecodes) {
    // mapping to new bytecode
    Map<Integer,Integer> oldToNew = HashMapFactory.make();
    for(int i = newBytecodesToOldBytecodes.length - 1; i >= 0; i--) {
      oldToNew.put(newBytecodesToOldBytecodes[i], i);
    }
    
    // positions of frames
    int i = 1;
    int positions[] = new int[ sm.size()];
    Iterator<StackMapFrame> sms = sm.iterator();
    int position = sms.next().getOffset();
    positions[0] = oldToNew.get(position);
    while (sms.hasNext()) {
      position = position + sms.next().getOffset() + 1;
      positions[i++] = oldToNew.get(position);
    }
    
    // positions turned into offsets
    for(i = positions.length-1; i > 0; i--) {
      positions[i] = positions[i] - positions[i-1] - 1;
    }
    
    // frames with new offsets
    List<StackMapFrame> newFrames = new ArrayList<>(sm.size());
    for(i = 0; i < sm.size(); i++) {
      newFrames.add(new StackMapFrame(sm.get(i), positions[i]));
    }
    
    return newFrames;
  }

  public StackMapTableWriter(ClassWriter w, List<StackMapFrame> sm, int[] newBytecodesToOldBytecodes) throws IOException {
    this(w, remapStackFrames(sm, newBytecodesToOldBytecodes));
  }

  static StackMapType item(String type) {
    if (type == null) {
      return Item.ITEM_Top;
    } else if (type.equals(TYPE_null)) {
      return Item.ITEM_Null;
    } else if (type.equals(Analyzer.topType)) {
      return Item.ITEM_Top;
    } else if (type.equals(Analyzer.thisType)) {
      return Item.ITEM_UninitializedThis;
    } else if (type.equals(TYPE_int)) {
      return Item.ITEM_Integer;
    } else if (type.equals(TYPE_float)) {
      return Item.ITEM_Float;
    } else if (type.equals(TYPE_double)) {
      return Item.ITEM_Double;
    } else if (type.equals(TYPE_long)) {
      return Item.ITEM_Long;
    } else {
      if (type.startsWith("#")) {
        return new UninitializedType(type);        
      } else {
        return new ObjectType(type);
      }
    }
  }
  
  static void writeUByte(OutputStream s, int v) throws IOException {
    byte bytes[] = new byte[1];
    ClassWriter.setUByte(bytes, 0, v);
    s.write(bytes);
  }

  static void writeUShort(OutputStream s, int v) throws IOException {
    byte bytes[] = new byte[2];
    ClassWriter.setUShort(bytes, 0, v);
    s.write(bytes);
  }

  static void writeInt(OutputStream s, int v) throws IOException {
    byte bytes[] = new byte[4];
    ClassWriter.setInt(bytes, 0, v);
    s.write(bytes);
  }

  static StackMapType[] trim(StackMapType[] types) {
    int i = types.length-1;
    while (i >= 0 && (types[i] == null || types[i] == Item.ITEM_Null || types[i] == Item.ITEM_Top)) {
      i--;
    }
    
    if (i < 0) {
      return new StackMapType[0];
    } else if (i < types.length-1) {
      StackMapType[] trimmed = new StackMapType[ i+1 ];
      System.arraycopy(types, 0, trimmed, 0, i+1);
      return trimmed;
    } else {
      return types;
    }
  }
  
  private static String hackUnknown(String type) {
    if (type == null) {
      return type;
    } else if (type.startsWith("[")) {
      return "[" + hackUnknown(type.substring(1));
    } else if ("L?;".equals(type)) {
      return "Ljava/lang/Object;";
    } else {
      return type;
    }
  }
  
  static StackMapType[] types(String[] types, boolean locals) {
    StackMapType[] stackTypes = new StackMapType[ types.length ];
    
    int x = 0;
    for(int j = 0; j < types.length; j++) {
      StackMapType stackType = item(hackUnknown(types[j]));
      stackTypes[x++] = stackType;
      if (locals && stackType.size() == 2) {
        j++;
      }
    }
 
    return trim(stackTypes);
  }
  
  private static boolean isUselessGoto(IInstruction inst, int index) {
    if (inst instanceof GotoInstruction) {
      if (((GotoInstruction)inst).getBranchTargets()[0] == index+1) {
        return true;
      }
    }
    
    return false;
  }
  
  public static List<StackMapFrame> stackMapTable(ClassWriter writer, MethodData method, Output output, ClassHierarchyProvider cha, String[][] vars, List<StackMapFrame> reuseFrames) throws FailureException, IOException {
    int idx = 0;
    
    List<StackMapFrame> frames = new ArrayList<>();
    
    int[] instructionToBytecode = output.getInstructionOffsets();
    IInstruction[] insts = method.getInstructions();
    
    Verifier typeChecker = new Verifier(method, instructionToBytecode, vars);
    if (cha != null) {
      typeChecker.setClassHierarchy(cha);
    }
    typeChecker.computeTypes();
    BitSet bbs = typeChecker.getBasicBlockStarts();
    
    int offset = 0;
    for(int i = 1; i < insts.length; i++) {
      if (bbs.get(i)) {

        // Shrike does not generate goto i+1
        if (isUselessGoto(insts[i], i)) {
          continue;
        }
        
        // offset delta
        int position = instructionToBytecode[i];
        assert position - offset > 0 || offset == 0;        
        int frameOffset =  offset==0? position: position - offset - 1;
        offset = position;
        
        if (reuseFrames != null) {
          if (idx < reuseFrames.size() && reuseFrames.get(idx).getOffset() == frameOffset) {
            frames.add(reuseFrames.get(idx++));
            continue;
          } else {
            reuseFrames = null;
          } 
        } 
        
        // full frame
        byte frameType = (byte)255;

        // locals
        String[] localTypes = typeChecker.getLocalTypes()[i];
        StackMapType[] localWriteTypes;
        if (localTypes != null) {
          localWriteTypes = types(localTypes, true);
        } else {
          localWriteTypes = new StackMapType[0]; 
        }
        
        // stack
        String[] stackTypes = typeChecker.getStackTypes()[i];
        StackMapType[] stackWriteTypes;
        if (stackTypes != null) {
          stackWriteTypes = types(stackTypes, false);
        } else {
          stackWriteTypes = new StackMapType[0];         
        }

        frames.add(new StackMapFrame(frameType, frameOffset, localWriteTypes, stackWriteTypes));
      }
    }
    
    return frames;
  }
 
  @Override
  public int getSize() {
    return data.length;
  }

  @Override
  public int copyInto(byte[] buf, int offset) {
    for(int i = 0; i < data.length; i++) {
      buf[offset+i] = data[i];
    }
    return data.length+offset;
  }

}
