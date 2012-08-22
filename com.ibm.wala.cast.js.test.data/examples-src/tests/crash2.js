var standalone = false;
try {
   document; // ReferenceError
} catch(error) {
   standalone = true;
}
assert(standalone);