
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

var result = outer( 5 );
