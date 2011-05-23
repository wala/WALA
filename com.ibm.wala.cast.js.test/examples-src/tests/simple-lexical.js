
function outer( x ) {
  x++;
  var z = x;

  function inner( y ) {
    x += y;
    z += 1
    x++
  };

  var innerName = inner;

  function inner2( y ) {
    if (y < 3) innerName = inner3;
    innerName( y );
  }

  function inner3( y ) {
    for(x = 0; x < 10; x++) {
      y++;
    }
  }

  inner2( x );

  inner( 7 );

  inner3( 2 );

  (function indirect( f, x ) { f(x); })( innerName, 6 );

  function level1() {
    
    function level5() {
      x++;
      return x;
    }

    function level4() {
      return level5();
    }

    function level3() {
      if (x == 3) {
        level4();
      }
      return x;
    }

    function level2() {
      if (x > 2) {
        level3();
        return x;
      } else {
        return 7;
      }
    }

    x++;
    if (x < 7) {
      level2();
    }
    return x;
  }
    
  level1();

  return x+z;
}

function c2() {
	
}

function c3() {
	
}

function fa2(x) {
	x();
}

function fa3(x) {
	x();
}

function aa() {
	var c1 = function _c1() {
		
	}
	
	var fa = function _fa1(x) {
		x();
	}
	
	function bb(x) {
		fa = x;
	}
	
	fa(c1);
	
	bb(fa2);
	
	fa(c2);
	
	bb(fa3);
	
	fa(c3);
}

var result = outer( 5 );

aa();
