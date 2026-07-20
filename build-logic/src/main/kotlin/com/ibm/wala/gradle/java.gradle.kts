package com.ibm.wala.gradle

// Build configuration for subprojects that include Java source code.

import net.ltgt.gradle.errorprone.errorprone

plugins {
  eclipse
  jacoco
  `java-library`
  `java-test-fixtures`
  `maven-publish`
  signing
  id("com.diffplug.spotless")
  id("com.ibm.wala.gradle.eclipse-compatible-java")
  id("com.ibm.wala.gradle.javadoc")
  id("com.ibm.wala.gradle.subproject")
  id("net.ltgt.errorprone")
}

jacoco.toolVersion = "0.8.14"

repositories {
  mavenCentral()
  // to get r8
  maven { url = uri("https://storage.googleapis.com/r8-releases/raw") }
}

java.toolchain {
  languageVersion = JavaLanguageVersion.of(property("com.ibm.wala.jdk-version") as String)
}

base.archivesName = "com.ibm.wala${path.replace(':', '.')}"

configurations {
  resolvable("ecj")
  named("javadocClasspath") { extendsFrom(compileClasspath) }
}

dependencies {
  "ecj"(catalogLibrary("eclipse-ecj"))
  "errorprone"(catalogLibrary("errorprone-core"))

  testFixturesImplementation(platform(catalogLibrary("junit-bom")))

  testImplementation(platform(catalogLibrary("junit-bom")))
  testRuntimeOnly(catalogLibrary("junit-jupiter-engine"))
  testRuntimeOnly(catalogLibrary("junit-platform-launcher"))
  testRuntimeOnly(catalogLibrary("junit-vintage-engine"))
}

