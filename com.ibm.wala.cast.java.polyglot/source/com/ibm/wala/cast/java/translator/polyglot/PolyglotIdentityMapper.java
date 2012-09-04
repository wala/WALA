/**
 * 
 */
package com.ibm.wala.cast.java.translator.polyglot;

import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import polyglot.types.ArrayType;
import polyglot.types.ClassType;
import polyglot.types.CodeInstance;
import polyglot.types.ConstructorInstance;
import polyglot.types.FieldInstance;
import polyglot.types.InitializerInstance;
import polyglot.types.MemberDef;
import polyglot.types.MethodInstance;
import polyglot.types.PrimitiveType;
import polyglot.types.ProcedureInstance;
import polyglot.types.Type;

import com.ibm.wala.cast.java.translator.polyglot.PolyglotJava2CAstTranslator.IdentityMapper;
import com.ibm.wala.cast.java.types.JavaPrimitiveTypeMap;
import com.ibm.wala.types.ClassLoaderReference;
import com.ibm.wala.types.Descriptor;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.Selector;
import com.ibm.wala.types.TypeName;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.debug.Assertions;
import com.ibm.wala.util.strings.Atom;

/**
 * Class responsible for mapping Polyglot type system objects representing types,
 * methods and fields to the corresponding WALA TypeReferences, MethodReferences
 * and FieldReferences. Used during translation and by clients to help correlate
 * WALA analysis results to the various AST nodes.
 * @author rfuhrer
 */
public class PolyglotIdentityMapper implements IdentityMapper<Type, CodeInstance, FieldInstance> {
  private final Map<Type, TypeReference> fTypeMap = HashMapFactory.make();

  private final Map<FieldInstance, FieldReference> fFieldMap = HashMapFactory.make();

  private final Map<CodeInstance, MethodReference> fMethodMap = HashMapFactory.make();

  /**
   * Map from Polyglot local ClassTypes to their enclosing methods. Used by localTypeToTypeID().<br>
   * Needed since Polyglot doesn't provide this information. (It doesn't need to, since it
   * doesn't need to generate unambiguous names for such entities -- it hands the source
   * off to javac to generate bytecode. It probably also wouldn't want to, since that would
   * create back-pointers from Type objects in the TypeSystem to AST's.)
   */
  protected Map<ClassType, CodeInstance> fLocalTypeMap = new LinkedHashMap<ClassType, CodeInstance>();

  protected final ClassLoaderReference fClassLoaderRef;

