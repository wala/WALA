
function trivial(one, two) {
  var local = two + 7;
  if (local > 5)
    return one;
  else {
    return two;
  }	
}

function silly(one, two) {
  var local = two + 7;
  var result = 0;
  if (local > Math.E)
    result = --two;
  else  {
    result = 5;
    result = result<<1;
  }
  return result;
}

function weirder ( one ) {
  var result = Math.abs( one );
  for(var i = 0; i < 7; i++) {
    if ( ! ( (one + i) < 5 ) ) {
      result = (6, result / i);
    } else {
      result = 7;
    }
  }
  return result;
}

function stranger ( one ) {
  var result = ~one;
  result /= 7;
  var i = 0;
  do {
    if ( (one * i) < -5 ) {
      result = result % i;
    } else {
      result = 4;
    }
  } while (++i <= 7);

  return result;
}

function fib(x) {
  return (x < 2)? 1: (fib(x-1) + fib(x-2));
}

function bad(one, two) {
  var local = one + 7;
  var result = 0;
  switch (local) {
  case 1:
  case 2:
  case 3:
  case 4:
    result = +5;
    result = result>>>2;
    break;
  case 5:
  case 6:
  case 7:
    result = two>>1;
    break;
  default:
    result =-1;
  }
  return result;
}

function rubbish(one, two) {
  if (one(5))
    return one( 3 );
  else
    return rubbish(two, one);
}

rubbish(stranger, weirder);

trivial(3, 2);
bad(4, 5);
weirder( silly( "whatever", 7 ) );
