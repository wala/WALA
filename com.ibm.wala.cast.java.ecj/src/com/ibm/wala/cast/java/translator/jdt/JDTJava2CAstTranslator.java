/*
 * This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html.
 * 
 * This file is a derivative of code released by the University of
 * California under the terms listed below.  
 *
 * WALA JDT Frontend is Copyright (c) 2008 The Regents of the
 * University of California (Regents). Provided that this notice and
 * the following two paragraphs are included in any distribution of
 * Refinement Analysis Tools or its derivative work, Regents agrees
 * not to assert any of Regents' copyright rights in Refinement
 * Analysis Tools against recipient for recipient's reproduction,
 * preparation of derivative works, public display, public
 * performance, distribution or sublicensing of Refinement Analysis
 * Tools and derivative works, in source code and object code form.
 * This agreement not to assert does not confer, by implication,
 * estoppel, or otherwise any license or rights in any intellectual
 * property of Regents, including, but not limited to, any patents
 * of Regents or Regents' employees.
 * 
 * IN NO EVENT SHALL REGENTS BE LIABLE TO ANY PARTY FOR DIRECT,
 * INDIRECT, SPECIAL, INCIDENTAL, OR CONSEQUENTIAL DAMAGES,
 * INCLUDING LOST PROFITS, ARISING OUT OF THE USE OF THIS SOFTWARE
 * AND ITS DOCUMENTATION, EVEN IF REGENTS HAS BEEN ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *   
 * REGENTS SPECIFICALLY DISCLAIMS ANY WARRANTIES, INCLUDING, BUT NOT
 * LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS
 * FOR A PARTICULAR PURPOSE AND FURTHER DISCLAIMS ANY STATUTORY
 * WARRANTY OF NON-INFRINGEMENT. THE SOFTWARE AND ACCOMPANYING
 * DOCUMENTATION, IF ANY, PROVIDED HEREUNDER IS PROVIDED "AS
 * IS". REGENTS HAS NO OBLIGATION TO PROVIDE MAINTENANCE, SUPPORT,
 * UPDATES, ENHANCEMENTS, OR MODIFICATIONS.
 */
package com.ibm.wala.cast.java.translator.jdt;

import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTNode;
import org.eclipse.jdt.core.dom.AbstractTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeDeclaration;
import org.eclipse.jdt.core.dom.AnnotationTypeMemberDeclaration;
import org.eclipse.jdt.core.dom.AnonymousClassDeclaration;
import org.eclipse.jdt.core.dom.ArrayAccess;
import org.eclipse.jdt.core.dom.ArrayCreation;
import org.eclipse.jdt.core.dom.ArrayInitializer;
import org.eclipse.jdt.core.dom.AssertStatement;
import org.eclipse.jdt.core.dom.Assignment;
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.BodyDeclaration;
import org.eclipse.jdt.core.dom.BooleanLiteral;
import org.eclipse.jdt.core.dom.BreakStatement;
import org.eclipse.jdt.core.dom.CastExpression;
import org.eclipse.jdt.core.dom.CatchClause;
import org.eclipse.jdt.core.dom.CharacterLiteral;
import org.eclipse.jdt.core.dom.ClassInstanceCreation;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.ConditionalExpression;
import org.eclipse.jdt.core.dom.ConstructorInvocation;
import org.eclipse.jdt.core.dom.ContinueStatement;
import org.eclipse.jdt.core.dom.DoStatement;
import org.eclipse.jdt.core.dom.EmptyStatement;
import org.eclipse.jdt.core.dom.EnhancedForStatement;
import org.eclipse.jdt.core.dom.EnumConstantDeclaration;
import org.eclipse.jdt.core.dom.EnumDeclaration;
import org.eclipse.jdt.core.dom.Expression;
import org.eclipse.jdt.core.dom.ExpressionStatement;
import org.eclipse.jdt.core.dom.FieldAccess;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.ForStatement;
import org.eclipse.jdt.core.dom.IAnnotationBinding;
import org.eclipse.jdt.core.dom.IBinding;
import org.eclipse.jdt.core.dom.IMemberValuePairBinding;
import org.eclipse.jdt.core.dom.IMethodBinding;
import org.eclipse.jdt.core.dom.ITypeBinding;
import org.eclipse.jdt.core.dom.IVariableBinding;
import org.eclipse.jdt.core.dom.IfStatement;
import org.eclipse.jdt.core.dom.InfixExpression;
import org.eclipse.jdt.core.dom.Initializer;
import org.eclipse.jdt.core.dom.InstanceofExpression;
import org.eclipse.jdt.core.dom.LabeledStatement;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.Modifier;
import org.eclipse.jdt.core.dom.NullLiteral;
import org.eclipse.jdt.core.dom.NumberLiteral;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.ParenthesizedExpression;
import org.eclipse.jdt.core.dom.PostfixExpression;
import org.eclipse.jdt.core.dom.PrefixExpression;
import org.eclipse.jdt.core.dom.QualifiedName;
import org.eclipse.jdt.core.dom.ReturnStatement;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.SimpleType;
import org.eclipse.jdt.core.dom.SingleVariableDeclaration;
import org.eclipse.jdt.core.dom.Statement;
import org.eclipse.jdt.core.dom.StringLiteral;
import org.eclipse.jdt.core.dom.SuperConstructorInvocation;
import org.eclipse.jdt.core.dom.SuperFieldAccess;
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.SwitchCase;
import org.eclipse.jdt.core.dom.SwitchStatement;
import org.eclipse.jdt.core.dom.SynchronizedStatement;
import org.eclipse.jdt.core.dom.ThisExpression;
import org.eclipse.jdt.core.dom.ThrowStatement;
import org.eclipse.jdt.core.dom.TryStatement;
import org.eclipse.jdt.core.dom.Type;
import org.eclipse.jdt.core.dom.TypeDeclaration;
import org.eclipse.jdt.core.dom.TypeDeclarationStatement;
import org.eclipse.jdt.core.dom.TypeLiteral;
import org.eclipse.jdt.core.dom.VariableDeclarationExpression;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;
import org.eclipse.jdt.core.dom.VariableDeclarationStatement;
import org.eclipse.jdt.core.dom.WhileStatement;

import com.ibm.wala.analysis.typeInference.JavaPrimitiveType;
import com.ibm.wala.cast.ir.translator.AstTranslator.InternalCAstSymbol;
import com.ibm.wala.cast.ir.translator.TranslatorToCAst;
import com.ibm.wala.cast.ir.translator.TranslatorToCAst.DoLoopTranslator;
import com.ibm.wala.cast.java.loader.JavaSourceLoaderImpl;
import com.ibm.wala.cast.java.loader.Util;
import com.ibm.wala.cast.java.translator.JavaProcedureEntity;
import com.ibm.wala.cast.tree.CAst;
import com.ibm.wala.cast.tree.CAstAnnotation;
import com.ibm.wala.cast.tree.CAstControlFlowMap;
import com.ibm.wala.cast.tree.CAstEntity;
import com.ibm.wala.cast.tree.CAstNode;
import com.ibm.wala.cast.tree.CAstNodeTypeMap;
import com.ibm.wala.cast.tree.CAstQualifier;
import com.ibm.wala.cast.tree.CAstSourcePositionMap;
import com.ibm.wala.cast.tree.CAstSourcePositionMap.Position;
import com.ibm.wala.cast.tree.CAstType;
import com.ibm.wala.cast.tree.impl.CAstControlFlowRecorder;
import com.ibm.wala.cast.tree.impl.CAstImpl;
import com.ibm.wala.cast.tree.impl.CAstNodeTypeMapRecorder;
import com.ibm.wala.cast.tree.impl.CAstOperator;
import com.ibm.wala.cast.tree.impl.CAstSourcePositionRecorder;
import com.ibm.wala.cast.tree.impl.CAstSymbolImpl;
import com.ibm.wala.cast.util.CAstPrinter;
import com.ibm.wala.classLoader.CallSiteReference;
import com.ibm.wala.shrikeBT.IInvokeInstruction;
import com.ibm.wala.types.FieldReference;
import com.ibm.wala.types.MethodReference;
import com.ibm.wala.types.TypeReference;
import com.ibm.wala.util.collections.EmptyIterator;
import com.ibm.wala.util.collections.HashMapFactory;
import com.ibm.wala.util.collections.HashSetFactory;
import com.ibm.wala.util.collections.Pair;
import com.ibm.wala.util.debug.Assertions;

// TOTEST:
// "1/0" surrounded by catch ArithmeticException & RunTimeException (TryCatchContext.getCatchTypes"
// another subtype of ArithmeticException surrounded by catch ArithmeticException
// binary ops with all kinds of type conversions
// simplenames: fields of this, fields of an enclosing class, fields of an enclosing method, static fields, fully qualified fields w/ package stuff
// exceptional CFG edges, somehow. call nodes, new nodes, division by zero in binary ops, null pointer in field accesses, etc.   
// implicit constructors

// FIXME 1.4: thing about / ask agout TAGALONG (JDT stuff tagging along in memory cos we keep it).
// FIXME 1.4: find LEFTOUT and find out why polyglot has extra code / infrastructure, if it's used and what for, etc.

// Java 1.6:
// * type parameters/generics: see getArgumentTypes in ProcedureEntity$anon. also anywhere we use ITypeBinding.equals() may be affected
//   see anywhere in code labeled GENERICS for present "treat as raw types"/"pretend it's 1.4 and casts" solution.
// * boxing (YUCK). see resolveBoxing()
// * enums (probably in simplename or something. but using resolveConstantExpressionValue() possible)

public abstract class JDTJava2CAstTranslator<T extends Position> {
  protected boolean dump = false;
  
  protected final CAst fFactory = new CAstImpl();

  // ///////////////////////////////////////////
  // / HANDLINGS OF VARIOUS THINGS //
  // ///////////////////////////////////////////
  protected final AST ast; // TAGALONG

  protected final JDTIdentityMapper fIdentityMapper; // TAGALONG

  protected final JDTTypeDictionary fTypeDict;

  protected final JavaSourceLoaderImpl fSourceLoader;

  protected final ITypeBinding fDivByZeroExcType;

  protected final ITypeBinding fNullPointerExcType;

  protected final ITypeBinding fClassCastExcType;

  protected final ITypeBinding fRuntimeExcType;

  protected final ITypeBinding NoClassDefFoundError;

  protected final ITypeBinding ExceptionInInitializerError;

  protected final ITypeBinding OutOfMemoryError;

  protected final DoLoopTranslator doLoopTranslator;
  
  protected final String fullPath;
  
  protected final CompilationUnit cu;

  //
  // COMPILATION UNITS & TYPES
  //

  public JDTJava2CAstTranslator(JavaSourceLoaderImpl sourceLoader, CompilationUnit astRoot, String fullPath, boolean replicateForDoLoops) {
    this(sourceLoader, astRoot, fullPath, replicateForDoLoops, false);
  }

  public JDTJava2CAstTranslator(JavaSourceLoaderImpl sourceLoader, CompilationUnit astRoot, String fullPath, boolean replicateForDoLoops, boolean dump) {
    fDivByZeroExcType = FakeExceptionTypeBinding.arithmetic;
    fNullPointerExcType = FakeExceptionTypeBinding.nullPointer;
    fClassCastExcType = FakeExceptionTypeBinding.classCast;
    NoClassDefFoundError = FakeExceptionTypeBinding.noClassDef;
    ExceptionInInitializerError = FakeExceptionTypeBinding.initException;
    OutOfMemoryError = FakeExceptionTypeBinding.outOfMemory;

    this.fSourceLoader = sourceLoader;
    this.cu = astRoot;

    this.fullPath = fullPath;
    this.ast = astRoot.getAST();

    this.doLoopTranslator = new DoLoopTranslator(replicateForDoLoops, fFactory);

    this.dump = dump;
    
    // FIXME: we might need one AST (-> "Object" class) for all files.
    fIdentityMapper = new JDTIdentityMapper(fSourceLoader.getReference(), ast);
    fTypeDict = new JDTTypeDictionary(ast, fIdentityMapper);

    fRuntimeExcType = ast.resolveWellKnownType("java.lang.RuntimeException");
    assert fRuntimeExcType != null;
  }

  public CAstEntity translateToCAst() {

    List<CAstEntity> declEntities = new ArrayList<>();

    for (AbstractTypeDeclaration decl : (Iterable<AbstractTypeDeclaration>) cu.types()) {
      // can be of type AnnotationTypeDeclaration, EnumDeclaration, TypeDeclaration
      declEntities.add(visit(decl, new RootContext()));
    }

    if (dump) {
      for(CAstEntity d : declEntities) {
        CAstPrinter.printTo(d, new PrintWriter(System.err));
      }
    }
    
    return new CompilationUnitEntity(cu.getPackage(), declEntities);
  }

  //
  // TYPES
  //

  protected final class ClassEntity implements CAstEntity {
    // TAGALONG (not static, will keep reference to ast, fIdentityMapper, etc)

    private final String fName;

    private final Collection<CAstQualifier> fQuals;

    private final Collection<CAstEntity> fEntities;

    private final ITypeBinding fJdtType; // TAGALONG

    private final T fSourcePosition;

    public ClassEntity(ITypeBinding jdtType, String name, Collection<CAstQualifier> quals, Collection<CAstEntity> entities,
        T pos) {
      fName = name;
      fQuals = quals;
      fEntities = entities;
      fJdtType = jdtType;
      fSourcePosition = pos;
    }

    @Override
	public Collection<CAstAnnotation> getAnnotations() {
		// TODO Auto-generated method stub
		return null;
	}


	@Override
  public int getKind() {
      return TYPE_ENTITY;
    }

    @Override
    public String getName() {
      return fName; // unqualified?
    }

    @Override
    public String getSignature() {
      return "L" + fName.replace('.', '/') + ";";
    }

    @Override
    public String[] getArgumentNames() {
      return new String[0];
    }

    @Override
    public CAstNode[] getArgumentDefaults() {
      return new CAstNode[0];
    }

    @Override
    public int getArgumentCount() {
      return 0;
    }

    @Override
    public CAstNode getAST() {
      // This entity has no AST nodes, really.
      return null;
    }

    @Override
    public Map<CAstNode, Collection<CAstEntity>> getAllScopedEntities() {
      return Collections.singletonMap(null, fEntities);
    }

    @Override
    public Iterator<CAstEntity> getScopedEntities(CAstNode construct) {
      Assertions.UNREACHABLE("Non-AST-bearing entity (ClassEntity) asked for scoped entities related to a given AST node");
      return null;
    }

    @Override
    public CAstControlFlowMap getControlFlow() {
      // This entity has no AST nodes, really.
      return null;
    }

    @Override
    public CAstSourcePositionMap getSourceMap() {
      // This entity has no AST nodes, really.
      return null;
    }

    @Override
    public CAstSourcePositionMap.Position getPosition() {
      return fSourcePosition;
    }

    @Override
    public CAstNodeTypeMap getNodeTypeMap() {
      // This entity has no AST nodes, really.
      return new CAstNodeTypeMap() {
        @Override
        public CAstType getNodeType(CAstNode node) {
          throw new UnsupportedOperationException();
        }

		@Override
    public Collection<CAstNode> getMappedNodes() {
			throw new UnsupportedOperationException();
		}
      };
    }

    @Override
    public Collection<CAstQualifier> getQualifiers() {
      return fQuals;
    }

    @Override
    public CAstType getType() {
      // return new JdtJavaType(fCT, getTypeDict(), fTypeSystem);
      return fTypeDict.new JdtJavaType(fJdtType);
    }

  }

  private static boolean isInterface(AbstractTypeDeclaration decl) {
    return decl instanceof AnnotationTypeDeclaration ||
      (decl instanceof TypeDeclaration && ((TypeDeclaration)decl).isInterface());
  }
  
  private CAstEntity visitTypeDecl(AbstractTypeDeclaration n, WalkContext context) {
    return createClassDeclaration(n, n.bodyDeclarations(), null, n.resolveBinding(), n.getName().getIdentifier(), n.getModifiers(),
        isInterface(n), n instanceof AnnotationTypeDeclaration, context);
  }

  /**
   * 
   * @param n
   * @param bodyDecls
   * @param enumConstants
   * @param typeBinding
   * @param name Used in creating default constructor, and passed into new ClassEntity()
   * @param context
   */
  private CAstEntity createClassDeclaration(ASTNode n, List<BodyDeclaration> bodyDecls,
      List<EnumConstantDeclaration> enumConstants, ITypeBinding typeBinding, String name, int modifiers, 
      boolean isInterface, boolean isAnnotation, WalkContext context) {
    final List<CAstEntity> memberEntities = new ArrayList<>();

    // find and collect all initializers (type Initializer) and field initializers (type VariableDeclarationFragment).
    // instance initializer code will be inserted into each constructors.
    // all static initializer code will be grouped together in its own entity.
    ArrayList<ASTNode> inits = new ArrayList<>();
    ArrayList<ASTNode> staticInits = new ArrayList<>();

    if (enumConstants != null) {
      // always (implicitly) static,final (actually, no modifiers allowed)
      staticInits.addAll(enumConstants);
    }

    for (BodyDeclaration decl : bodyDecls) {
      if (decl instanceof Initializer) {
        Initializer initializer = (Initializer) decl;
        boolean isStatic = ((initializer.getModifiers() & Modifier.STATIC) != 0);
        (isStatic ? staticInits : inits).add(initializer);
      } else if (decl instanceof FieldDeclaration) {
        FieldDeclaration fd = (FieldDeclaration) decl;

        for (VariableDeclarationFragment frag : (Iterable<VariableDeclarationFragment>) fd.fragments()) {
          if (frag.getInitializer() != null) {
            boolean isStatic = ((fd.getModifiers() & Modifier.STATIC) != 0);
            (isStatic ? staticInits : inits).add(frag);
          }
        }
      }
    }

    // process entities. initializers will be folded in here.
    if (enumConstants != null) {
      for (EnumConstantDeclaration decl : enumConstants) {
        memberEntities.add(visit(decl, context));
      }
    }

    for (BodyDeclaration decl : bodyDecls) {
      if (decl instanceof FieldDeclaration) {
        FieldDeclaration fieldDecl = (FieldDeclaration) decl;
        Collection<CAstQualifier> quals = JDT2CAstUtils.mapModifiersToQualifiers(fieldDecl.getModifiers(), false, false);
        for (VariableDeclarationFragment fieldFrag : (Iterable<VariableDeclarationFragment>) fieldDecl.fragments()) {
          IVariableBinding fieldBinding = fieldFrag.resolveBinding();
		memberEntities.add(new FieldEntity(fieldFrag.getName().getIdentifier(), fieldBinding.getType(), quals,
              makePosition(fieldFrag.getStartPosition(), fieldFrag.getStartPosition() + fieldFrag.getLength()),
              handleAnnotations(fieldBinding)));
        }
      } else if (decl instanceof Initializer) {
        // Initializers are inserted into constructors when making constructors.
      } else if (decl instanceof MethodDeclaration) {
        MethodDeclaration metDecl = (MethodDeclaration) decl;

        if (typeBinding.isEnum() && metDecl.isConstructor())
          memberEntities.add(createEnumConstructorWithParameters(metDecl.resolveBinding(), metDecl, context, inits, metDecl));
        else {
          memberEntities.add(visit(metDecl, typeBinding, context, inits));

          // /////////////// Java 1.5 "overridden with subtype" thing (covariant return type) ///////////
          Collection<IMethodBinding> overriddenMets = JDT2CAstUtils.getOverriddenMethod(metDecl.resolveBinding());
          if (overriddenMets != null) {
            for (IMethodBinding overridden : overriddenMets)
              if (!JDT2CAstUtils.sameErasedSignatureAndReturnType(metDecl.resolveBinding(), overridden))
                memberEntities.add(makeSyntheticCovariantRedirect(metDecl, metDecl.resolveBinding(), overridden, context));
          }
        }
      } else if (decl instanceof AbstractTypeDeclaration) {
        memberEntities.add(visit((AbstractTypeDeclaration) decl, context));
      } else if (decl instanceof AnnotationTypeMemberDeclaration) {
        // TODO: need to decide what to do with these
      } else {
        Assertions.UNREACHABLE("BodyDeclaration not Field, Initializer, or Method");
      }
    }

    // add default constructor(s) if necessary
    // most default constructors have no parameters; however, those created by anonymous classes will have parameters
    // (they just call super with those parameters)
    for (IMethodBinding met : typeBinding.getDeclaredMethods()) {
      if (met.isDefaultConstructor()) {
        if (typeBinding.isEnum())
          memberEntities.add(createEnumConstructorWithParameters(met, n, context, inits, null));
        else if (met.getParameterTypes().length > 0)
          memberEntities.add(createDefaultConstructorWithParameters(met, n, context, inits));
        else
          memberEntities.add(createDefaultConstructor(typeBinding, context, inits, n));
      }
    }

    if (typeBinding.isEnum() && !typeBinding.isAnonymous())
      doEnumHiddenEntities(typeBinding, memberEntities, context);

    // collect static inits
    if (!staticInits.isEmpty()) {
      Map<CAstNode, CAstEntity> childEntities = HashMapFactory.make();
      final MethodContext newContext = new MethodContext(context, childEntities);
      // childEntities is the same one as in the ProcedureEntity. later visit(New), etc. may add to this.

      CAstNode[] bodyNodes = new CAstNode[staticInits.size()];
      for (int i = 0; i < staticInits.size(); i++)
        bodyNodes[i] = visitFieldInitNode(staticInits.get(i), newContext);
      CAstNode staticInitAst = makeNode(newContext, fFactory, n, CAstNode.BLOCK_STMT, bodyNodes);
      memberEntities.add(new ProcedureEntity(staticInitAst, typeBinding, childEntities, newContext, null));
    }

    Collection<CAstQualifier> quals = JDT2CAstUtils.mapModifiersToQualifiers(modifiers, isInterface, isAnnotation);

    return new ClassEntity(typeBinding, name, quals, memberEntities, makePosition(n));
  }

