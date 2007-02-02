function foo(x, y) {
  return x + y;
}

function bar(n, s) {
	if (n == 0) {
		return s;
	}
	return "a" + bar(n-1, s) + "b";
}

// simple concatenation
a = "strA";
b = "strB";
r1 = foo(a, b);
document.write("r1: " + r1 + "<br/>");

// conditional branch
if (a == "strA") {
  r2 = foo("a", a);
}
else {
  r2 = foo("b", b);
}
document.write("r2: " + r2 + "<br/>");

// for-loop statement
r3 = a;
for (var i = 0; i<10; i++) {
  r3 = r3 + b;
}
document.write("r3: " + r3 + "<br/>");

// while-loop
r4 = a;
while (r4.length<20) {
  r4 = r4 + b;
}
document.write("r4: " + r4 + "<br/>");

// recursive function
r5 = bar(5, a);
document.write("r5: " + r5 + "<br/>");

// array
r10 = ["ARY", a, b];
r11 = r10[1];

// associative array
r20 = {"KA":a, "KB":b, "KC":"ASSOC"};
r21 = r20["KC"];

// constructor & property
function FooClass(){
  this.name = "foo";
  this.code = 123;
}
var fooObj = new FooClass();
rProp1 = fooObj.name;
document.write("rProp1: " + rProp1 + "<br/>");

// closure
function myFunc1() {
  var lvar = "local variable";
  var f = function(){ return lvar; };
  return f;
}

myFunc2 = function() {
  var lvar = "local variable";
  var f = function(){ return lvar; };
  var g = function(){
    var lvar = "local variable2";
    return function(){ return lvar; };
  };
  return [f,g];
}

// pre-defined functions
rSubstr1 = a.substring(0, 3);
document.write("rSubstr1: " + rSubstr1 + "<br/>");

if (a.length > 4) {
  rSubstr2 = a.substring(0, 3);
}
else {
  rSubstr2 = b.substring(3, 4);
}
document.write("rSubstr2: " + rSubstr2 + "<br/>");

rSubstr3 = a.substr(1,3);
document.write("rSubstr3: " + rSubstr3 + "<br/>");

rToUpperCase1 = a.toUpperCase();
document.write("rToUpperCase1: " + rToUpperCase1 + "<br/>");

rToLowerCase1 = a.toLowerCase();
document.write("rToLowerCase1: " + rToLowerCase1 + "<br/>");

rToLocaleUpperCase1 = a.toLocaleUpperCase();
document.write("rToLocaleUpperCase1: " + rToLocaleUpperCase1 + "<br/>");

rToLocaleLowerCase1 = a.toLocaleLowerCase();
document.write("rToLocaleLowerCase1: " + rToLocaleLowerCase1 + "<br/>");

rIndexOf1 = a.indexOf("r");
document.write("rIndexOf1: " + rIndexOf1 + "<br/>");

rConcat1 = a.concat(b,b,b);
document.write("rConcat1: " + rConcat1 + "<br/>");

rCharAt1 = a.charAt(1);
document.write("rCharAt1: " + rCharAt1 + "<br/>");

rCharAt2 = a.charAt(4);
document.write("rCharAt2: " + rCharAt2 + "<br/>");

rSplit1 = "a:b:c".split(":");
document.write("rSplit1: " + rSplit1 + "<br/>");

// for-loop with function calls
r40 = "abcdefg";
for (var i = 0; i < 1; i++) {
  r40 = r40.substr(1, 3);
}
rCyclic1 = r40;
document.write("rSplitCyclic1: " + rCyclic1 + "<br/>");

// non-constant parameters
var s40 = "abc"
if (cond) {
  s40 = s40.substr(1,2);
}
else {
  s40 = s40.substr(0,2);
}
r40 = "abc".replace("c",s40);
document.write("r40: " + r40 + "<br />");
