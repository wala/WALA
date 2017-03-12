package com.ibm.wala.shrikeCT;

import java.util.ArrayList;
import java.util.List;

import com.ibm.wala.shrikeCT.ClassReader.AttrIterator;
import com.ibm.wala.shrikeCT.StackMapConstants.Item;
import com.ibm.wala.shrikeCT.StackMapConstants.ObjectType;
import com.ibm.wala.shrikeCT.StackMapConstants.StackMapFrame;
import com.ibm.wala.shrikeCT.StackMapConstants.StackMapType;
import com.ibm.wala.shrikeCT.StackMapConstants.UninitializedType;

public class StackMapTableReader extends AttributeReader {
  private List<StackMapFrame> frames;
  
  public List<StackMapFrame> frames() {
    return frames;
  }
  
  private StackMapType item(int offset) throws InvalidClassFileException {
    Item item = StackMapConstants.items[cr.getByte(offset)];
    if (Item.ITEM_Uninitalized == item) {
      return new UninitializedType("#" + cr.getUShort(offset+1) + "#unknown");      
    } else if (Item.ITEM_Object == item) {
      return new ObjectType(cr.getCP().getCPClass(cr.getUShort(offset+1)));
    } else {
      return item;
    }
  }
  
  public StackMapTableReader(AttrIterator iter) throws InvalidClassFileException {
    super(iter, "StackMapTable");
    frames = new ArrayList<>();
    int entries = cr.getUShort(attr+6);
    int ptr = attr + 8;
    for(int i = 0; i < entries; i++) {
      int frameType = (0x000000ff & cr.getByte(ptr++));
      if (frameType < 64) {
        int offset = frameType;
        frames.add(new StackMapFrame(frameType, offset, new StackMapType[0], new StackMapType[0]));
      } else if (frameType < 128) {
        int offset = frameType - 64;
        StackMapType stack1 = item(ptr);
        ptr += (stack1 instanceof ObjectType)? 3: 1;
        frames.add(new StackMapFrame(frameType, offset, new StackMapType[0], new StackMapType[]{ stack1 }));        
      } else if (frameType == 247) {
        int offset = cr.getUShort(ptr); ptr += 2;
        StackMapType stack1 = item(ptr);
        ptr += (stack1 instanceof ObjectType)? 3: 1;
        frames.add(new StackMapFrame(frameType, offset, new StackMapType[0], new StackMapType[]{ stack1 }));                
      } else if (frameType >= 248 && frameType <= 250) {
        int offset = cr.getUShort(ptr); ptr += 2;
        frames.add(new StackMapFrame(frameType, offset, new StackMapType[0], new StackMapType[0]));        
      } else if (frameType == 251) {
        int offset = cr.getUShort(ptr); ptr += 2;
        frames.add(new StackMapFrame(frameType, offset, new StackMapType[0], new StackMapType[0]));                
      } else if (frameType >= 252 && frameType <= 254) {
        StackMapType[] locals = new StackMapType[ frameType - 251 ];
        int offset = cr.getUShort(ptr); ptr += 2;
        for(int j = 0; j < locals.length; j++) {
          locals[j] = item(ptr);
          ptr += (locals[j] instanceof ObjectType)? 3: 1;
        }
        frames.add(new StackMapFrame(frameType, offset, locals, new StackMapType[0]));                
      } else if (frameType == 255) {
        int offset = cr.getUShort(ptr); ptr += 2;
        
        int numLocals = cr.getUShort(ptr); ptr += 2;
        StackMapType[] locals = new StackMapType[ numLocals ];
        for(int j = 0; j < numLocals; j++) {
          locals[j] = item(ptr);
          ptr += (locals[j] instanceof ObjectType)? 3: 1;
        }
        
        int numStack = cr.getUShort(ptr); ptr += 2;
        StackMapType[] stack = new StackMapType[ numStack ];
        for(int j = 0; j < numStack; j++) {
          stack[j] = item(ptr);
          ptr += (stack[j].isObject())? 3: 1;
        }
        
        frames.add(new StackMapFrame(frameType, offset, locals, stack));                
      }
    }
  }
  
  public static List<StackMapFrame> readStackMap(CodeReader code) throws InvalidClassFileException, IllegalArgumentException {
    ClassReader.AttrIterator iter = new ClassReader.AttrIterator();
    code.initAttributeIterator(iter);
    for (; iter.isValid(); iter.advance()) {
      if (iter.getName().equals("StackMapTable")) {
        StackMapTableReader r = new StackMapTableReader(iter);
        return r.frames();
      }
    }
    
    return null;
  }
}
