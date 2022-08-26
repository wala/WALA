package com.ibm.wala.dalvik.classLoader;

import static org.jf.dexlib2.ValueType.ANNOTATION;
import static org.jf.dexlib2.ValueType.ARRAY;
import static org.jf.dexlib2.ValueType.BOOLEAN;
import static org.jf.dexlib2.ValueType.BYTE;
import static org.jf.dexlib2.ValueType.CHAR;
import static org.jf.dexlib2.ValueType.DOUBLE;
import static org.jf.dexlib2.ValueType.ENUM;
import static org.jf.dexlib2.ValueType.FIELD;
import static org.jf.dexlib2.ValueType.FLOAT;
import static org.jf.dexlib2.ValueType.INT;
import static org.jf.dexlib2.ValueType.LONG;
import static org.jf.dexlib2.ValueType.METHOD;
import static org.jf.dexlib2.ValueType.NULL;
import static org.jf.dexlib2.ValueType.SHORT;
import static org.jf.dexlib2.ValueType.STRING;
import static org.jf.dexlib2.ValueType.TYPE;

import com.ibm.wala.core.util.strings.Atom;
import com.ibm.wala.core.util.strings.ImmutableByteArray;
import com.ibm.wala.shrike.shrikeCT.AnnotationsReader.AnnotationAttribute;
import com.ibm.wala.shrike.shrikeCT.AnnotationsReader.ArrayElementValue;
import com.ibm.wala.shrike.shrikeCT.AnnotationsReader.ConstantElementValue;
import com.ibm.wala.shrike.shrikeCT.AnnotationsReader.ElementValue;
import com.ibm.wala.shrike.shrikeCT.AnnotationsReader.EnumElementValue;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import org.jf.dexlib2.iface.AnnotationElement;
import org.jf.dexlib2.iface.value.AnnotationEncodedValue;
import org.jf.dexlib2.iface.value.ArrayEncodedValue;
import org.jf.dexlib2.iface.value.BooleanEncodedValue;
import org.jf.dexlib2.iface.value.ByteEncodedValue;
import org.jf.dexlib2.iface.value.CharEncodedValue;
import org.jf.dexlib2.iface.value.DoubleEncodedValue;
import org.jf.dexlib2.iface.value.EncodedValue;
import org.jf.dexlib2.iface.value.EnumEncodedValue;
import org.jf.dexlib2.iface.value.FieldEncodedValue;
import org.jf.dexlib2.iface.value.FloatEncodedValue;
import org.jf.dexlib2.iface.value.IntEncodedValue;
import org.jf.dexlib2.iface.value.LongEncodedValue;
import org.jf.dexlib2.iface.value.MethodEncodedValue;
import org.jf.dexlib2.iface.value.ShortEncodedValue;
import org.jf.dexlib2.iface.value.StringEncodedValue;
import org.jf.dexlib2.iface.value.TypeEncodedValue;

public class DexUtil {

  static Collection<Annotation> getAnnotations(
      Collection<org.jf.dexlib2.iface.Annotation> as, ClassLoaderReference clr) {
    Collection<Annotation> result = HashSetFactory.make();
    for (org.jf.dexlib2.iface.Annotation a : as) {
      result.add(getAnnotation(a, clr));
    }
    return result;
  }

  static Annotation getAnnotation(org.jf.dexlib2.iface.Annotation ea, ClassLoaderReference clr) {
    Map<String, ElementValue> values = HashMapFactory.make();
    TypeReference at = getTypeRef(ea.getType(), clr);

    for (AnnotationElement elt : ea.getElements()) {
      String name = elt.getName();
      EncodedValue v = elt.getValue();
      ElementValue value = getValue(clr, v);
      values.put(name, value);
    }

    return Annotation.makeWithNamed(at, values);
  }