tasks.withType<JavaCompile>().configureEach {
  // Always compile with a recent JDK version, to get the latest bug fixes in the compiler toolchain
  javaCompiler = javaToolchains.compilerFor { languageVersion = JavaLanguageVersion.of(26) }
  // Generate JDK 17 bytecodes; that is the minimum version supported by WALA
  options.run {
    isDeprecation = true
    release = 17

    errorprone {

      // warning-level checks upgraded to error, since we've fixed all the warnings
      error(
          "AddressSelection",
          "AnnotateFormatMethod",
          "AnnotateFormatMethod",
          "AnnotationPosition",
          "ArgumentSelectionDefectChecker",
          "ArgumentSelectionDefectChecker",
          "ArrayAsKeyOfSetOrMap",
          "AssertEqualsArgumentOrderChecker",
          "AssertEqualsArgumentOrderChecker",
          "AssertionFailureIgnored",
          "AssertSameIncompatible",
          "AssertThrowsBlockToExpression",
          "AssertThrowsMinimizer",
          "AssertThrowsMultipleStatements",
          "AssistedInjectAndInjectOnSameConstructor",
          "ASTHelpersSuggestions",
          "AttemptedNegativeZero",
          "AutoValueBoxedValues",
          "AutoValueFinalMethods",
          "AutoValueImmutableFields",
          "AutoValueSubclassLeaked",
          "AvoidValueSetter",
          "BadComparable",
          "BadInstanceof",
          "BadInstanceof",
          "BareDotMetacharacter",
          "BigDecimalEquals",
          "BigDecimalLiteralDouble",
          "BoxedPrimitiveConstructor",
          "BoxingComparator",
          "BugPatternNaming",
          "ByteBufferBackingArray",
          "CacheLoaderNull",
          "CanonicalDuration",
          "CatchFail",
          "ChainedAssertionLosesContext",
          "CharacterGetNumericValue",
          "ClassCanBeStatic",
          "ClassInitializationDeadlock",
          "ClassNewInstance",
          "CloseableProvides",
          "ClosingStandardOutputStreams",
          "CollectionUndefinedEquality",
          "CollectorShouldNotUseState",
          "ComparableAndComparator",
          "CompareToZero",
          "ComplexBooleanConstant",
          "DateChecker",
          "DateFormatConstant",
          "DeeplyNested",
          "DefaultPackage",
          "DeprecatedVariable",
          "DirectInvocationOnMock",
          "DistinctVarargsChecker",
          "DoNotClaimAnnotations",
          "DoNotCallSuggester",
          "DoNotMockAutoValue",
          "DoubleCheckedLocking",
          "DuplicateAssertion",
          "DuplicateDateFormatField",
          "EmptySetMultibindingContributions",
          "EmptyTopLevelDeclaration",
          "EqualsIncompatibleType",
          "EqualsUnsafeCast",
          "EqualsUsingHashCode",
          "ErroneousBitwiseExpression",
          "ErroneousThreadPoolConstructorChecker",
          "EscapedEntity",
          "ExpensiveLenientFormatString",
          "ExtendingJUnitAssert",
          "ExtendsObject",
          "FallThrough",
          "FallThrough",
          "Finalize",
          "Finally",
          "FloatCast",
          "FloatingPointAssertionWithinEpsilon",
          "FloatingPointLiteralPrecision",
          "FloggerArgumentToString",
          "FloggerPerWithoutRateLimit",
          "FloggerStringConcatenation",
          "FormatStringShouldUsePlaceholders",
          "FormatStringShouldUsePlaceholders",
          "FragmentInjection",
          "FragmentNotInstantiable",
          "FunctionalInterfaceClash",
          "FutureReturnValueIgnored",
          "FutureTransformAsync",
          "GetClassOnEnum",
          "GuiceNestedCombine",
          "ICCProfileGetInstance",
          "IdentityHashMapUsage",
          "IfChainToSwitch",
          "IfChainToSwitch",
          "IgnoredPureGetter",
          "ImmutableAnnotationChecker",
          "InconsistentHashCode",
          "IncorrectMainMethod",
          "IncrementInForLoopAndHeader",
          "InheritDoc",
          "InjectedConstructorAnnotations",
          "InjectInvalidTargetingOnScopingAnnotation",
          "InjectOnBugCheckers",
          "InjectOnConstructorOfAbstractClass",
          "InjectScopeAnnotationOnInterfaceOrAbstractClass",
          "InlineFormatString",
          "InlineMeInliner",
          "InlineTrivialConstant",
          "InputStreamSlowMultibyteRead",
          "InstanceOfAndCastMatchWrongType",
          "InstanceOfAndCastMatchWrongType",
          "InterruptedInCatchBlock",
          "IntFloatConversion",
          "IntLiteralCast",
          "IntLongMath",
          "InvalidBlockTag",
          "InvalidInlineTag",
          "InvalidLink",
          "InvalidParam",
          "InvalidSnippet",
          "InvalidThrows",
          "InvalidThrowsLink",
          "IsInstanceOfClass",
          "JavaDurationGetSecondsGetNano",
          "JavaDurationGetSecondsToToSeconds",
          "JavaDurationWithNanos",
          "JavaDurationWithSeconds",
          "JavaInstantGetSecondsGetNano",
          "JavaLocalDateTimeGetNano",
          "JavaLocalTimeGetNano",
          "JavaPeriodGetDays",
          "JavaTimeDefaultTimeZone",
          "JavaUtilDate",
          "JavaxInjectOnFinalField",
          "JdkObsolete",
          "JdkObsolete",
          "JodaConstructors",
          "JodaDateTimeConstants",
          "JodaDurationWithMillis",
          "JodaInstantWithMillis",
          "JodaNewPeriod",
          "JodaPlusMinusLong",
          "JodaTimeConverterManager",
          "JodaWithDurationAddedLong",
          "JUnit3FloatingPointComparisonWithoutDelta",
          "JUnit4ClassUsedInJUnit3",
          "JUnit4EmptyMethods",
          "JUnitAmbiguousTestClass",
          "JUnitIncompatibleType",
          "JUnitMethodInvoked",
          "LambdaFunctionalInterface",
          "ListRemoveAmbiguous",
          "LiteEnumValueOf",
          "LiteProtoToString",
          "LockNotBeforeTry",
          "LockOnNonEnclosingClassLiteral",
          "LogicalAssignment",
          "LongDoubleConversion",
          "LongFloatConversion",
          "LoopOverCharArray",
          "MalformedInlineTag",
          "MathAbsoluteNegative",
          "MemoizeConstantVisitorStateLookups",
          "MisformattedTestData",
          "MissingCasesInEnumSwitch",
          "MissingFail",
          "MissingImplementsComparable",
          "MissingOverride",
          "MissingRefasterAnnotation",
          "MissingSummary",
          "MockIllegalThrows",
          "MockNotUsedInProduction",
          "ModifiedButNotUsed",
          "ModifyCollectionInEnhancedForLoop",
          "ModifySourceCollectionInStream",
          "MultimapKeys",
          "MultipleNullnessAnnotations",
          "MultipleParallelOrSequentialCalls",
          "NamedLikeContextualKeyword",
          "NarrowCalculation",
          "NarrowingCompoundAssignment",
          "NegativeCharLiteral",
          "NestedInstanceOfConditions",
          "NestedInstanceOfConditions",
          "NewFileSystem",
          "NonAtomicVolatileUpdate",
          "NonOverridingEquals",
          "NullableConstructor",
          "NullableOptional",
          "NullablePrimitive",
          "NullableTypeParameter",
          "NullableVoid",
          "NullableWildcard",
          "NullOptional",
          "ObjectEqualsForPrimitives",
          "ObjectsHashCodePrimitive",
          "ObjectToString",
          "OptionalMapToOptional",
          "OptionalNotPresent",
          "OrphanedFormatString",
          "OutlineNone",
          "Overrides",
          "OverridesGuiceInjectableMethod",
          "OverrideThrowableToString",
          "OverridingMethodInconsistentArgumentNamesChecker",
          "ParameterName",
          "PatternMatchingInstanceof",
          "PatternMatchingInstanceof",
          "PreconditionsCheckNotNullRepeated",
          "PreferInstanceofOverGetKind",
          "PreferInstanceofOverGetKind",
          "PreferTestParameter",
          "PreferThrowsTag",
          "PrimitiveAtomicReference",
          "ProtectedMembersInFinalClass",
          "ProtectedMembersInFinalClass",
          "ProtoDurationGetSecondsGetNano",
          "ProtoTimestampGetSecondsGetNano",
          "QualifierOrScopeOnInjectMethod",
          "ReachabilityFenceUsage",
          "RecordComponentOverride",
          "RedundantControlFlow",
          "RefactorSwitch",
          "RefactorSwitch",
          "ReferenceEquality",
          "RethrowReflectiveOperationExceptionAsLinkageError",
          "ReturnAtTheEndOfVoidFunction",
          "ReturnFromVoid",
          "RobolectricShadowDirectlyOn",
          "RuleNotRun",
          "RxReturnValueIgnored",
          "ScannerUseDelimiter",
          "SelfAlwaysReturnsThis",
          "SelfSet",
          "StatementSwitchToExpressionSwitch",
          "StatementSwitchToExpressionSwitch",
          "StaticAssignmentInConstructor",
          "StaticAssignmentOfThrowable",
          "StaticGuardedByInstance",
          "StaticMockMember",
          "StreamResourceLeak",
          "StreamToIterable",
          "StringCharset",
          "StringConcatToTextBlock",
          "StringConcatToTextBlock",
          "SwigMemoryLeak",
          "SynchronizeOnNonFinalField",
          "SystemConsoleNull",
          "ThreadJoinLoop",
          "ThreadLocalUsage",
          "ThreadPriorityCheck",
          "ThreeLetterTimeZoneID",
          "ThrowableEqualsHashCode",
          "ThrowIfUncheckedKnownUnchecked",
          "TimeInStaticInitializer",
          "TimeUnitConversionChecker",
          "ToStringReturnsNull",
          "TraditionalSwitchExpression",
          "TraditionalSwitchExpression",
          "TruthAssertExpected",
          "TruthConstantAsserts",
          "TruthGetOrDefault",
          "TruthIncompatibleType",
          "TypeEquals",
          "TypeNameShadowing",
          "TypeParameterShadowing",
          "TypeParameterUnusedInFormals",
          "UndefinedEquals",
          "UnicodeEscape",
          "UnnamedVariable",
          "UnnamedVariable",
          "UnnecessaryAssignment",
          "UnnecessaryAsync",
          "UnnecessaryBreakInSwitch",
          "UnnecessaryBreakInSwitch",
          "UnnecessaryCopy",
          "UnnecessaryDefaultInEnumSwitch",
          "UnnecessaryLambda",
          "UnnecessaryLambda",
          "UnnecessaryLongToIntConversion",
          "UnnecessaryMethodInvocationMatcher",
          "UnnecessaryMethodReference",
          "UnnecessaryMethodReference",
          "UnnecessaryOptionalGet",
          "UnnecessaryParentheses",
          "UnnecessaryParentheses",
          "UnnecessaryQualifier",
          "UnnecessaryStringBuilder",
          "UnrecognisedJavadocTag",
          "UnsafeFinalization",
          "UnsafeReflectiveConstructionCast",
          "UnsynchronizedOverridesSynchronized",
          "UnusedLabel",
          "UnusedMethod",
          "UnusedNestedClass",
          "UnusedTypeParameter",
          "UnusedVariable",
          "UnusedVariable",
          "URLEqualsHashCode",
          "UseBinds",
          "UseEnumSwitch",
          "VariableNameSameAsType",
          "VoidUsed",
          "WaitNotInLoop",
          "WakelockReleasedDangerously",
      )

      // warning-level checks that are enabled by default, but which we violate one or more times
      disable(
          "AlmostJavadoc",
          "AlreadyChecked",
          "AmbiguousMethodReference",
          "ArrayRecordComponent",
          "AssignmentExpression",
          "AvoidCommonTypeNames",
          "BadImport",
          "BooleanLiteral",
          "CatchAndPrintStackTrace",
          "DefaultCharset",
          "DuplicateBranches",
          "EffectivelyPrivate",
          "EmptyBlockTag",
          "EmptyCatch",
          "EnumOrdinal",
          "EqualsGetClass",
          "ExposedPrivateType",
          "HidingField",
          "ImmutableEnumChecker",
          "InconsistentCapitalization",
          "InlineMeSuggester",
          "IterableAndIterator",
          "MixedMutabilityReturnType",
          "MutablePublicArray",
          "NonApiType",
          "NonCanonicalType",
          "NotJavadoc",
          "NullablePrimitiveArray",
          "OperatorPrecedence",
          "SameNameButDifferent",
          "ShortCircuitBoolean",
          "StringCaseLocaleUsage",
          "StringSplitter",
          "SuperCallToObjectMethod",
      )

      // checks we do not intend to try to fix in the near-term:

      // Just too many of these; proper Javadoc would be a great long-term goal
      disable("MissingSummary")

      // WALA uses Java, not C.  Evaluation order for argument lists is well-specified.
      disable("MultipleUnaryOperatorsInMethodCall")

      // WALA has many optimizations involving using == to check reference equality.  They
      // may be unnecessary on modern JITs, but fixing these issues requires subtle changes
      // that could introduce bugs
      disable("ReferenceEquality")

      // Example for running Error Prone's auto-patcher.  To run, uncomment and change the
      // check name to the one you want to patch, and also disable -Werror below
      //    		errorproneArgs.addAll(
      //    				"-XepPatchChecks:UnnecessaryParentheses",
      //    				"-XepPatchLocation:IN_PLACE"
      //    		)
    }
  }
}