  private CAstEntity visit(AnonymousClassDeclaration n, WalkContext context) {
    return createClassDeclaration(n, n.bodyDeclarations(), null, n.resolveBinding(),
        JDT2CAstUtils.anonTypeName(n.resolveBinding()), 0 /* no modifiers */, false, false, context);
  }

  private CAstNode visit(TypeDeclarationStatement n, WalkContext context) {
    // TODO 1.6: enums of course...
    AbstractTypeDeclaration decl = n.getDeclaration();
    assert decl instanceof TypeDeclaration : "Local enum declaration not yet supported";
    CAstEntity classEntity = visitTypeDecl(decl, context);

    // these statements doin't actually do anything, just define a type
    final CAstNode lcdNode = makeNode(context, fFactory, n, CAstNode.EMPTY);

    // so define it!
    context.addScopedEntity(lcdNode, classEntity);
    return lcdNode;
  }

  // ////////////////////////////////
  // METHODS
  // ////////////////////////////////

  /**
   * @param n for positioning.
   *
   * Make a constructor with parameters that calls super(...) with parameters. Used for anonymous classes with arguments to a
   * constructor, like new Foo(arg1,arg2) { }
   */
  private CAstEntity createDefaultConstructorWithParameters(IMethodBinding ctor, ASTNode n, WalkContext oldContext,
      ArrayList<ASTNode> inits) {
    // PART I: find super ctor to call
    ITypeBinding newType = ctor.getDeclaringClass();
    ITypeBinding superType = newType.getSuperclass();
    IMethodBinding superCtor = null;

    for (IMethodBinding m : superType.getDeclaredMethods())
      if (m.isConstructor() && Arrays.equals(m.getParameterTypes(), ctor.getParameterTypes()))
        superCtor = m;

    assert superCtor != null : "couldn't find constructor for anonymous class";

    // PART II: make ctor with simply "super(a,b,c...)"
    final Map<CAstNode, CAstEntity> memberEntities = new LinkedHashMap<>();
    final MethodContext context = new MethodContext(oldContext, memberEntities);
    MethodDeclaration fakeCtor = ast.newMethodDeclaration();
    fakeCtor.setConstructor(true);
    fakeCtor.setSourceRange(n.getStartPosition(), n.getLength());
    fakeCtor.setBody(ast.newBlock());

    // PART IIa: make a fake JDT constructor method with the proper number of args
    // Make fake args that will be passed
    String[] fakeArguments = new String[superCtor.getParameterTypes().length + 1];
    ArrayList<CAstType> paramTypes = new ArrayList<>(superCtor.getParameterTypes().length);
    for (int i = 0; i < fakeArguments.length; i++)
      fakeArguments[i] = (i == 0) ? "this" : ("argument" + i); // TODO: change to invalid name and don't use
                                                               // singlevariabledeclaration below
    for (int i = 1; i < fakeArguments.length; i++) {
      // the name
      SingleVariableDeclaration svd = ast.newSingleVariableDeclaration();
      svd.setName(ast.newSimpleName(fakeArguments[i]));
      fakeCtor.parameters().add(svd);

      // the type
      paramTypes.add(fTypeDict.getCAstTypeFor(ctor.getParameterTypes()[i - 1]));
    }

    // PART IIb: create the statements in the constructor
    // one super() call plus the inits
    CAstNode[] bodyNodes = new CAstNode[inits.size() + 1];

    // make super(...) call
    // this, call ref, args
    CAstNode[] children = new CAstNode[fakeArguments.length + 1];
    children[0] = makeNode(context, fFactory, n, CAstNode.SUPER);
    CallSiteReference callSiteRef = CallSiteReference.make(0, fIdentityMapper.getMethodRef(superCtor),
        IInvokeInstruction.Dispatch.SPECIAL);
    children[1] = fFactory.makeConstant(callSiteRef);
    for (int i = 1; i < fakeArguments.length; i++) {
      CAstNode argName = fFactory.makeConstant(fakeArguments[i]);
      CAstNode argType = fFactory.makeConstant(paramTypes.get(i-1));
      children[i + 1] = makeNode(context, fFactory, n, CAstNode.VAR, argName, argType);
    }
    bodyNodes[0] = makeNode(context, fFactory, n, CAstNode.CALL, children);
    // QUESTION: no handleExceptions?

    for (int i = 0; i < inits.size(); i++)
      bodyNodes[i + 1] = visitFieldInitNode(inits.get(i), context);

    // finally, make the procedure entity
    CAstNode ast = makeNode(context, fFactory, n, CAstNode.BLOCK_STMT, bodyNodes);
    return new ProcedureEntity(ast, fakeCtor, newType, memberEntities, context, paramTypes, null, null);

  }

  private CAstEntity createDefaultConstructor(ITypeBinding classBinding, WalkContext oldContext, ArrayList<ASTNode> inits,
      ASTNode positioningNode) {
    MethodDeclaration fakeCtor = ast.newMethodDeclaration();
    fakeCtor.setConstructor(true);
    // fakeCtor.setName(ast.newSimpleName(className)); will crash on anonymous types...
    fakeCtor.setSourceRange(positioningNode.getStartPosition(), positioningNode.getLength());
    fakeCtor.setBody(ast.newBlock());

    return visit(fakeCtor, classBinding, oldContext, inits);
  }

  private static IMethodBinding findDefaultCtor(ITypeBinding superClass) {
    for (IMethodBinding met : superClass.getDeclaredMethods()) {
      if (met.isConstructor() && met.getParameterTypes().length == 0)
        return met;
    }
    Assertions.UNREACHABLE("Couldn't find default ctor");
    return null;
  }

  /**
   * Setup constructor body. Here we add the initializer code (both initalizer blocks and initializers in field declarations). We
   * may also need to add an implicit super() call.
   * 
   * @param n
   * @param classBinding Used so we can use this with fake MethodDeclaration nodes, as in the case of creating a default
   *          constructor.
   * @param context
   * @param inits
   */
  private CAstNode createConstructorBody(MethodDeclaration n, ITypeBinding classBinding, WalkContext context,
      ArrayList<ASTNode> inits) {
    // three possibilites: has super(), has this(), has neither.

    Statement firstStatement = null;
    if (!n.getBody().statements().isEmpty())
      firstStatement = (Statement) n.getBody().statements().get(0);
    if (firstStatement instanceof SuperConstructorInvocation) {
      // Split at call to super:
      // super();
      // field initializer code
      // remainder of ctor body
      ArrayList<CAstNode> origStatements = createBlock(n.getBody(), context);
      CAstNode[] bodyNodes = new CAstNode[inits.size() + origStatements.size()];
      int idx = 0;
      bodyNodes[idx++] = origStatements.get(0);
      for (ASTNode init : inits)
        bodyNodes[idx++] = visitFieldInitNode(init, context); // visit each in this constructor's context, ensuring
      // proper handling of exceptions (we can't just reuse the
      // CAstNodes)
      for (int i = 1; i < origStatements.size(); i++)
        bodyNodes[idx++] = origStatements.get(i);

      return makeNode(context, fFactory, n.getBody(), CAstNode.BLOCK_STMT, bodyNodes); // QUESTION: why no LOCAL_SCOPE?
      // that's the way it is in
      // polyglot.
    } else if (firstStatement instanceof ConstructorInvocation) {
      return visitNode(n.getBody(), context); // has this(...) call; initializers will be set somewhere else.
    } else {
      // add explicit call to default super()
      // QUESTION their todo: following superClass lookup of default ctor won't work if we
      // process Object in source...

      ITypeBinding superType = classBinding.getSuperclass();
      
      // find default constructor. IT is an error to have a constructor
      // without super() when the default constructor of the superclass does not exist.
      IMethodBinding defaultSuperCtor = findDefaultCtor(superType);
      CallSiteReference callSiteRef = CallSiteReference.make(0, fIdentityMapper.getMethodRef(defaultSuperCtor),
          IInvokeInstruction.Dispatch.SPECIAL);

      // QUESTION: why isn't first arg this like in visit(ConstructorInvocation) ?
      // why don't we handle exceptions like in visit(ConstructorInvocation) ? (these two things are same in polyglot
      // implementation)

      CAstNode superCall = makeNode(context, fFactory, n.getBody(), CAstNode.CALL, makeNode(context, fFactory, n.getBody(),
          CAstNode.SUPER), fFactory.makeConstant(callSiteRef));
      Object mapper = new Object(); // dummy used for mapping this node in CFG
      handleThrowsFromCall(defaultSuperCtor, mapper, context);
      context.cfg().map(mapper, superCall);

      ArrayList<CAstNode> origStatements = createBlock(n.getBody(), context);
      CAstNode[] bodyNodes = new CAstNode[inits.size() + origStatements.size() + 1];
      // superCall, inits, ctor body
      int idx = 0;
      bodyNodes[idx++] = superCall;
      for (ASTNode init : inits)
        bodyNodes[idx++] = visitFieldInitNode(init, context);
      for (int i = 0; i < origStatements.size(); i++)
        bodyNodes[idx++] = origStatements.get(i);
      return makeNode(context, fFactory, n.getBody(), CAstNode.BLOCK_STMT, bodyNodes);
    }
  }

  /**
   * Make a "fake" function (it doesn't exist in source code but it does in bytecode) for covariant return types.
   * 
   * @param overriding Declaration of the overriding method.
   * @param overridden Binding of the overridden method, in a a superclass or implemented interface.
   * @param oldContext
   */
  private CAstEntity makeSyntheticCovariantRedirect(MethodDeclaration overriding, IMethodBinding overridingBinding,
      IMethodBinding overridden, WalkContext oldContext) {
    // SuperClass foo(A, B, C...)
    // SubClass foo(A,B,C...)
    //
    // add a method exactly like overridden that calls overriding

    final Map<CAstNode, CAstEntity> memberEntities = new LinkedHashMap<>();
    final MethodContext context = new MethodContext(oldContext, memberEntities);

    CAstNode calltarget;
    if ((overridingBinding.getModifiers() & Modifier.STATIC) == 0)
      calltarget = makeNode(context, fFactory, null, CAstNode.SUPER);
    else
      calltarget = makeNode(context, fFactory, null, CAstNode.VOID);

    ITypeBinding paramTypes[] = overridden.getParameterTypes();

    ArrayList<CAstNode> arguments = new ArrayList<>();
    int i = 0;
    for (SingleVariableDeclaration svd : (Iterable<SingleVariableDeclaration>) overriding.parameters()) {
      CAstNode varNode = makeNode(context, fFactory, null, CAstNode.VAR, fFactory.makeConstant(svd.getName().getIdentifier()));
      ITypeBinding fromType = JDT2CAstUtils.getErasedType(paramTypes[i], ast);
      ITypeBinding toType = JDT2CAstUtils.getErasedType(overridingBinding.getParameterTypes()[i], ast);
      if (fromType.equals(toType)) {
        arguments.add(varNode);
      } else {
        arguments.add(createCast(null, varNode, fromType, toType, context));
      }
      i++;
    }
    CAstNode callnode = createMethodInvocation(null, overridingBinding, calltarget, arguments, context);
    CAstNode mdast = makeNode(context, fFactory, null, CAstNode.LOCAL_SCOPE, makeNode(context, fFactory, null, CAstNode.BLOCK_STMT,
        makeNode(context, fFactory, null, CAstNode.RETURN, callnode)));

    // make parameters to new synthetic method
    // use RETURN TYPE of overridden, everything else from overriding (including parameter names)
    ArrayList<CAstType> paramCAstTypes = new ArrayList<>(overridden.getParameterTypes().length);
    for (ITypeBinding paramType : overridden.getParameterTypes())
      paramCAstTypes.add(fTypeDict.getCAstTypeFor(paramType));
    return new ProcedureEntity(mdast, overriding, overridingBinding.getDeclaringClass(), memberEntities, context, paramCAstTypes,
        overridden.getReturnType(), null);
  }

  /**
   * @param inits Instance intializers & field initializers. Only used if method is a constructor, in which case the initializers
   *          will be inserted in.
   */
  private CAstEntity visit(MethodDeclaration n, ITypeBinding classBinding, WalkContext oldContext, ArrayList<ASTNode> inits) {

    // pass in memberEntities to the context, later visit(New) etc. may add classes
    final Map<CAstNode, CAstEntity> memberEntities = new LinkedHashMap<>();
    final MethodContext context = new MethodContext(oldContext, memberEntities); // LEFTOUT: in polyglot there is a
    // class context in between method and
    // root

    CAstNode mdast;

    if (n.isConstructor())
      mdast = createConstructorBody(n, classBinding, context, inits);
    else if ((n.getModifiers() & Modifier.ABSTRACT) != 0) // abstract
      mdast = null;
    else if (n.getBody() == null || n.getBody().statements().size() == 0) // empty
      mdast = makeNode(context, fFactory, n, CAstNode.RETURN);
    else
      mdast = visitNode(n.getBody(), context);
    // Polyglot comment: Presumably the MethodContext's parent is a ClassContext,
    // and he has the list of initializers. Hopefully the following
    // will glue that stuff in the right place in any constructor body.
    
    Set<CAstAnnotation> annotations = null;
    if (n.resolveBinding() != null) {
    	annotations = handleAnnotations(n.resolveBinding());
    }
    
    return new ProcedureEntity(mdast, n, classBinding, memberEntities, context, annotations);
  }

  private Set<CAstAnnotation> handleAnnotations(IBinding binding) {
    IAnnotationBinding[] annotations = binding.getAnnotations();
    
    if(annotations == null || annotations.length == 0) {
    	return null;
    }
    
    Set<CAstAnnotation> castAnnotations = HashSetFactory.make();
    for(IAnnotationBinding annotation : annotations) {
    	ITypeBinding annotationTypeBinding = annotation.getAnnotationType();
    	final CAstType annotationType = fTypeDict.getCAstTypeFor(annotationTypeBinding);
    	final Map<String,Object> args = HashMapFactory.make();
    	for(IMemberValuePairBinding mvpb : annotation.getAllMemberValuePairs()) {
    		String name = mvpb.getName();
    		Object value = mvpb.getValue();
    		args.put(name, value);
    	}
    	castAnnotations.add(new CAstAnnotation() {
			@Override
			public CAstType getType() {
				return annotationType;
			}
			@Override
			public Map<String, Object> getArguments() {
				return args;
			}
			@Override
			public String toString() {
				return annotationType.getName() + args;
			}
    	});
    }
    
    return castAnnotations;
}

  protected final class ProcedureEntity implements JavaProcedureEntity { // TAGALONG (make static, access ast)

    // From Code Body Entity
    private final Map<CAstNode, Collection<CAstEntity>> fEntities;

    @Override
    public Map<CAstNode, Collection<CAstEntity>> getAllScopedEntities() {
      return Collections.unmodifiableMap(fEntities);
    }

    @Override
    public Iterator<CAstEntity> getScopedEntities(CAstNode construct) {
      if (fEntities.containsKey(construct)) {
        return (fEntities.get(construct)).iterator();
      } else {
        return EmptyIterator.instance();
      }
    }

    @Override
    public String getSignature() {
      return Util.methodEntityToSelector(this).toString();
    }

    private final CAstNode fAst;

    MethodDeclaration fDecl; // TAGALONG serious tagalong...

    private String[] fParameterNames; // INCLUDING this

    private ArrayList<CAstType> fParameterTypes;

    private MethodContext fContext; // possibly TAGALONG, maybe not

    private ITypeBinding fType; // TAGALONG

    protected ITypeBinding fReturnType;

    private int fModifiers;

    private final Set<CAstAnnotation> annotations;

    // can be method, constructor, "fake" default constructor, or null decl = static initializer
    /**
     * For a static initializer, pass a null decl.
     */
    // FIXME: get rid of decl and pass in everything instead of having to do two different things with parameters
    // regular case
    private ProcedureEntity(CAstNode mdast, MethodDeclaration decl, ITypeBinding type, Map<CAstNode, CAstEntity> entities,
        MethodContext context, Set<CAstAnnotation> annotations) {
      this(mdast, decl, type, entities, context, null, null, decl.getModifiers(), annotations);
    }

    // static init
    private ProcedureEntity(CAstNode mdast, ITypeBinding type, Map<CAstNode, CAstEntity> entities, MethodContext context, Set<CAstAnnotation> annotations) {
      this(mdast, null, type, entities, context, null, null, 0, annotations);
    }

    private ProcedureEntity(CAstNode mdast, MethodDeclaration decl, ITypeBinding type, Map<CAstNode, CAstEntity> entities,
        MethodContext context, ArrayList<CAstType> parameterTypes, ITypeBinding returnType, Set<CAstAnnotation> annotations) {
      this(mdast, decl, type, entities, context, parameterTypes, returnType, decl.getModifiers(), annotations);
    }

    private ProcedureEntity(CAstNode mdast, MethodDeclaration decl, ITypeBinding type, Map<CAstNode, CAstEntity> entities,
        MethodContext context, ArrayList<CAstType> parameterTypes, ITypeBinding returnType, int modifiers, Set<CAstAnnotation> annotations) {
      // TypeSystem system, CodeInstance pd, String[] argumentNames,
      // }
      // Map<CAstNode, CAstEntity> entities, MethodContext mc) {
      fDecl = decl;
      fAst = mdast; // "procedure decl ast"
      fContext = context;
      fType = type;
      fReturnType = returnType;
      fModifiers = modifiers;
      this.annotations = annotations;

      // from CodeBodyEntity
      fEntities = new LinkedHashMap<>();
      for (CAstNode key : entities.keySet()) {
        fEntities.put(key, Collections.singleton(entities.get(key)));
      }

      if (fDecl != null) {
        int i = 0; // index to start filling up with real params
        if ((fModifiers & Modifier.STATIC) != 0) {
          fParameterNames = new String[fDecl.parameters().size()];
          i = 0;
        } else {
          fParameterNames = new String[fDecl.parameters().size() + 1];
          fParameterNames[0] = "this";
          i = 1;
        }

        if (parameterTypes == null) {
          fParameterTypes = new ArrayList<>(fDecl.parameters().size());
          for (SingleVariableDeclaration p : (Iterable<SingleVariableDeclaration>) fDecl.parameters()) {
            fParameterNames[i++] = p.getName().getIdentifier();
            fParameterTypes.add(fTypeDict.getCAstTypeFor(p.resolveBinding().getType()));
          }
        } else {
          // currently this is only used in making a default constructor with arguments (anonymous classes).
          // this is because we cannot synthesize bindings.
          fParameterTypes = parameterTypes;
          for (SingleVariableDeclaration p : (Iterable<SingleVariableDeclaration>) fDecl.parameters()) {
            fParameterNames[i++] = p.getName().getIdentifier();
          }
        }
      } else {
        fParameterNames = new String[0];
        fParameterTypes = new ArrayList<>(0); // static initializer
      }
    }

