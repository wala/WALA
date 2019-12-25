function copyObjRec(to, from) {
	
	for (var p in from) {
		(function _forin_body(name) {
			to[name] = from[name];
			copyObjRec(to,from[name]);
		})(p);
	}
}


var obj = {
		  foo: function testForIn1() { return 7; },
		  bar: function testForIn2() { return "whatever"; }
		}

obj.baz = obj;

var copy = new Object();

copyObjRec(copy, obj);