  public PolyglotIdentityMapper(ClassLoaderReference clr) {
    fClassLoaderRef= clr;
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

  public MethodReference getMethodRef(CodeInstance method) {
    if (!fMethodMap.containsKey(method)) {
      MethodReference sel= referenceForMethod(method);
      fMethodMap.put(method, sel);
      return sel;
    }
    return fMethodMap.get(method);
  }

  public void mapLocalAnonTypeToMethod(ClassType anonLocalType, CodeInstance owningProc) {
    fLocalTypeMap.put(anonLocalType, owningProc);
  }

  /**
   * Create a FieldReference for the given Polyglot FieldInstance.<br>
   * N.B.: This method <b>does not canonicalize</b> the FieldReferences,
   * but rather creates a new one for each call.
   * You more likely want to call getFieldRef(). This method is exposed
   * so that multiple Polyglot instances can use the translation services
   * without having FieldInstances collide (producing the dreaded "we are
   * TypeSystem_c but type Foo is from TypeSystem_c" exception).
   */
  public FieldReference referenceForField(FieldInstance field) {
    Type targetType= field.container();
    Type fieldType= field.type();
    TypeReference targetTypeRef= TypeReference.findOrCreate(fClassLoaderRef, typeToTypeID(targetType));
    TypeReference fieldTypeRef= TypeReference.findOrCreate(fClassLoaderRef, typeToTypeID(fieldType));
    Atom fieldName= Atom.findOrCreateUnicodeAtom(field.name().toString());
    FieldReference fieldRef= FieldReference.findOrCreate(targetTypeRef, fieldName, fieldTypeRef);

    return fieldRef;
  }

  /**
   * Create a TypeReference for the given Polyglot Type.<br>
   * N.B.: This method <b>does not canonicalize</b> the TypeReferences,
   * but rather creates a new one for each call.
   * You more likely want to call getTypeRef(). This method is exposed
   * so that multiple Polyglot instances can use the translation services
   * without having Types collide (producing the dreaded "we are
   * TypeSystem_c but type Foo is from TypeSystem_c" exception).
   */
  public TypeReference referenceForType(Type type) {
    TypeName typeName= TypeName.string2TypeName(typeToTypeID(type));
    TypeReference typeRef= TypeReference.findOrCreate(fClassLoaderRef, typeName);

    return typeRef;
  }

  private Selector selectorForMethod(CodeInstance procInstance) {
    Atom name= 
      (procInstance instanceof ConstructorInstance) ?
          MethodReference.initAtom : 
            (procInstance instanceof InitializerInstance) ?
                MethodReference.clinitName : 
                  Atom.findOrCreateUnicodeAtom(((MethodInstance) procInstance).name().toString());

    TypeName[] argTypeNames = null;
    if (! (procInstance instanceof InitializerInstance)) {
      List formalTypes = ((ProcedureInstance)procInstance).formalTypes();
      int numArgs = formalTypes.size();
      // Descriptor prefers null to an empty array
      if (numArgs > 0) {
        argTypeNames = new TypeName[numArgs];
      
        int i = 0;
        for(Iterator iter = formalTypes.iterator(); iter.hasNext(); i++)  {
          Type argType= (Type) iter.next();
          argTypeNames[i]= TypeName.string2TypeName(typeToTypeID(argType));
        }
      }
    }

    Type retType= 
      (procInstance instanceof MethodInstance) ? ((MethodInstance) procInstance).returnType() : procInstance.typeSystem().Void();
	TypeName retTypeName= TypeName.string2TypeName(typeToTypeID(retType));

	Descriptor desc= Descriptor.findOrCreate(argTypeNames, retTypeName);

	return new Selector(name, desc);
  }

  /**
   * Create a MethodReference for the given Polyglot MethodInstance.<br>
   * N.B.: This method <b>does not canonicalize</b> the MethodReferences,
   * but rather creates a new one for each call.
   * You more likely want to call getMethodRef(). This method is exposed
   * so that multiple Polyglot instances can use the translation services
   * without having MethodInstances collide (producing the dreaded "we are
   * TypeSystem_c but type Foo is from TypeSystem_c" exception).
   */
  public MethodReference referenceForMethod(CodeInstance procInstance) {
    // Handles both ConstructorInstance's and MethodInstance's
    TypeName ownerType= TypeName.string2TypeName(typeToTypeID(((MemberDef) procInstance.def()).container().get()));
    TypeReference ownerTypeRef= TypeReference.findOrCreate(fClassLoaderRef, ownerType);
    MethodReference methodRef= MethodReference.findOrCreate(ownerTypeRef, selectorForMethod(procInstance));

    return methodRef;
  }

  /**
   * Translates the given Polyglot type to a name suitable for use in a WALA TypeReference
   * (i.e. a bytecode-compliant type name).
   */
  public String typeToTypeID(Type type) {
    if (type.isPrimitive()) {
      PrimitiveType ptype= (PrimitiveType) type;

      return JavaPrimitiveTypeMap.getShortName(ptype.name().toString());
    } else if (type.isArray()) {
      ArrayType atype= (ArrayType) type;
      return "[" + typeToTypeID(atype.base());
    } else if (type.isNull()) {
      Assertions.UNREACHABLE("typeToTypeID() encountered a null type!");
      return null;
    }
    Assertions.productionAssertion(type.isClass(), "typeToTypeID() encountered the type " + type + " that is neither primitive, array, nor class!");

    ClassType ctype= (ClassType) type;

    return (ctype.isLocal() || ctype.isAnonymous()) ? anonLocalTypeToTypeID(ctype) : composeWALATypeDescriptor(ctype);
  }

  public String anonLocalTypeToTypeID(ClassType ctype) {
    CodeInstance procInstance= (CodeInstance) fLocalTypeMap.get(ctype);

    String outerTypeID= typeToTypeID(ctype.outer());
    String shortName= (ctype.isAnonymous()) ? PolyglotJava2CAstTranslator.anonTypeName(ctype) : ctype.fullName().name().toString();

    return outerTypeID + '/' + getMethodRef(procInstance).getSelector() + '/' + shortName;
  }

  public String composeWALATypeDescriptor(ClassType ctype) {
    return "L" + composeWALATypeName(ctype);
  }

  public String composeWALATypeName(ClassType ctype) {
    if (ctype.package_() != null) {
      String packageName = ctype.package_().fullName().toString();

      Assertions.productionAssertion(ctype.fullName().toString().startsWith(packageName));
      return packageName.replace('.','/') + "/" + ctype.fullName().toString().substring( packageName.length()+1 ).replace('.','$'); 
    } else {
      return ctype.fullName().toString().replace('.', '$');
    }
  }
}