    @Override
	public Collection<CAstAnnotation> getAnnotations() {
		return annotations;
	}

	@Override
  public String toString() {
      return fDecl == null ? "<clinit>" : fDecl.toString();
    }

    @Override
    public int getKind() {
      return CAstEntity.FUNCTION_ENTITY;
    }

    @Override
    public String getName() {
      if (fDecl == null)
        return MethodReference.clinitName.toString();
      else if (fDecl.isConstructor())
        return MethodReference.initAtom.toString();
      else
        return fDecl.getName().getIdentifier();
    }

    /**
     * INCLUDING first parameter 'this' (for non-static methods)
     */
    @Override
    public String[] getArgumentNames() {
      return fParameterNames;
    }

    @Override
    public CAstNode[] getArgumentDefaults() {
      return new CAstNode[0];
    }

    /**
     * INCLUDING first parameter 'this' (for non-static methods)
     */
    @Override
    public int getArgumentCount() {
      return fParameterNames.length;
    }

    @Override
    public CAstNode getAST() {
      return fAst;
    }

    @Override
    public CAstControlFlowMap getControlFlow() {
      return fContext.cfg();
    }

    @Override
    public CAstSourcePositionMap getSourceMap() {
      return fContext.pos();
    }

    @Override
    public CAstSourcePositionMap.Position getPosition() {
      return fDecl==null? getSourceMap().getPosition(fAst): makePosition(fDecl);
    }

    @Override
    public CAstNodeTypeMap getNodeTypeMap() {
      return fContext.getNodeTypeMap();
    }

    @Override
    public Collection<CAstQualifier> getQualifiers() {
      if (fDecl == null)
        return JDT2CAstUtils.mapModifiersToQualifiers(Modifier.STATIC, false, false); // static init
      else
        return JDT2CAstUtils.mapModifiersToQualifiers(fModifiers, false, false);
    }

    @Override
    public CAstType getType() {
      return new CAstType.Method() {
        private Collection<CAstType> fExceptionTypes = null;

        @Override
        public CAstType getReturnType() {
          if (fReturnType != null)
            return fTypeDict.getCAstTypeFor(fReturnType);
          Type type = fDecl == null ? null : (ast.apiLevel() == 2 ? fDecl.getReturnType() : fDecl.getReturnType2());
          if (type == null)
            return fTypeDict.getCAstTypeFor(ast.resolveWellKnownType("void"));
          else
            return fTypeDict.getCAstTypeFor(type.resolveBinding());
        }

        /**
         * NOT INCLUDING first parameter 'this' (for non-static methods)
         */
        @Override
        public List<CAstType> getArgumentTypes() {
          return fParameterTypes;
        }

        /**
         * NOT INCLUDING first parameter 'this' (for non-static methods)
         */
        @Override
        public int getArgumentCount() {
          return fDecl == null ? 0 : fParameterTypes.size();
        }

        @Override
        public String getName() {
          Assertions.UNREACHABLE("CAstType.FunctionImpl#getName() called???");
          return "?";
        }

        @Override
        public Collection<CAstType> getSupertypes() {
          Assertions.UNREACHABLE("CAstType.FunctionImpl#getSupertypes() called???");
          return null;
        }

        @Override
        public Collection<CAstType>/* <CAstType> */getExceptionTypes() {
          if (fExceptionTypes == null) {
            fExceptionTypes = new LinkedHashSet<>();
            if (fDecl != null)
              for (SimpleType exception : (Iterable<SimpleType>) fDecl.thrownExceptionTypes())
                
                fExceptionTypes.add(fTypeDict.getCAstTypeFor(exception.resolveBinding()));
          }
          return fExceptionTypes;
        }

        @Override
        public CAstType getDeclaringType() {
          return fTypeDict.getCAstTypeFor(fType);
        }
      };
    }
  }

  // ////////////////////////////////////
  // FIELDS ////////////////////////////
  // ////////////////////////////////////

  // FIELDS ADDED DIRECTLY IN visit(TypeDeclaration,WalkContext)

  private CAstNode visitFieldInitNode(ASTNode node, WalkContext context) {
    // there are gathered by createClassDeclaration and can only be of two types:
    if (node instanceof Initializer) {
      return visitNode(((Initializer) node).getBody(), context);
    } else if (node instanceof VariableDeclarationFragment) {
      VariableDeclarationFragment f = (VariableDeclarationFragment) node; // this is guaranteed to have an initializer

      // Generate CAST node for the initializer (init())
      // Type targetType = f.memberInstance().container();
      // Type fieldType = f.type().type();
      FieldReference fieldRef = fIdentityMapper.getFieldRef(f.resolveBinding());
      // We use null to indicate an OBJECT_REF to a static field, as the
      // FieldReference doesn't
      // hold enough info to determine this. In this case, (unlike field ref)
      // we don't have a
      // target expr to evaluate.
      boolean isStatic = ((f.resolveBinding().getModifiers() & Modifier.STATIC) != 0);
      CAstNode thisNode = isStatic ? makeNode(context, fFactory, null, CAstNode.VOID) : makeNode(context, fFactory, f,
          CAstNode.THIS);
      CAstNode lhsNode = makeNode(context, fFactory, f, CAstNode.OBJECT_REF, thisNode, fFactory.makeConstant(fieldRef));

      Expression init = f.getInitializer();
      CAstNode rhsNode = visitNode(init, context);
      CAstNode assNode = makeNode(context, fFactory, f, CAstNode.ASSIGN, lhsNode, rhsNode);

      return assNode; // their naming, not mine

    } else if (node instanceof EnumConstantDeclaration) {
      return createEnumConstantDeclarationInit((EnumConstantDeclaration) node, context);
    } else {
      Assertions.UNREACHABLE("invalid init node gathered by createClassDeclaration");
      return null;
    }
  }

  protected final class FieldEntity implements CAstEntity {
    private final ITypeBinding type;

    private final String name;

    private final Collection<CAstQualifier> quals;

    private final T position;

    private final Set<CAstAnnotation> annotations;

    private FieldEntity(String name, ITypeBinding type, Collection<CAstQualifier> quals, T position, Set<CAstAnnotation> annotations) {
      super();
      this.type = type;
      this.quals = quals;
      this.name = name;
      this.position = position;
      this.annotations = annotations;
    }

    
    @Override
	public Collection<CAstAnnotation> getAnnotations() {
		return annotations;
	}

	@Override
  public int getKind() {
      return CAstEntity.FIELD_ENTITY;
    }

    @Override
    public String getName() {
      return name;
    }

    @Override
    public String getSignature() {
      return name + fIdentityMapper.typeToTypeID(type);
    }

    @Override
    public String[] getArgumentNames() {
      return new String[0];
    }

    @Override
    public CAstNode[] getArgumentDefaults() {
      return new CAstNode[0];
    }

    @Override
    public int getArgumentCount() {
      return 0;
    }

    @Override
    public Iterator<CAstEntity> getScopedEntities(CAstNode construct) {
      return EmptyIterator.instance();
    }

    @Override
    public Map<CAstNode, Collection<CAstEntity>> getAllScopedEntities() {
      return Collections.emptyMap();
    }

    @Override
    public CAstNode getAST() {
      // No AST for a field decl; initializers folded into
      // constructor processing...
      return null;
    }

    @Override
    public CAstControlFlowMap getControlFlow() {
      // No AST for a field decl; initializers folded into
      // constructor processing...
      return null;
    }

    @Override
    public CAstSourcePositionMap getSourceMap() {
      // No AST for a field decl; initializers folded into
      // constructor processing...
      return null;
    }

    @Override
    public CAstSourcePositionMap.Position getPosition() {
      return position;
    }

    @Override
    public CAstNodeTypeMap getNodeTypeMap() {
      // No AST for a field decl; initializers folded into
      // constructor processing...
      return null;
    }

    @Override
    public Collection<CAstQualifier> getQualifiers() {
      return quals;
    }

    @Override
    public CAstType getType() {
      return fTypeDict.getCAstTypeFor(type);
    }
  }

  // /////////////////////////////////////
  // / NODES /////////////////////////////
  // /////////////////////////////////////

  /**
   * Visit all the statements in the block and return an arraylist of the statements. Some statements
   * (VariableDeclarationStatements) may expand to more than one CAstNode.
   */
  private ArrayList<CAstNode> createBlock(Block n, WalkContext context) {
    ArrayList<CAstNode> stmtNodes = new ArrayList<>();
    for (ASTNode s : (Iterable<ASTNode>) n.statements())
      visitNodeOrNodes(s, context, stmtNodes);
    return stmtNodes;
  }

  private CAstNode visit(Block n, WalkContext context) {
    ArrayList<CAstNode> stmtNodes = createBlock(n, context);
    CAstNode stmtNodesArray[] = stmtNodes.toArray(new CAstNode[stmtNodes.size()]);
    return makeNode(context, fFactory, n, CAstNode.LOCAL_SCOPE, makeNode(context, fFactory, n, CAstNode.BLOCK_STMT, stmtNodesArray));
  }

  private CAstNode visit(VariableDeclarationFragment n, WalkContext context) {
    int modifiers;
    if (n.getParent() instanceof VariableDeclarationStatement)
      modifiers = ((VariableDeclarationStatement) n.getParent()).getModifiers();
    else if (n.getParent() instanceof VariableDeclarationExpression)
      modifiers = ((VariableDeclarationExpression) n.getParent()).getModifiers();
    else
      modifiers = ((FieldDeclaration) n.getParent()).getModifiers();
    boolean isFinal = (modifiers & Modifier.FINAL) != 0;
    assert n.resolveBinding() != null : n;
    ITypeBinding type = n.resolveBinding().getType();
    Expression init = n.getInitializer();
    CAstNode initNode;

    String t = type.getBinaryName();
    if (init == null) {
      if (JDT2CAstUtils.isLongOrLess(type)) // doesn't include boolean
        initNode = fFactory.makeConstant(0);
      else if (t.equals("D") || t.equals("F"))
        initNode = fFactory.makeConstant(0.0);
      else
        initNode = fFactory.makeConstant(null);
    } else
      initNode = visitNode(init, context);

    Object defaultValue = JDT2CAstUtils.defaultValueForType(type);
    return makeNode(context, fFactory, n, CAstNode.DECL_STMT, fFactory.makeConstant(new CAstSymbolImpl(n.getName().getIdentifier(), fTypeDict.getCAstTypeFor(type),
        isFinal, defaultValue)), initNode);
  }

  /*
   * One VariableDeclarationStatement represents more than one CAstNode statement.
   */
  private ArrayList<CAstNode> visit(VariableDeclarationStatement n, WalkContext context) {
    ArrayList<CAstNode> result = new ArrayList<>();

    for (VariableDeclarationFragment o : (Iterable<VariableDeclarationFragment>) n.fragments())
      result.add(visit(o, context));
    return result;
  }

  private CAstNode visit(ArrayInitializer n, WalkContext context) {
    ITypeBinding type = n.resolveTypeBinding();
    assert type != null : "Could not determine type of ArrayInitializer";
    TypeReference newTypeRef = fIdentityMapper.getTypeRef(type);
    CAstNode[] eltNodes = new CAstNode[n.expressions().size() + 1];
    int idx = 0;

    eltNodes[idx++] = makeNode(context, fFactory, n, CAstNode.NEW, fFactory.makeConstant(newTypeRef), fFactory.makeConstant(n
        .expressions().size()));
    for (Expression element : (Iterable<Expression>) n.expressions()) {
      eltNodes[idx] = visitNode(element, context);
      if (eltNodes[idx] == null)
        assert eltNodes[idx] != null : element.toString();
      ++idx;
    }

    return makeNode(context, fFactory, n, CAstNode.ARRAY_LITERAL, eltNodes);
  }

  private CAstNode visit(ClassInstanceCreation n, WalkContext context) {
    return createClassInstanceCreation(n, n.arguments(), n.resolveConstructorBinding(), n.getExpression(), n
        .getAnonymousClassDeclaration(), context);
  }

  private CAstNode createClassInstanceCreation(ASTNode nn, List<?> arguments, IMethodBinding ctorBinding,
      Expression qual, AnonymousClassDeclaration anonDecl, WalkContext context) {
    // a new instruction is actually two things: a NEW object and a CALL to a constructor
    CAstNode newNode;
    CAstNode callNode;
    final String tmpName = "ctor temp";
    // this name is an illegal Java identifier. this var will hold the new object so we can call the constructor on it

    // GENERICS getMethodDeclaration()
    ctorBinding = ctorBinding.getMethodDeclaration(); // unlike polyglot, this will
    // point to a default
    // constructor in the anon class
    MethodReference ctorRef = fIdentityMapper.getMethodRef(ctorBinding);

    // ////////////// PART I: make the NEW expression ///////////////////////////////////////////////////////

    ITypeBinding newType = ctorBinding.getDeclaringClass();
    TypeReference newTypeRef = fIdentityMapper.getTypeRef(newType);

    // new nodes with an explicit enclosing argument, e.g. "outer.new Inner()". They are mostly treated the same, except
    // in JavaCAst2IRTranslator.doNewObject
    CAstNode qualNode = null;

    if (qual == null && newType.getDeclaringClass() != null && ((newType.getModifiers() & Modifier.STATIC) == 0)
        && !newType.isLocal()) {
      // "new X()" expanded into "new this.X()" or "new MyClass.this.X"
      // check isLocal because anonymous classes and local classes are not included.
      ITypeBinding plainThisType = JDT2CAstUtils.getDeclaringClassOfNode(nn); // type of "this"
      ITypeBinding implicitThisType = findClosestEnclosingClassSubclassOf(plainThisType, newType.getDeclaringClass(), ((newType
          .getModifiers() & Modifier.PRIVATE) != 0));
      if (implicitThisType.isEqualTo(plainThisType))
        qualNode = makeNode(context, fFactory, nn, CAstNode.THIS); // "new this.X()"
      else
        qualNode = makeNode(context, fFactory, nn, CAstNode.THIS, fFactory.makeConstant(fIdentityMapper
            .getTypeRef(implicitThisType))); // "new Bla.this.X()"
    } else if (qual != null)
      qualNode = visitNode(qual, context);

    if (qualNode != null)
      newNode = makeNode(context, fFactory, nn, CAstNode.NEW_ENCLOSING, fFactory.makeConstant(newTypeRef), qualNode);
    else
      newNode = makeNode(context, fFactory, nn, CAstNode.NEW, fFactory.makeConstant(newTypeRef));

    ITypeBinding[] newExceptions = new ITypeBinding[] { NoClassDefFoundError, ExceptionInInitializerError, OutOfMemoryError };
    context.cfg().map(newNode, newNode);
    for (ITypeBinding exp : newExceptions) {
      for (Pair<ITypeBinding, Object> catchTarget : context.getCatchTargets(exp)) {
        context.cfg().add(newNode, catchTarget.snd, catchTarget.fst);
      }
    }

    // ANONYMOUS CLASSES
    // ctor already points to right place, so should type ref, so all we have to do is make the entity
    if (anonDecl != null)
      context.addScopedEntity(newNode, visit(anonDecl, context));

    // ////////////// PART II: make the CALL expression ///////////////////////////////////////////////////////
    // setup args & handle exceptions

    CAstNode[] argNodes = new CAstNode[arguments.size() + 2];
    int idx = 0; // args: [ this, callsiteref, <actual args> ]

    // arg 0: this
    argNodes[idx++] = makeNode(context, fFactory, nn, CAstNode.VAR, fFactory.makeConstant(tmpName), fFactory.makeConstant(fTypeDict.getCAstTypeFor(newType)));
    // contains output from newNode (see part III)

    // arg 1: call site ref (WHY?)
    int dummyPC = 0; // Just want to wrap the kind of call; the "rear end" won't care about anything else
    CallSiteReference callSiteRef = CallSiteReference.make(dummyPC, ctorRef, IInvokeInstruction.Dispatch.SPECIAL);
    argNodes[idx++] = fFactory.makeConstant(callSiteRef);

    // rest of args
    for (Object arg : arguments) {
      argNodes[idx++] = (arg instanceof CAstNode) ? ((CAstNode) arg) : visitNode((Expression) arg, context);
    }
    callNode = makeNode(context, fFactory, nn, CAstNode.CALL, argNodes);
    context.cfg().map(nn, callNode);

    handleThrowsFromCall(ctorBinding, nn, context);

    // PART III: make a node with both NEW and CALL
    // Make a LOCAL_SCOPE with a BLOCK_EXPR node child which does three things:
    // 1) declare a temporary variable and assign the new object to it (LOCAL_SCOPE is needed to chain this new variable
    // to this block)
    // 2) CALL the constructor on the new variable
    // 3) access this temporary variable. Since the value of the block is the last thing in the block, the resultant
    // value will be the variable
    return makeNode(context, fFactory, nn, CAstNode.LOCAL_SCOPE, makeNode(context, fFactory, nn, CAstNode.BLOCK_EXPR, makeNode(
        context, fFactory, nn, CAstNode.DECL_STMT, fFactory.makeConstant(new InternalCAstSymbol(tmpName, fTypeDict.getCAstTypeFor(newType), true)), newNode),
        callNode, makeNode(context, fFactory, nn, CAstNode.VAR, fFactory.makeConstant(tmpName))));
  }

  /**
   * 
   * @param met
   * @param mappedAstNode An AST node or object mapped in the CFG: we will call context.cfg().add() on it. Caller must worry about
   *          mapping it with context.cfg().map().
   * @param context
   */
  private void handleThrowsFromCall(IMethodBinding met, Object mappedAstNode, WalkContext context) {
    ITypeBinding[] throwTypes = met.getExceptionTypes();
    for (ITypeBinding thrownType : throwTypes) {
      Collection<Pair<ITypeBinding, Object>> catchTargets = context.getCatchTargets(thrownType);
      for (Pair<ITypeBinding, Object> catchTarget : catchTargets)
        context.cfg().add(mappedAstNode, catchTarget.snd, catchTarget.fst);
    }
    // can also throw runtime exception
    for (Pair<ITypeBinding, Object> catchTarget : context.getCatchTargets(fRuntimeExcType))
      context.cfg().add(mappedAstNode, catchTarget.snd, catchTarget.fst);
  }

  private CAstNode visit(ExpressionStatement n, WalkContext context) {
    return visitNode(n.getExpression(), context);
  }

  private CAstNode visit(SuperMethodInvocation n, WalkContext context) {
    CAstNode target;
    if (n.getQualifier() == null)
      target = makeNode(context, fFactory, n, CAstNode.SUPER);
    else {
      TypeReference owningTypeRef = fIdentityMapper.getTypeRef(n.getQualifier().resolveTypeBinding());
      target = makeNode(context, fFactory, n, CAstNode.SUPER, fFactory.makeConstant(owningTypeRef));
    }
    // GENERICS getMethodDeclaration()
    return createMethodInvocation(n, n.resolveMethodBinding().getMethodDeclaration(), target, n.arguments(), context);
  }

