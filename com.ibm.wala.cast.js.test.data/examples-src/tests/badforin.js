function copyObj(to, from) {
    for(var p in from) {
	(function _forin_body (name) {
	    to[name] = from[name];
	})(p);
    }
}

function testForIn( x ) {
    var z;
    for(var y in x) {
	if (y in x) {
	    z = (x[y])();
	}
    }
}

var obj = {
  foo: function testForIn1() { return 7; },
  bar: function testForIn2() { return "whatever"; }
}

testForIn(obj);

(function _check_obj_foo () { obj.foo(); })();
(function _check_obj_bar () { obj.bar(); })();

var copy = new Object();
copyObj(copy, obj);

(function _check_copy_foo () { copy.foo(); })();
(function _check_copy_bar () { copy.bar(); })();

