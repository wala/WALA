// Code generated with the assistance of ChatGPT (OpenAI)
// Date: March 10, 2025
function testExponentationConstant() {
  return 2 ** -6;
}

function testExponentationVariables(x, y) {
  return x ** y;
}

function testExponentationWithinFunction(two) {
  return  function w(i) { return i ** 1; } ( two );
}

function runExponentationTests() {
    var x = 2, y = 3;
    testExponentationConstant();
    testExponentationVariables(x, y);
    testExponentationWithinFunction(x);
}

runExponentationTests();




function testSimpleTemplateLiteral() {
    let name = "Alice";
    let result = `Hello, ${name}!`;
    return result;
}

function testMultilineTemplateLiteral() {
    let multiline = `Line 1
Line 2
Line 3`;
    return multiline;  // Expected: "Line 1\nLine 2\nLine 3"
}

function testTemplateLiteralWithExpression() {
    let a = 10, b = 20;
    let result = `The sum of ${a} and ${b} is ${a + b}.`;
    return result;  // Expected: "The sum of 10 and 20 is 30."
}

function testFunctionInTemplateLiteral() {
    function multiply(x, y) {
        return x * y;
    }
    let result = `The product of 5 and 6 is ${multiply(5, 6)}.`;
    return result;  // Expected: "The product of 5 and 6 is 30."
}

function testTemplateLiteralWithFallback() {
    let name = null;
    let result = `Hello, ${name || "Guest"}!`;
    return result;  // Expected: "Hello, Guest!"
}

function testIllegalEscapeSequence() {
    try {
        let invalidTemplate = `Unescaped sequence: \u{D800}`;  // Invalid Unicode escape
        return invalidTemplate;
    } catch (e) {
        return `Error with illegal escape sequence: ${e}`;  // Expected: Error about invalid Unicode escape sequence
    }
}

function testNestedExpressions() {
    let x = 2, y = 3, z = 4;
    let result = `Result is: ${x + y * z}`; // Parentheses around x + y * z to control order of operations
    return result;  // Expected: "Result is: 14"
}

function testTemplateWithFunctions() {
    function greet(name) {
        return `Hello, ${name}!`;
    }

    let result = `Message: ${greet('Alice')}`;
    return result;  // Expected: "Message: Hello, Alice!"
}

function testNestedTemplateLiterals() {
    let name = "World";
    let greeting = `Hello, ${`Dear ${name}`}`;
    return greeting;  // Expected: "Hello, Dear World"
}

function testTemplateLiteralWithObject() {
    let user = { name: "John", age: 30 };
    let result = `User info: ${user.name}, Age: ${user.age}`;
    return result;  // Expected: "User info: John, Age: 30"
}

function testTemplateLiteralWithUndefined() {
    let x;
    let result = `Value is ${x}`;
    return result;  // Expected: "Value is undefined"

    let y = null;
    result = `Value is ${y}`;
    return result;  // Expected: "Value is null"
}

function testEscapeSequences() {
    let escaped = `This is a backslash: \\ and this is a quote: \"`;
    return escaped;  // Expected: "This is a backslash: \\ and this is a quote: \""
}

function testTemplateWithFunctionCalls() {
    let func = (x) => `Result: ${x}`;
    let result = `Output: ${func(5)}`;
    return result;  // Expected: "Output: Result: 5"
}

function runTemplateTests() {
    testSimpleTemplateLiteral();
    testMultilineTemplateLiteral();
    testTemplateLiteralWithExpression();
    testFunctionInTemplateLiteral();
    testTemplateLiteralWithFallback();
    testIllegalEscapeSequence();
    testNestedExpressions();
    testTemplateWithFunctions();
    testNestedTemplateLiterals();
    testTemplateLiteralWithObject();
    testTemplateLiteralWithUndefined();
    testEscapeSequences();
    testTemplateWithFunctionCalls();
}

runTemplateTests();