  // FIXME: implicit this
  private CAstNode visit(MethodInvocation n, WalkContext context) {
    // GENERICS getMethodDeclaration()
    IMethodBinding binding = n.resolveMethodBinding().getMethodDeclaration();
    if ((binding.getModifiers() & Modifier.STATIC) != 0) {
      CAstNode target;

      // JLS says: evaluate qualifier & throw away unless of course it's just a class name (or null),
      // in which case we replace the EMPTY with a VOID
      // of course, "this" has no side effects either.
      target = visitNode(n.getExpression(), context);
      if (target.getKind() == CAstNode.EMPTY || target.getKind() == CAstNode.THIS)
        return createMethodInvocation(n, binding, makeNode(context, fFactory, null, CAstNode.VOID), n.arguments(), context);
      else
        return makeNode(context, fFactory, n, CAstNode.BLOCK_EXPR, target, createMethodInvocation(n, binding, makeNode(context,
            fFactory, null, CAstNode.VOID), n.arguments(), context));
      // target is evaluated but thrown away, and only result of method invocation is kept

    } else {
      CAstNode target;
      if (n.getExpression() != null) {
        target = visitNode(n.getExpression(), context);
      } else {
        ITypeBinding typeOfThis = JDT2CAstUtils.getDeclaringClassOfNode(n);

        boolean methodIsPrivate = (binding.getModifiers() & Modifier.PRIVATE) != 0;

        // how could it be in the subtype and private? this only happens the supertype is also an
        // enclosing type. in that case the variable refers to the field in the enclosing instance.
        // NOTE: method may be defined in MyClass's superclass, but we still want to expand
        // this into MyClass, so we have to find the enclosing class which defines this function.

        ITypeBinding implicitThisClass = findClosestEnclosingClassSubclassOf(typeOfThis, binding.getDeclaringClass(),
            methodIsPrivate);
        if (typeOfThis.isEqualTo(implicitThisClass))
          // "foo = 5" -> "this.foo = 5": expand into THIS + class
          target = makeNode(context, fFactory, n, CAstNode.THIS);
        else
          // "foo = 5" -> "MyClass.this.foo = 5" -- inner class: expand into THIS + class
          target = makeNode(context, fFactory, n, CAstNode.THIS, fFactory.makeConstant(fIdentityMapper
              .getTypeRef(implicitThisClass)));
        // NOTE: method may be defined in MyClass's superclass, but we still want to expand
        // this into MyClass, so we have to find the enclosing class which defines this function.
      }
      CAstNode node = createMethodInvocation(n, binding, target, n.arguments(), context);
      // TODO: maybe not exactly right... what if it's a capture? we may have to cast it down a little bit.
      if (binding.getReturnType().isTypeVariable()) {
        // GENERICS: add a cast
        ITypeBinding realtype = JDT2CAstUtils.getErasedType(n.resolveMethodBinding().getReturnType(), ast);
        ITypeBinding fromtype = JDT2CAstUtils.getTypesVariablesBase(binding.getReturnType(), ast);
        if (!realtype.isEqualTo(fromtype))
          return createCast(n, node, fromtype, realtype, context);
      }
      return node;
    }
  }

  private CAstNode createMethodInvocation(ASTNode pos, IMethodBinding methodBinding, CAstNode target,
      List<?> arguments, WalkContext context) {
    // MethodMethodInstance methodInstance = n.methodInstance();
    boolean isStatic = (methodBinding.getModifiers() & Modifier.STATIC) != 0;
    ITypeBinding methodOwner = methodBinding.getDeclaringClass();

    if (!(methodOwner.isInterface() || methodOwner.isClass() || methodOwner.isEnum())) {
      assert false : "owner " + methodOwner + " of " + methodBinding + " is not a class";
    }

    // POPULATE PARAMETERS
    // this (or void for static), method reference, rest of args
    int nFormals = methodBinding.getParameterTypes().length;
    CAstNode[] children = new CAstNode[2 + nFormals];

    // this (or void for static)
    assert target != null : "no receiver for " + methodBinding;
    children[0] = target;

    // method reference
    // unlike polyglot, expression will never be super here. this is handled in SuperMethodInvocation.
    IInvokeInstruction.IDispatch dispatchType;
    if (isStatic)
      dispatchType = IInvokeInstruction.Dispatch.STATIC;
    else if (methodOwner.isInterface())
      dispatchType = IInvokeInstruction.Dispatch.INTERFACE;
    else if ((methodBinding.getModifiers() & Modifier.PRIVATE) != 0 || target.getKind() == CAstNode.SUPER)
      // only one possibility, not a virtual call (I guess?)
      dispatchType = IInvokeInstruction.Dispatch.SPECIAL;
    else
      dispatchType = IInvokeInstruction.Dispatch.VIRTUAL;
    // pass 0 for dummyPC: Just want to wrap the kind of call; the "rear end" won't care about anything else...
    CallSiteReference callSiteRef = CallSiteReference.make(0, fIdentityMapper.getMethodRef(methodBinding), dispatchType);

    children[1] = fFactory.makeConstant(callSiteRef);

    populateArguments(children, methodBinding, arguments, context);

    Object fakeCfgMap = new Object();

    handleThrowsFromCall(methodBinding, fakeCfgMap, context);

    CAstNode result = makeNode(context, fFactory, pos, CAstNode.CALL, children);
    context.cfg().map(fakeCfgMap, result);
    return result;
  }

  /**
   * Populate children, starting at index 2, for the invocation of methodBinding. If varargs are used this function will collapse
   * the proper arguments into an array.
   * 
   * If the number of actuals equals the number of formals and the function is varargs, we have to check the type of the last
   * argument to see if we should "box" it in an array. If the arguments[arguments.length-1] is not an Expression, we cannot get the
   * type, so we do not box it. (Making covariant varargs functions require this behavior)
   * 
   * @param children
   * @param methodBinding
   * @param arguments
   * @param context
   */
  private void populateArguments(CAstNode[] children, IMethodBinding methodBinding, List<?/* CAstNode or Expression */> arguments,
      WalkContext context) {
    int nFormals = methodBinding.getParameterTypes().length;
    assert children.length == nFormals + 2;
    int nActuals = arguments.size();

    ITypeBinding lastArgType = null;
    if (nActuals > 0 && arguments.get(nActuals - 1) instanceof Expression)
      lastArgType = ((Expression) arguments.get(nActuals - 1)).resolveTypeBinding();
    // if the # of actuals equals the # of formals, AND the function is varargs, we have to check
    // to see if the lastArgType is subtype compatible with the type of last parameter (which will be an array).
    // If it is, we pass this array in directly. Otherwise this it is wrapped in an array init.
    // Example: 'void foo(int... x)' can be run via 'foo(5)' or 'foo(new int[] { 5, 6 })' -- both have one argument so we must check
    // the type

    if (nActuals == nFormals
        && (!methodBinding.isVarargs() || lastArgType == null || lastArgType
            .isSubTypeCompatible(methodBinding.getParameterTypes()[nFormals - 1]))) {
      int i = 2;
      for (Object arg : arguments)
        children[i++] = (arg instanceof CAstNode) ? ((CAstNode) arg) : visitNode((Expression) arg, context);
    } else {
      assert nActuals >= (nFormals - 1) && methodBinding.isVarargs() : "Invalid number of parameters for constructor call";
      for (int i = 0; i < nFormals - 1; i++) {
        Object arg = arguments.get(i);
        children[i + 2] = (arg instanceof CAstNode) ? ((CAstNode) arg) : visitNode((Expression) arg, context);
      }

      CAstNode subargs[] = new CAstNode[nActuals - nFormals + 2];
      // nodes for args and one extra for NEW expression
      TypeReference newTypeRef = fIdentityMapper.getTypeRef(methodBinding.getParameterTypes()[nFormals - 1]);
      subargs[0] = makeNode(context, fFactory, null, CAstNode.NEW, fFactory.makeConstant(newTypeRef), fFactory
          .makeConstant(subargs.length - 1));
      for (int j = 1; j < subargs.length; j++) {
        Object arg = arguments.get(j + nFormals - 2);
        subargs[j] = (arg instanceof CAstNode) ? ((CAstNode) arg) : visitNode((Expression) arg, context);
      }
      children[nFormals + 1] = makeNode(context, fFactory, (ASTNode) null, CAstNode.ARRAY_LITERAL, subargs);
    }
  }

  private CAstNode visit(ReturnStatement r, WalkContext context) {
    Expression retExpr = r.getExpression();
    if (retExpr == null)
      return makeNode(context, fFactory, r, CAstNode.RETURN);
    else
      return makeNode(context, fFactory, r, CAstNode.RETURN, visitNode(retExpr, context));
  }

  private CAstNode visit(Assignment n, WalkContext context) {
    if (n.getOperator() == Assignment.Operator.ASSIGN)
      return makeNode(context, fFactory, n, CAstNode.ASSIGN, visitNode(n.getLeftHandSide(), new AssignmentContext(context)),
          visitNode(n.getRightHandSide(), context));
    else {
      CAstNode left = visitNode(n.getLeftHandSide(), context);
      // GENERICs lvalue for pre op hack
      if (left.getKind() == CAstNode.CAST) {
        return doFunkyGenericAssignPreOpHack(n, context);
      }

      // +=, %=, &=, etc.
      CAstNode result = makeNode(context, fFactory, n, CAstNode.ASSIGN_PRE_OP, left, visitNode(n.getRightHandSide(), context),
          JDT2CAstUtils.mapAssignOperator(n.getOperator()));

      // integer division by zero
      if (JDT2CAstUtils.isLongOrLess(n.resolveTypeBinding())
          && (n.getOperator() == Assignment.Operator.DIVIDE_ASSIGN || n.getOperator() == Assignment.Operator.REMAINDER_ASSIGN)) {
        Collection<Pair<ITypeBinding, Object>> excTargets = context.getCatchTargets(fDivByZeroExcType);
        if (!excTargets.isEmpty()) {
          for (Pair<ITypeBinding, Object> catchPair : excTargets)
            context.cfg().add(result, catchPair.snd, fDivByZeroExcType);
        } else {
          context.cfg().add(result, CAstControlFlowMap.EXCEPTION_TO_EXIT, fDivByZeroExcType);
        }
      }

      return result;
    }
  }

  /**
   * Consider the case:
   * 
   * <pre>
   * String real_oneheyya = (((returnObjectWithSideEffects().y))+=&quot;hey&quot;)+&quot;ya&quot;
   * </pre>
   * 
   * where field 'y' is parameterized to type string. then += is not defined for type 'object'. This function is a hack that expands
   * the code into an assignment and binary operation.
   * 
   * @param context
   */
  private CAstNode doFunkyGenericAssignPreOpHack(Assignment assign, WalkContext context) {
    Expression left = assign.getLeftHandSide();
    Expression right = assign.getRightHandSide();

    // consider the case:
    // String real_oneheyya = (((returnObjectWithSideEffects().y))+="hey")+"ya"; // this is going to be a MAJOR pain...
    // where field 'y' is parameterized to type string. then += is not defined for type 'object'. we want to transform
    // it kind of like this, except we have to define temp.
    // String real_oneheyya = (String)((temp=cg2WithSideEffects()).y = (String)temp.y + "hey")+"ya";
    // ----------------------------------------------------------------
    //
    // we are responsible for underlined portion
    // CAST(LOCAL SCOPE(BLOCK EXPR(DECL STMT(temp,
    // left.target),ASSIGN(OBJECT_REF(temp,y),BINARY_EXPR(CAST(OBJECT_REF(Temp,y)),RIGHT)))))
    // yeah, I know, it's cheating, LOCAL SCOPE / DECL STMT inside an expression ... will it work?

    while (left instanceof ParenthesizedExpression)
      left = ((ParenthesizedExpression) left).getExpression();
    assert left instanceof FieldAccess : "Cast in assign pre-op but no field access?!";

    FieldAccess field = (FieldAccess) left;
    InfixExpression.Operator infixop = JDT2CAstUtils.mapAssignOperatorToInfixOperator(assign.getOperator());

    // DECL_STMT: temp = ...;
    final String tmpName = "temp generic preop hack"; // illegal Java identifier
    CAstNode exprNode = visitNode(field.getExpression(), context);
    CAstNode tmpDeclNode = makeNode(context, fFactory, left, CAstNode.DECL_STMT, fFactory.makeConstant(new InternalCAstSymbol(
        tmpName, fTypeDict.getCAstTypeFor(field.getExpression().resolveTypeBinding()), true)), exprNode);

    // need two object refndoes "temp.y"
    CAstNode obref1 = createFieldAccess(makeNode(context, fFactory, left, CAstNode.VAR, fFactory.makeConstant(tmpName), fFactory.makeConstant(fTypeDict.getCAstTypeFor(field.resolveFieldBinding().getType()))), field
        .getName().getIdentifier(), field.resolveFieldBinding(), left, new AssignmentContext(context));

    CAstNode obref2 = createFieldAccess(makeNode(context, fFactory, left, CAstNode.VAR, fFactory.makeConstant(tmpName), fFactory.makeConstant(fTypeDict.getCAstTypeFor(field.resolveFieldBinding().getType()))), field
        .getName().getIdentifier(), field.resolveFieldBinding(), left, context);
    ITypeBinding realtype = JDT2CAstUtils.getErasedType(field.resolveFieldBinding().getType(), ast);
    ITypeBinding fromtype = JDT2CAstUtils
        .getTypesVariablesBase(field.resolveFieldBinding().getVariableDeclaration().getType(), ast);
    CAstNode castedObref = obref2;// createCast(left, obref2, fromtype, realtype, context);

    // put it all together
    // CAST(LOCAL SCOPE(BLOCK EXPR(DECL STMT(temp,
    // left.target),ASSIGN(OBJECT_REF(temp,y),BINARY_EXPR(CAST(OBJECT_REF(Temp,y)),RIGHT)))))
    CAstNode result = makeNode(context, fFactory, assign, CAstNode.LOCAL_SCOPE, makeNode(context, fFactory, assign,
        CAstNode.BLOCK_EXPR, tmpDeclNode, makeNode(context, fFactory, assign, CAstNode.ASSIGN, obref1, createInfixExpression(
            infixop, realtype, left.getStartPosition(), left.getLength(), castedObref, right, context))));

    return createCast(assign, result, fromtype, realtype, context);
  }

  private CAstNode visit(ParenthesizedExpression n, WalkContext context) {
    return visitNode(n.getExpression(), context);
  }

  private CAstNode visit(BooleanLiteral n) {
    return fFactory.makeConstant(n.booleanValue());
  }

  private CAstNode visit(CharacterLiteral n) {
    return fFactory.makeConstant(n.charValue());
  }

  private CAstNode visit() {
    return fFactory.makeConstant(null);
  }

  private CAstNode visit(StringLiteral n) {
    return fFactory.makeConstant(n.getLiteralValue());
  }

  private CAstNode visit(TypeLiteral n, WalkContext context) {
    String typeName = fIdentityMapper.typeToTypeID(n.resolveTypeBinding());
    return makeNode(context, fFactory, n, CAstNode.TYPE_LITERAL_EXPR, fFactory.makeConstant(typeName));
  }

  private CAstNode visit(NumberLiteral n) {
    return fFactory.makeConstant(n.resolveConstantExpressionValue());
  }

  /**
   * SimpleName can be a field access, local, or class name (do nothing case)
   * 
   * @param n
   * @param context
   */
  private CAstNode visit(SimpleName n, WalkContext context) {
    // class name, handled above in either method invocation, qualified name, or qualified this
    if (n.resolveBinding() instanceof ITypeBinding)
      return makeNode(context, fFactory, null, CAstNode.EMPTY);

    assert n.resolveBinding() instanceof IVariableBinding : "SimpleName's binding, " + n.resolveBinding() + ", is not a variable or a type binding!";

    IVariableBinding binding = (IVariableBinding) n.resolveBinding();
    binding = binding.getVariableDeclaration(); // ignore weird generic stuff

    // TODO: enum constants
    if (binding.isField()) {
      // enum constants ...

      // implicit field access -- implicit this or class
      CAstNode targetNode;

      if ((binding.getModifiers() & Modifier.STATIC) != 0) {
        // "foo = 5" -> "MyClass.foo = 5" or "SomeEnclosingClass.foo" = 5
        targetNode = makeNode(context, fFactory, null, CAstNode.EMPTY); // we will get type from binding. no side
        // effects in evaluating a class name, so NOP
        // here.
      } else {
        ITypeBinding typeOfThis = JDT2CAstUtils.getDeclaringClassOfNode(n);

        boolean fieldIsPrivate = (binding.getModifiers() & Modifier.PRIVATE) != 0;

        // how could it be in the subtype and private? this only happens the supertype is also an
        // enclosing type. in that case the variable refers to the field in the enclosing instance.
        // NOTE: method may be defined in MyClass's superclass, but we still want to expand
        // this into MyClass, so we have to find the enclosing class which defines this function.

        ITypeBinding implicitThisClass = findClosestEnclosingClassSubclassOf(typeOfThis, binding.getDeclaringClass(),
            fieldIsPrivate);
        if (typeOfThis.isEqualTo(implicitThisClass))
          // "foo = 5" -> "this.foo = 5": expand into THIS + class
          targetNode = makeNode(context, fFactory, n, CAstNode.THIS);
        else
          // "foo = 5" -> "MyClass.this.foo = 5" -- inner class: expand into THIS + class
          targetNode = makeNode(context, fFactory, n, CAstNode.THIS, fFactory.makeConstant(fIdentityMapper
              .getTypeRef(implicitThisClass)));
        // NOTE: method may be defined in MyClass's superclass, but we still want to expand
        // this into MyClass, so we have to find the enclosing class which defines this function.
        // fFactory.makeConstant(owningTypeRef));
      }
      return createFieldAccess(targetNode, n.getIdentifier(), binding, n, context);

    } else {
      // local
      CAstType t = fTypeDict.getCAstTypeFor(((IVariableBinding)n.resolveBinding()).getType());
      return makeNode(context, fFactory, n, CAstNode.VAR, fFactory.makeConstant(n.getIdentifier()), fFactory.makeConstant(t));
    }

  }

  /**
   * Sees if a field defined in owningTypeRef is contained & accessible to a type of typeOfThis. That is, if owningTypeRef ==
   * typeOfThis or typeOfThis is a subtype and isPrivate is false. If this is not that case, looks in the enclosing class of
   * typeOfThis and tries again, and its enclosing class, ...
   * 
   * Essentially if we have a field/method referenced only by name and we know its type (owningTypeRef), this function will return
   * owningTypeRef or the subtype that the field is accessed thru, for expanding "f = 5" into "TheClass.this.f = 5".
   * 
   * @param typeOfThis
   * @param isPrivate
   */
  private static ITypeBinding findClosestEnclosingClassSubclassOf(ITypeBinding typeOfThis, ITypeBinding owningType, boolean isPrivate) {
    // GENERICS
//    if (owningType.isParameterizedType())
//      owningType = owningType.getTypeDeclaration();
//    if (typeOfThis.isParameterizedType())
//      typeOfThis = typeOfThis.getTypeDeclaration();
//    // typeOfThis.getTypeDeclaration()
    owningType = owningType.getErasure();

    ITypeBinding current = typeOfThis;
    while (current != null) {
      current = current.getErasure();
      // Walk the hierarchy rather than using isSubTypeCompatible to handle
      // generics -- we need to perform erasure of super types
      boolean isInSubtype = false;//current.isSubTypeCompatible(owningType);
      ITypeBinding supertp = current;
      while (supertp != null){
        supertp = supertp.getErasure();
        // Use isSubTypeCompatible even though we are manually walking type hierarchy --
        // that way interfaces are handled without us having to do it manually.
        if (supertp.isSubTypeCompatible(owningType)){
          isInSubtype = true;
          break;
        }
        supertp = supertp.getSuperclass();
      }

      // how could it be in the subtype and private? this only happens the supertype is also an
      // enclosing type. in that case the variable refers to the field in the enclosing instance.

      if (current.isEqualTo(owningType) || (isInSubtype && !isPrivate))
        return current;

      current = current.getDeclaringClass();
    }

    Assertions.UNREACHABLE("Couldn't find field in class or enclosing class or superclasses of these");
    return null;
  }

  /**
   * Process a field access. Semantics differ for static and instance fields. Fields can throw null pointer exceptions so we must
   * connect proper exceptional edges in the CFG.
   * 
   * @param n
   * @param context
   */
  private CAstNode visit(FieldAccess n, WalkContext context) {
    CAstNode targetNode = visitNode(n.getExpression(), context);
    return createFieldAccess(targetNode, n.getName().getIdentifier(), n.resolveFieldBinding(), n, context);
  }

