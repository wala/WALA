// the internal primitive mechanism
primitive = new Primitives();

// core definitions needed to make anything work, even what follows
Object = primitive("NewObject");
Function = primitive("NewFunction");
Array = primitive("NewArray");
String = primitive("NewString");
Number = primitive("NewNumber");
RegExp = primitive("NewRegExp");

/************************************************************************/
/* Global properties, see spec 15.1					*/
/************************************************************************/

undefined = primitive("NewUndefined");

eval = function eval (x) {
  return primitive("GlobalEval", x); 
};

parseInt = function parseInt (string, radix) { 
  return primitive("GlobalParseInt", string, radix); 
};

parseFloat = function parseFloat (string) {
  return primitive("GlobalParseFloat", string);
};

isNaN = function isNaN (number) {
  return primitive("GlobalIsNaN", number)
};

isFinite = function isFinite (number) {
  return primitive("GlobalIsFinite", number);
};

decodeURI = primitive("GlobalDecodeURI");

decodeURIComponent = primitive("GlobalDecodeURIComponent");

encodeURI = primitive("GlobalEncodeURI");

encodeURIComponent = primitive("GlobalEncodeURIComponent");


/************************************************************************/
/* Object properties, see spec 15.2					*/
/************************************************************************/
    
Object.prototype = {

  prototype: null,

  constructor: Object,

  toString: function toString() {
    return primitive("ObjectToString", this);
  },

  toLocaleString: function toLocaleString() {
    return primitive("ObjectToLocaleString", this);
  },

  valueOf: function valueOf() { return this },

  hasOwnProperty: function hasOwnProperty (V) {
    return primitive("ObjectHasOwnProperty", this, V);
  },

  isPrototypeOf: function isPrototypeOf (V) {
    return primitive("ObjectIsPrototypeOf", this, V);
  },

  propertyIsEnumerable: function propertyIsEnumerable (V) {
    return primitive("ObjectPropertyIsEnumerable", this, V);
  }
};


/************************************************************************/
/* Function properties, see spec 15.3					*/
/************************************************************************/

Function.prototype = {

  constructor: Function,

  toString: function functionToString() {
    return primitive("FunctionToString", this);
  },

  apply: function functionApply (thisArg, argArray) {
    return primitive("FunctionApply", this, thisArg, argArray);
  },

  call: function functionCall (thisArg) {
    arguments.shift();
    return primitive("FunctionApply", this, thisArg, arguments);
  }
};

