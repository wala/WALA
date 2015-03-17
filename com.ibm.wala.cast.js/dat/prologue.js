// the internal primitive mechanism
primitive = new Primitives();

// core definitions needed to make anything work, even what follows
Object = primitive("NewObject");
var local_function = primitive("NewFunction");
Function = local_function;
var local_array = primitive("NewArray");
Array = local_array;
var local_string = primitive("NewString");
String = local_string;
var local_number = primitive("NewNumber");
Number = local_number;
var local_regexp = primitive("NewRegExp");
RegExp = local_regexp;

/************************************************************************/
/* Global properties, see spec 15.1					*/
/************************************************************************/
NaN = primitive("GlobalNaN");

Infinity = primitive("GlobalInfinity");

undefined = primitive("NewUndefined");
$$undefined = undefined;

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

decodeURI = function decodeURI(str) {
    return new String(primitive("GlobalDecodeURI", str));
};

decodeURIComponent = function decodeURIComponent(str) {
    return new String(primitive("GlobalDecodeURIComponent", str));
};

encodeURI = function encodeURI(str) {
    return new String(primitive("GlobalEncodeURI", str));
};

encodeURIComponent = function encodeURIComponent(str) {
    return new String(primitive("GlobalEncodeURIComponent", str));
};

unescape = function unescape(str){
	return new String(primitive("GlobalUnEscape", str));
};

escape = function escape(str){
	return new String(primitive("GlobalEscape", str));
};

/************************************************************************/
/* Object properties, see spec 15.2					*/
/************************************************************************/

Object$proto$__WALA__ =  {

  prototype: null,

  __proto__: null,
  
  constructor: Object,

  toString: function Object_prototype_toString() {
    return primitive("ObjectToString", this);
  },

  toLocaleString: function Object_prototype_toLocaleString() {
    return primitive("ObjectToLocaleString", this);
  },

  valueOf: function valueOf() { return this },

  hasOwnProperty: function Object_prototype_hasOwnProperty (V) {
    return primitive("ObjectHasOwnProperty", this, V);
  },

  isPrototypeOf: function Object_prototype_isPrototypeOf (V) {
    return primitive("ObjectIsPrototypeOf", this, V);
  },
  
  propertyIsEnumerable: function Object_prototype_propertyIsEnumerable (V) {
    return primitive("ObjectPropertyIsEnumerable", this, V);
  }
};

Object.prototype = Object$proto$__WALA__;

/************************************************************************/
/* Function properties, see spec 15.3					*/
/************************************************************************/

Function$proto$__WALA__ = {

  constructor: Function,

  __proto__: Object.prototype, 
  
  toString: function Function_prototype_toString() {
    return primitive("FunctionToString", this);
  },

  apply: function Function_prototype_apply (thisArg, argArray) {
    return primitive("FunctionApply", this, thisArg, argArray);
  },

  call: function Function_prototype_call (thisArg) {
    arguments.shift();
    return primitive("FunctionCall", this, thisArg, arguments);
  },

  bind: function Function_prototype_bind (thisArg) {
    arguments.shift();
    return primitive("FunctionBind", this, thisArg, arguments);
  }
};

local_function.prototype = Function$proto$__WALA__;

local_function.__proto__ = Function.prototype;

/************************************************************************/
/* Array properties, see spec 15.4					*/
/************************************************************************/

local_array.__proto__ = Function.prototype;

