var propName = "f";
var reachable1 = function reachable1() { return 3; };
var reachable2 = function reachable2() { return 4; };
var m = function m(x,y) {
	function n(a,b,p,r1,r2) {
		var n_inner = function n_inner(p_inner) {
			this[p_inner] = this.f1;
			return this;
		}
	    a.f1 = r1;
		n_inner.call(a,p);
		b.f1 = r2;
		n_inner.call(b,p)
	}
	n(x,y,propName,reachable1,reachable2);
}

var a = {};
var b = {};

m(a,b);
var p = a[propName]();
var q = b[propName]();