  /**
   * Used by visit(FieldAccess) and visit(SimpleName) -- implicit "this" / static field access. things from 'this' cannot throw an
   * exception. maybe handle this in here as a special case? i don't know... or check if targetNode is THIS, that should even work
   * for this.x = 5 and (this).x = 5
   * 
   * @param targetNode Used to evaluate the field access. In the case of static field accesses, this is included in the first part of a
   *          block -- thus it is evaluated for any side effects but thrown away.
   * @param fieldName Name of the field.
   * @param positioningNode Used only for making a JdtPosition.
   * @param context
   */
  private CAstNode createFieldAccess(CAstNode targetNode, String fieldName, IVariableBinding possiblyParameterizedBinding,
      ASTNode positioningNode, WalkContext context) {

    IVariableBinding fieldBinding = possiblyParameterizedBinding.getVariableDeclaration();

    ITypeBinding targetType = fieldBinding.getDeclaringClass();

    if (targetType == null) { // array
      assert fieldName.equals("length") : "null targetType but not aray length access";
      return makeNode(context, fFactory, positioningNode, CAstNode.ARRAY_LENGTH, targetNode);
    }

    assert fieldBinding.isField() : "Field binding is not a field?!"; // we can probably safely delete this
    // check

    // translate JDT field ref to WALA field ref
    FieldReference fieldRef = fIdentityMapper.getFieldRef(fieldBinding);

    if ((fieldBinding.getModifiers() & Modifier.STATIC) != 0) {
      // JLS says: evaluate the target of the field ref and throw it away.
      // Hence the following block expr, whose 2 children are the target
      // evaluation
      // followed by the OBJECT_REF with a null target child (which the
      // "back-end"
      // CAst -> IR translator interprets as a static ref).
      // TODO: enum constants

      // don't worry about generics (can't declare static fields with type variables)
      if (fieldBinding.getConstantValue() != null) {
        return makeNode(context, fFactory, positioningNode, CAstNode.BLOCK_EXPR, targetNode, fFactory.makeConstant(fieldBinding
            .getConstantValue()));
      } else {
        return makeNode(context, fFactory, positioningNode, CAstNode.BLOCK_EXPR, targetNode,
            makeNode(context, fFactory, positioningNode, CAstNode.OBJECT_REF, makeNode(context, fFactory, null, CAstNode.VOID),
                fFactory.makeConstant(fieldRef)));
      }
    } else {
      CAstNode refNode = makeNode(context, fFactory, positioningNode, CAstNode.OBJECT_REF, targetNode, fFactory
          .makeConstant(fieldRef));

      if (targetNode.getKind() != CAstNode.THIS) { // this.x will never throw a null pointer exception, because this
        // can never be null
        Collection<Pair<ITypeBinding, Object>> excTargets = context.getCatchTargets(fNullPointerExcType);
        if (!excTargets.isEmpty()) {
          // connect NPE exception edge to relevant catch targets
          // (presumably only one)
          for (Pair<ITypeBinding, Object> catchPair : excTargets) {
            context.cfg().add(refNode, catchPair.snd, fNullPointerExcType);
          }
        } else {
          // connect exception edge to exit
          context.cfg().add(refNode, CAstControlFlowMap.EXCEPTION_TO_EXIT, fNullPointerExcType);
        }

        context.cfg().map(refNode, refNode);
      }

      if (fieldBinding.getConstantValue() != null) {
        // don't have to worry about generics, a constant of generic type can only be null
        return makeNode(context, fFactory, positioningNode, CAstNode.BLOCK_EXPR, refNode,
        // evaluating 'refNode' can have side effects, so we must still evaluate it!
            fFactory.makeConstant(fieldBinding.getConstantValue()));
      } else {
        if (fieldBinding.getType().isTypeVariable() && !context.needLValue()) {
          // GENERICS: add a cast
          ITypeBinding realtype = JDT2CAstUtils.getErasedType(possiblyParameterizedBinding.getType(), ast);
          ITypeBinding fromtype = JDT2CAstUtils.getTypesVariablesBase(fieldBinding.getType(), ast);
          if (!realtype.isEqualTo(fromtype))
            return createCast(positioningNode, refNode, fromtype, realtype, context);
        }
        return refNode;
      }
    }

  }

  private CAstNode visit(ThisExpression n, WalkContext context) {
    if (n.getQualifier() != null) {
      ITypeBinding owningType = n.getQualifier().resolveTypeBinding();
      TypeReference owningTypeRef = fIdentityMapper.getTypeRef(owningType);
      return makeNode(context, fFactory, n, CAstNode.THIS, fFactory.makeConstant(owningTypeRef));
    } else
      return makeNode(context, fFactory, n, CAstNode.THIS);
  }

  /**
   * QualifiedNames may be: 1) static of non-static field accesses -- we handle this case here 2) type names used in the context of:
   * a) field access (QualifiedName) b) method invocation c) qualifier of "this" in these cases we get the binding info in each of
   * these three functions and use them there, thus we return an EMPTY (no-op) here. 3) package names used in the context of a
   * QualifiedName class we return a EMPTY (no-op) here.
   * 
   * @param n
   * @param context
   */
  private CAstNode visit(QualifiedName n, WalkContext context) {
    // "package.Class" is a QualifiedName, but also is "Class.staticField"
    // only handle if it's a "Class.staticField" ("Field" in polyglot AST)

    if (n.resolveBinding() instanceof IVariableBinding) {
      IVariableBinding binding = (IVariableBinding) n.resolveBinding();
      assert binding.isField() : "Non-field variable QualifiedName!";

      // if field access is static, visitNode(n.getQualifier()) will come back here
      // and we will return an EMPTY node
      return createFieldAccess(visitNode(n.getQualifier(), context), n.getName().getIdentifier(), binding, n, context);
    } else
      return makeNode(context, fFactory, null, CAstNode.EMPTY);
    // type name, handled in surrounding context
  }

  private CAstNode visit(InfixExpression n, WalkContext context) {
    Expression left = n.getLeftOperand();
    ITypeBinding leftType = left.resolveTypeBinding();
    int leftStartPosition = left.getStartPosition();

    CAstNode leftNode = visitNode(left, context);

    int leftLength = n.getLeftOperand().getLength();
    CAstNode result = createInfixExpression(n.getOperator(), leftType, leftStartPosition, leftLength, leftNode,
        n.getRightOperand(), context);

    if (n.hasExtendedOperands()) {
      // keep on adding operands on the right side

      leftLength = n.getRightOperand().getStartPosition() + n.getRightOperand().getLength() - leftStartPosition;
      for (Expression operand : (Iterable<Expression>) n.extendedOperands()) {
        result = createInfixExpression(n.getOperator(), leftType, leftStartPosition, leftLength, result, operand, context);

        if (leftType.isPrimitive() && operand.resolveTypeBinding().isPrimitive())
          leftType = JDT2CAstUtils.promoteTypes(leftType, operand.resolveTypeBinding(), ast); // TODO: boxing
        else
          leftType = operand.resolveTypeBinding();

        // leftStartPosition doesn't change, beginning is always the first operand
        leftLength = operand.getStartPosition() + operand.getLength() - leftStartPosition;
      }
    }

    return result;
  }

  private CAstNode createInfixExpression(InfixExpression.Operator op, ITypeBinding leftType, int leftStartPosition, int leftLength,
      CAstNode leftNode, Expression right, WalkContext context) {
    CAstNode rightNode = visitNode(right, context);

    int start = leftStartPosition;
    int end = right.getStartPosition() + right.getLength();
    T pos = makePosition(start, end);
    T rightPos = makePosition(leftStartPosition, leftStartPosition + leftLength);
    T leftPos = makePosition(right.getStartPosition(), right.getStartPosition() + right.getLength());

    if (op == InfixExpression.Operator.CONDITIONAL_AND) {
      return makeNode(context, fFactory, pos, CAstNode.IF_EXPR, leftNode, rightNode, fFactory.makeConstant(false));
    } else if (op == InfixExpression.Operator.CONDITIONAL_OR) {
      return makeNode(context, fFactory, pos, CAstNode.IF_EXPR, leftNode, fFactory.makeConstant(true), rightNode);
    } else {
      ITypeBinding rightType = right.resolveTypeBinding();
      if (leftType.isPrimitive() && rightType.isPrimitive()) {
        // TODO: boxing
        ITypeBinding result = JDT2CAstUtils.promoteTypes(leftType, rightType, ast);

        // cast to proper type
        if (!result.isEqualTo(leftType))
          leftNode = makeNode(context, fFactory, leftPos, CAstNode.CAST, fFactory.makeConstant(fTypeDict.getCAstTypeFor(result)),
              leftNode, fFactory.makeConstant(fTypeDict.getCAstTypeFor(leftType)));
        if (!result.isEqualTo(rightType))
          rightNode = makeNode(context, fFactory, rightPos, CAstNode.CAST, fFactory.makeConstant(fTypeDict.getCAstTypeFor(result)),
              rightNode, fFactory.makeConstant(fTypeDict.getCAstTypeFor(rightType)));

        CAstNode opNode = makeNode(context, fFactory, pos, CAstNode.BINARY_EXPR, JDT2CAstUtils.mapBinaryOpcode(op), leftNode,
            rightNode);

        // divide by zero exception implicitly thrown
        if (JDT2CAstUtils.isLongOrLess(leftType)
            && JDT2CAstUtils.isLongOrLess(rightType)
            && (JDT2CAstUtils.mapBinaryOpcode(op) == CAstOperator.OP_DIV || JDT2CAstUtils.mapBinaryOpcode(op) == CAstOperator.OP_MOD)) {
          Collection<Pair<ITypeBinding, Object>> excTargets = context.getCatchTargets(fDivByZeroExcType);
          if (!excTargets.isEmpty()) {
            for (Pair<ITypeBinding, Object> catchPair : excTargets) {
              context.cfg().add(op, catchPair.snd, fDivByZeroExcType);
            }
          } else {
            context.cfg().add(op, CAstControlFlowMap.EXCEPTION_TO_EXIT, fDivByZeroExcType);
          }
        }

        return opNode;

      } else {
        return makeNode(context, fFactory, pos, CAstNode.BINARY_EXPR, JDT2CAstUtils.mapBinaryOpcode(op), leftNode, rightNode);
      }
    }
  }

  private CAstNode visit(ConstructorInvocation n, WalkContext context) {
    // GENERICS getMethodDeclaration()
    return createConstructorInvocation(n.resolveConstructorBinding().getMethodDeclaration(), n.arguments(), n, context, false);
  }

  private CAstNode visit(SuperConstructorInvocation n, WalkContext context) {
    // FIXME: use expression?! polyglot doesn't handle it and it seems to be a very rare case.
    // class E { class X {} }
    // class Y extends E.X { Y(E e) { e.super(); } }
    // GENERICS getMethodDeclaration()
    return createConstructorInvocation(n.resolveConstructorBinding().getMethodDeclaration(), n.arguments(), n, context, true);
  }

  private CAstNode visit(SuperFieldAccess n, WalkContext context) {
    CAstNode targetNode;
    if (n.getQualifier() == null)
      targetNode = makeNode(context, fFactory, n, CAstNode.SUPER);
    else {
      TypeReference owningTypeRef = fIdentityMapper.getTypeRef(n.getQualifier().resolveTypeBinding());
      targetNode = makeNode(context, fFactory, n, CAstNode.SUPER, fFactory.makeConstant(owningTypeRef));
    }
    return createFieldAccess(targetNode, n.getName().getIdentifier(), n.resolveFieldBinding(), n, context);
  }

  /**
   * callerNode: used for positioning and also in CFG (handleThrowsFrom Call)
   */
  private CAstNode createConstructorInvocation(IMethodBinding ctorBinding, List<Expression> arguments, ASTNode callerNode,
      WalkContext context, boolean isSuper) {
    ITypeBinding ctorType = ctorBinding.getDeclaringClass();
    assert ctorType.isClass();

    // dummy PC = 0 -- Just want to wrap the kind of call; the "rear end"
    // won't care about anything else...
    CallSiteReference callSiteRef = CallSiteReference.make(0, fIdentityMapper.getMethodRef(ctorBinding),
        IInvokeInstruction.Dispatch.SPECIAL);

    int nFormals = ctorBinding.getParameterTypes().length;
    CAstNode[] children = new CAstNode[2 + nFormals];
    // this, call site ref, args

    CAstNode targetNode = makeNode(context, fFactory, callerNode, isSuper ? CAstNode.SUPER : CAstNode.THIS);

    children[0] = targetNode;
    children[1] = fFactory.makeConstant(callSiteRef);

    populateArguments(children, ctorBinding, arguments, context);

    handleThrowsFromCall(ctorBinding, callerNode, context);

    CAstNode result = makeNode(context, fFactory, callerNode, CAstNode.CALL, children);
    context.cfg().map(context, result);
    return result;

  }

  private CAstNode visit(IfStatement n, WalkContext context) {
    return makeNode(context, fFactory, n, CAstNode.IF_STMT, visitNode(n.getExpression(), context), visitNode(n.getThenStatement(),
        context), visitNode(n.getElseStatement(), context));
  }

  private CAstNode visit(InstanceofExpression n, WalkContext context) {
    return makeNode(context, fFactory, n, CAstNode.INSTANCEOF, fFactory.makeConstant(fTypeDict.getCAstTypeFor(n.getRightOperand()
        .resolveBinding())), visitNode(n.getLeftOperand(), context));
  }

  private CAstNode visit(CastExpression n, WalkContext context) {
    Expression arg = n.getExpression();
    ITypeBinding castedFrom = arg.resolveTypeBinding();
    ITypeBinding castedTo = n.getType().resolveBinding();
    return createCast(n, visitNode(arg, context), castedFrom, castedTo, context);
  }

  private CAstNode createCast(ASTNode pos, CAstNode argNode, ITypeBinding castedFrom, ITypeBinding castedTo, WalkContext context) {
    Object cfgMapDummy = new Object(); // safer as 'pos' may be used for another purpose (i.e., this could be an implicit cast)

    // null can go into anything (e.g. in "((Foobar) null)" null can be assumed to be of type Foobar already)
    if (castedFrom.isNullType())
      castedFrom = castedTo;

    CAstNode ast = makeNode(context, fFactory, pos, CAstNode.CAST, fFactory.makeConstant(fTypeDict.getCAstTypeFor(castedTo)),
        argNode, fFactory.makeConstant(fTypeDict.getCAstTypeFor(castedFrom)));

    Collection<Pair<ITypeBinding, Object>> excTargets = context.getCatchTargets(fClassCastExcType);
    if (!excTargets.isEmpty()) {
      // connect ClassCastException exception edge to relevant catch targets
      // (presumably only one)
      for (Pair<ITypeBinding, Object> catchPair : excTargets) {
        context.cfg().add(cfgMapDummy, catchPair.snd, fClassCastExcType);
      }
    } else {
      // connect exception edge to exit
      context.cfg().add(cfgMapDummy, CAstControlFlowMap.EXCEPTION_TO_EXIT, fClassCastExcType);
    }

    context.cfg().map(cfgMapDummy, ast);
    return ast;
  }

  private CAstNode visit(ConditionalExpression n, WalkContext context) {
    return makeNode(context, fFactory, n, CAstNode.IF_EXPR, visitNode(n.getExpression(), context), visitNode(n.getThenExpression(),
        context), visitNode(n.getElseExpression(), context));
  }

  private CAstNode visit(PostfixExpression n, WalkContext context) {
    CAstOperator op = (n.getOperator() == PostfixExpression.Operator.DECREMENT) ? CAstOperator.OP_SUB : CAstOperator.OP_ADD;
    return makeNode(context, fFactory, n, CAstNode.ASSIGN_POST_OP, visitNode(n.getOperand(), context), fFactory.makeConstant(1), op);
  }

  private CAstNode visit(PrefixExpression n, WalkContext context) {
    PrefixExpression.Operator op = n.getOperator();

    if (op == PrefixExpression.Operator.DECREMENT || op == PrefixExpression.Operator.INCREMENT) {
      CAstOperator castOp = (n.getOperator() == PrefixExpression.Operator.DECREMENT) ? CAstOperator.OP_SUB : CAstOperator.OP_ADD;
      return makeNode(context, fFactory, n, CAstNode.ASSIGN_PRE_OP, visitNode(n.getOperand(), context), fFactory.makeConstant(1),
          castOp);
    } else if (op == PrefixExpression.Operator.PLUS) {
      return visitNode(n.getOperand(), context); // drop useless unary plus operator
    } else if (op == PrefixExpression.Operator.MINUS) {
      CAstNode zero;
      if (JDT2CAstUtils.isLongOrLess(n.getOperand().resolveTypeBinding()))
        zero = fFactory.makeConstant(0L);
      else
        zero = fFactory.makeConstant(0.0);
      return makeNode(context, fFactory, n, CAstNode.BINARY_EXPR, CAstOperator.OP_SUB, zero, visitNode(n.getOperand(), context));
    } else { // ! and ~
      CAstOperator castOp = (n.getOperator() == PrefixExpression.Operator.NOT) ? CAstOperator.OP_NOT : CAstOperator.OP_BITNOT;
      return makeNode(context, fFactory, n, CAstNode.UNARY_EXPR, castOp, visitNode(n.getOperand(), context));
    }
  }

  private CAstNode visit(EmptyStatement n, WalkContext context) {
    CAstNode result = makeNode(context, fFactory, n, CAstNode.EMPTY);
    context.cfg().map(n, result); // why is this necessary? for break / continue targets? (they use an empty statement)
    return result;
  }

  private CAstNode visit(AssertStatement n, WalkContext context) {
    return makeNode(context, fFactory, n, CAstNode.ASSERT, visitNode(n.getExpression(), context));
  }

  // ////////////////
  // LOOPS -- special handling of for and continue
  // ////////////////

  private CAstNode visit(LabeledStatement n, WalkContext context) {

    // find the first non-block statement ant set-up the label map (useful for breaking many fors)
    ASTNode stmt = n.getBody();
    while (stmt instanceof Block)
      stmt = (ASTNode) ((Block) stmt).statements().iterator().next();

    if (n.getParent() != null)
      // if not a synthetic node from a break/continue -- don't pollute namespace with label, we get it thru the context
      context.getLabelMap().put(stmt, n.getLabel().getIdentifier());

    CAstNode result;
    if (!(n.getBody() instanceof EmptyStatement)) {
      ASTNode breakTarget = makeBreakOrContinueTarget(n, n.getLabel().getIdentifier());
      CAstNode breakNode = visitNode(breakTarget, context);
      WalkContext child = new BreakContext(context, n.getLabel().getIdentifier(), breakTarget);

      result = makeNode(context, fFactory, n, CAstNode.BLOCK_STMT, makeNode(context, fFactory, n, CAstNode.LABEL_STMT, fFactory
          .makeConstant(n.getLabel().getIdentifier()), visitNode(n.getBody(), child)), breakNode);
    } else {
      result = makeNode(context, fFactory, n, CAstNode.LABEL_STMT, fFactory.makeConstant(n.getLabel().getIdentifier()), visitNode(n
          .getBody(), context));
    }

    context.cfg().map(n, result);

    if (n.getParent() != null)
      // if not a synthetic node from a break/continue -- don't pollute namespace with label, we get it thru the context
      context.getLabelMap().remove(stmt);

    return result;
  }

  /**
   * Make a fake labeled node with no body, as an anchor to go to
   */
  private ASTNode makeBreakOrContinueTarget(ASTNode loop, String name) {
    LabeledStatement labeled = ast.newLabeledStatement();
    labeled.setBody(ast.newEmptyStatement());
    labeled.setSourceRange(loop.getStartPosition(), loop.getLength());
    labeled.setLabel(ast.newSimpleName(name)); // we don't have to worry about namespace conflicts as it is only
    // definedwithin
    return labeled;
  }

  private CAstNode visit(BreakStatement n, WalkContext context) {
    String label = n.getLabel() == null ? null : n.getLabel().getIdentifier();
    ASTNode target = context.getBreakFor(label);
    assert target != null;
    CAstNode result = makeNode(context, fFactory, n, CAstNode.GOTO);
    context.cfg().map(n, result);
    context.cfg().add(n, target, null);
    return result;
  }

  private CAstNode visit(ContinueStatement n, WalkContext context) {
    String label = n.getLabel() == null ? null : n.getLabel().getIdentifier();
    ASTNode target = context.getContinueFor(label);
    assert target != null;
    CAstNode result = makeNode(context, fFactory, n, CAstNode.GOTO);
    context.cfg().map(n, result);
    context.cfg().add(n, target, null);
    return result;
  }

