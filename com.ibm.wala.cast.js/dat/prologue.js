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
NaN = primitive("GlobalNaN");

Infinity = primitive("GlobalInfinity");

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


/************************************************************************/
/* Array properties, see spec 15.4					*/
/************************************************************************/

Array.prototype = {

  prototype: Object.prototype,

  constructor: Array,

  toString: function arrayToString () {
    return this.join(",");
  },

  toLocaleString: function arrayToLocalString () {
    var result = "";
    var limit = this.length;
    for(var k = 0; k < limit; k++) {
      result = result.concat( this[k].toLocaleString() );
      result = result.concat( "," );
    }

    return result;
  },

  concat: function concat () {
    var result = new Array();
    var n = 0;
    
    for(var i = 0; i < this.length; i++)
      result[n++] = this[i];

    for(i = 0; i < arguments.length; i++)
      for(var j = 0; j < arguments[i].length; j++)
        result[n++] = arguments[i][j];

    result.length = n;

    return result;
  },

  join: function join (separator) {
    var result = "";
    var limit = this.length;
    for(var k = 0; k < limit; k++) {
      result = result.concat( this[k].toString() );
      result = result.concat( separator );
    }

    return result;
  },

  pop: function pop () {
    return this[ --this.length ];
  },

  push: function push () {
    var n = this.length
    for(var i = 0; i < arguments.length; i++) {
      this[ n++ ] = arguments[i];
    }

    this.length = n;
    return n;
  },

  reverse: function reverse () {
    var n = this.length;
    for (var k = 0; k < (n/2); k++) {
      var tmp = this[k];
      this[k] = this[n-k];
      this[n-k] = tmp;
    }

    return this;
  },

  shift: function shift () {
    var result = this[ 0 ];
    for(var i = 0; i < this.length-1; i++)
      this[i] = this[i+1];

    this.length--;

    return result;
  },

  slice: function slice (start, end) {
    var j = 0;
    if (start < 0) start = this.length + start;
    if (end < 0) end = this.length + end;
    var result = new Array();
    for(var i = start; i < end; i++)
      result[j++] = this[i];

    result.length = j;

    return result;
  },

  sort: function sort (fn) {
    for(var l = 0; i < this.length; l++) {
      var mindex = l;
      for(var i = l; i < this.length; i++) {
        if (fn(this[mindex], this[i]) < 0) {
          mindex = i;
        }
      }

      if (mindex != l) {
        var tmp = this[l];
        this[l] = this[mindex];
        this[mindex] = this[l];
      }
    }
  }
};


/************************************************************************/
/* String properties, see spec 15.4					*/
/************************************************************************/

String.prototype = {

  prototype: Object.prototype,

  constructor: String,

  $value: "",

  toString: function stringToString() {
    return this.$value;
  },

  valueOf: function stringValueOf() {
    return this.$value;
  },

  charAt: function stringCharAt(pos) {
    return new String(primitive("StringCharAt", pos));
  },

  charCodeAt: function stringCharCodeAt(pos) {
    return new Number(primitive("StringCharCodeAt", pos));
  },

  toUpperCase: function toUpperCase() {
    return new String(primitive("StringToUpperCase", this));
  },

  toLocaleUpperCase: function toLocaleUpperCase() {
    return new String(primitive("StringToLocaleUpperCase", this));
  },

  toLowerCase: function toLowerCase() {
    return new String(primitive("StringToLowerCase", this));
  },

  toLocaleLowerCase: function toLocaleLowerCase() {
    return new String(primitive("StringToLocaleLowerCase", this));
  },

  indexOf: function indexOf(str) {
    return new Number(primitive("StringIndexOf", this, str));
  },

  split: function stringSplit(separator, limit) {
    var y = primitive("splitCount", this, separator, limit);
    var x = new Array(y);
    for(var i = 0; i < y; i++) {
      x[i] = new String( primitive("splitNth", this, separator, limit, i) );
    }
    return x;
  },

  substring: function substring(from, to) {
    return new String(primitive("StringSubString", this, from, to));
  },

  replace: function replace(regex, withStr) {
    return new String(primitive("StringReplace", this, regex, withStr));
  }
};


/************************************************************************/
/* Number properties, see spec 15.7					*/
/************************************************************************/

Number.prototype = {

  prototype: Object.prototype,

  constructor: Number,

  $value: 0

};


/************************************************************************/
/* Math properties, see spec 15.8					*/
/************************************************************************/
Math = {

 E: primitive("MathE"),

 LN10: primitive("MathLN10"),

 LN2: primitive("MathLN2"),

 LOG2E: primitive("MathLOG2E"),

 LOG10E: primitive("MathLOG10E"),

 PI: primitive("PI"),

 SQRT1_2: primitive("MathSQRT1_2"),

 SQRT2: primitive("MathSQRT2"),

 abs: function abs (x) { return (x<0)? -x: x; },

 acos: function acos (x) { return primitive("MathACos", x); },

 asin: function asin (x) { return primitive("MathASin", x); },

 atan: function atan (x) { return primitive("MathATan", x); },

 atan2: function atan2 (y, x) { return primitive("MathATan2", y, x); },

 ceil: function ceil (x) { return primitive("MathCeil", x); },

 cos: function cos (x) { return primitive("MathCos", x); },

 exp: function exp (x) { return primitive("MathExp", x); },

 floor: function floor (x) { return primitive("MathFloor", x); },

 log: function log (x) { return primitive("MathLog", x); },

 max: function max () {
   var i = -Infinity;
   for(var j = 0; j < arguments.length; j++)
     if (arguments[j] > i)
       i = arguments[j];

   return i;
 },

 min: function min () {
   var i = Infinity;
   for(var j = 0; j < arguments.length; j++)
     if (arguments[j] < i)
       i = arguments[j];

   return i;
 },

 pow: function pow (x, y) { return primitive("MathPow", x, y); },

 random: function random() { return primitive("MathRandom"); },

 round: function round (x) { return primitive("MathRound", x); },

 sin: function sin (x) { return primitive("MathSin", x); },

 sqrt: function sqrt (x) { return primitive("MathSqrt", x);},

 tan: function tan (x) { return primitive("MathTan", x); }
};


/************************************************************************/
/* RegExp properties, see spec 15.10					*/
/************************************************************************/

RegExp.prototype = {

  prototype: Object.prototype,

  constructor: RegExp

};