  static ElementValue getValue(ClassLoaderReference clr, EncodedValue v) {
    switch (v.getValueType()) {
      case ANNOTATION:
        {
          Map<String, ElementValue> values = HashMapFactory.make();
          String at = ((AnnotationEncodedValue) v).getType();

          for (AnnotationElement elt : ((AnnotationEncodedValue) v).getElements()) {
            String name = elt.getName();
            EncodedValue ev = elt.getValue();
            ElementValue value = getValue(clr, ev);
            values.put(name, value);
          }

          return new AnnotationAttribute(at, values);
        }

      case ARRAY:
        {
          List<? extends EncodedValue> vs = ((ArrayEncodedValue) v).getValue();
          ElementValue[] rs = new ElementValue[vs.size()];
          int idx = 0;
          for (EncodedValue ev : vs) {
            rs[idx++] = getValue(clr, ev);
          }
          return new ArrayElementValue(rs);
        }

      case BOOLEAN:
        Boolean bl = ((BooleanEncodedValue) v).getValue();
        return new ConstantElementValue(bl);

      case BYTE:
        Byte bt = ((ByteEncodedValue) v).getValue();
        return new ConstantElementValue(bt);

      case CHAR:
        Character c = ((CharEncodedValue) v).getValue();
        return new ConstantElementValue(c);

      case DOUBLE:
        Double d = ((DoubleEncodedValue) v).getValue();
        return new ConstantElementValue(d);

      case ENUM:
        org.jf.dexlib2.iface.reference.FieldReference o = ((EnumEncodedValue) v).getValue();
        return new EnumElementValue(o.getType(), o.getName());

      case FIELD:
        o =
            v.getValueType() == ENUM
                ? ((EnumEncodedValue) v).getValue()
                : ((FieldEncodedValue) v).getValue();
        String fieldName = o.getName();
        TypeReference ft = getTypeRef(o.getType(), clr);
        TypeReference ct = getTypeRef(o.getDefiningClass(), clr);
        return new ConstantElementValue(
            FieldReference.findOrCreate(ct, Atom.findOrCreateUnicodeAtom(fieldName), ft));

      case FLOAT:
        Float f = ((FloatEncodedValue) v).getValue();
        return new ConstantElementValue(f);

      case INT:
        Integer iv = ((IntEncodedValue) v).getValue();
        return new ConstantElementValue(iv);

      case LONG:
        Long l = ((LongEncodedValue) v).getValue();
        return new ConstantElementValue(l);

      case METHOD:
        org.jf.dexlib2.iface.reference.MethodReference m = ((MethodEncodedValue) v).getValue();
        ct = getTypeRef(m.getDefiningClass(), clr);
        String methodName = m.getName();
        String methodSig = getSignature(m);
        return new ConstantElementValue(
            MethodReference.findOrCreate(
                ct,
                Atom.findOrCreateUnicodeAtom(methodName),
                Descriptor.findOrCreateUTF8(methodSig)));

      case NULL:
        return new ConstantElementValue(null);

      case SHORT:
        Short s = ((ShortEncodedValue) v).getValue();
        return new ConstantElementValue(s);

      case STRING:
        String str = ((StringEncodedValue) v).getValue();
        return new ConstantElementValue(str);

      case TYPE:
        String t = ((TypeEncodedValue) v).getValue();
        return new ConstantElementValue(getTypeName(t) + ";");

      default:
        assert false : v;
        return null;
    }
  }

  static String getSignature(org.jf.dexlib2.iface.reference.MethodReference ref) {
    StringBuilder sig = new StringBuilder("(");
    for (CharSequence p : ref.getParameterTypes()) {
      sig = sig.append(p);
    }
    sig.append(')').append(ref.getReturnType());
    return sig.toString();
  }

  static TypeReference getTypeRef(String type, ClassLoaderReference clr) {
    return TypeReference.findOrCreate(clr, getTypeName(type));
  }

  static TypeName getTypeName(String fieldType) {
    ImmutableByteArray fieldTypeArray = ImmutableByteArray.make(fieldType);
    TypeName T = null;
    if (fieldTypeArray.get(fieldTypeArray.length() - 1) == ';') {
      T = TypeName.findOrCreate(fieldTypeArray, 0, fieldTypeArray.length() - 1);
    } else {
      T = TypeName.findOrCreate(fieldTypeArray);
    }
    return T;
  }
}