  private CAstNode visit(WhileStatement n, WalkContext context) {
    Expression cond = n.getExpression();
    Statement body = n.getBody();

    ASTNode breakTarget = makeBreakOrContinueTarget(n, "breakLabel" + n.getStartPosition());
    CAstNode breakNode = visitNode(breakTarget, context);

    ASTNode continueTarget = makeBreakOrContinueTarget(n, "continueLabel" + n.getStartPosition());
    CAstNode continueNode = visitNode(continueTarget, context);

    String loopLabel = context.getLabelMap().get(n);
    LoopContext lc = new LoopContext(context, loopLabel, breakTarget, continueTarget);

    /*
     * The following loop is created sligtly differently than in jscore. It doesn't have a specific target for continue.
     */
    return makeNode(context, fFactory, n, CAstNode.BLOCK_STMT, makeNode(context, fFactory, n, CAstNode.LOOP, visitNode(cond,
        context), makeNode(context, fFactory, n, CAstNode.BLOCK_STMT, visitNode(body, lc), continueNode)),
        breakNode);
  }

  private CAstNode getSwitchCaseConstant(SwitchCase n, WalkContext context) {
    // TODO: enums
    Expression expr = n.getExpression();
    Object constant = (expr == null) ? new Integer(0) : expr.resolveConstantExpressionValue(); // default case label of
    // "0" (what polyglot
    // does). we also set
    // SWITCH_DEFAULT
    // somewhere else
    // polyglot converts all labels to longs. why? who knows...
    if (constant instanceof Character)
      constant = new Long(((Character) constant).charValue());
    else if (constant instanceof Byte)
      constant = new Long(((Byte) constant).longValue());
    else if (constant instanceof Integer)
      constant = new Long(((Integer) constant).longValue());
    else if (constant instanceof Short)
      constant = new Long(((Short) constant).longValue());

    if (constant != null) {
      return fFactory.makeConstant(constant);
    } else if (expr instanceof SimpleName) {
      // enum constant
      return visit((SimpleName) expr, context);
    } else {
      Assertions.UNREACHABLE("null constant for non-enum switch case!");
      return null;
    }
  }

  private CAstNode visit(SwitchCase n, WalkContext context) {
    CAstNode label = makeNode(context, fFactory, n, CAstNode.LABEL_STMT, getSwitchCaseConstant(n, context));

    context.cfg().map(n, label);
    return label;
  }

  private CAstNode visit(SwitchStatement n, WalkContext context) {
    ASTNode breakTarget = makeBreakOrContinueTarget(n, "breakLabel" + n.getStartPosition());
    CAstNode breakAst = visitNode(breakTarget, context);
    String loopLabel = context.getLabelMap().get(n); // set by labeled statement (if there is one before this
    // switch statement)
    WalkContext childContext = new BreakContext(context, loopLabel, breakTarget);
    Expression cond = n.getExpression();
    List<Statement> cases = n.statements();

    // First compute the control flow edges for the various case labels
    for (int i = 0; i < cases.size(); i++) {
      Statement se = cases.get(i);
      if (se instanceof SwitchCase) {
        SwitchCase c = (SwitchCase) se;

        if (c.isDefault())
          context.cfg().add(n, c, CAstControlFlowMap.SWITCH_DEFAULT);
        else
          context.cfg().add(n, c, getSwitchCaseConstant(c, context));
        // if we don't do this, we may not get a constant but a
        // block expression or something else
      }
    }

    ArrayList<CAstNode> caseNodes = new ArrayList<>();

    // polyglot bundles all statements in between two statements into a block.
    // this is temporary place to hold current bundle of nodes.
    ArrayList<CAstNode> currentBlock = new ArrayList<>();

    // Now produce the CAst representation for each case
    for (Statement s : cases) {
      if (s instanceof SwitchCase) {
        if (!currentBlock.isEmpty()) {
          // bundle up statements before this case
          CAstNode stmtNodes[] = currentBlock.toArray(new CAstNode[currentBlock.size()]);
          // make position from start of first statement to end of last statement
          T positionOfAll = makePosition(childContext.pos().getPosition(stmtNodes[0]).getFirstOffset(), childContext
              .pos().getPosition(stmtNodes[stmtNodes.length - 1]).getLastOffset());
          caseNodes.add(makeNode(childContext, fFactory, positionOfAll, CAstNode.BLOCK_STMT, stmtNodes));
          currentBlock.clear();
        }
        caseNodes.add(visitNode(s, childContext));
      } else {
        visitNodeOrNodes(s, childContext, currentBlock);
      }
    }
    if (!currentBlock.isEmpty()) {
      // bundle up statements before this case
      CAstNode stmtNodes[] = currentBlock.toArray(new CAstNode[currentBlock.size()]);
      // make position from start of first statement to end of last statement
      T positionOfAll = makePosition(childContext.pos().getPosition(stmtNodes[0]).getFirstOffset(), childContext.pos()
          .getPosition(stmtNodes[stmtNodes.length - 1]).getLastOffset());
      caseNodes.add(makeNode(childContext, fFactory, positionOfAll, CAstNode.BLOCK_STMT, stmtNodes));
    }

    // Now produce the switch stmt itself
    CAstNode switchAst = makeNode(context, fFactory, n, CAstNode.SWITCH, visitNode(cond, context), makeNode(context, fFactory, n,
        CAstNode.BLOCK_STMT, caseNodes.toArray(new CAstNode[caseNodes.size()])));

    context.cfg().map(n, switchAst);

    // Finally, wrap the entire switch in a block so that we have a
    // well-defined place to 'break' to.
    return makeNode(context, fFactory, n, CAstNode.BLOCK_STMT, switchAst, breakAst);

  }

  private CAstNode visit(DoStatement n, WalkContext context) {
    String loopLabel = context.getLabelMap().get(n); // set by visit(LabeledStatement)
    String token = loopLabel==null? "at_" + n.getStartPosition(): loopLabel;
    
    ASTNode breakTarget = makeBreakOrContinueTarget(n, "breakLabel_" + token);
    CAstNode breakNode = visitNode(breakTarget, context);

    ASTNode continueTarget = makeBreakOrContinueTarget(n, "continueLabel_" + token);
    CAstNode continueNode = visitNode(continueTarget, context);
 
    CAstNode loopTest = visitNode(n.getExpression(), context);

    WalkContext loopContext = new LoopContext(context, loopLabel, breakTarget, continueTarget);
    CAstNode loopBody = visitNode(n.getBody(), loopContext);

    CAstNode madeNode = doLoopTranslator.translateDoLoop(loopTest, loopBody, continueNode, breakNode, context);
    context.pos().setPosition(madeNode, makePosition(n));
    return madeNode;
  }

  /**
   * Expands the form: for ( [final] Type var: iterable ) { ... } Into something equivalent to: for ( Iterator iter =
   * iterable.iter(); iter.hasNext(); ) { [final] Type var = (Type) iter.next(); ... } Or, in the case of an array: for ( int idx =
   * 0; i &lt; iterable.length; i++ ) { [final] Type var = iterable[idx]; ... } Except that the expression "iterable" is only evaluate
   * once (or is it?)
   * 
   * @param n
   * @param context
   */
  private CAstNode visit(EnhancedForStatement n, WalkContext context) {
    if (n.getExpression().resolveTypeBinding().isArray())
      return makeArrayEnhancedForLoop(n, context);
    else
      return makeIteratorEnhancedForLoop(n, context);
  }

  private CAstNode makeIteratorEnhancedForLoop(EnhancedForStatement n, WalkContext context) {
    // case 1: iterator
    CAstNode exprNode = visitNode(n.getExpression(), context);
    SingleVariableDeclaration svd = n.getParameter();
    Statement body = n.getBody();

    // expand into:

    // typical for loop:
    // { [inits]; while (cond) { [body]; [label continueTarget]; iters } [label breakTarget]
    // BLOCK(BLOCK(init1,init2,...),LOOP(cond,BLOCK(bodyblock,continuetarget,BLOCK(iter1,iter2,...))),breaktarget

    // in our case:
    // the only init is "Iterator iter = iterable.iter()"
    // cond is "iter.hasNext()"
    // bodyblock should be prepended with "[final] Type var = iter.next()" (put in the block that body belongs to)
    // iter is null
    // continuetarget and breaktarget are the same as in a regular for loop
    // BLOCK(iterassign,LOOP(cond,BLOCK(paramassign,bodyblock,continuetarget)),breaktarget)

    final String tmpName = "iter tmp"; // this is an illegal Java identifier, we will use this variable to hold the "invisible"
                                       // iterator

    /*-------- make "iter = iterator.iter()" ---------*/
    // make a fake method ref
    MethodReference iterMethodRef = fIdentityMapper.fakeMethodRefNoArgs("Ljava/lang/Iterable;.iterator()Ljava/util/Iterator<TT;>;",
        "Ljava/lang/Iterable", "iterator", "Ljava/util/Iterator");
    CAstNode iterCallSiteRef = fFactory.makeConstant(CallSiteReference
        .make(0, iterMethodRef, IInvokeInstruction.Dispatch.INTERFACE));
    // Iterable.iter() throws no exceptions.

    CAstNode iterCallNode = makeNode(context, fFactory, n, CAstNode.CALL, exprNode, iterCallSiteRef);
    // handle runtimeexception
    Object o1 = new Object(); // dummy object used for mapping / exceptions
    for (Pair<ITypeBinding, Object> catchTarget : context.getCatchTargets(fRuntimeExcType))
      context.cfg().add(o1, catchTarget.snd, catchTarget.fst);
    context.cfg().map(o1, iterCallNode); // TODO: this might not work, lots of calls in this one statement.

    CAstNode iterAssignNode = makeNode(context, fFactory, n, CAstNode.DECL_STMT, fFactory.makeConstant(new InternalCAstSymbol(
        tmpName, fTypeDict.getCAstTypeFor(ast.resolveWellKnownType("int")), true)), iterCallNode);

    // MATCHUP: wrap in a block
    iterAssignNode = makeNode(context, fFactory, n, CAstNode.BLOCK_STMT, iterAssignNode);

    // TODO: TOTEST: using this and Iterable.hasNext() explicitly in same file.

    /*---------- cond: iter.hasNext(); -----------*/
    MethodReference hasNextMethodRef = fIdentityMapper.fakeMethodRefNoArgs("Ljava/util/Iterator;.hasNext()Z",
        "Ljava/util/Iterator", "hasNext", "Z");
    CAstNode iterVar = makeNode(context, fFactory, n, CAstNode.VAR, fFactory.makeConstant(tmpName));
    CAstNode hasNextCallSiteRef = fFactory.makeConstant(CallSiteReference.make(0, hasNextMethodRef,
        IInvokeInstruction.Dispatch.INTERFACE));

    // throws no exceptions.
    CAstNode hasNextCallNode = makeNode(context, fFactory, n, CAstNode.CALL, iterVar, hasNextCallSiteRef);
    // handle runtimeexception
    Object o2 = new Object(); // dummy object used for mapping / exceptions
    for (Pair<ITypeBinding, Object> catchTarget : context.getCatchTargets(fRuntimeExcType))
      context.cfg().add(o2, catchTarget.snd, catchTarget.fst);
    context.cfg().map(o2, hasNextCallNode); // TODO: this might not work, lots of calls in this one statement.

    /*---------- paramassign: var = (Type) iter.next() ---------*/
    MethodReference nextMethodRef = fIdentityMapper.fakeMethodRefNoArgs("Ljava/util/Iterator;.next()TE;", "Ljava/util/Iterator",
        "next", "Ljava/lang/Object");
    CAstNode nextCallSiteRef = fFactory.makeConstant(CallSiteReference
        .make(0, nextMethodRef, IInvokeInstruction.Dispatch.INTERFACE));
    // throws no exceptions.
    CAstNode iterVar2 = makeNode(context, fFactory, n, CAstNode.VAR, fFactory.makeConstant(tmpName));
    CAstNode nextCallNode = makeNode(context, fFactory, n, CAstNode.CALL, iterVar2, nextCallSiteRef);
    for (Pair<ITypeBinding, Object> catchTarget : context.getCatchTargets(fRuntimeExcType))
      context.cfg().add(svd, catchTarget.snd, catchTarget.fst);
    context.cfg().map(svd, nextCallNode);

    // TODO: another cfg edge associated with svd! is this okay? prolly not... associate it with the cast, somehow...
    CAstNode castedNode = createCast(svd, nextCallNode, ast.resolveWellKnownType("java.lang.Object"), svd.resolveBinding()
        .getType(), context);

    Object defaultValue = JDT2CAstUtils.defaultValueForType(svd.resolveBinding().getType());
    CAstNode nextAssignNode = makeNode(context, fFactory, n, CAstNode.DECL_STMT, fFactory.makeConstant(new CAstSymbolImpl(svd
        .getName().getIdentifier(), fTypeDict.getCAstTypeFor(svd.resolveBinding().getType()), (svd.getModifiers() & Modifier.FINAL) != 0, defaultValue)), castedNode);

    /*----------- put it all together ----------*/
    ASTNode breakTarget = makeBreakOrContinueTarget(n, "breakLabel" + n.getStartPosition());
    ASTNode continueTarget = makeBreakOrContinueTarget(n, "continueLabel" + n.getStartPosition());
    String loopLabel = context.getLabelMap().get(n);
    WalkContext loopContext = new LoopContext(context, loopLabel, breakTarget, continueTarget);

    // LOCAL_SCOPE(BLOCK(iterassign,LOOP(cond,BLOCK(BLOCK(paramassign,bodyblock),continuetarget,BLOCK())),breaktarget))
    return makeNode(context, fFactory, n, CAstNode.LOCAL_SCOPE, makeNode(context, fFactory, n, CAstNode.BLOCK_STMT, iterAssignNode,
        makeNode(context, fFactory, n, CAstNode.LOOP, hasNextCallNode, makeNode(context, fFactory, n, CAstNode.BLOCK_STMT,
            makeNode(context, fFactory, n, CAstNode.LOCAL_SCOPE, makeNode(context, fFactory, n, CAstNode.BLOCK_STMT,
                nextAssignNode, visitNode(body, loopContext))), visitNode(continueTarget, context), makeNode(context, fFactory, n,
                CAstNode.BLOCK_STMT))), visitNode(breakTarget, context)));
  }

  private CAstNode makeArrayEnhancedForLoop(EnhancedForStatement n, WalkContext context) {
    // ********* BEFORE:
    // for ( String x: doSomething() ) { ... }
    // ********* AFTER:
    // {
    // String tmparray[] = doSomething();
    // for ( int tmpindex = 0; i < tmparray.length; tmpindex++ ) {
    // String x = tmparray[tmpindex];
    // ...
    // }
    // }
    // simplest:
    // LOCAL_SCOPE(BLOCK(arrayDecl,indexDecl,LOOP(cond,BLOCK(nextAssign,bodyblock,continuetarget,iter)),breaktarget))
    // match up exactly:
    // LOCAL_SCOPE(BLOCK(arrayDecl,LOCAL_SCOPE(BLOCK(BLOCK(indexDecl),LOOP(cond,BLOCK(LOCAL_SCOPE(BLOCK(nextAssign,bodyblock)),continuetarget,BLOCK(iter))),breaktarget))))

    /*------ arrayDecl --------- String tmparray[] = doSomething() ------*/
    final String tmpArrayName = "for temp array"; // illegal java identifier
    CAstNode exprNode = visitNode(n.getExpression(), context);
    CAstNode arrayDeclNode = makeNode(context, fFactory, n, CAstNode.DECL_STMT, fFactory.makeConstant(new InternalCAstSymbol(
        tmpArrayName, fTypeDict.getCAstTypeFor(n.getExpression().resolveTypeBinding()), true)), exprNode);

    /*------ indexDecl --------- int tmpindex = 0 ------*/
    final String tmpIndexName = "for temp index";
    CAstNode indexDeclNode = makeNode(context, fFactory, n, CAstNode.DECL_STMT, fFactory.makeConstant(new InternalCAstSymbol(
        tmpIndexName, fTypeDict.getCAstTypeFor(ast.resolveWellKnownType("int")), true)), fFactory.makeConstant(new Integer(0)));

    /*------ cond ------------- tmpindex < tmparray.length ------*/
    CAstNode tmpArrayLengthNode = makeNode(context, fFactory, n, CAstNode.ARRAY_LENGTH, makeNode(context, fFactory, n,
        CAstNode.VAR, fFactory.makeConstant(tmpArrayName), fFactory.makeConstant(fTypeDict.getCAstTypeFor(n.getExpression().resolveTypeBinding()))));
    CAstNode condNode = makeNode(context, fFactory, n, CAstNode.BINARY_EXPR, CAstOperator.OP_LT, makeNode(context, fFactory, n,
        CAstNode.VAR, fFactory.makeConstant(tmpIndexName), fFactory.makeConstant(JavaPrimitiveType.INT)), tmpArrayLengthNode);

    /*------ tmpIndexInc -------- tmpindex++ ------*/
    CAstNode tmpArrayIncNode = makeNode(context, fFactory, n, CAstNode.ASSIGN_POST_OP, makeNode(context, fFactory, n, CAstNode.VAR,
        fFactory.makeConstant(tmpIndexName)), fFactory.makeConstant(1), CAstOperator.OP_ADD);

    /*------ tmpArrayAccess ----- String x = tmparray[tmpindex] ------*/
    CAstNode tmpArrayAccessNode = makeNode(context, fFactory, n, CAstNode.ARRAY_REF, makeNode(context, fFactory, n, CAstNode.VAR,
        fFactory.makeConstant(tmpArrayName)), fFactory.makeConstant(fIdentityMapper.getTypeRef(n.getExpression()
        .resolveTypeBinding().getComponentType())), makeNode(context, fFactory, n, CAstNode.VAR, fFactory
        .makeConstant(tmpIndexName)));

    SingleVariableDeclaration svd = n.getParameter();
    Object defaultValue = JDT2CAstUtils.defaultValueForType(svd.resolveBinding().getType());
    CAstNode nextAssignNode = makeNode(context, fFactory, n, CAstNode.DECL_STMT, fFactory.makeConstant(new CAstSymbolImpl(svd
        .getName().getIdentifier(), fTypeDict.getCAstTypeFor(n.getExpression()
        .resolveTypeBinding().getComponentType()), (svd.getModifiers() & Modifier.FINAL) != 0, defaultValue)), tmpArrayAccessNode);

    // LOCAL_SCOPE(BLOCK(arrayDecl,LOCAL_SCOPE(BLOCK(BLOCK(indexDecl),LOOP(cond,BLOCK(LOCAL_SCOPE(BLOCK(nextAssign,bodyblock)),continuetarget,BLOCK(iter))),breaktarget))))
    // more complicated than it has to be, but it matches up exactly with the Java expansion above.

    ASTNode breakTarget = makeBreakOrContinueTarget(n, "breakLabel" + n.getStartPosition());
    ASTNode continueTarget = makeBreakOrContinueTarget(n, "continueLabel" + n.getStartPosition());
    String loopLabel = context.getLabelMap().get(n);
    WalkContext loopContext = new LoopContext(context, loopLabel, breakTarget, continueTarget);

    return makeNode(context, fFactory, n, CAstNode.LOCAL_SCOPE,
        makeNode(context, fFactory, n, CAstNode.BLOCK_STMT, arrayDeclNode, makeNode(context, fFactory, n, CAstNode.LOCAL_SCOPE,
            makeNode(context, fFactory, n, CAstNode.BLOCK_STMT, makeNode(context, fFactory, n, CAstNode.BLOCK_STMT, indexDeclNode),
                makeNode(context, fFactory, n, CAstNode.LOOP, condNode, makeNode(context, fFactory, n, CAstNode.BLOCK_STMT,
                    makeNode(context, fFactory, n, CAstNode.LOCAL_SCOPE, makeNode(context, fFactory, n, CAstNode.BLOCK_STMT,
                        nextAssignNode, visitNode(n.getBody(), loopContext))), visitNode(continueTarget, context), makeNode(
                        context, fFactory, n, CAstNode.BLOCK_STMT, tmpArrayIncNode))), visitNode(breakTarget, context)))));

  }

