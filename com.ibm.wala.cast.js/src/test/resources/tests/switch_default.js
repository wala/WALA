function fun1() { return 1; }
function fun2() { return 2; }

function withSwitch(i) {
  switch (i) {
    case 0: return fun1();
    default: return fun2();
  }
}

withSwitch(2);

function fun3() { return 3; }
function fun4() { return 4; }

function withSwitchStr(s) {
  switch (s) {
    case 'Hello': return fun3();
    default: return fun4();
  }
}

withSwitchStr('Goodbye');