Array$proto$__WALA__ = {

  __proto__: Object.prototype,

  constructor: Array,

  toString: function Array_prototype_toString () {
    return this.join(",");
  },

  toLocaleString: function Array_prototype_toLocaleString () {
    var result = "";
    var limit = this.length;
    for(var k = 0; k < limit; k++) {
      result = result.concat( this[k].toLocaleString() );
      result = result.concat( "," );
    }

    return result;
  },

  concat: function Array_prototype_concat () {
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

  join: function Array_prototype_join (separator) {
    var result = "";
    var limit = this.length;
    for(var k = 0; k < limit; k++) {
      result = result.concat( this[k].toString() );
      result = result.concat( separator );
    }

    return result;
  },

  pop: function Array_prototype_pop () {
	  var n0 = this.length;
	  if (n0) {
		  var n1 = this[n0-1];
		  this.length = n0-1;
		  // needed for non-arrays
		  delete this[n0-1];
		  return n1;
	  } else {
		  // needed for non-arrays
		  this.length = 0;
	  }
  },

  push: function Array_prototype_push () {
    var n = this.length;
    
    // nasty hack for field-sensitive builders
    // TODO: fix this somehow
    if (n == 0) {
      this[0] = arguments[0]; 
    }

    for(var i = 0; i < arguments.length; i++) {
      this[ n++ ] = arguments[i];
    }

    this.length = n;
    return n;
  },

  reverse: function Array_prototype_reverse () {
    var n = this.length;
    for (var k = 0; k < (n/2); k++) {
      var tmp = this[k];
      this[k] = this[n-k];
      this[n-k] = tmp;
    }

    return this;
  },

  shift: function Array_prototype_shift () {
    var result = this[ 0 ];
    for(var i = 0; i < this.length-1; i++)
      this[i] = this[i+1];

    this.length--;

    return result;
  },

  unshift: function Array_prototype_unshift () {
	  var n = arguments.length;
	  for(var i=this.length+n-1;i>=n;--i)
		  this[i] = this[i-n];
	  for(;i>=0;--i)
		  this[i] = arguments[i];
	  this.length += n;
	  return this.length;
  },

  slice: function Array_prototype_slice (start, end) {
    var j = 0;
    if (start < 0) start = this.length + start;
    if (end < 0) end = this.length + end;
    var result = new Array();
    for(var i = start; i < end; i++)
      result[j++] = this[i];

    result.length = j;

    return result;
  },

  sort: function Array_prototype_sort (fn) {
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
  },
  
  splice: function Array_prototype_splice(start, delete_count) {
	  var old_len = arguments.length,
	      new_count = arguments.length - 2;
          new_len = old_len - deleteCount + new_count;
          
	  var deleted = this.slice(start, start + delete_count),
	  	  remainder = this.slice(start + delete_count, old_len);

	  for(var i=start;i<start+new_count;++i)
		  this[i] = arguments[2+start-i];
	  
	  for(var k=0;k<remainder.length;++k,++i)
		  this[i] = remainder[k];
	  
	  for(;i<old_len;++i)
		  delete this[i];
	  
	  this.length = new_len;
	  
	  return deleted;
  },
  
  indexOf: function Array_prototype_indexOf(elt, start) {
	  if(arguments.length < 2)
		  start = 0;
	  if(start < 0) start += this.length;
	  if(start < 0) start = 0;
	  for(var i=start;i<this.length;++i)
		  if(this[i] === elt)
			  return i;
	  return -1;
  },
  
  forEach: function Array_prototype_forEach(callback, thisArg) {
	  for(var i=0;i<this.length;++i)
		  callback.call(thisArg, this[i], i, this);
  },
  
  map: function Array_prototype_map(callback, thisArg) {
	  var res = [];
	  for(var i=0;i<this.length;++i)
		  res[i] = callback.call(thisArg, this[i], i, this);
	  res.length = this.length;
	  return res;
  },
  
  item: function Array_prototype_item(index) {
	  return this[index];
  },

  every: function Array_prototype_every(arg1, arg2) {
	  var n0 = this.length;
	  var n3 = true;
	  for (var i = 0; i < n0; i += 1) {
	    var n1 = i in this;
	    if (n1) {
	      var n2 = this[i];
	      n3 = arg1.call(arg2, n2, i, this);
	      if (!n3) {
	        break;
	      }
	    }
	  }
	  return n3;
  },

  some: function Array_prototype_some(arg1, arg2) {
	  var n0 = this.length;
	  var n3 = false;
	  for (var i = 0; i < n0; i += 1) {
	    var n1 = i in this;
	    if (n1) {
	      var n2 = this[i];
	      n3 = arg1.call(arg2, n2, i, this);
	      if (n3) {
	        break;
	      }
	    }
	  }
	  return n3;
  },

  reduce: function Array_prototype_reduce(arg1, arg2) {
	  var result = arg2;
	  var n0 = this.length;
	  for (var i = 0; i < n0; i += 1) {
	    var n1 = i in this;
	    if (n1) {
	      var n2 = this[i];
	      var n3 = arg1.call(undefined, result, n2, i, this);
	      result = n3;
	    }
	  }
	  return result;
  },

  reduceRight: function Array_prototype_reduceRight(arg1, arg2) {
	  var result = arg2;
	  var n0 = this.length;
	  for (var i = 0; i < n0; i += 1) {
	    var n1 = ((n0-i)-1) in this;
	    if (n1) {
	      var n2 = this[(n0-i)-1];
	      var n3 = arg1.call(undefined, result, n2, (n0-i)-1, this);
	      result = n3;
	    }
	  }
	  return result;
  },

  filter: function Array_prototype_filter(arg1, arg2) {
	  var result = [];
	  var n0 = this.length;
	  for (var i = 0; i < n0; i += 1) {
	    var n1 = i in this;
	    if (n1) {
	      var n2 = this[i];
	      var n3 = arg1.call(arg2, n2, i, this);
	      if (n3) {
	        result[result.length] = n2;
	      }
	    }
	  }
	  return result;
  }

};

Array.isArray = function Array_isArray(a) {
	return true || false;
};

local_array.prototype = Array$proto$__WALA__;

/************************************************************************/
/* String properties, see spec 15.4					*/
/************************************************************************/

local_string.__proto__ = Function.prototype;

String$proto$__WALA__ = {

  __proto__: Object.prototype,

  constructor: String,

  $value: "",

  toString: function String_prototype_toString() {
    return this.$value;
  },

  valueOf: function stringValueOf() {
    return this.$value;
  },

  charAt: function String_prototype_charAt(pos) {
    return new String(primitive("StringCharAt", pos));
  },

  charCodeAt: function String_prototype_charCodeAt(pos) {
    return new Number(primitive("StringCharCodeAt", pos));
  },

  concat: function String_prototype_concat () {
    var result = this;
    
    for(i = 0; i < arguments.length; i++)
      result = result + arguments[i];

    return result;
  },

  toUpperCase: function String_prototype_toUpperCase() {
    return new String(primitive("StringToUpperCase", this));
  },

  toLocaleUpperCase: function String_prototype_toLocaleUpperCase() {
    return new String(primitive("StringToLocaleUpperCase", this));
  },

  toLowerCase: function String_prototype_toLowerCase() {
    return new String(primitive("StringToLowerCase", this));
  },

  toLocaleLowerCase: function String_prototype_toLocaleLowerCase() {
    return new String(primitive("StringToLocaleLowerCase", this));
  },

  indexOf: function String_prototype_indexOf(str) {
    return new Number(primitive("StringIndexOf", this, str));
  },

  split: function String_prototype_split(separator, limit) {
    var y = primitive("splitCount", this, separator, limit);
    var x = new Array(y);
    for(var i = 0; i < y; i++) {
      x[i] = new String( primitive("splitNth", this, separator, limit, i) );
    }
    return x;
  },

  substring: function String_prototype_substring(from, to) {
	return new String(primitive("StringSubString", this, from, to)); 
  },
  
  slice: function String_prototype_slice(from, to) {
	  if(from < 0) from += this.length;
	  if(to < 0) to += this.length;
	  return this.substring(from, to);
  },
	  
  substr: function String_prototype_substr(from, to) {
	return new String(primitive("StringSubStr", this, from, to));
  },

  replace: function String_prototype_replace(regex, withStr) {
    // return new String(primitive("StringReplace", this, regex, withStr));
    return this || withStr;
  },
  
  match: function String_prototype_match(regexp) {
	  return new Array(primitive("StringMatch", this, regexp));
  },
  
  trim: function String_prototype_trim() {
	  return new String(primitive("StringTrim", this));
  },
  
  loadFile: function loadFile() {
    // magic function body handled in analysis.
  },
  
  link: function String_prototype_link(url) {
  },
  
  anchor: function String_prototype_anchor(url) {
	  return new String();
  }

};

local_string.prototype = String$proto$__WALA__;

/************************************************************************/
/* Number properties, see spec 15.7					*/
/************************************************************************/

local_number.__proto__ = Function.prototype;

Number$proto$__WALA__ = {

  __proto__: Object.prototype,

  constructor: Number,

  $value: 0,
  
  toString: function Number_prototype_toString() {
	  return primitive("NumberToString", this);
  }

};

local_number.prototype = Number$proto$__WALA__;

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

 abs: function Math_abs (x) { return (x<0)? -x: x; },

 acos: function Math_acos (x) { return primitive("MathACos", x); },

 asin: function Math_asin (x) { return primitive("MathASin", x); },

 atan: function Math_atan (x) { return primitive("MathATan", x); },

 atan2: function Math_atan2 (y, x) { return primitive("MathATan2", y, x); },

 ceil: function Math_ceil (x) { return primitive("MathCeil", x); },

 cos: function Math_cos (x) { return primitive("MathCos", x); },

 exp: function Math_exp (x) { return primitive("MathExp", x); },

 floor: function Math_floor (x) { return primitive("MathFloor", x); },

 log: function Math_log (x) { return primitive("MathLog", x); },

 max: function Math_max () {
   var i = -Infinity;
   for(var j = 0; j < arguments.length; j++)
     if (arguments[j] > i)
       i = arguments[j];

   return i;
 },

 min: function Math_min () {
   var i = Infinity;
   for(var j = 0; j < arguments.length; j++)
     if (arguments[j] < i)
       i = arguments[j];

   return i;
 },

 pow: function Math_pow (x, y) { return primitive("MathPow", x, y); },

 random: function Math_random() { return primitive("MathRandom"); },

 round: function Math_round (x) { return primitive("MathRound", x); },

 sin: function Math_sin (x) { return primitive("MathSin", x); },

 sqrt: function Math_sqrt (x) { return primitive("MathSqrt", x);},

 tan: function Math_tan (x) { return primitive("MathTan", x); }
};


