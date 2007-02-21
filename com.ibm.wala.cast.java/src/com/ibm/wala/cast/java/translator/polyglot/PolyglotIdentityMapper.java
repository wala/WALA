/**
 * 
 */
package com.ibm.wala.cast.java.translator.polyglot;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;

import polyglot.types.*;

import com.ibm.wala.cast.java.translator.polyglot.PolyglotJava2CAstTranslator.IdentityMapper;
import com.ibm.wala.cast.java.types.JavaPrimitiveTypeMap;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.Atom;
import com.ibm.wala.util.debug.Assertions;

/**
 * Class responsible for mapping Polyglot type system objects representing types,
 * methods and fields to the corresponding WALA TypeReferences, MethodReferences
 * and FieldReferences. Used during translation and by clients to help correlate
 * WALA analysis results to the various AST nodes.
 * @author rfuhrer
 */
public class PolyglotIdentityMapper implements IdentityMapper<Type,ProcedureInstance,FieldInstance> {
    private final Map<Type,TypeReference> fTypeMap= new HashMap<Type,TypeReference>();
    private final Map<FieldInstance,FieldReference> fFieldMap= new HashMap<FieldInstance,FieldReference>();
    private final Map<ProcedureInstance,MethodReference> fMethodMap= new HashMap<ProcedureInstance,MethodReference>();

    /**
     * Map from Polyglot local ClassTypes to their enclosing methods. Used by localTypeToTypeID().<br>
     * Needed since Polyglot doesn't provide this information. (It doesn't need to, since it
     * doesn't need to generate unambiguous names for such entities -- it hands the source
     * off to javac to generate bytecode. It probably also wouldn't want to, since that would
     * create back-pointers from Type objects in the TypeSystem to AST's.)
     */
    protected Map<ClassType,ProcedureInstance> fLocalTypeMap= new LinkedHashMap<ClassType,ProcedureInstance>();

    private final ClassLoaderReference fClassLoaderRef;
    private final TypeSystem fTypeSystem;

    public PolyglotIdentityMapper(ClassLoaderReference clr, TypeSystem ts) {
        fClassLoaderRef= clr;
        fTypeSystem= ts;
    }

    public FieldReference getFieldRef(FieldInstance field) {
        if (!fFieldMap.containsKey(field)) {
    	FieldReference ref= referenceForField(field);
    	fFieldMap.put(field, ref);
    	return ref;
        }
        return fFieldMap.get(field);
    }

    public TypeReference getTypeRef(Type type) {
        if (!fTypeMap.containsKey(type)) {
    	TypeReference ref= referenceForType(type);
    	fTypeMap.put(type, ref);
    	return ref;
        }
        return fTypeMap.get(type);
    }

    public MethodReference getMethodRef(ProcedureInstance method) {
        if (!fMethodMap.containsKey(method)) {
    	MethodReference sel= referenceForMethod(method);
    	fMethodMap.put(method, sel);
    	return sel;
        }
        return fMethodMap.get(method);
    }

    public void mapLocalAnonTypeToMethod(ClassType anonLocalType, ProcedureInstance owningProc) {
        fLocalTypeMap.put(anonLocalType, owningProc);
    }

    private FieldReference referenceForField(FieldInstance field) {
        Type targetType= field.container();
        Type fieldType= field.type();
        TypeReference targetTypeRef= TypeReference.findOrCreate(fClassLoaderRef, typeToTypeID(targetType));
        TypeReference fieldTypeRef= TypeReference.findOrCreate(fClassLoaderRef, typeToTypeID(fieldType));
        Atom fieldName= Atom.findOrCreateUnicodeAtom(field.name());
        FieldReference fieldRef= FieldReference.findOrCreate(targetTypeRef, fieldName, fieldTypeRef);

        return fieldRef;
    }

    private TypeReference referenceForType(Type type) {
        TypeName typeName= TypeName.string2TypeName(typeToTypeID(type));
        TypeReference typeRef= TypeReference.findOrCreate(fClassLoaderRef, typeName);

        return typeRef;
    }

    private Selector selectorForMethod(ProcedureInstance procInstance) {
        Atom name= (procInstance instanceof ConstructorInstance) ? MethodReference.initAtom : Atom
    	    .findOrCreateUnicodeAtom(((MethodInstance) procInstance).name());

        int numArgs= procInstance.formalTypes().size();
        TypeName[] argTypeNames= (numArgs == 0) ? null : new TypeName[numArgs]; // Descriptor prefers null to an empty array
        int i= 0;
        for(Iterator iter= procInstance.formalTypes().iterator(); iter.hasNext(); i++) {
    	Type argType= (Type) iter.next();
    	argTypeNames[i]= TypeName.string2TypeName(typeToTypeID(argType));
        }

        Type retType= (procInstance instanceof ConstructorInstance) ? fTypeSystem.Void() : ((MethodInstance) procInstance).returnType();
        TypeName retTypeName= TypeName.string2TypeName(typeToTypeID(retType));

        Descriptor desc= Descriptor.findOrCreate(argTypeNames, retTypeName);

        return new Selector(name, desc);
    }

    private MethodReference referenceForMethod(ProcedureInstance procInstance) {
        // Handles both ConstructorInstance's and MethodInstance's
        TypeName ownerType= TypeName.string2TypeName(typeToTypeID(((MemberInstance)procInstance).container()));
        TypeReference ownerTypeRef= TypeReference.findOrCreate(fClassLoaderRef, ownerType);
        MethodReference methodRef= MethodReference.findOrCreate(ownerTypeRef, selectorForMethod(procInstance));

        return methodRef;
    }

    /**
     * Translates the given Polyglot type to a name suitable for use in a DOMO TypeReference
     * (i.e. a bytecode-compliant type name).
     */
    public String typeToTypeID(Type type) {
        if (type.isPrimitive()) {
    	PrimitiveType ptype= (PrimitiveType) type;

    	return JavaPrimitiveTypeMap.getShortName(ptype.name());
        } else if (type.isArray()) {
    	ArrayType atype= (ArrayType) type;
    	return "[" + typeToTypeID(atype.base());
        } else if (type.isNull()) {
    	Assertions.UNREACHABLE("typeToTypeID() encountered a null type!");
    	return null;
        }
        Assertions._assert(type.isClass(), "typeToTypeID() encountered type that is neither primitive, array, nor class!");

        ClassType ctype= (ClassType) type;

        return (ctype.isLocal() || ctype.isAnonymous()) ? anonLocalTypeToTypeID(ctype) : composeDOMOTypeDescriptor(ctype);
    }

    public String anonLocalTypeToTypeID(ClassType ctype) {
        ProcedureInstance procInstance= (ProcedureInstance) fLocalTypeMap.get(ctype);

        String outerTypeID= typeToTypeID(ctype.outer());
        String shortName= (ctype.isAnonymous()) ? PolyglotJava2CAstTranslator.anonTypeName(ctype) : ctype.fullName();

        return outerTypeID + '/' + getMethodRef(procInstance).getSelector() + '/' + shortName;
    }

    public String composeDOMOTypeDescriptor(ClassType ctype) {
        return "L" + composeDOMOTypeName(ctype);
    }

    public String composeDOMOTypeName(ClassType ctype) {
        if (ctype.package_() != null) {
    	String packageName = ctype.package_().fullName();
        	
    	Assertions._assert( ctype.fullName().startsWith( packageName ) );
    	return packageName.replace('.','/') + "/" + ctype.fullName().substring( packageName.length()+1 ).replace('.','$'); 
        } else {
    	return ctype.fullName().replace('.', '$');
        }
    }
}
