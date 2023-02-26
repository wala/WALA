function f() {
 var x = 10;
 var y;
 y = x;
 if(y == 100) {
     return true;
 }
 return false;
}

function ff() {
	 var x = 10;
	 var y;
	 y = x;
	 var z;
	 z = y;
	 if(z == 100) {
	     return true;
	 }
	 return false;
	}

var b = f();
var c = ff();