  private CAstNode visit(ForStatement n, WalkContext context) {
    ASTNode breakTarget = makeBreakOrContinueTarget(n, "breakLabel" + n.getStartPosition());
    ASTNode continueTarget = makeBreakOrContinueTarget(n, "continueLabel" + n.getStartPosition());
    String loopLabel = context.getLabelMap().get(n);
    WalkContext loopContext = new LoopContext(context, loopLabel, breakTarget, continueTarget);

    ArrayList<CAstNode> inits = new ArrayList<>();
    for (int i = 0; i < n.initializers().size(); i++) {
      ASTNode init = (ASTNode) n.initializers().get(i);
      if (init instanceof VariableDeclarationExpression) {
        for (ASTNode o : (Iterable<ASTNode>) ((VariableDeclarationExpression) init).fragments())
          inits.add(visitNode(o, context));
      } else
        inits.add(visitNode(init, context));
    }

    CAstNode[] iters = new CAstNode[n.updaters().size()];
    for (int i = 0; i < iters.length; i++)
      iters[i] = visitNode((ASTNode) n.updaters().get(i), context);

    CAstNode initsBlock = makeNode(context, fFactory, n, CAstNode.BLOCK_STMT, inits.toArray(new CAstNode[inits.size()]));
    CAstNode itersBlock = makeNode(context, fFactory, n, CAstNode.BLOCK_STMT, iters);

    // { [inits]; while (cond) { [body]; [label continueTarget]; iters } [label breakTarget]
    return makeNode(context, fFactory, n, CAstNode.LOCAL_SCOPE, makeNode(context, fFactory, n, CAstNode.BLOCK_STMT, initsBlock,
        makeNode(context, fFactory, n, CAstNode.LOOP, visitNode(n.getExpression(), context), makeNode(context, fFactory, n,
            CAstNode.BLOCK_STMT, visitNode(n.getBody(), loopContext), visitNode(continueTarget, context), itersBlock)), visitNode(
            breakTarget, context)));

  }

  private CAstNode visit(TryStatement n, WalkContext context) {
    List<CatchClause> catchBlocks = n.catchClauses();
    Block finallyBlock = n.getFinally();
    Block tryBlock = n.getBody();

    // try/finally
    if (catchBlocks.isEmpty()) {
      return makeNode(context, fFactory, n, CAstNode.UNWIND, visitNode(tryBlock, context), visitNode(finallyBlock, context));

      // try/catch/[finally]
    } else {
      TryCatchContext tc = new TryCatchContext(context, n);

      CAstNode tryNode = visitNode(tryBlock, tc);
      for (CatchClause catchClause : catchBlocks) {
        tryNode = makeNode(context, fFactory, n, CAstNode.TRY, tryNode, visitNode(catchClause, context));
      }

      // try/catch
      if (finallyBlock == null) {
        return tryNode;

        // try/catch/finally
      } else {
        return makeNode(context, fFactory, n, CAstNode.UNWIND, tryNode, visitNode(finallyBlock, context));
      }
    }

  }

  private CAstNode visit(CatchClause n, WalkContext context) {
    Block body = n.getBody();
    SingleVariableDeclaration formal = n.getException();

    CAstNode excDecl = makeNode(context, fFactory, n, CAstNode.CATCH, fFactory.makeConstant(formal.getName().getIdentifier()),
        visitNode(body, context));
    CAstNode localScope = makeNode(context, fFactory, n, CAstNode.LOCAL_SCOPE, excDecl);

    context.cfg().map(n, excDecl);
    context.getNodeTypeMap().add(excDecl, fTypeDict.getCAstTypeFor(n.getException().resolveBinding().getType()));
    return localScope;
  }

  private CAstNode visit(ThrowStatement n, WalkContext context) {
    CAstNode result = makeNode(context, fFactory, n, CAstNode.THROW, visitNode(n.getExpression(), context));
    ITypeBinding label = n.getExpression().resolveTypeBinding();

    context.cfg().map(n, result);

    Collection<Pair<ITypeBinding, Object>> catchNodes = context.getCatchTargets(label);

    for (Pair<ITypeBinding, Object> catchNode : catchNodes) {
      context.cfg().add(n, catchNode.snd, catchNode.fst);
    }

    return result;
  }

  private void hookUpNPETargets(ASTNode n, WalkContext wc) {
    Collection<Pair<ITypeBinding, Object>> excTargets = wc.getCatchTargets(fNullPointerExcType);
    if (!excTargets.isEmpty()) {
      // connect NPE exception edge to relevant catch targets
      // (presumably only one)
      for (Pair<ITypeBinding, Object> catchPair : excTargets) {
        wc.cfg().add(n, catchPair.snd, fNullPointerExcType);
      }
    } else {
      // connect exception edge to exit
      wc.cfg().add(n, CAstControlFlowMap.EXCEPTION_TO_EXIT, fNullPointerExcType);
    }
  }

  //
  // ARRAYS
  //

  private CAstNode visit(ArrayAccess n, WalkContext context) {
    TypeReference eltTypeRef = fIdentityMapper.getTypeRef(n.resolveTypeBinding());

    CAstNode cast = makeNode(context, fFactory, n, CAstNode.ARRAY_REF, visitNode(n.getArray(), context), fFactory.makeConstant(eltTypeRef),
        visitNode(n.getIndex(), context));

    hookUpNPETargets(n, context);
    
    context.cfg().map(n, cast);
    
    return cast;
  }

  // FIXME: inner classes here, probably too...
  private CAstNode visit(ArrayCreation n, WalkContext context) {
    ITypeBinding newType = n.resolveTypeBinding();
    ArrayInitializer ai = n.getInitializer();
    assert newType.isArray();

    if (ai != null) {
      return visitNode(ai, context);
    } else {
      TypeReference arrayTypeRef = fIdentityMapper.getTypeRef(newType);

      List<Expression> dims = n.dimensions();
      CAstNode[] args = new CAstNode[dims.size() + 1];

      int idx = 0;
      args[idx++] = fFactory.makeConstant(arrayTypeRef);
      for (Expression dimExpr : dims) {
        args[idx++] = visitNode(dimExpr, context);
      }
      return makeNode(context, fFactory, n, CAstNode.NEW, args);
    }
  }

  private CAstNode visit(SynchronizedStatement n, WalkContext context) {
    CAstNode exprNode = visitNode(n.getExpression(), context);
    String exprName = fFactory.makeUnique();
    CAstNode declStmt = makeNode(context, fFactory, n, CAstNode.DECL_STMT, fFactory
        .makeConstant(new CAstSymbolImpl(exprName, fTypeDict.getCAstTypeFor(n.getExpression().resolveTypeBinding()), true)), exprNode);

    CAstNode monitorEnterNode = makeNode(context, fFactory, n, CAstNode.MONITOR_ENTER, makeNode(context, fFactory, n, CAstNode.VAR,
        fFactory.makeConstant(exprName)));
    context.cfg().map(monitorEnterNode, monitorEnterNode);
    for (Pair<ITypeBinding, Object> catchTarget : context.getCatchTargets(fNullPointerExcType))
      context.cfg().add(monitorEnterNode, catchTarget.snd, catchTarget.fst);

    CAstNode bodyNodes = visitNode(n.getBody(), context);

    CAstNode monitorExitNode = makeNode(context, fFactory, n, CAstNode.MONITOR_EXIT, makeNode(context, fFactory, n, CAstNode.VAR,
        fFactory.makeConstant(exprName)));
    context.cfg().map(monitorExitNode, monitorExitNode);
    for (Pair<ITypeBinding, Object> catchTarget : context.getCatchTargets(fNullPointerExcType))
      context.cfg().add(monitorExitNode, catchTarget.snd, catchTarget.fst);

    CAstNode tryBody = makeNode(context, fFactory, n, CAstNode.BLOCK_STMT, monitorEnterNode, bodyNodes);
    CAstNode bigBody = makeNode(context, fFactory, n, CAstNode.UNWIND, tryBody, monitorExitNode);

    return makeNode(context, fFactory, n, CAstNode.BLOCK_STMT, declStmt, bigBody);
  }

  // ///////////////////////////////////////////
  // / THE GIANT SWITCH STATEMENT ( BORING ) ///
  // ///////////////////////////////////////////

  /**
   * Giant switch statement.
   * 
   * @param n
   */
  private CAstEntity visit(AbstractTypeDeclaration n, WalkContext context) {
    // handling of compilationunit in translate()
    if (n instanceof TypeDeclaration) {
      return visitTypeDecl(n, context);
    } else if (n instanceof EnumDeclaration) {
      return visit((EnumDeclaration) n, context);
    } else if (n instanceof AnnotationTypeDeclaration) {
      return visitTypeDecl(n, context);
    } else {
      Assertions.UNREACHABLE("Unhandled type declaration type");
      return null;
    }
  }

  /**
   * Giant switch statement, part deux
   * 
   * @param context
   */
  private CAstNode visitNode(ASTNode n, WalkContext context) {
    if (n == null)
      return makeNode(context, fFactory, null, CAstNode.EMPTY);

    if (n instanceof ArrayAccess) {
      return visit((ArrayAccess) n, context);
    } else if (n instanceof ArrayCreation) {
      return visit((ArrayCreation) n, context);
    } else if (n instanceof ArrayInitializer) {
      return visit((ArrayInitializer) n, context);
    } else if (n instanceof AssertStatement) {
      return visit((AssertStatement) n, context);
    } else if (n instanceof Assignment) {
      return visit((Assignment) n, context);
    } else if (n instanceof Block) {
      return visit((Block) n, context);
    } else if (n instanceof BooleanLiteral) {
      return visit((BooleanLiteral) n);
    } else if (n instanceof BreakStatement) {
      return visit((BreakStatement) n, context);
    } else if (n instanceof CastExpression) {
      return visit((CastExpression) n, context);
    } else if (n instanceof CatchClause) {
      return visit((CatchClause) n, context);
    } else if (n instanceof CharacterLiteral) {
      return visit((CharacterLiteral) n);
    } else if (n instanceof ClassInstanceCreation) {
      return visit((ClassInstanceCreation) n, context);
    } else if (n instanceof ConditionalExpression) {
      return visit((ConditionalExpression) n, context);
    } else if (n instanceof ConstructorInvocation) {
      return visit((ConstructorInvocation) n, context);
    } else if (n instanceof ContinueStatement) {
      return visit((ContinueStatement) n, context);
    } else if (n instanceof DoStatement) {
      return visit((DoStatement) n, context);
    } else if (n instanceof EmptyStatement) {
      return visit((EmptyStatement) n, context);
    } else if (n instanceof EnhancedForStatement) {
      return visit((EnhancedForStatement) n, context);
    } else if (n instanceof ExpressionStatement) {
      return visit((ExpressionStatement) n, context);
    } else if (n instanceof FieldAccess) {
      return visit((FieldAccess) n, context);
    } else if (n instanceof ForStatement) {
      return visit((ForStatement) n, context);
    } else if (n instanceof IfStatement) {
      return visit((IfStatement) n, context);
    } else if (n instanceof InfixExpression) {
      return visit((InfixExpression) n, context);
    } else if (n instanceof InstanceofExpression) {
      return visit((InstanceofExpression) n, context);
    } else if (n instanceof LabeledStatement) {
      return visit((LabeledStatement) n, context);
    } else if (n instanceof MethodInvocation) {
      return visit((MethodInvocation) n, context);
    } else if (n instanceof NumberLiteral) {
      return visit((NumberLiteral) n);
    } else if (n instanceof NullLiteral) {
      return visit();
    } else if (n instanceof ParenthesizedExpression) {
      return visit((ParenthesizedExpression) n, context);
    } else if (n instanceof PostfixExpression) {
      return visit((PostfixExpression) n, context);
    } else if (n instanceof PrefixExpression) {
      return visit((PrefixExpression) n, context);
    } else if (n instanceof QualifiedName) {
      return visit((QualifiedName) n, context);
    } else if (n instanceof ReturnStatement) {
      return visit((ReturnStatement) n, context);
    } else if (n instanceof SimpleName) {
      return visit((SimpleName) n, context);
    } else if (n instanceof StringLiteral) {
      return visit((StringLiteral) n);
    } else if (n instanceof SuperConstructorInvocation) {
      return visit((SuperConstructorInvocation) n, context);
    } else if (n instanceof SuperFieldAccess) {
      return visit((SuperFieldAccess) n, context);
    } else if (n instanceof SuperMethodInvocation) {
      return visit((SuperMethodInvocation) n, context);
    } else if (n instanceof SynchronizedStatement) {
      return visit((SynchronizedStatement) n, context);
    } else if (n instanceof SwitchStatement) {
      return visit((SwitchStatement) n, context);
    } else if (n instanceof SwitchCase) {
      return visit((SwitchCase) n, context);
    } else if (n instanceof ThisExpression) {
      return visit((ThisExpression) n, context);
    } else if (n instanceof TypeLiteral) {
      return visit((TypeLiteral) n, context);
    } else if (n instanceof ThrowStatement) {
      return visit((ThrowStatement) n, context);
    } else if (n instanceof TryStatement) {
      return visit((TryStatement) n, context);
    } else if (n instanceof TypeDeclarationStatement) {
      return visit((TypeDeclarationStatement) n, context);
    } else if (n instanceof VariableDeclarationFragment) {
      return visit((VariableDeclarationFragment) n, context);
    } else if (n instanceof WhileStatement) {
      return visit((WhileStatement) n, context);
    }

    // VariableDeclarationStatement handled as special case (returns multiple statements)

    Assertions.UNREACHABLE("Unhandled JDT node type " + n.getClass().getCanonicalName());

    return null;
  }

  private void visitNodeOrNodes(ASTNode n, WalkContext context, Collection<CAstNode> coll) {
    if (n instanceof VariableDeclarationStatement)
      coll.addAll(visit((VariableDeclarationStatement) n, context));
    else
      coll.add(visitNode(n, context));
  }

  // /////////////////////////////////////////
  // SPECIALIZED CASTENTITYs AND CASTNODEs //
  // /////////////////////////////////////////

  protected static final class CompilationUnitEntity implements CAstEntity {
    private final String fName;

    private final Collection<CAstEntity> fTopLevelDecls;

    public CompilationUnitEntity(PackageDeclaration packageDeclaration, List<CAstEntity> topLevelDecls) {
      fName = (packageDeclaration == null) ? "" : packageDeclaration.getName().getFullyQualifiedName().replace('.', '/');
      fTopLevelDecls = topLevelDecls;
    }

    @Override
	public Collection<CAstAnnotation> getAnnotations() {
		return null;
	}

	@Override
  public int getKind() {
      return FILE_ENTITY;
    }

    @Override
    public String getName() {
      return fName;
    }

    @Override
    public String getSignature() {
      Assertions.UNREACHABLE();
      return null;
    }

    @Override
    public String[] getArgumentNames() {
      return new String[0];
    }

    @Override
    public CAstNode[] getArgumentDefaults() {
      return new CAstNode[0];
    }

    @Override
    public int getArgumentCount() {
      return 0;
    }

    @Override
    public Map<CAstNode, Collection<CAstEntity>> getAllScopedEntities() {
      return Collections.singletonMap(null, fTopLevelDecls);
    }

    @Override
    public Iterator<CAstEntity> getScopedEntities(CAstNode construct) {
      Assertions.UNREACHABLE("CompilationUnitEntity asked for AST-related entities, but it has no AST.");
      return null;
    }

    @Override
    public CAstNode getAST() {
      return null;
    }

    @Override
    public CAstControlFlowMap getControlFlow() {
      Assertions.UNREACHABLE("CompilationUnitEntity.getControlFlow()");
      return null;
    }

    @Override
    public CAstSourcePositionMap getSourceMap() {
      Assertions.UNREACHABLE("CompilationUnitEntity.getSourceMap()");
      return null;
    }

    @Override
    public CAstSourcePositionMap.Position getPosition() {
      return null;
    }

    @Override
    public CAstNodeTypeMap getNodeTypeMap() {
      Assertions.UNREACHABLE("CompilationUnitEntity.getNodeTypeMap()");
      return null;
    }

    @Override
    public Collection<CAstQualifier> getQualifiers() {
      return Collections.emptyList();
    }

    @Override
    public CAstType getType() {
      Assertions.UNREACHABLE("CompilationUnitEntity.getType()");
      return null;
    }
  }

  // /////////////////////////////
  // WALK CONTEXTS
  // WHY????
  // ////////////////////////////////

  /**
   * Contains things needed by in the visit() of some nodes to process the nodes. For example, pos() contains the source position
   * mapping which each node registers
   */
  public static interface WalkContext extends TranslatorToCAst.WalkContext<WalkContext, ASTNode> {

    public Collection<Pair<ITypeBinding, Object>> getCatchTargets(ITypeBinding type);

    public Map<ASTNode, String> getLabelMap();

    public boolean needLValue();
  }

  /**
   * Default context functions. When one context doesn't handle something, it the next one up does. For example, there is only one
   * source pos. mapping per MethodContext, so loop contexts delegate it up.
   */
  public static class DelegatingContext extends TranslatorToCAst.DelegatingContext<WalkContext, ASTNode> implements WalkContext {

    public DelegatingContext(WalkContext parent) {
      super(parent);
    }

    @Override
    public Collection<Pair<ITypeBinding, Object>> getCatchTargets(ITypeBinding type) {
      return parent.getCatchTargets(type);
    }

    @Override
    public Map<ASTNode, String> getLabelMap() {
      return parent.getLabelMap();
    }

     @Override
    public boolean needLValue() {
      return parent.needLValue();
    }
  }

  /*
   * Root context. Doesn't do anything.
   */
  public static class RootContext extends TranslatorToCAst.RootContext<WalkContext, ASTNode> implements WalkContext {
     @Override
    public Collection<Pair<ITypeBinding, Object>> getCatchTargets(ITypeBinding type) {
      Assertions.UNREACHABLE("RootContext.getCatchTargets()");
      return null;
    }

    @Override
    public Map<ASTNode, String> getLabelMap() {
      Assertions.UNREACHABLE("RootContext.getLabelMap()");
      return null;
    }

    @Override
    public boolean needLValue() {
      Assertions.UNREACHABLE("Rootcontext.needLValue()");
      return false;
    }
  }

  private class AssignmentContext extends DelegatingContext {

    protected AssignmentContext(WalkContext parent) {
      super(parent);
    }

    @Override
    public boolean needLValue() {
      return true;
    }
  }

  private static class TryCatchContext extends DelegatingContext {
    Collection<Pair<ITypeBinding, Object>> fCatchNodes = new ArrayList<>();

    TryCatchContext(WalkContext parent, TryStatement tryNode) {
      super(parent);

      for (CatchClause c : (Iterable<CatchClause>) tryNode.catchClauses()) {
        Pair<ITypeBinding, Object> p = Pair.make(c.getException().resolveBinding().getType(), (Object) c);

        fCatchNodes.add(p);
      }
    }

    @Override
    public Collection<Pair<ITypeBinding, Object>> getCatchTargets(ITypeBinding label) {
      // Look for all matching targets for this thrown type:
      // if supertpe match, then return only matches at this catch
      // if subtype match, then matches here and parent matches
      Collection<Pair<ITypeBinding, Object>> catchNodes = new ArrayList<>();

      for (Pair<ITypeBinding, Object> p : fCatchNodes) {
        ITypeBinding catchType = p.fst;

        // catchType here should NEVER be FakeExceptionTypeBinary, because these can only be thrown (not caught) by
        // "1/0", implicit null pointer exceptions, etc.
        assert !(catchNodes instanceof FakeExceptionTypeBinding) : "catchNodes instanceof FakeExceptionTypeBinary!";

        if (label.isSubTypeCompatible(catchType) || label.isEqualTo(catchType)) {
          catchNodes.add(p);
          return catchNodes;
          // _might_ get caught
        } else if (catchType.isSubTypeCompatible(label)) {
          catchNodes.add(p);
          continue;
        }
      }
      catchNodes.addAll(parent.getCatchTargets(label));
      return catchNodes;
    }
  }

  private class BreakContext extends DelegatingContext {
    protected final String label;

    private final ASTNode breakTo;

    BreakContext(WalkContext parent, String label, ASTNode breakTo) {
      super(parent);
      this.label = label;
      this.breakTo = breakTo;
    }