eclipse.synchronizationTasks("processTestResources")

tasks.named<Test>("test") {
  useJUnitPlatform()

  include("**/*Test.class")
  include("**/*TestCase.class")
  include("**/*Tests.class")
  include("**/Test*.class")
  exclude("**/*AndroidLibs*.class")

  val trial = providers.gradleProperty("trial").orNull
  if (trial != null) {
    outputs.upToDateWhen { false }
    val csvResultsFile = rootProject.layout.buildDirectory.file("time-trials.csv").map { it.asFile }

    fun File.appendRow(
        trial: String,
        className: String,
        name: String,
        resultType: Any,
        startTime: Any,
        endTime: Any,
    ) =
        appendText(
            listOf(trial, className, name, resultType, startTime, endTime)
                .joinToString(",", postfix = "\n")
        )

    addTestListener(
        object : TestListener {
          override fun afterTest(descriptor: TestDescriptor, result: TestResult) {
            csvResultsFile.get().let {
              if (!it.exists()) {
                it.appendRow("trial", "className", "name", "resultType", "startTime", "endTime")
              }
              it.appendRow(
                  trial,
                  descriptor.className!!,
                  "\"${descriptor.name}\"",
                  result.resultType,
                  result.startTime,
                  result.endTime,
              )
            }
          }
        }
    )
  } else {
    maxParallelForks = (Runtime.getRuntime().availableProcessors() / 2).coerceAtLeast(1)
  }
}

