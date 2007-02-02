
function testSwitch( x ) {
  var result = -1;
  switch ( x ) {
  case 3:
    result = 7;

  case 4:
  case 5: {
    result = result+3;
    break;
  }

  case 6:
    result = 2;
  
  default:
    result += 4;
  }

  return result;
}

function testDoWhile( x ) {
  var result = 6;
  do {
    if (x > 100)
      continue;
    else if (x < 0)
      break;
    else
      result += 1;
  } while (--x > 4);
  return result;
}

function testWhile( x ) {
  var result = 6;
  while (--x > 4) {
    if (x > 100)
      continue;
    else if (x < 0)
      break;
    else
      result += 1;
  } 
  return result;
}

function testFor( x ) {
  for(var result = 6; x > 4; x--) {
    if (x > 100)
      continue;
    else if (x < 0)
      break;
    else
      result += 1;
  } 
  return result;
}

function testReturn( x ) {
  if (x < 17) 
    return 8;
  x++;
  return x;
}

function testDeadLoop( x ) {
  while (x < 17) {
    return x++;
  }
  while (x < 17) {
    if (x != 5) continue;
    return x++;
  }
  return 0;
}

testSwitch( 7 );

testDoWhile( 5 );

testWhile( 11 );

testFor( 16 );

testReturn( 2 );

testDeadLoop( 12 );