    @Override
    public ASTNode getBreakFor(String label) {
      return (label == null || label.equals(this.label)) ? breakTo : super.getBreakFor(label);
    }
  }

  private class LoopContext extends BreakContext {
    private final ASTNode continueTo;

    protected LoopContext(WalkContext parent, String label, ASTNode breakTo, ASTNode continueTo) {
      super(parent, label, breakTo);
      this.continueTo = continueTo;
    }

    @Override
    public ASTNode getContinueFor(String label) {
      return (label == null || label.equals(this.label)) ? continueTo : super.getContinueFor(label);
    }
  }

  public class MethodContext extends DelegatingContext {
    private final Map<CAstNode, CAstEntity> fEntities;

    private final Map<ASTNode, String> labelMap = HashMapFactory.make(2);

    public MethodContext(WalkContext parent, Map<CAstNode, CAstEntity> entities) {
      // constructor did take: pd.procedureInstance(), memberEntities, context
      super(parent);
      fEntities = entities;
    }

    @Override
    public Map<ASTNode, String> getLabelMap() {
      return labelMap; // labels are kept within a method.
    }

    final CAstSourcePositionRecorder fSourceMap = new CAstSourcePositionRecorder();

    final CAstControlFlowRecorder fCFG = new CAstControlFlowRecorder(fSourceMap);

    final CAstNodeTypeMapRecorder fNodeTypeMap = new CAstNodeTypeMapRecorder();

    @Override
    public CAstControlFlowRecorder cfg() {
      return fCFG;
    }

    @Override
    public void addScopedEntity(CAstNode node, CAstEntity entity) {
      fEntities.put(node, entity);
    }

    @Override
    public CAstSourcePositionRecorder pos() {
      return fSourceMap;
    }

    @Override
    public CAstNodeTypeMapRecorder getNodeTypeMap() {
      return fNodeTypeMap;
    }

    @Override
    public Collection<Pair<ITypeBinding, Object>> getCatchTargets(ITypeBinding label) {
      // TAGALONG (need fRuntimeExcType)
      // Why do we seemingly catch a RuntimeException in every method? this won't catch the RuntimeException above where
      // it is supposed to be caught?
      Collection<Pair<ITypeBinding, Object>> result = Collections.singleton(Pair.<ITypeBinding, Object> make(fRuntimeExcType,
          CAstControlFlowMap.EXCEPTION_TO_EXIT));
      return result;
    }

    @Override
    public boolean needLValue() {
      return false;
    }

  }

  // ////////////////////////////////////
  // MAKE NODE VARIATIONS & POSITIONS (BORING)
  // maybe moved to different file.
  // makeNode() simply calls Ast.makeNode() and sets the position for the node
  // ////////////////////////////////////
  protected CAstNode makeNode(WalkContext wc, CAst Ast, ASTNode n, int kind) {
    CAstNode cn = Ast.makeNode(kind);
    setPos(wc, cn, n);
    return cn;
  }

  protected CAstNode makeNode(WalkContext wc, CAst Ast, ASTNode n, int kind, CAstNode c[]) {
    CAstNode cn = Ast.makeNode(kind, c);
    setPos(wc, cn, n);
    return cn;
  }

  protected CAstNode makeNode(WalkContext wc, CAst Ast, T pos, int kind, CAstNode c[]) {
    CAstNode cn = Ast.makeNode(kind, c);
    wc.pos().setPosition(cn, pos);
    return cn;
  }

  protected CAstNode makeNode(WalkContext wc, CAst Ast, ASTNode n, int kind, CAstNode c1, CAstNode c2) {
    CAstNode cn = Ast.makeNode(kind, c1, c2);
    setPos(wc, cn, n);
    return cn;
  }

  protected CAstNode makeNode(WalkContext wc, CAst Ast, ASTNode n, int kind, CAstNode c) {
    CAstNode cn = Ast.makeNode(kind, c);
    setPos(wc, cn, n);
    return cn;
  }

  protected CAstNode makeNode(WalkContext wc, CAst Ast, ASTNode n, int kind, CAstNode c1, CAstNode c2, CAstNode c3) {
    CAstNode cn = Ast.makeNode(kind, c1, c2, c3);
    setPos(wc, cn, n);
    return cn;
  }

  protected CAstNode makeNode(WalkContext wc, CAst Ast, ASTNode n, int kind, CAstNode c1, CAstNode c2, CAstNode c3, CAstNode c4) {
    CAstNode cn = Ast.makeNode(kind, c1, c2, c3, c4);
    setPos(wc, cn, n);
    return cn;
  }

  protected CAstNode makeNode(WalkContext wc, CAst Ast, T pos, int kind, CAstNode c1, CAstNode c2, CAstNode c3) {
    CAstNode cn = Ast.makeNode(kind, c1, c2, c3);
    wc.pos().setPosition(cn, pos);
    return cn;
  }

  protected void setPos(WalkContext wc, CAstNode cn, ASTNode jdtNode) {
    if (jdtNode != null)
      wc.pos().setPosition(cn, makePosition(jdtNode));
  }

  public T makePosition(ASTNode n) {
    return makePosition(n.getStartPosition(), n.getStartPosition() + n.getLength());
  }

  public abstract T makePosition(int start, int end);
 
  // /////////////////////////////////////////////////////////////////
  // // ENUM TRANSFORMATION //////////////////////////////////////////
  // /////////////////////////////////////////////////////////////////

  private static final ArrayList<CAstQualifier> enumQuals = new ArrayList<>(3);
  static {
    enumQuals.add(CAstQualifier.PUBLIC);
    enumQuals.add(CAstQualifier.STATIC);
    enumQuals.add(CAstQualifier.FINAL);
  }

  /**
   * Only called from createClassDeclaration.
   * 
   * @param decl
   * @param context
   */
  private CAstEntity visit(EnumConstantDeclaration decl, WalkContext context) {
    return new FieldEntity(decl.getName().getIdentifier(), decl.resolveVariable().getType(), enumQuals, makePosition(decl
        .getStartPosition(), decl.getStartPosition() + decl.getLength()), null);
  }

  /**
   * Called only from visitFieldInitNode(node,context)
   */
  private CAstNode createEnumConstantDeclarationInit(EnumConstantDeclaration node, WalkContext context) {
    String hiddenVariableName = (String) node.getProperty("com.ibm.wala.cast.java.translator.jdt.fakeValuesDeclName");
    if (hiddenVariableName == null) {
      FieldReference fieldRef = fIdentityMapper.getFieldRef(node.resolveVariable());
      // We use null to indicate an OBJECT_REF to a static field
      CAstNode lhsNode = makeNode(context, fFactory, node, CAstNode.OBJECT_REF, makeNode(context, fFactory, null, CAstNode.VOID),
          fFactory.makeConstant(fieldRef));

      // CONSTRUCT ARGUMENTS & "new MyEnum(...)" statement
      ArrayList<Object> arguments = new ArrayList<>();
      arguments.add(fFactory.makeConstant(node.getName().getIdentifier())); // name of constant
      arguments.add(fFactory.makeConstant(node.resolveVariable().getVariableId())); // id
      arguments.addAll(node.arguments());
      CAstNode rhsNode = createClassInstanceCreation(node, arguments, node.resolveConstructorBinding(), null, node
          .getAnonymousClassDeclaration(), context);

      CAstNode assNode = makeNode(context, fFactory, node, CAstNode.ASSIGN, lhsNode, rhsNode);

      return assNode; // their naming, not mine
    } else {

      // String[] x = (new Direction[] {
      // NORTH, EAST, SOUTH, WEST, $VALUES, $VALUES$
      // });

      return null;
    }
  }

  private CAstEntity createEnumValueOfMethod(ITypeBinding enumType, WalkContext oldContext) {
    IMethodBinding met = null, superMet = null;
    // find our valueOf(String)
    for (IMethodBinding m : enumType.getDeclaredMethods())
      if (m.getName().equals("valueOf") && m.getParameterTypes().length == 1
          && m.getParameterTypes()[0].isEqualTo(ast.resolveWellKnownType("java.lang.String")))
        met = m;
    // find Enum.valueOf(Class, String)
    for (IMethodBinding m : enumType.getSuperclass().getTypeDeclaration().getDeclaredMethods())
      if (m.getName().equals("valueOf") && m.getParameterTypes().length == 2)
        superMet = m;
    assert met != null && superMet != null : "Couldn't find enum values() function in JDT bindings!";

    Map<CAstNode, CAstEntity> memberEntities = new LinkedHashMap<>();
    final MethodContext context = new MethodContext(oldContext, memberEntities);

    MethodDeclaration fakeMet = ast.newMethodDeclaration();
    fakeMet.setName(ast.newSimpleName("valueOf"));
    fakeMet.setSourceRange(-1, 0);
    fakeMet.setBody(ast.newBlock());
    SingleVariableDeclaration stringS = ast.newSingleVariableDeclaration();
    stringS.setName(ast.newSimpleName("s"));
    fakeMet.parameters().add(stringS);

    // TODO: probably uses reflection so isn't very useful for analyses. Is there something more useful we could put in here?
    // return (MyEnum)Enum.valueOf(MyEnum.class, s);
    // cast(call(type_literal, var)))

    CAstNode typeLit = makeNode(context, fFactory, fakeMet, CAstNode.TYPE_LITERAL_EXPR, fFactory.makeConstant(fIdentityMapper
        .typeToTypeID(enumType)));
    CAstNode stringSvar = makeNode(context, fFactory, fakeMet, CAstNode.VAR, fFactory.makeConstant("s"), fFactory.makeConstant(fTypeDict.getCAstTypeFor(ast.resolveWellKnownType("java.lang.String"))));
    ArrayList<Object> args = new ArrayList<>();
    args.add(typeLit);
    args.add(stringSvar);
    CAstNode call = createMethodInvocation(fakeMet, superMet, makeNode(context, fFactory, fakeMet, CAstNode.VOID), args, context);
    CAstNode cast = createCast(fakeMet, call, enumType, superMet.getReturnType(), context);
    CAstNode bodyNode = makeNode(context, fFactory, fakeMet, CAstNode.LOCAL_SCOPE, makeNode(context, fFactory, fakeMet,
        CAstNode.BLOCK_STMT, makeNode(context, fFactory, fakeMet, CAstNode.RETURN, cast)));

    ArrayList<CAstType> paramTypes = new ArrayList<>(1);
    paramTypes.add(fTypeDict.getCAstTypeFor(ast.resolveWellKnownType("java.lang.String")));

    return new ProcedureEntity(bodyNode, fakeMet, enumType, memberEntities, context, paramTypes, enumType, met.getModifiers(), handleAnnotations(met));
  }

  private CAstEntity createEnumValuesMethod(ITypeBinding enumType, ArrayList<IVariableBinding> constants, WalkContext oldContext) {
    IMethodBinding met = null;
    for (IMethodBinding m : enumType.getDeclaredMethods())
      if (m.getName().equals("values") && m.getParameterTypes().length == 0)
        met = m;
    assert met != null : "Couldn't find enum values() function in JDT bindings!";

    Map<CAstNode, CAstEntity> memberEntities = new LinkedHashMap<>();
    final MethodContext context = new MethodContext(oldContext, memberEntities);

    MethodDeclaration fakeMet = ast.newMethodDeclaration();
    fakeMet.setName(ast.newSimpleName("values"));
    fakeMet.setSourceRange(-1, 0);
    fakeMet.setBody(ast.newBlock());

    // make enum constant values array: new MyEnum() { MYENUMCST1, MYENUMCST2, ... }
    CAstNode[] eltNodes = new CAstNode[constants.size() + 1];
    int idx = 0;
    TypeReference arrayTypeRef = fIdentityMapper.getTypeRef(enumType.createArrayType(1));
    eltNodes[idx++] = makeNode(context, fFactory, fakeMet, CAstNode.NEW, fFactory.makeConstant(arrayTypeRef), fFactory
        .makeConstant(constants.size()));
    for (IVariableBinding cst : constants)
      eltNodes[idx++] = createFieldAccess(makeNode(context, fFactory, fakeMet, CAstNode.VOID), cst.getName(), cst, fakeMet, context);

    CAstNode bodyNode = makeNode(context, fFactory, fakeMet, CAstNode.LOCAL_SCOPE, makeNode(context, fFactory, fakeMet,
        CAstNode.BLOCK_STMT, makeNode(context, fFactory, fakeMet, CAstNode.RETURN, makeNode(context, fFactory, fakeMet,
            CAstNode.ARRAY_LITERAL, eltNodes))));

    ArrayList<CAstType> paramTypes = new ArrayList<>(0);
    return new ProcedureEntity(bodyNode, fakeMet, enumType, memberEntities, context, paramTypes, enumType.createArrayType(1), met
        .getModifiers(), handleAnnotations(enumType));
  }

  private void doEnumHiddenEntities(ITypeBinding typeBinding, List<CAstEntity> memberEntities, WalkContext context) {
    // PART I: create a $VALUES field
    // collect constants
    // ArrayList<String> constants = new ArrayList<String>();
    // for ( ASTNode n: staticInits )
    // if ( n instanceof EnumConstantDeclaration )
    // constants.add(((EnumConstantDeclaration)n).getName().getIdentifier());
    // figure out a suitable untaken name
    // String hiddenFieldName = "hidden values field"; // illegal name
    // // public static final MyEnum[] $VALUES;
    // memberEntities.add(new FieldEntity(hiddenFieldName,
    // typeBinding.createArrayType(1), enumQuals,
    // makePosition(-1,-1)));
    //
    // EnumConstantDeclaration fakeValuesDecl = ast.newEnumConstantDeclaration();
    // // pass along values that we will use in createEnumConstantDeclarationInit() in creating static initializer
    // fakeValuesDecl.setProperty("com.ibm.wala.cast.java.translator.jdt.fakeValuesDeclName", hiddenFieldName);
    // fakeValuesDecl.setProperty("com.ibm.wala.cast.java.translator.jdt.fakeValuesDeclConstants", constants);
    // staticInits.add(fakeValuesDecl);

    ArrayList<IVariableBinding> constants = new ArrayList<>();
    for (IVariableBinding var : typeBinding.getDeclaredFields())
      if (var.isEnumConstant())
        constants.add(var);

    // constants are unsorted by default
    Collections.sort(constants, (arg0, arg1) -> arg0.getVariableId() - arg1.getVariableId());

    // PART II: create values()
    memberEntities.add(createEnumValuesMethod(typeBinding, constants, context));

    // PART III: create valueOf()
    memberEntities.add(createEnumValueOfMethod(typeBinding, context));
  }

  private CAstEntity visit(EnumDeclaration n, WalkContext context) {

    // JDT contains correct type info / class / subclass info for the enum
    return createClassDeclaration(n, n.bodyDeclarations(), n.enumConstants(), n.resolveBinding(), n.getName().getIdentifier(), n
        .resolveBinding().getModifiers(), false, false, context);
  }

  /**
   * @param n for positioning.
   */
  private CAstEntity createEnumConstructorWithParameters(IMethodBinding ctor, ASTNode n, WalkContext oldContext,
      ArrayList<ASTNode> inits, MethodDeclaration nonDefaultCtor) {
    // PART I: find super ctor to call
    ITypeBinding newType = ctor.getDeclaringClass();
    ITypeBinding javalangenumType = newType.getSuperclass();
    IMethodBinding superCtor = null;

    if (newType.isEnum()) {
      for (IMethodBinding met : javalangenumType.getDeclaredMethods())
        if (met.isConstructor()) {
          superCtor = met;
          break;
        }
    }

    assert superCtor != null : "enum";

    // PART II: make ctor with simply "super(a,b,c...)"
    // TODO: extra CAstNodes
    final Map<CAstNode, CAstEntity> memberEntities = new LinkedHashMap<>();
    final MethodContext context = new MethodContext(oldContext, memberEntities);
    MethodDeclaration fakeCtor = ast.newMethodDeclaration();
    fakeCtor.setConstructor(true);
    fakeCtor.setSourceRange(n.getStartPosition(), n.getLength());
    fakeCtor.setBody(ast.newBlock());

    // PART IIa: make a fake JDT constructor method with the proper number of args
    // Make fake args that will be passed
    String[] fakeArguments = new String[3 + ctor.getParameterTypes().length];
    if (nonDefaultCtor == null) {
      for (int i = 3; i < fakeArguments.length; i++)
        fakeArguments[i] = "__wala_jdtcast_argument" + i; // this is in the case of an anonymous class with parameters, eg NORTH in
                                                          // the following example: public enum A { NORTH("south") { ...} A(String
                                                          // s){} }
    } else {
      for (int i = 3; i < fakeArguments.length; i++)
        fakeArguments[i] = ((SingleVariableDeclaration) nonDefaultCtor.parameters().get(i - 3)).getName().getIdentifier();
    }

    ArrayList<CAstType> paramTypes = new ArrayList<>(superCtor.getParameterTypes().length);
    fakeArguments[0] = "this";
    fakeArguments[1] = "__wala_jdtcast_argument1"; // TODO FIXME: change to invalid name in the case that nonDefaultCtor != null
    fakeArguments[2] = "__wala_jdtcast_argument2"; // otherwise there will be conflicts if we name our variable
                                                   // __wala_jdtcast_argument1!!!
    for (int i = 1; i < fakeArguments.length; i++) {
      // the name
      SingleVariableDeclaration svd = ast.newSingleVariableDeclaration();
      svd.setName(ast.newSimpleName(fakeArguments[i]));
      fakeCtor.parameters().add(svd);

      // the type
      if (i == 1)
        paramTypes.add(fTypeDict.getCAstTypeFor(ast.resolveWellKnownType("java.lang.String")));
      else if (i == 2)
        paramTypes.add(fTypeDict.getCAstTypeFor(ast.resolveWellKnownType("int")));
      else
        paramTypes.add(fTypeDict.getCAstTypeFor(ctor.getParameterTypes()[i - 3]));
    }

    // PART IIb: create the statements in the constructor
    // one super() call plus the inits
    CAstNode[] bodyNodes;
    if (nonDefaultCtor == null)
      bodyNodes = new CAstNode[inits.size() + 1];
    else
      bodyNodes = new CAstNode[inits.size() + 2];

    // make super(...) call
    // this, call ref, args
    CAstNode[] children;
    if (ctor.isDefaultConstructor())
      children = new CAstNode[4 + ctor.getParameterTypes().length]; // anonymous class' implicit constructors call constructors with
                                                                    // more than standard two enum args
    else
      children = new CAstNode[4]; // explicit constructor
    children[0] = makeNode(context, fFactory, n, CAstNode.SUPER);
    CallSiteReference callSiteRef = CallSiteReference.make(0, fIdentityMapper.getMethodRef(superCtor),
        IInvokeInstruction.Dispatch.SPECIAL);
    children[1] = fFactory.makeConstant(callSiteRef);
    children[2] = makeNode(context, fFactory, n, CAstNode.VAR, fFactory.makeConstant(fakeArguments[1]), fFactory.makeConstant(paramTypes.get(0)));
    children[3] = makeNode(context, fFactory, n, CAstNode.VAR, fFactory.makeConstant(fakeArguments[2]), fFactory.makeConstant(paramTypes.get(1)));

    if (ctor.isDefaultConstructor())
      for (int i = 0; i < ctor.getParameterTypes().length; i++)
        children[i + 4] = makeNode(context, fFactory, n, CAstNode.VAR, fFactory.makeConstant(fakeArguments[i + 3]), fFactory.makeConstant(paramTypes.get(i+2)));

    bodyNodes[0] = makeNode(context, fFactory, n, CAstNode.CALL, children);
    // QUESTION: no handleExceptions?

    for (int i = 0; i < inits.size(); i++)
      bodyNodes[i + 1] = visitFieldInitNode(inits.get(i), context);

    if (nonDefaultCtor != null)
      bodyNodes[bodyNodes.length - 1] = visitNode(nonDefaultCtor.getBody(), context);

    // finally, make the procedure entity
    CAstNode ast = makeNode(context, fFactory, n, CAstNode.BLOCK_STMT, bodyNodes);
    return new ProcedureEntity(ast, fakeCtor, newType, memberEntities, context, paramTypes, null, handleAnnotations(ctor));

  }

}
