// dummy definitions of the browser objects

Document = primitive("NewDocumentObject");

Document.prototype = {
  write: function(str) {
    primitive("DocumentWrite");
  }
}

document = new Document();
frame = new Document();