/************************************************************************/
/* RegExp properties, see spec 15.10					*/
/************************************************************************/

local_regexp.__proto__ = Function.prototype;

RegExp$proto$__WALA__ = {

  __proto__: Object.prototype,

  constructor: RegExp,
  
  exec: function RegExp_prototype_exec(string) {
	  return [ string, string, string, string, string ] || null;
  },
  
  test: function RegExp_prototype_test(string) {
	  return true || false;
  }

};

local_regexp.prototype = RegExp$proto$__WALA__;

/************************************************************************/
/* Date properties, see spec 15.9					*/
/************************************************************************/

Date = function Date() {};

Data$proto$__WALA__ = {

  __proto__: Object.prototype,

  constructor: Date,
  
  getTime: function Date_prototype_getTime() {
	  return primitive("DateGetTime", this); 
  },
  
  getDate: function Date_prototype_getDate() {
	  // TODO: model me
  },
  
  setDate: function Date_prototype_setDate() {
	  // TODO: model me
  },
  
  getDay: function Date_prototype_getDay() {
	  // TODO: model me
  },
  
  setDay: function Date_prototype_setDay() {
	  // TODO: model me
  },
  
  getMonth: function Date_prototype_getMonth() {
	  // TODO: model me
  },
  
  setMonth: function Date_prototype_setMonth() {
	  // TODO: model me
  },
  
  getHours: function Date_prototype_getHours() {
	  // TODO: model me
  },
  
  setHours: function Date_prototype_setHours() {
	  // TODO: model me
  },
  
  getMinutes: function Date_prototype_getMinutes() {
	  // TODO: model me
  },
  
  setMinutes: function Date_prototype_setMinutes() {
	  // TODO: model me
  },
  
  getSeconds: function Date_prototype_getSeconds() {
	  // TODO: model me
  },
  
  setSeconds: function Date_prototype_setSeconds() {
	  // TODO: model me
  },
  
  getMilliseconds: function Date_prototype_getMilliseconds() {
	  // TODO: model me
  },
  
  setMilliseconds: function Date_prototype_setMilliseconds() {
	  // TODO: model me
  },
  
  getFullYear: function Date_prototype_getFullYear() {
	  // TODO: model me
  }

};

Date.now = function Date_now() {
	return new Date().valueOf();
};

Date.prototype = Data$proto$__WALA__;


/************************************************************************/
/* internal stuff
/************************************************************************/

function Error(str) {
	this.message = new String();
}

function EvalError(str) {
	this.message = new String();
}


