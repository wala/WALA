package com.ibm.wala.dalvik.classLoader;

import java.util.Collection;
import java.util.Map;

import org.jf.dexlib.AnnotationItem;
import org.jf.dexlib.FieldIdItem;
import org.jf.dexlib.MethodIdItem;
import org.jf.dexlib.TypeIdItem;
import org.jf.dexlib.EncodedValue.AnnotationEncodedSubValue;
import org.jf.dexlib.EncodedValue.ArrayEncodedValue;
import org.jf.dexlib.EncodedValue.BooleanEncodedValue;
import org.jf.dexlib.EncodedValue.ByteEncodedValue;
import org.jf.dexlib.EncodedValue.CharEncodedValue;
import org.jf.dexlib.EncodedValue.DoubleEncodedValue;
import org.jf.dexlib.EncodedValue.EncodedValue;
import org.jf.dexlib.EncodedValue.EnumEncodedValue;
import org.jf.dexlib.EncodedValue.FieldEncodedValue;
import org.jf.dexlib.EncodedValue.FloatEncodedValue;
import org.jf.dexlib.EncodedValue.IntEncodedValue;
import org.jf.dexlib.EncodedValue.LongEncodedValue;
import org.jf.dexlib.EncodedValue.MethodEncodedValue;
import org.jf.dexlib.EncodedValue.ShortEncodedValue;
import org.jf.dexlib.EncodedValue.StringEncodedValue;
import org.jf.dexlib.EncodedValue.TypeEncodedValue;
import org.jf.dexlib.EncodedValue.ValueType;

import com.ibm.wala.shrikeCT.AnnotationsReader.AnnotationAttribute;
import com.ibm.wala.shrikeCT.AnnotationsReader.ArrayElementValue;
import com.ibm.wala.shrikeCT.AnnotationsReader.ConstantElementValue;
import com.ibm.wala.shrikeCT.AnnotationsReader.ElementValue;
import com.ibm.wala.shrikeCT.AnnotationsReader.EnumElementValue;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.types.annotations.Annotation;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.strings.Atom;
import com.ibm.wala.util.strings.ImmutableByteArray;

public class DexUtil {

	static Collection<Annotation> getAnnotations(Collection<AnnotationItem> as, ClassLoaderReference clr) {
		Collection<Annotation> result = HashSetFactory.make();
		for(AnnotationItem a : as) {
			result.add(getAnnotation(a, clr));
		}
		return result;
	}

	static Annotation getAnnotation(AnnotationItem a, ClassLoaderReference clr) {
		return getAnnotation(a.getEncodedAnnotation(), clr);
	}

	static Annotation getAnnotation(AnnotationEncodedSubValue ea, ClassLoaderReference clr) {
		Map<String,ElementValue> values = HashMapFactory.make();
		TypeReference at = getTypeRef(ea.annotationType, clr);

		for(int i = 0; i < ea.names.length; i++) {
			String name = ea.names[i].getStringValue();
			EncodedValue v = ea.values[i];
			ElementValue value = getValue(clr, v);
			values.put(name, value);
		}
		
		return Annotation.makeWithNamed(at, values);
	}

	static ElementValue getValue(ClassLoaderReference clr, EncodedValue v) {
		switch (v.getValueType()) {
		case VALUE_ANNOTATION:
			Annotation a = getAnnotation((AnnotationEncodedSubValue)v, clr);
			return new AnnotationAttribute(a.getType().getName().toString() +";", a.getNamedArguments());
			
		case VALUE_ARRAY:
			EncodedValue[] vs = ((ArrayEncodedValue)v).values;
			ElementValue rs[] = new ElementValue[ vs.length ];
			for(int idx = 0; idx < vs.length; idx++) {
				rs[idx] = getValue(clr, vs[idx]);
			}
			return new ArrayElementValue(rs);
			
		case VALUE_BOOLEAN:
			Boolean bl = ((BooleanEncodedValue)v).value;
			return new ConstantElementValue(bl);
			
		case VALUE_BYTE:
			Byte bt = ((ByteEncodedValue)v).value;
			return new ConstantElementValue(bt);
			
		case VALUE_CHAR:
			Character c = ((CharEncodedValue)v).value;
			return new ConstantElementValue(c);
			
		case VALUE_DOUBLE:
			Double d = ((DoubleEncodedValue)v).value;
			return new ConstantElementValue(d);
			
		case VALUE_ENUM:
			FieldIdItem o = ((EnumEncodedValue)v).value;
			return new EnumElementValue(o.getFieldType().getTypeDescriptor(), o.getFieldName().getStringValue());
			
		case VALUE_FIELD:
			o = v.getValueType()==ValueType.VALUE_ENUM? ((EnumEncodedValue)v).value: ((FieldEncodedValue)v).value;
			String fieldName = o.getFieldName().getStringValue();
			TypeReference ft = getTypeRef(o.getFieldType(), clr);
			TypeReference ct = getTypeRef(o.getContainingClass(), clr);
			return new ConstantElementValue(FieldReference.findOrCreate(ct, Atom.findOrCreateUnicodeAtom(fieldName), ft));
						
		case VALUE_FLOAT:
			Float f = ((FloatEncodedValue)v).value;
			return new ConstantElementValue(f);
			
		case VALUE_INT:
			Integer iv = ((IntEncodedValue)v).value;
			return new ConstantElementValue(iv);
			
		case VALUE_LONG:
			Long l = ((LongEncodedValue)v).value;
			return new ConstantElementValue(l);
			
		case VALUE_METHOD:
			MethodIdItem m = ((MethodEncodedValue)v).value;
			ct = getTypeRef(m.getContainingClass(), clr);
			String methodName = m.getMethodName().getStringValue();
			String methodSig = m.getPrototype().getPrototypeString();
			return new ConstantElementValue(MethodReference.findOrCreate(ct, Atom.findOrCreateUnicodeAtom(methodName), Descriptor.findOrCreateUTF8(methodSig)));
			
		case VALUE_NULL:
			return new ConstantElementValue(null);
			
		case VALUE_SHORT:
			Short s = ((ShortEncodedValue)v).value;
			return new ConstantElementValue(s);

		case VALUE_STRING:
			String str = ((StringEncodedValue)v).value.getStringValue();
			return new ConstantElementValue(str);
			
		case VALUE_TYPE:
			TypeIdItem t = ((TypeEncodedValue)v).value;
			return new ConstantElementValue(getTypeName(t) + ";");
			
		default:
			assert false : v;
			return null;
		}
	}
	
	static TypeReference getTypeRef(TypeIdItem type, ClassLoaderReference clr) {
		return TypeReference.findOrCreate(clr, getTypeName(type));
	}
	
	static TypeName getTypeName(TypeIdItem fieldType) {
		ImmutableByteArray fieldTypeArray = ImmutableByteArray.make(fieldType.getTypeDescriptor());
	    TypeName T = null;
	    if (fieldTypeArray.get(fieldTypeArray.length() - 1) == ';') {
	        T = TypeName.findOrCreate(fieldTypeArray, 0, fieldTypeArray.length() - 1);
	    } else {
	        T = TypeName.findOrCreate(fieldTypeArray);
	    }
		return T;
	}

}