if (providers.gradleProperty("excludeSlowTests").isPresent) {
  tasks.named<Test>("test") { useJUnitPlatform { excludeTags("slow") } }
}

val ecjCompileTaskProviders =
    sourceSets.map { sourceSet -> JavaCompileUsingEcj.withSourceSet(project, sourceSet) }

tasks.named("check") { dependsOn(ecjCompileTaskProviders) }

tasks.withType<JavaCompile>().configureEach {
  options.run {
    encoding = "UTF-8"
    compilerArgs.add("-Werror")
    compilerArgs.add("-parameters")
  }
}

tasks.withType<JavaCompileUsingEcj>().configureEach {
  // ECJ warning / error levels are set via a configuration file, not this argument
  options.compilerArgs.remove("-Werror")
}

// Special hack for WALA as an included build.  Composite
// builds only build and use artifacts from the default
// configuration of included builds:
// <https://docs.gradle.org/current/userguide/composite_builds.html#included_build_substitution_limitations>.
// This known limitation makes WALA test fixtures unavailable
// when WALA is included in a composite build.  As a
// workaround for composite projects that rely on those test
// fixtures, we extend the main sourceSet to include all
// test-fixture sources too.  This hack is only applied when
// WALA itself is an included build.
if (gradle.parent != null) {
  afterEvaluate {
    sourceSets["main"].java.srcDirs(sourceSets["testFixtures"].java.srcDirs)

    dependencies { "implementation"(configurations["testFixturesImplementation"].dependencies) }
  }
}

spotless { java { googleJavaFormat(catalogVersion("google-java-format")) } }
