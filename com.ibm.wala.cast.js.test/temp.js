// Copyright 2005 Google
//
// Author: Steffen Meschkat <mesch@google.com>
//
// Miscellaneous utility and placeholder functions.

// Dummy implmentation for the logging functions. Replace by something
// useful when you want to debug.
function xpathLog(msg) {};
function xsltLog(msg) {};
function xsltLogXml(msg) {};

// Throws an exception if false.
function assert(b) {
  if (!b) {
    throw "Assertion failed";
  }
}

// Splits a string s at all occurrences of character c. This is like
// the split() method of the string object, but IE omits empty
// strings, which violates the invariant (s.split(x).join(x) == s).
function stringSplit(s, c) {
  var a = s.indexOf(c);
  if (a == -1) {
    return [ s ];
  }
  var parts = [];
  parts.push(s.substr(0,a));
  while (a != -1) {
    var a1 = s.indexOf(c, a + 1);
    if (a1 != -1) {
      parts.push(s.substr(a + 1, a1 - a - 1));
    } else {
      parts.push(s.substr(a + 1));
    }
    a = a1;
  }
  return parts;
}

// The following function does what document.importNode(node, true)
// would do for us here; however that method is broken in Safari/1.3,
// so we have to emulate it.
function xmlImportNode(doc, node) {
  if (node.nodeType == DOM_TEXT_NODE) {
    return domCreateTextNode(doc, node.nodeValue);

  } else if (node.nodeType == DOM_CDATA_SECTION_NODE) {
    return domCreateCDATASection(doc, node.nodeValue);

  } else if (node.nodeType == DOM_ELEMENT_NODE) {
    var newNode = domCreateElement(doc, node.nodeName);
    for (var i = 0; i < node.attributes.length; ++i) {
      var an = node.attributes[i];
      var name = an.nodeName;
      var value = an.nodeValue;
      domSetAttribute(newNode, name, value);
    }

    for (var c = node.firstChild; c; c = c.nextSibling) {
      var cn = arguments.callee(doc, c);
      domAppendChild(newNode, cn);
    }

    return newNode;

  } else {
    return domCreateComment(doc, node.nodeName);
  }
}

// A set data structure. It can also be used as a map (i.e. the keys
// can have values other than 1), but we don't call it map because it
// would be ambiguous in this context. Also, the map is iterable, so
// we can use it to replace for-in loops over core javascript Objects.
// For-in iteration breaks when Object.prototype is modified, which
// some clients of the maps API do.
//
// NOTE(mesch): The set keys by the string value of its element, NOT
// by the typed value. In particular, objects can't be used as keys.
//
// @constructor
function Set() {
  this.keys = [];
}

Set.prototype.size = function() {
  return this.keys.length;
}

// Adds the entry to the set, ignoring if it is present.
Set.prototype.add = function(key, opt_value) {
  var value = opt_value || 1;
  if (!this.contains(key)) {
    this[':' + key] = value;
    this.keys.push(key);
  }
}

// Sets the entry in the set, adding if it is not yet present.
Set.prototype.set = function(key, opt_value) {
  var value = opt_value || 1;
  if (!this.contains(key)) {
    this[':' + key] = value;
    this.keys.push(key);
  } else {
    this[':' + key] = value;
  }
}

// Increments the key's value by 1. This works around the fact that
// numbers are always passed by value, never by reference, so that we
// can't increment the value returned by get(), or the iterator
// argument. Sets the key's value to 1 if it doesn't exist yet.
Set.prototype.inc = function(key) {
  if (!this.contains(key)) {
    this[':' + key] = 1;
    this.keys.push(key);
  } else {
    this[':' + key]++;
  }
}

Set.prototype.get = function(key) {
  if (this.contains(key)) {
    return this[':' + key];
  } else {
    var undefined;
    return undefined;
  }
}

// Removes the entry from the set.
Set.prototype.remove = function(key) {
  if (this.contains(key)) {
    delete this[':' + key];
    removeFromArray(this.keys, key, true);
  }
}

// Tests if an entry is in the set.
Set.prototype.contains = function(entry) {
  return typeof this[':' + entry] != 'undefined';
}

// Gets a list of values in the set.
Set.prototype.items = function() {
  var list = [];
  for (var i = 0; i < this.keys.length; ++i) {
    var k = this.keys[i];
    var v = this[':' + k];
    list.push(v);
  }
  return list;
}


// Invokes function f for every key value pair in the set as a method
// of the set.
Set.prototype.map = function(f) {
  for (var i = 0; i < this.keys.length; ++i) {
    var k = this.keys[i];
    f.call(this, k, this[':' + k]);
  }
}

Set.prototype.clear = function() {
  for (var i = 0; i < this.keys.length; ++i) {
    delete this[':' + this.keys[i]];
  }
  this.keys.length = 0;
}


// Applies the given function to each element of the array, preserving
// this, and passing the index.
function mapExec(array, func) {
  for (var i = 0; i < array.length; ++i) {
    func.call(this, array[i], i);
  }
}

// Returns an array that contains the return value of the given
// function applied to every element of the input array.
function mapExpr(array, func) {
  var ret = [];
  for (var i = 0; i < array.length; ++i) {
    ret.push(func(array[i]));
  }
  return ret;
};

// Reverses the given array in place.
function reverseInplace(array) {
  for (var i = 0; i < array.length / 2; ++i) {
    var h = array[i];
    var ii = array.length - i - 1;
    array[i] = array[ii];
    array[ii] = h;
  }
}

// Removes value from array. Returns the number of instances of value
// that were removed from array.
function removeFromArray(array, value, opt_notype) {
  var shift = 0;
  for (var i = 0; i < array.length; ++i) {
    if (array[i] === value || (opt_notype && array[i] == value)) {
      array.splice(i--, 1);
      shift++;
    }
  }
  return shift;
}

// Shallow-copies an array.
function copyArray(dst, src) {
  for (var i = 0; i < src.length; ++i) {
    dst.push(src[i]);
  }
}

// Returns the text value of a node; for nodes without children this
// is the nodeValue, for nodes with children this is the concatenation
// of the value of all children.
function xmlValue(node) {
  if (!node) {
    return '';
  }

  var ret = '';
  if (node.nodeType == DOM_TEXT_NODE ||
      node.nodeType == DOM_CDATA_SECTION_NODE ||
      node.nodeType == DOM_ATTRIBUTE_NODE) {
    ret += node.nodeValue;

  } else if (node.nodeType == DOM_ELEMENT_NODE ||
             node.nodeType == DOM_DOCUMENT_NODE ||
             node.nodeType == DOM_DOCUMENT_FRAGMENT_NODE) {
    for (var i = 0; i < node.childNodes.length; ++i) {
      ret += arguments.callee(node.childNodes[i]);
    }
  }
  return ret;
}

// Returns the representation of a node as XML text.
function xmlText(node, opt_cdata) {
  var buf = [];
  xmlTextR(node, buf, opt_cdata);
  return buf.join('');
}

function xmlTextR(node, buf, cdata) {
  if (node.nodeType == DOM_TEXT_NODE) {
    buf.push(xmlEscapeText(node.nodeValue));

  } else if (node.nodeType == DOM_CDATA_SECTION_NODE) {
    if (cdata) {
      buf.push(node.nodeValue);
    } else {
      buf.push('<![CDATA[' + node.nodeValue + ']]>');
    }

  } else if (node.nodeType == DOM_COMMENT_NODE) {
    buf.push('<!--' + node.nodeValue + '-->');

  } else if (node.nodeType == DOM_ELEMENT_NODE) {
    buf.push('<' + xmlFullNodeName(node));
    for (var i = 0; i < node.attributes.length; ++i) {
      var a = node.attributes[i];
      if (a && a.nodeName && a.nodeValue) {
        buf.push(' ' + xmlFullNodeName(a) + '="' +
                 xmlEscapeAttr(a.nodeValue) + '"');
      }
    }

    if (node.childNodes.length == 0) {
      buf.push('/>');
    } else {
      buf.push('>');
      for (var i = 0; i < node.childNodes.length; ++i) {
        arguments.callee(node.childNodes[i], buf, cdata);
      }
      buf.push('</' + xmlFullNodeName(node) + '>');
    }

  } else if (node.nodeType == DOM_DOCUMENT_NODE ||
             node.nodeType == DOM_DOCUMENT_FRAGMENT_NODE) {
    for (var i = 0; i < node.childNodes.length; ++i) {
      arguments.callee(node.childNodes[i], buf, cdata);
    }
  }
}

function xmlFullNodeName(n) {
  if (n.prefix && n.nodeName.indexOf(n.prefix + ':') != 0) {
    return n.prefix + ':' + n.nodeName;
  } else {
    return n.nodeName;
  }
}

// Escape XML special markup chracters: tag delimiter < > and entity
// reference start delimiter &. The escaped string can be used in XML
// text portions (i.e. between tags).
function xmlEscapeText(s) {
  return ('' + s).replace(/&/g, '&amp;').replace(/</g, '&lt;').
    replace(/>/g, '&gt;');
}

// Escape XML special markup characters: tag delimiter < > entity
// reference start delimiter & and quotes ". The escaped string can be
// used in double quoted XML attribute value portions (i.e. in
// attributes within start tags).
function xmlEscapeAttr(s) {
  return xmlEscapeText(s).replace(/\"/g, '&quot;');
}

// Escape markup in XML text, but don't touch entity references. The
// escaped string can be used as XML text (i.e. between tags).
function xmlEscapeTags(s) {
  return s.replace(/</g, '&lt;').replace(/>/g, '&gt;');
}

/**
 * Wrapper function to access the owner document uniformly for document
 * and other nodes: for the document node, the owner document is the
 * node itself, for all others it's the ownerDocument property.
 *
 * @param {Node} node
 * @return {Document}
 */
function xmlOwnerDocument(node) {
  if (node.nodeType == DOM_DOCUMENT_NODE) {
    return node;
  } else {
    return node.ownerDocument;
  }
}

// Wrapper around DOM methods so we can condense their invocations.
function domGetAttribute(node, name) {
  return node.getAttribute(name);
}

function domSetAttribute(node, name, value) {
  return node.setAttribute(name, value);
}

function domRemoveAttribute(node, name) {
  return node.removeAttribute(name);
}

function domAppendChild(node, child) {
  return node.appendChild(child);
}

function domRemoveChild(node, child) {
  return node.removeChild(child);
}

function domReplaceChild(node, newChild, oldChild) {
  return node.replaceChild(newChild, oldChild);
}

function domInsertBefore(node, newChild, oldChild) {
  return node.insertBefore(newChild, oldChild);
}

function domRemoveNode(node) {
  return domRemoveChild(node.parentNode, node);
}

function domCreateTextNode(doc, text) {
  return doc.createTextNode(text);
}

function domCreateElement(doc, name) {
  return doc.createElement(name);
}

function domCreateAttribute(doc, name) {
  return doc.createAttribute(name);
}

function domCreateCDATASection(doc, data) {
  return doc.createCDATASection(data);
}

function domCreateComment(doc, text) {
  return doc.createComment(text);
}

function domCreateDocumentFragment(doc) {
  return doc.createDocumentFragment();
}

function domGetElementById(doc, id) {
  return doc.getElementById(id);
}

// Same for window methods.
function windowSetInterval(win, fun, time) {
  return win.setInterval(fun, time);
}

function windowClearInterval(win, id) {
  return win.clearInterval(id);
}


// Copyright 2006 Google Inc.
// All Rights Reserved
//
// Defines regular expression patterns to extract XML tokens from string.
// See <http://www.w3.org/TR/REC-xml/#sec-common-syn>,
// <http://www.w3.org/TR/xml11/#sec-common-syn> and
// <http://www.w3.org/TR/REC-xml-names/#NT-NCName> for the specifications.
//
// Author: Junji Takagi <jtakagi@google.com>

// Detect whether RegExp supports Unicode characters or not.

var REGEXP_UNICODE = function() {
  var tests = [' ', '\u0120', -1,  // Konquerer 3.4.0 fails here.
               '!', '\u0120', -1,
               '\u0120', '\u0120', 0,
               '\u0121', '\u0120', -1,
               '\u0121', '\u0120|\u0121', 0,
               '\u0122', '\u0120|\u0121', -1,
               '\u0120', '[\u0120]', 0,  // Safari 2.0.3 fails here.
               '\u0121', '[\u0120]', -1,
               '\u0121', '[\u0120\u0121]', 0,  // Safari 2.0.3 fails here.
               '\u0122', '[\u0120\u0121]', -1,
               '\u0121', '[\u0120-\u0121]', 0,  // Safari 2.0.3 fails here.
               '\u0122', '[\u0120-\u0121]', -1];
  for (var i = 0; i < tests.length; i += 3) {
    if (tests[i].search(new RegExp(tests[i + 1])) != tests[i + 2]) {
      return false;
    }
  }
  return true;
}();

// Common tokens in XML 1.0 and XML 1.1.

var XML_S = '[ \t\r\n]+';
var XML_EQ = '(' + XML_S + ')?=(' + XML_S + ')?';
var XML_CHAR_REF = '&#[0-9]+;|&#x[0-9a-fA-F]+;';

// XML 1.0 tokens.

var XML10_VERSION_INFO = XML_S + 'version' + XML_EQ + '("1\\.0"|' + "'1\\.0')";
var XML10_BASE_CHAR = (REGEXP_UNICODE) ?
  '\u0041-\u005a\u0061-\u007a\u00c0-\u00d6\u00d8-\u00f6\u00f8-\u00ff' +
  '\u0100-\u0131\u0134-\u013e\u0141-\u0148\u014a-\u017e\u0180-\u01c3' +
  '\u01cd-\u01f0\u01f4-\u01f5\u01fa-\u0217\u0250-\u02a8\u02bb-\u02c1\u0386' +
  '\u0388-\u038a\u038c\u038e-\u03a1\u03a3-\u03ce\u03d0-\u03d6\u03da\u03dc' +
  '\u03de\u03e0\u03e2-\u03f3\u0401-\u040c\u040e-\u044f\u0451-\u045c' +
  '\u045e-\u0481\u0490-\u04c4\u04c7-\u04c8\u04cb-\u04cc\u04d0-\u04eb' +
  '\u04ee-\u04f5\u04f8-\u04f9\u0531-\u0556\u0559\u0561-\u0586\u05d0-\u05ea' +
  '\u05f0-\u05f2\u0621-\u063a\u0641-\u064a\u0671-\u06b7\u06ba-\u06be' +
  '\u06c0-\u06ce\u06d0-\u06d3\u06d5\u06e5-\u06e6\u0905-\u0939\u093d' +
  '\u0958-\u0961\u0985-\u098c\u098f-\u0990\u0993-\u09a8\u09aa-\u09b0\u09b2' +
  '\u09b6-\u09b9\u09dc-\u09dd\u09df-\u09e1\u09f0-\u09f1\u0a05-\u0a0a' +
  '\u0a0f-\u0a10\u0a13-\u0a28\u0a2a-\u0a30\u0a32-\u0a33\u0a35-\u0a36' +
  '\u0a38-\u0a39\u0a59-\u0a5c\u0a5e\u0a72-\u0a74\u0a85-\u0a8b\u0a8d' +
  '\u0a8f-\u0a91\u0a93-\u0aa8\u0aaa-\u0ab0\u0ab2-\u0ab3\u0ab5-\u0ab9' +
  '\u0abd\u0ae0\u0b05-\u0b0c\u0b0f-\u0b10\u0b13-\u0b28\u0b2a-\u0b30' +
  '\u0b32-\u0b33\u0b36-\u0b39\u0b3d\u0b5c-\u0b5d\u0b5f-\u0b61\u0b85-\u0b8a' +
  '\u0b8e-\u0b90\u0b92-\u0b95\u0b99-\u0b9a\u0b9c\u0b9e-\u0b9f\u0ba3-\u0ba4' +
  '\u0ba8-\u0baa\u0bae-\u0bb5\u0bb7-\u0bb9\u0c05-\u0c0c\u0c0e-\u0c10' +
  '\u0c12-\u0c28\u0c2a-\u0c33\u0c35-\u0c39\u0c60-\u0c61\u0c85-\u0c8c' +
  '\u0c8e-\u0c90\u0c92-\u0ca8\u0caa-\u0cb3\u0cb5-\u0cb9\u0cde\u0ce0-\u0ce1' +
  '\u0d05-\u0d0c\u0d0e-\u0d10\u0d12-\u0d28\u0d2a-\u0d39\u0d60-\u0d61' +
  '\u0e01-\u0e2e\u0e30\u0e32-\u0e33\u0e40-\u0e45\u0e81-\u0e82\u0e84' +
  '\u0e87-\u0e88\u0e8a\u0e8d\u0e94-\u0e97\u0e99-\u0e9f\u0ea1-\u0ea3\u0ea5' +
  '\u0ea7\u0eaa-\u0eab\u0ead-\u0eae\u0eb0\u0eb2-\u0eb3\u0ebd\u0ec0-\u0ec4' +
  '\u0f40-\u0f47\u0f49-\u0f69\u10a0-\u10c5\u10d0-\u10f6\u1100\u1102-\u1103' +
  '\u1105-\u1107\u1109\u110b-\u110c\u110e-\u1112\u113c\u113e\u1140\u114c' +
  '\u114e\u1150\u1154-\u1155\u1159\u115f-\u1161\u1163\u1165\u1167\u1169' +
  '\u116d-\u116e\u1172-\u1173\u1175\u119e\u11a8\u11ab\u11ae-\u11af' +
  '\u11b7-\u11b8\u11ba\u11bc-\u11c2\u11eb\u11f0\u11f9\u1e00-\u1e9b' +
  '\u1ea0-\u1ef9\u1f00-\u1f15\u1f18-\u1f1d\u1f20-\u1f45\u1f48-\u1f4d' +
  '\u1f50-\u1f57\u1f59\u1f5b\u1f5d\u1f5f-\u1f7d\u1f80-\u1fb4\u1fb6-\u1fbc' +
  '\u1fbe\u1fc2-\u1fc4\u1fc6-\u1fcc\u1fd0-\u1fd3\u1fd6-\u1fdb\u1fe0-\u1fec' +
  '\u1ff2-\u1ff4\u1ff6-\u1ffc\u2126\u212a-\u212b\u212e\u2180-\u2182' +
  '\u3041-\u3094\u30a1-\u30fa\u3105-\u312c\uac00-\ud7a3' :
  'A-Za-z';
var XML10_IDEOGRAPHIC = (REGEXP_UNICODE) ?
  '\u4e00-\u9fa5\u3007\u3021-\u3029' :
  '';
var XML10_COMBINING_CHAR = (REGEXP_UNICODE) ?
  '\u0300-\u0345\u0360-\u0361\u0483-\u0486\u0591-\u05a1\u05a3-\u05b9' +
  '\u05bb-\u05bd\u05bf\u05c1-\u05c2\u05c4\u064b-\u0652\u0670\u06d6-\u06dc' +
  '\u06dd-\u06df\u06e0-\u06e4\u06e7-\u06e8\u06ea-\u06ed\u0901-\u0903\u093c' +
  '\u093e-\u094c\u094d\u0951-\u0954\u0962-\u0963\u0981-\u0983\u09bc\u09be' +
  '\u09bf\u09c0-\u09c4\u09c7-\u09c8\u09cb-\u09cd\u09d7\u09e2-\u09e3\u0a02' +
  '\u0a3c\u0a3e\u0a3f\u0a40-\u0a42\u0a47-\u0a48\u0a4b-\u0a4d\u0a70-\u0a71' +
  '\u0a81-\u0a83\u0abc\u0abe-\u0ac5\u0ac7-\u0ac9\u0acb-\u0acd\u0b01-\u0b03' +
  '\u0b3c\u0b3e-\u0b43\u0b47-\u0b48\u0b4b-\u0b4d\u0b56-\u0b57\u0b82-\u0b83' +
  '\u0bbe-\u0bc2\u0bc6-\u0bc8\u0bca-\u0bcd\u0bd7\u0c01-\u0c03\u0c3e-\u0c44' +
  '\u0c46-\u0c48\u0c4a-\u0c4d\u0c55-\u0c56\u0c82-\u0c83\u0cbe-\u0cc4' +
  '\u0cc6-\u0cc8\u0cca-\u0ccd\u0cd5-\u0cd6\u0d02-\u0d03\u0d3e-\u0d43' +
  '\u0d46-\u0d48\u0d4a-\u0d4d\u0d57\u0e31\u0e34-\u0e3a\u0e47-\u0e4e\u0eb1' +
  '\u0eb4-\u0eb9\u0ebb-\u0ebc\u0ec8-\u0ecd\u0f18-\u0f19\u0f35\u0f37\u0f39' +
  '\u0f3e\u0f3f\u0f71-\u0f84\u0f86-\u0f8b\u0f90-\u0f95\u0f97\u0f99-\u0fad' +
  '\u0fb1-\u0fb7\u0fb9\u20d0-\u20dc\u20e1\u302a-\u302f\u3099\u309a' :
  '';
var XML10_DIGIT = (REGEXP_UNICODE) ?
  '\u0030-\u0039\u0660-\u0669\u06f0-\u06f9\u0966-\u096f\u09e6-\u09ef' +
  '\u0a66-\u0a6f\u0ae6-\u0aef\u0b66-\u0b6f\u0be7-\u0bef\u0c66-\u0c6f' +
  '\u0ce6-\u0cef\u0d66-\u0d6f\u0e50-\u0e59\u0ed0-\u0ed9\u0f20-\u0f29' :
  '0-9';
var XML10_EXTENDER = (REGEXP_UNICODE) ?
  '\u00b7\u02d0\u02d1\u0387\u0640\u0e46\u0ec6\u3005\u3031-\u3035' +
  '\u309d-\u309e\u30fc-\u30fe' :
  '';
var XML10_LETTER = XML10_BASE_CHAR + XML10_IDEOGRAPHIC;
var XML10_NAME_CHAR = XML10_LETTER + XML10_DIGIT + '\\._:' +
                      XML10_COMBINING_CHAR + XML10_EXTENDER + '-';
var XML10_NAME = '[' + XML10_LETTER + '_:][' + XML10_NAME_CHAR + ']*';

var XML10_ENTITY_REF = '&' + XML10_NAME + ';';
var XML10_REFERENCE = XML10_ENTITY_REF + '|' + XML_CHAR_REF;
var XML10_ATT_VALUE = '"(([^<&"]|' + XML10_REFERENCE + ')*)"|' +
                      "'(([^<&']|" + XML10_REFERENCE + ")*)'";
var XML10_ATTRIBUTE =
  '(' + XML10_NAME + ')' + XML_EQ + '(' + XML10_ATT_VALUE + ')';

// XML 1.1 tokens.
// TODO(jtakagi): NameStartChar also includes \u10000-\ueffff.
// ECMAScript Language Specifiction defines UnicodeEscapeSequence as
// "\u HexDigit HexDigit HexDigit HexDigit" and we may need to use
// surrogate pairs, but any browser doesn't support surrogate paris in
// character classes of regular expression, so avoid including them for now.

var XML11_VERSION_INFO = XML_S + 'version' + XML_EQ + '("1\\.1"|' + "'1\\.1')";
var XML11_NAME_START_CHAR = (REGEXP_UNICODE) ?
  ':A-Z_a-z\u00c0-\u00d6\u00d8-\u00f6\u00f8-\u02ff\u0370-\u037d' +
  '\u037f-\u1fff\u200c-\u200d\u2070-\u218f\u2c00-\u2fef\u3001-\ud7ff' +
  '\uf900-\ufdcf\ufdf0-\ufffd' :
  ':A-Z_a-z';
var XML11_NAME_CHAR = XML11_NAME_START_CHAR +
  ((REGEXP_UNICODE) ? '\\.0-9\u00b7\u0300-\u036f\u203f-\u2040-' : '\\.0-9-');
var XML11_NAME = '[' + XML11_NAME_START_CHAR + '][' + XML11_NAME_CHAR + ']*';

var XML11_ENTITY_REF = '&' + XML11_NAME + ';';
var XML11_REFERENCE = XML11_ENTITY_REF + '|' + XML_CHAR_REF;
var XML11_ATT_VALUE = '"(([^<&"]|' + XML11_REFERENCE + ')*)"|' +
                      "'(([^<&']|" + XML11_REFERENCE + ")*)'";
var XML11_ATTRIBUTE =
  '(' + XML11_NAME + ')' + XML_EQ + '(' + XML11_ATT_VALUE + ')';

// XML Namespace tokens.
// Used in XML parser and XPath parser.

var XML_NC_NAME_CHAR = XML10_LETTER + XML10_DIGIT + '\\._' +
                       XML10_COMBINING_CHAR + XML10_EXTENDER + '-';
var XML_NC_NAME = '[' + XML10_LETTER + '_][' + XML_NC_NAME_CHAR + ']*';


// Copyright 2005 Google Inc.
// All Rights Reserved
//
// Author: Steffen Meschkat <mesch@google.com>
//
// An XML parse and a minimal DOM implementation that just supportes
// the subset of the W3C DOM that is used in the XSLT implementation.

// NOTE: The split() method in IE omits empty result strings. This is
// utterly annoying. So we don't use it here.

// Resolve entities in XML text fragments. According to the DOM
// specification, the DOM is supposed to resolve entity references at
// the API level. I.e. no entity references are passed through the
// API. See "Entities and the DOM core", p.12, DOM 2 Core
// Spec. However, different browsers actually pass very different
// values at the API. See <http://mesch.nyc/test-xml-quote>.
function xmlResolveEntities(s) {

  var parts = stringSplit(s, '&');

  var ret = parts[0];
  for (var i = 1; i < parts.length; ++i) {
    var rp = parts[i].indexOf(';');
    if (rp == -1) {
      // no entity reference: just a & but no ;
      ret += parts[i];
      continue;
    }

    var entityName = parts[i].substring(0, rp);
    var remainderText = parts[i].substring(rp + 1);

    var ch;
    switch (entityName) {
      case 'lt':
        ch = '<';
        break;
      case 'gt':
        ch = '>';
        break;
      case 'amp':
        ch = '&';
        break;
      case 'quot':
        ch = '"';
        break;
      case 'apos':
        ch = '\'';
        break;
      case 'nbsp':
        ch = String.fromCharCode(160);
        break;
      default:
        // Cool trick: let the DOM do the entity decoding. We assign
        // the entity text through non-W3C DOM properties and read it
        // through the W3C DOM. W3C DOM access is specified to resolve
        // entities.
        var span = domCreateElement(window.document, 'span');
        span.innerHTML = '&' + entityName + '; ';
        ch = span.childNodes[0].nodeValue.charAt(0);
    }
    ret += ch + remainderText;
  }

  return ret;
}

var XML10_TAGNAME_REGEXP = new RegExp('^(' + XML10_NAME + ')');
var XML10_ATTRIBUTE_REGEXP = new RegExp(XML10_ATTRIBUTE, 'g');

var XML11_TAGNAME_REGEXP = new RegExp('^(' + XML11_NAME + ')');
var XML11_ATTRIBUTE_REGEXP = new RegExp(XML11_ATTRIBUTE, 'g');

// Parses the given XML string with our custom, JavaScript XML parser. Written
// by Steffen Meschkat (mesch@google.com).
function xmlParse(xml) {
  var regex_empty = /\/$/;

  var regex_tagname;
  var regex_attribute;
  if (xml.match(/^<\?xml/)) {
    // When an XML document begins with an XML declaration
    // VersionInfo must appear.
    if (xml.search(new RegExp(XML10_VERSION_INFO)) == 5) {
      regex_tagname = XML10_TAGNAME_REGEXP;
      regex_attribute = XML10_ATTRIBUTE_REGEXP;
    } else if (xml.search(new RegExp(XML11_VERSION_INFO)) == 5) {
      regex_tagname = XML11_TAGNAME_REGEXP;
      regex_attribute = XML11_ATTRIBUTE_REGEXP;
    } else {
      // VersionInfo is missing, or unknown version number.
      // TODO : Fallback to XML 1.0 or XML 1.1, or just return null?
      alert('VersionInfo is missing, or unknown version number.');
    }
  } else {
    // When an XML declaration is missing it's an XML 1.0 document.
    regex_tagname = XML10_TAGNAME_REGEXP;
    regex_attribute = XML10_ATTRIBUTE_REGEXP;
  }

  var xmldoc = new XDocument();
  var root = xmldoc;

  // For the record: in Safari, we would create native DOM nodes, but
  // in Opera that is not possible, because the DOM only allows HTML
  // element nodes to be created, so we have to do our own DOM nodes.

  // xmldoc = document.implementation.createDocument('','',null);
  // root = xmldoc; // .createDocumentFragment();
  // NOTE(mesch): using the DocumentFragment instead of the Document
  // crashes my Safari 1.2.4 (v125.12).
  var stack = [];

  var parent = root;
  stack.push(parent);

  // The token that delimits a section that contains markup as
  // content: CDATA or comments.
  var slurp = '';

  var x = stringSplit(xml, '<');
  for (var i = 1; i < x.length; ++i) {
    var xx = stringSplit(x[i], '>');
    var tag = xx[0];
    var text = xmlResolveEntities(xx[1] || '');

    if (slurp) {
      // In a "slurp" section (CDATA or comment): only check for the
      // end of the section, otherwise append the whole text.
      var end = x[i].indexOf(slurp);
      if (end != -1) {
        var data = x[i].substring(0, end);
        parent.nodeValue += '<' + data;
        stack.pop();
        parent = stack[stack.length-1];
        text = x[i].substring(end + slurp.length);
        slurp = '';
      } else {
        parent.nodeValue += '<' + x[i];
        text = null;
      }

    } else if (tag.indexOf('![CDATA[') == 0) {
      var start = '![CDATA['.length;
      var end = x[i].indexOf(']]>');
      if (end != -1) {
        var data = x[i].substring(start, end);
        var node = domCreateCDATASection(xmldoc, data);
        domAppendChild(parent, node);
      } else {
        var data = x[i].substring(start);
        text = null;
        var node = domCreateCDATASection(xmldoc, data);
        domAppendChild(parent, node);
        parent = node;
        stack.push(node);
        slurp = ']]>';
      }

    } else if (tag.indexOf('!--') == 0) {
      var start = '!--'.length;
      var end = x[i].indexOf('-->');
      if (end != -1) {
        var data = x[i].substring(start, end);
        var node = domCreateComment(xmldoc, data);
        domAppendChild(parent, node);
      } else {
        var data = x[i].substring(start);
        text = null;
        var node = domCreateComment(xmldoc, data);
        domAppendChild(parent, node);
        parent = node;
        stack.push(node);
        slurp = '-->';
      }

    } else if (tag.charAt(0) == '/') {
      stack.pop();
      parent = stack[stack.length-1];

    } else if (tag.charAt(0) == '?') {
      // Ignore XML declaration and processing instructions
    } else if (tag.charAt(0) == '!') {
      // Ignore notation and comments
    } else {
      var empty = tag.match(regex_empty);
      var tagname = regex_tagname.exec(tag)[1];
      var node = domCreateElement(xmldoc, tagname);

      var att;
      while (att = regex_attribute.exec(tag)) {
        var val = xmlResolveEntities(att[5] || att[7] || '');
        domSetAttribute(node, att[1], val);
      }

      domAppendChild(parent, node);
      if (!empty) {
        parent = node;
        stack.push(node);
      }
    }

    if (text && parent != root) {
      domAppendChild(parent, domCreateTextNode(xmldoc, text));
    }
  }

  return root;
}

// Based on <http://www.w3.org/TR/2000/REC-DOM-Level-2-Core-20001113/
// core.html#ID-1950641247>
var DOM_ELEMENT_NODE = 1;
var DOM_ATTRIBUTE_NODE = 2;
var DOM_TEXT_NODE = 3;
var DOM_CDATA_SECTION_NODE = 4;
var DOM_ENTITY_REFERENCE_NODE = 5;
var DOM_ENTITY_NODE = 6;
var DOM_PROCESSING_INSTRUCTION_NODE = 7;
var DOM_COMMENT_NODE = 8;
var DOM_DOCUMENT_NODE = 9;
var DOM_DOCUMENT_TYPE_NODE = 10;
var DOM_DOCUMENT_FRAGMENT_NODE = 11;
var DOM_NOTATION_NODE = 12;

// Traverses the element nodes in the DOM section underneath the given
// node and invokes the given callbacks as methods on every element
// node encountered. Function opt_pre is invoked before a node's
// children are traversed; opt_post is invoked after they are
// traversed. Traversal will not be continued if a callback function
// returns boolean false. NOTE(mesch): copied from
// <//google3/maps/webmaps/javascript/dom.js>.
function domTraverseElements(node, opt_pre, opt_post) {
  var ret;
  if (opt_pre) {
    ret = opt_pre.call(null, node);
    if (typeof ret == 'boolean' && !ret) {
      return false;
    }
  }

  for (var c = node.firstChild; c; c = c.nextSibling) {
    if (c.nodeType == DOM_ELEMENT_NODE) {
      ret = arguments.callee.call(this, c, opt_pre, opt_post);
      if (typeof ret == 'boolean' && !ret) {
        return false;
      }
    }
  }

  if (opt_post) {
    ret = opt_post.call(null, node);
    if (typeof ret == 'boolean' && !ret) {
      return false;
    }
  }
}

// Our W3C DOM Node implementation. Note we call it XNode because we
// can't define the identifier Node. We do this mostly for Opera,
// where we can't reuse the HTML DOM for parsing our own XML, and for
// Safari, where it is too expensive to have the template processor
// operate on native DOM nodes.
function XNode(type, name, opt_value, opt_owner) {
  this.attributes = [];
  this.childNodes = [];

  XNode.init.call(this, type, name, opt_value, opt_owner);
}

// Don't call as method, use apply() or call().
XNode.init = function(type, name, value, owner) {
  this.nodeType = type - 0;
  this.nodeName = '' + name;
  this.nodeValue = '' + value;
  this.ownerDocument = owner;

  this.firstChild = null;
  this.lastChild = null;
  this.nextSibling = null;
  this.previousSibling = null;
  this.parentNode = null;
}

XNode.unused_ = [];

XNode.recycle = function(node) {
  if (!node) {
    return;
  }

  if (node.constructor == XDocument) {
    XNode.recycle(node.documentElement);
    return;
  }

  if (node.constructor != this) {
    return;
  }

  XNode.unused_.push(node);
  for (var a = 0; a < node.attributes.length; ++a) {
    XNode.recycle(node.attributes[a]);
  }
  for (var c = 0; c < node.childNodes.length; ++c) {
    XNode.recycle(node.childNodes[c]);
  }
  node.attributes.length = 0;
  node.childNodes.length = 0;
  XNode.init.call(node, 0, '', '', null);
}

XNode.create = function(type, name, value, owner) {
  if (XNode.unused_.length > 0) {
    var node = XNode.unused_.pop();
    XNode.init.call(node, type, name, value, owner);
    return node;
  } else {
    return new XNode(type, name, value, owner);
  }
}

XNode.prototype.appendChild = function(node) {
  // firstChild
  if (this.childNodes.length == 0) {
    this.firstChild = node;
  }

  // previousSibling
  node.previousSibling = this.lastChild;

  // nextSibling
  node.nextSibling = null;
  if (this.lastChild) {
    this.lastChild.nextSibling = node;
  }

  // parentNode
  node.parentNode = this;

  // lastChild
  this.lastChild = node;

  // childNodes
  this.childNodes.push(node);
}


XNode.prototype.replaceChild = function(newNode, oldNode) {
  if (oldNode == newNode) {
    return;
  }

  for (var i = 0; i < this.childNodes.length; ++i) {
    if (this.childNodes[i] == oldNode) {
      this.childNodes[i] = newNode;

      var p = oldNode.parentNode;
      oldNode.parentNode = null;
      newNode.parentNode = p;

      p = oldNode.previousSibling;
      oldNode.previousSibling = null;
      newNode.previousSibling = p;
      if (newNode.previousSibling) {
        newNode.previousSibling.nextSibling = newNode;
      }

      p = oldNode.nextSibling;
      oldNode.nextSibling = null;
      newNode.nextSibling = p;
      if (newNode.nextSibling) {
        newNode.nextSibling.previousSibling = newNode;
      }

      if (this.firstChild == oldNode) {
        this.firstChild = newNode;
      }

      if (this.lastChild == oldNode) {
        this.lastChild = newNode;
      }

      break;
    }
  }
}


XNode.prototype.insertBefore = function(newNode, oldNode) {
  if (oldNode == newNode) {
    return;
  }

  if (oldNode.parentNode != this) {
    return;
  }

  if (newNode.parentNode) {
    newNode.parentNode.removeChild(newNode);
  }

  var newChildren = [];
  for (var i = 0; i < this.childNodes.length; ++i) {
    var c = this.childNodes[i];
    if (c == oldNode) {
      newChildren.push(newNode);

      newNode.parentNode = this;

      newNode.previousSibling = oldNode.previousSibling;
      oldNode.previousSibling = newNode;
      if (newNode.previousSibling) {
        newNode.previousSibling.nextSibling = newNode;
      }

      newNode.nextSibling = oldNode;

      if (this.firstChild == oldNode) {
        this.firstChild = newNode;
      }
    }
    newChildren.push(c);
  }
  this.childNodes = newChildren;
}


XNode.prototype.removeChild = function(node) {
  var newChildren = [];
  for (var i = 0; i < this.childNodes.length; ++i) {
    var c = this.childNodes[i];
    if (c != node) {
      newChildren.push(c);
    } else {
      if (c.previousSibling) {
        c.previousSibling.nextSibling = c.nextSibling;
      }
      if (c.nextSibling) {
        c.nextSibling.previousSibling = c.previousSibling;
      }
      if (this.firstChild == c) {
        this.firstChild = c.nextSibling;
      }
      if (this.lastChild == c) {
        this.lastChild = c.previousSibling;
      }
    }
  }
  this.childNodes = newChildren;
}


XNode.prototype.hasAttributes = function() {
  return this.attributes.length > 0;
}


XNode.prototype.setAttribute = function(name, value) {
  for (var i = 0; i < this.attributes.length; ++i) {
    if (this.attributes[i].nodeName == name) {
      this.attributes[i].nodeValue = '' + value;
      return;
    }
  }
  this.attributes.push(XNode.create(DOM_ATTRIBUTE_NODE, name, value, this));
}


XNode.prototype.getAttribute = function(name) {
  for (var i = 0; i < this.attributes.length; ++i) {
    if (this.attributes[i].nodeName == name) {
      return this.attributes[i].nodeValue;
    }
  }
  return null;
}


XNode.prototype.removeAttribute = function(name) {
  var a = [];
  for (var i = 0; i < this.attributes.length; ++i) {
    if (this.attributes[i].nodeName != name) {
      a.push(this.attributes[i]);
    }
  }
  this.attributes = a;
}


XNode.prototype.getElementsByTagName = function(name) {
  var ret = [];
  domTraverseElements(this, function(node) {
    if (node.nodeName == name) {
      ret.push(node);
    }
  }, null);
  return ret;
}


XNode.prototype.getElementById = function(id) {
  var ret = null;
  domTraverseElements(this, function(node) {
    if (node.getAttribute('id') == id) {
      ret = node;
      return false;
    }
  }, null);
  return ret;
}


function XDocument() {
  // NOTE(mesch): Acocording to the DOM Spec, ownerDocument of a
  // document node is null.
  XNode.call(this, DOM_DOCUMENT_NODE, '#document', null, null);
  this.documentElement = null;
}

XDocument.prototype = new XNode(DOM_DOCUMENT_NODE, '#document');

XDocument.prototype.clear = function() {
  XNode.recycle(this.documentElement);
  this.documentElement = null;
}

XDocument.prototype.appendChild = function(node) {
  XNode.prototype.appendChild.call(this, node);
  this.documentElement = this.childNodes[0];
}

XDocument.prototype.createElement = function(name) {
  return XNode.create(DOM_ELEMENT_NODE, name, null, this);
}

XDocument.prototype.createDocumentFragment = function() {
  return XNode.create(DOM_DOCUMENT_FRAGMENT_NODE, '#document-fragment',
                    null, this);
}

XDocument.prototype.createTextNode = function(value) {
  return XNode.create(DOM_TEXT_NODE, '#text', value, this);
}

XDocument.prototype.createAttribute = function(name) {
  return XNode.create(DOM_ATTRIBUTE_NODE, name, null, this);
}

XDocument.prototype.createComment = function(data) {
  return XNode.create(DOM_COMMENT_NODE, '#comment', data, this);
}

XDocument.prototype.createCDATASection = function(data) {
  return XNode.create(DOM_CDATA_SECTION_NODE, '#cdata-section', data, this);
}


// Copyright 2005 Google Inc.
// All Rights Reserved
//
// An XPath parser and evaluator written in JavaScript. The
// implementation is complete except for functions handling
// namespaces.
//
// Reference: [XPATH] XPath Specification
// <http://www.w3.org/TR/1999/REC-xpath-19991116>.
//
//
// The API of the parser has several parts:
//
// 1. The parser function xpathParse() that takes a string and returns
// an expession object.
//
// 2. The expression object that has an evaluate() method to evaluate the
// XPath expression it represents. (It is actually a hierarchy of
// objects that resembles the parse tree, but an application will call
// evaluate() only on the top node of this hierarchy.)
//
// 3. The context object that is passed as an argument to the evaluate()
// method, which represents the DOM context in which the expression is
// evaluated.
//
// 4. The value object that is returned from evaluate() and represents
// values of the different types that are defined by XPath (number,
// string, boolean, and node-set), and allows to convert between them.
//
// These parts are near the top of the file, the functions and data
// that are used internally follow after them.
//
//
// Author: Steffen Meschkat <mesch@google.com>


// The entry point for the parser.
//
// @param expr a string that contains an XPath expression.
// @return an expression object that can be evaluated with an
// expression context.

function xpathParse(expr) {
  xpathLog('parse ' + expr);
  xpathParseInit();

  var cached = xpathCacheLookup(expr);
  if (cached) {
    xpathLog(' ... cached');
    return cached;
  }

  // Optimize for a few common cases: simple attribute node tests
  // (@id), simple element node tests (page), variable references
  // ($address), numbers (4), multi-step path expressions where each
  // step is a plain element node test
  // (page/overlay/locations/location).

  if (expr.match(/^(\$|@)?\w+$/i)) {
    var ret = makeSimpleExpr(expr);
    xpathParseCache[expr] = ret;
    xpathLog(' ... simple');
    return ret;
  }

  if (expr.match(/^\w+(\/\w+)*$/i)) {
    var ret = makeSimpleExpr2(expr);
    xpathParseCache[expr] = ret;
    xpathLog(' ... simple 2');
    return ret;
  }

  var cachekey = expr; // expr is modified during parse

  var stack = [];
  var ahead = null;
  var previous = null;
  var done = false;

  var parse_count = 0;
  var lexer_count = 0;
  var reduce_count = 0;

  while (!done) {
    parse_count++;
    expr = expr.replace(/^\s*/, '');
    previous = ahead;
    ahead = null;

    var rule = null;
    var match = '';
    for (var i = 0; i < xpathTokenRules.length; ++i) {
      var result = xpathTokenRules[i].re.exec(expr);
      lexer_count++;
      if (result && result.length > 0 && result[0].length > match.length) {
        rule = xpathTokenRules[i];
        match = result[0];
        break;
      }
    }

    // Special case: allow operator keywords to be element and
    // variable names.

    // NOTE(mesch): The parser resolves conflicts by looking ahead,
    // and this is the only case where we look back to
    // disambiguate. So this is indeed something different, and
    // looking back is usually done in the lexer (via states in the
    // general case, called "start conditions" in flex(1)). Also,the
    // conflict resolution in the parser is not as robust as it could
    // be, so I'd like to keep as much off the parser as possible (all
    // these precedence values should be computed from the grammar
    // rules and possibly associativity declarations, as in bison(1),
    // and not explicitly set.

    if (rule &&
        (rule == TOK_DIV ||
         rule == TOK_MOD ||
         rule == TOK_AND ||
         rule == TOK_OR) &&
        (!previous ||
         previous.tag == TOK_AT ||
         previous.tag == TOK_DSLASH ||
         previous.tag == TOK_SLASH ||
         previous.tag == TOK_AXIS ||
         previous.tag == TOK_DOLLAR)) {
      rule = TOK_QNAME;
    }

    if (rule) {
      expr = expr.substr(match.length);
      xpathLog('token: ' + match + ' -- ' + rule.label);
      ahead = {
        tag: rule,
        match: match,
        prec: rule.prec ?  rule.prec : 0, // || 0 is removed by the compiler
        expr: makeTokenExpr(match)
      };

    } else {
      xpathLog('DONE');
      done = true;
    }

    while (xpathReduce(stack, ahead)) {
      reduce_count++;
      xpathLog('stack: ' + stackToString(stack));
    }
  }

  xpathLog('stack: ' + stackToString(stack));

  if (stack.length != 1) {
    throw 'XPath parse error ' + cachekey + ':\n' + stackToString(stack);
  }

  var result = stack[0].expr;
  xpathParseCache[cachekey] = result;

  xpathLog('XPath parse: ' + parse_count + ' / ' +
           lexer_count + ' / ' + reduce_count);

  return result;
}

var xpathParseCache = {};

function xpathCacheLookup(expr) {
  return xpathParseCache[expr];
}

function xpathReduce(stack, ahead) {
  var cand = null;

  if (stack.length > 0) {
    var top = stack[stack.length-1];
    var ruleset = xpathRules[top.tag.key];

    if (ruleset) {
      for (var i = 0; i < ruleset.length; ++i) {
        var rule = ruleset[i];
        var match = xpathMatchStack(stack, rule[1]);
        if (match.length) {
          cand = {
            tag: rule[0],
            rule: rule,
            match: match
          };
          cand.prec = xpathGrammarPrecedence(cand);
          break;
        }
      }
    }
  }

  var ret;
  if (cand && (!ahead || cand.prec > ahead.prec ||
               (ahead.tag.left && cand.prec >= ahead.prec))) {
    for (var i = 0; i < cand.match.matchlength; ++i) {
      stack.pop();
    }

    xpathLog('reduce ' + cand.tag.label + ' ' + cand.prec +
             ' ahead ' + (ahead ? ahead.tag.label + ' ' + ahead.prec +
                          (ahead.tag.left ? ' left' : '')
                          : ' none '));

    var matchexpr = mapExpr(cand.match, function(m) { return m.expr; });
    cand.expr = cand.rule[3].apply(null, matchexpr);

    stack.push(cand);
    ret = true;

  } else {
    if (ahead) {
      xpathLog('shift ' + ahead.tag.label + ' ' + ahead.prec +
               (ahead.tag.left ? ' left' : '') +
               ' over ' + (cand ? cand.tag.label + ' ' +
                           cand.prec : ' none'));
      stack.push(ahead);
    }
    ret = false;
  }
  return ret;
}

function xpathMatchStack(stack, pattern) {

  // NOTE(mesch): The stack matches for variable cardinality are
  // greedy but don't do backtracking. This would be an issue only
  // with rules of the form A* A, i.e. with an element with variable
  // cardinality followed by the same element. Since that doesn't
  // occur in the grammar at hand, all matches on the stack are
  // unambiguous.

  var S = stack.length;
  var P = pattern.length;
  var p, s;
  var match = [];
  match.matchlength = 0;
  var ds = 0;
  for (p = P - 1, s = S - 1; p >= 0 && s >= 0; --p, s -= ds) {
    ds = 0;
    var qmatch = [];
    if (pattern[p] == Q_MM) {
      p -= 1;
      match.push(qmatch);
      while (s - ds >= 0 && stack[s - ds].tag == pattern[p]) {
        qmatch.push(stack[s - ds]);
        ds += 1;
        match.matchlength += 1;
      }

    } else if (pattern[p] == Q_01) {
      p -= 1;
      match.push(qmatch);
      while (s - ds >= 0 && ds < 2 && stack[s - ds].tag == pattern[p]) {
        qmatch.push(stack[s - ds]);
        ds += 1;
        match.matchlength += 1;
      }

    } else if (pattern[p] == Q_1M) {
      p -= 1;
      match.push(qmatch);
      if (stack[s].tag == pattern[p]) {
        while (s - ds >= 0 && stack[s - ds].tag == pattern[p]) {
          qmatch.push(stack[s - ds]);
          ds += 1;
          match.matchlength += 1;
        }
      } else {
        return [];
      }

    } else if (stack[s].tag == pattern[p]) {
      match.push(stack[s]);
      ds += 1;
      match.matchlength += 1;

    } else {
      return [];
    }

    reverseInplace(qmatch);
    qmatch.expr = mapExpr(qmatch, function(m) { return m.expr; });
  }

  reverseInplace(match);

  if (p == -1) {
    return match;

  } else {
    return [];
  }
}

function xpathTokenPrecedence(tag) {
  return tag.prec || 2;
}

function xpathGrammarPrecedence(frame) {
  var ret = 0;

  if (frame.rule) { /* normal reduce */
    if (frame.rule.length >= 3 && frame.rule[2] >= 0) {
      ret = frame.rule[2];

    } else {
      for (var i = 0; i < frame.rule[1].length; ++i) {
        var p = xpathTokenPrecedence(frame.rule[1][i]);
        ret = Math.max(ret, p);
      }
    }
  } else if (frame.tag) { /* TOKEN match */
    ret = xpathTokenPrecedence(frame.tag);

  } else if (frame.length) { /* Q_ match */
    for (var j = 0; j < frame.length; ++j) {
      var p = xpathGrammarPrecedence(frame[j]);
      ret = Math.max(ret, p);
    }
  }

  return ret;
}

function stackToString(stack) {
  var ret = '';
  for (var i = 0; i < stack.length; ++i) {
    if (ret) {
      ret += '\n';
    }
    ret += stack[i].tag.label;
  }
  return ret;
}


// XPath expression evaluation context. An XPath context consists of a
// DOM node, a list of DOM nodes that contains this node, a number
// that represents the position of the single node in the list, and a
// current set of variable bindings. (See XPath spec.)
//
// The interface of the expression context:
//
//   Constructor -- gets the node, its position, the node set it
//   belongs to, and a parent context as arguments. The parent context
//   is used to implement scoping rules for variables: if a variable
//   is not found in the current context, it is looked for in the
//   parent context, recursively. Except for node, all arguments have
//   default values: default position is 0, default node set is the
//   set that contains only the node, and the default parent is null.
//
//     Notice that position starts at 0 at the outside interface;
//     inside XPath expressions this shows up as position()=1.
//
//   clone() -- creates a new context with the current context as
//   parent. If passed as argument to clone(), the new context has a
//   different node, position, or node set. What is not passed is
//   inherited from the cloned context.
//
//   setVariable(name, expr) -- binds given XPath expression to the
//   name.
//
//   getVariable(name) -- what the name says.
//
//   setNode(position) -- sets the context to the node at the given
//   position. Needed to implement scoping rules for variables in
//   XPath. (A variable is visible to all subsequent siblings, not
//   only to its children.)

function ExprContext(node, opt_position, opt_nodelist, opt_parent) {
  this.node = node;
  this.position = opt_position || 0;
  this.nodelist = opt_nodelist || [ node ];
  this.variables = {};
  this.parent = opt_parent || null;
  if (opt_parent) {
    this.root = opt_parent.root;
  } else if (this.node.nodeType == DOM_DOCUMENT_NODE) {
    // NOTE(mesch): DOM Spec stipulates that the ownerDocument of a
    // document is null. Our root, however is the document that we are
    // processing, so the initial context is created from its document
    // node, which case we must handle here explcitly.
    this.root = node;
  } else {
    this.root = node.ownerDocument;
  }
}

ExprContext.prototype.clone = function(opt_node, opt_position, opt_nodelist) {
  return new ExprContext(
      opt_node || this.node,
      typeof opt_position != 'undefined' ? opt_position : this.position,
      opt_nodelist || this.nodelist, this);
};

ExprContext.prototype.setVariable = function(name, value) {
  this.variables[name] = value;
};

ExprContext.prototype.getVariable = function(name) {
  if (typeof this.variables[name] != 'undefined') {
    return this.variables[name];

  } else if (this.parent) {
    return this.parent.getVariable(name);

  } else {
    return null;
  }
};

ExprContext.prototype.setNode = function(position) {
  this.node = this.nodelist[position];
  this.position = position;
};

ExprContext.prototype.contextSize = function() {
  return this.nodelist.length;
};


// XPath expression values. They are what XPath expressions evaluate
// to. Strangely, the different value types are not specified in the
// XPath syntax, but only in the semantics, so they don't show up as
// nonterminals in the grammar. Yet, some expressions are required to
// evaluate to particular types, and not every type can be coerced
// into every other type. Although the types of XPath values are
// similar to the types present in JavaScript, the type coercion rules
// are a bit peculiar, so we explicitly model XPath types instead of
// mapping them onto JavaScript types. (See XPath spec.)
//
// The four types are:
//
//   StringValue
//
//   NumberValue
//
//   BooleanValue
//
//   NodeSetValue
//
// The common interface of the value classes consists of methods that
// implement the XPath type coercion rules:
//
//   stringValue() -- returns the value as a JavaScript String,
//
//   numberValue() -- returns the value as a JavaScript Number,
//
//   booleanValue() -- returns the value as a JavaScript Boolean,
//
//   nodeSetValue() -- returns the value as a JavaScript Array of DOM
//   Node objects.
//

function StringValue(value) {
  this.value = value;
  this.type = 'string';
}

StringValue.prototype.stringValue = function() {
  return this.value;
}

StringValue.prototype.booleanValue = function() {
  return this.value.length > 0;
}

StringValue.prototype.numberValue = function() {
  return this.value - 0;
}

StringValue.prototype.nodeSetValue = function() {
  throw this;
}

function BooleanValue(value) {
  this.value = value;
  this.type = 'boolean';
}

BooleanValue.prototype.stringValue = function() {
  return '' + this.value;
}

BooleanValue.prototype.booleanValue = function() {
  return this.value;
}

BooleanValue.prototype.numberValue = function() {
  return this.value ? 1 : 0;
}

BooleanValue.prototype.nodeSetValue = function() {
  throw this;
}

function NumberValue(value) {
  this.value = value;
  this.type = 'number';
}

NumberValue.prototype.stringValue = function() {
  return '' + this.value;
}

NumberValue.prototype.booleanValue = function() {
  return !!this.value;
}

NumberValue.prototype.numberValue = function() {
  return this.value - 0;
}

NumberValue.prototype.nodeSetValue = function() {
  throw this;
}

function NodeSetValue(value) {
  this.value = value;
  this.type = 'node-set';
}

NodeSetValue.prototype.stringValue = function() {
  if (this.value.length == 0) {
    return '';
  } else {
    return xmlValue(this.value[0]);
  }
}

NodeSetValue.prototype.booleanValue = function() {
  return this.value.length > 0;
}

NodeSetValue.prototype.numberValue = function() {
  return this.stringValue() - 0;
}

NodeSetValue.prototype.nodeSetValue = function() {
  return this.value;
};

// XPath expressions. They are used as nodes in the parse tree and
// possess an evaluate() method to compute an XPath value given an XPath
// context. Expressions are returned from the parser. Teh set of
// expression classes closely mirrors the set of non terminal symbols
// in the grammar. Every non trivial nonterminal symbol has a
// corresponding expression class.
//
// The common expression interface consists of the following methods:
//
// evaluate(context) -- evaluates the expression, returns a value.
//
// toString() -- returns the XPath text representation of the
// expression (defined in xsltdebug.js).
//
// parseTree(indent) -- returns a parse tree representation of the
// expression (defined in xsltdebug.js).

function TokenExpr(m) {
  this.value = m;
}

TokenExpr.prototype.evaluate = function() {
  return new StringValue(this.value);
};

function LocationExpr() {
  this.absolute = false;
  this.steps = [];
}

LocationExpr.prototype.appendStep = function(s) {
  this.steps.push(s);
}

LocationExpr.prototype.prependStep = function(s) {
  var steps0 = this.steps;
  this.steps = [ s ];
  for (var i = 0; i < steps0.length; ++i) {
    this.steps.push(steps0[i]);
  }
};

LocationExpr.prototype.evaluate = function(ctx) {
  var start;
  if (this.absolute) {
    start = ctx.root;

  } else {
    start = ctx.node;
  }

  var nodes = [];
  xPathStep(nodes, this.steps, 0, start, ctx);
  return new NodeSetValue(nodes);
};

function xPathStep(nodes, steps, step, input, ctx) {
  var s = steps[step];
  var ctx2 = ctx.clone(input);
  var nodelist = s.evaluate(ctx2).nodeSetValue();

  for (var i = 0; i < nodelist.length; ++i) {
    if (step == steps.length - 1) {
      nodes.push(nodelist[i]);
    } else {
      xPathStep(nodes, steps, step + 1, nodelist[i], ctx);
    }
  }
}

function StepExpr(axis, nodetest, opt_predicate) {
  this.axis = axis;
  this.nodetest = nodetest;
  this.predicate = opt_predicate || [];
}

StepExpr.prototype.appendPredicate = function(p) {
  this.predicate.push(p);
}

StepExpr.prototype.evaluate = function(ctx) {
  var input = ctx.node;
  var nodelist = [];

  // NOTE(mesch): When this was a switch() statement, it didn't work
  // in Safari/2.0. Not sure why though; it resulted in the JavaScript
  // console output "undefined" (without any line number or so).

  if (this.axis ==  xpathAxis.ANCESTOR_OR_SELF) {
    nodelist.push(input);
    for (var n = input.parentNode; n; n = n.parentNode) {
      nodelist.push(n);
    }

  } else if (this.axis == xpathAxis.ANCESTOR) {
    for (var n = input.parentNode; n; n = n.parentNode) {
      nodelist.push(n);
    }

  } else if (this.axis == xpathAxis.ATTRIBUTE) {
    copyArray(nodelist, input.attributes);

  } else if (this.axis == xpathAxis.CHILD) {
    copyArray(nodelist, input.childNodes);

  } else if (this.axis == xpathAxis.DESCENDANT_OR_SELF) {
    nodelist.push(input);
    xpathCollectDescendants(nodelist, input);

  } else if (this.axis == xpathAxis.DESCENDANT) {
    xpathCollectDescendants(nodelist, input);

  } else if (this.axis == xpathAxis.FOLLOWING) {
    for (var n = input; n; n = n.parentNode) {
      for (var nn = n.nextSibling; nn; nn = nn.nextSibling) {
        nodelist.push(nn);
        xpathCollectDescendants(nodelist, nn);
      }
    }

  } else if (this.axis == xpathAxis.FOLLOWING_SIBLING) {
    for (var n = input.nextSibling; n; n = n.nextSibling) {
      nodelist.push(n);
    }

  } else if (this.axis == xpathAxis.NAMESPACE) {
    alert('not implemented: axis namespace');

  } else if (this.axis == xpathAxis.PARENT) {
    if (input.parentNode) {
      nodelist.push(input.parentNode);
    }

  } else if (this.axis == xpathAxis.PRECEDING) {
    for (var n = input; n; n = n.parentNode) {
      for (var nn = n.previousSibling; nn; nn = nn.previousSibling) {
        nodelist.push(nn);
        xpathCollectDescendantsReverse(nodelist, nn);
      }
    }

  } else if (this.axis == xpathAxis.PRECEDING_SIBLING) {
    for (var n = input.previousSibling; n; n = n.previousSibling) {
      nodelist.push(n);
    }

  } else if (this.axis == xpathAxis.SELF) {
    nodelist.push(input);

  } else {
    throw 'ERROR -- NO SUCH AXIS: ' + this.axis;
  }

  // process node test
  var nodelist0 = nodelist;
  nodelist = [];
  for (var i = 0; i < nodelist0.length; ++i) {
    var n = nodelist0[i];
    if (this.nodetest.evaluate(ctx.clone(n, i, nodelist0)).booleanValue()) {
      nodelist.push(n);
    }
  }

  // process predicates
  for (var i = 0; i < this.predicate.length; ++i) {
    var nodelist0 = nodelist;
    nodelist = [];
    for (var ii = 0; ii < nodelist0.length; ++ii) {
      var n = nodelist0[ii];
      if (this.predicate[i].evaluate(ctx.clone(n, ii, nodelist0)).booleanValue()) {
        nodelist.push(n);
      }
    }
  }

  return new NodeSetValue(nodelist);
};

function NodeTestAny() {
  this.value = new BooleanValue(true);
}

NodeTestAny.prototype.evaluate = function(ctx) {
  return this.value;
};

function NodeTestElementOrAttribute() {}

NodeTestElementOrAttribute.prototype.evaluate = function(ctx) {
  return new BooleanValue(
      ctx.node.nodeType == DOM_ELEMENT_NODE ||
      ctx.node.nodeType == DOM_ATTRIBUTE_NODE);
}

function NodeTestText() {}

NodeTestText.prototype.evaluate = function(ctx) {
  return new BooleanValue(ctx.node.nodeType == DOM_TEXT_NODE);
}

function NodeTestComment() {}

NodeTestComment.prototype.evaluate = function(ctx) {
  return new BooleanValue(ctx.node.nodeType == DOM_COMMENT_NODE);
}

function NodeTestPI(target) {
  this.target = target;
}

NodeTestPI.prototype.evaluate = function(ctx) {
  return new
  BooleanValue(ctx.node.nodeType == DOM_PROCESSING_INSTRUCTION_NODE &&
               (!this.target || ctx.node.nodeName == this.target));
}

function NodeTestNC(nsprefix) {
  this.regex = new RegExp("^" + nsprefix + ":");
  this.nsprefix = nsprefix;
}

NodeTestNC.prototype.evaluate = function(ctx) {
  var n = ctx.node;
  return new BooleanValue(this.regex.match(n.nodeName));
}

function NodeTestName(name) {
  this.name = name;
}

NodeTestName.prototype.evaluate = function(ctx) {
  var n = ctx.node;
  return new BooleanValue(n.nodeName == this.name);
}

function PredicateExpr(expr) {
  this.expr = expr;
}

PredicateExpr.prototype.evaluate = function(ctx) {
  var v = this.expr.evaluate(ctx);
  if (v.type == 'number') {
    // NOTE(mesch): Internally, position is represented starting with
    // 0, however in XPath position starts with 1. See functions
    // position() and last().
    return new BooleanValue(ctx.position == v.numberValue() - 1);
  } else {
    return new BooleanValue(v.booleanValue());
  }
};

function FunctionCallExpr(name) {
  this.name = name;
  this.args = [];
}

FunctionCallExpr.prototype.appendArg = function(arg) {
  this.args.push(arg);
};

FunctionCallExpr.prototype.evaluate = function(ctx) {
  var fn = '' + this.name.value;
  var f = this.xpathfunctions[fn];
  if (f) {
    return f.call(this, ctx);
  } else {
    xpathLog('XPath NO SUCH FUNCTION ' + fn);
    return new BooleanValue(false);
  }
};

FunctionCallExpr.prototype.xpathfunctions = {
  'last': function(ctx) {
    assert(this.args.length == 0);
    // NOTE(mesch): XPath position starts at 1.
    return new NumberValue(ctx.contextSize());
  },

  'position': function(ctx) {
    assert(this.args.length == 0);
    // NOTE(mesch): XPath position starts at 1.
    return new NumberValue(ctx.position + 1);
  },

  'count': function(ctx) {
    assert(this.args.length == 1);
    var v = this.args[0].evaluate(ctx);
    return new NumberValue(v.nodeSetValue().length);
  },

  'id': function(ctx) {
    assert(this.args.length == 1);
    var e = this.args[0].evaluate(ctx);
    var ret = [];
    var ids;
    if (e.type == 'node-set') {
      ids = [];
      var en = e.nodeSetValue();
      for (var i = 0; i < en.length; ++i) {
        var v = xmlValue(en[i]).split(/\s+/);
        for (var ii = 0; ii < v.length; ++ii) {
          ids.push(v[ii]);
        }
      }
    } else {
      ids = e.stringValue().split(/\s+/);
    }
    var d = ctx.node.ownerDocument;
    for (var i = 0; i < ids.length; ++i) {
      var n = d.getElementById(ids[i]);
      if (n) {
        ret.push(n);
      }
    }
    return new NodeSetValue(ret);
  },

  'local-name': function(ctx) {
    alert('not implmented yet: XPath function local-name()');
  },

  'namespace-uri': function(ctx) {
    alert('not implmented yet: XPath function namespace-uri()');
  },

  'name': function(ctx) {
    assert(this.args.length == 1 || this.args.length == 0);
    var n;
    if (this.args.length == 0) {
      n = [ ctx.node ];
    } else {
      n = this.args[0].evaluate(ctx).nodeSetValue();
    }

    if (n.length == 0) {
      return new StringValue('');
    } else {
      return new StringValue(n[0].nodeName);
    }
  },

  'string':  function(ctx) {
    assert(this.args.length == 1 || this.args.length == 0);
    if (this.args.length == 0) {
      return new StringValue(new NodeSetValue([ ctx.node ]).stringValue());
    } else {
      return new StringValue(this.args[0].evaluate(ctx).stringValue());
    }
  },

  'concat': function(ctx) {
    var ret = '';
    for (var i = 0; i < this.args.length; ++i) {
      ret += this.args[i].evaluate(ctx).stringValue();
    }
    return new StringValue(ret);
  },

  'starts-with': function(ctx) {
    assert(this.args.length == 2);
    var s0 = this.args[0].evaluate(ctx).stringValue();
    var s1 = this.args[1].evaluate(ctx).stringValue();
    return new BooleanValue(s0.indexOf(s1) == 0);
  },

  'contains': function(ctx) {
    assert(this.args.length == 2);
    var s0 = this.args[0].evaluate(ctx).stringValue();
    var s1 = this.args[1].evaluate(ctx).stringValue();
    return new BooleanValue(s0.indexOf(s1) != -1);
  },

  'substring-before': function(ctx) {
    assert(this.args.length == 2);
    var s0 = this.args[0].evaluate(ctx).stringValue();
    var s1 = this.args[1].evaluate(ctx).stringValue();
    var i = s0.indexOf(s1);
    var ret;
    if (i == -1) {
      ret = '';
    } else {
      ret = s0.substr(0,i);
    }
    return new StringValue(ret);
  },

  'substring-after': function(ctx) {
    assert(this.args.length == 2);
    var s0 = this.args[0].evaluate(ctx).stringValue();
    var s1 = this.args[1].evaluate(ctx).stringValue();
    var i = s0.indexOf(s1);
    var ret;
    if (i == -1) {
      ret = '';
    } else {
      ret = s0.substr(i + s1.length);
    }
    return new StringValue(ret);
  },

  'substring': function(ctx) {
    // NOTE: XPath defines the position of the first character in a
    // string to be 1, in JavaScript this is 0 ([XPATH] Section 4.2).
    assert(this.args.length == 2 || this.args.length == 3);
    var s0 = this.args[0].evaluate(ctx).stringValue();
    var s1 = this.args[1].evaluate(ctx).numberValue();
    var ret;
    if (this.args.length == 2) {
      var i1 = Math.max(0, Math.round(s1) - 1);
      ret = s0.substr(i1);

    } else {
      var s2 = this.args[2].evaluate(ctx).numberValue();
      var i0 = Math.round(s1) - 1;
      var i1 = Math.max(0, i0);
      var i2 = Math.round(s2) - Math.max(0, -i0);
      ret = s0.substr(i1, i2);
    }
    return new StringValue(ret);
  },

  'string-length': function(ctx) {
    var s;
    if (this.args.length > 0) {
      s = this.args[0].evaluate(ctx).stringValue();
    } else {
      s = new NodeSetValue([ ctx.node ]).stringValue();
    }
    return new NumberValue(s.length);
  },

  'normalize-space': function(ctx) {
    var s;
    if (this.args.length > 0) {
      s = this.args[0].evaluate(ctx).stringValue();
    } else {
      s = new NodeSetValue([ ctx.node ]).stringValue();
    }
    s = s.replace(/^\s*/,'').replace(/\s*$/,'').replace(/\s+/g, ' ');
    return new StringValue(s);
  },

  'translate': function(ctx) {
    assert(this.args.length == 3);
    var s0 = this.args[0].evaluate(ctx).stringValue();
    var s1 = this.args[1].evaluate(ctx).stringValue();
    var s2 = this.args[2].evaluate(ctx).stringValue();

    for (var i = 0; i < s1.length; ++i) {
      s0 = s0.replace(new RegExp(s1.charAt(i), 'g'), s2.charAt(i));
    }
    return new StringValue(s0);
  },

  'boolean': function(ctx) {
    assert(this.args.length == 1);
    return new BooleanValue(this.args[0].evaluate(ctx).booleanValue());
  },

  'not': function(ctx) {
    assert(this.args.length == 1);
    var ret = !this.args[0].evaluate(ctx).booleanValue();
    return new BooleanValue(ret);
  },

  'true': function(ctx) {
    assert(this.args.length == 0);
    return new BooleanValue(true);
  },

  'false': function(ctx) {
    assert(this.args.length == 0);
    return new BooleanValue(false);
  },

  'lang': function(ctx) {
    assert(this.args.length == 1);
    var lang = this.args[0].evaluate(ctx).stringValue();
    var xmllang;
    var n = ctx.node;
    while (n && n != n.parentNode /* just in case ... */) {
      xmllang = n.getAttribute('xml:lang');
      if (xmllang) {
        break;
      }
      n = n.parentNode;
    }
    if (!xmllang) {
      return new BooleanValue(false);
    } else {
      var re = new RegExp('^' + lang + '$', 'i');
      return new BooleanValue(xmllang.match(re) ||
                              xmllang.replace(/_.*$/,'').match(re));
    }
  },

  'number': function(ctx) {
    assert(this.args.length == 1 || this.args.length == 0);

    if (this.args.length == 1) {
      return new NumberValue(this.args[0].evaluate(ctx).numberValue());
    } else {
      return new NumberValue(new NodeSetValue([ ctx.node ]).numberValue());
    }
  },

  'sum': function(ctx) {
    assert(this.args.length == 1);
    var n = this.args[0].evaluate(ctx).nodeSetValue();
    var sum = 0;
    for (var i = 0; i < n.length; ++i) {
      sum += xmlValue(n[i]) - 0;
    }
    return new NumberValue(sum);
  },

  'floor': function(ctx) {
    assert(this.args.length == 1);
    var num = this.args[0].evaluate(ctx).numberValue();
    return new NumberValue(Math.floor(num));
  },

  'ceiling': function(ctx) {
    assert(this.args.length == 1);
    var num = this.args[0].evaluate(ctx).numberValue();
    return new NumberValue(Math.ceil(num));
  },

  'round': function(ctx) {
    assert(this.args.length == 1);
    var num = this.args[0].evaluate(ctx).numberValue();
    return new NumberValue(Math.round(num));
  },

  // TODO(mesch): The following functions are custom. There is a
  // standard that defines how to add functions, which should be
  // applied here.

  'ext-join': function(ctx) {
    assert(this.args.length == 2);
    var nodes = this.args[0].evaluate(ctx).nodeSetValue();
    var delim = this.args[1].evaluate(ctx).stringValue();
    var ret = '';
    for (var i = 0; i < nodes.length; ++i) {
      if (ret) {
        ret += delim;
      }
      ret += xmlValue(nodes[i]);
    }
    return new StringValue(ret);
  },

  // ext-if() evaluates and returns its second argument, if the
  // boolean value of its first argument is true, otherwise it
  // evaluates and returns its third argument.

  'ext-if': function(ctx) {
    assert(this.args.length == 3);
    if (this.args[0].evaluate(ctx).booleanValue()) {
      return this.args[1].evaluate(ctx);
    } else {
      return this.args[2].evaluate(ctx);
    }
  },

  // ext-cardinal() evaluates its single argument as a number, and
  // returns the current node that many times. It can be used in the
  // select attribute to iterate over an integer range.

  'ext-cardinal': function(ctx) {
    assert(this.args.length >= 1);
    var c = this.args[0].evaluate(ctx).numberValue();
    var ret = [];
    for (var i = 0; i < c; ++i) {
      ret.push(ctx.node);
    }
    return new NodeSetValue(ret);
  }
};

function UnionExpr(expr1, expr2) {
  this.expr1 = expr1;
  this.expr2 = expr2;
}

UnionExpr.prototype.evaluate = function(ctx) {
  var nodes1 = this.expr1.evaluate(ctx).nodeSetValue();
  var nodes2 = this.expr2.evaluate(ctx).nodeSetValue();
  var I1 = nodes1.length;
  for (var i2 = 0; i2 < nodes2.length; ++i2) {
    var n = nodes2[i2];
    var inBoth = false;
    for (var i1 = 0; i1 < I1; ++i1) {
      if (nodes1[i1] == n) {
        inBoth = true;
        i1 = I1; // break inner loop
      }
    }
    if (!inBoth) {
      nodes1.push(n);
    }
  }
  return new NodeSetValue(nodes1);
};

function PathExpr(filter, rel) {
  this.filter = filter;
  this.rel = rel;
}

PathExpr.prototype.evaluate = function(ctx) {
  var nodes = this.filter.evaluate(ctx).nodeSetValue();
  var nodes1 = [];
  for (var i = 0; i < nodes.length; ++i) {
    var nodes0 = this.rel.evaluate(ctx.clone(nodes[i], i, nodes)).nodeSetValue();
    for (var ii = 0; ii < nodes0.length; ++ii) {
      nodes1.push(nodes0[ii]);
    }
  }
  return new NodeSetValue(nodes1);
};

function FilterExpr(expr, predicate) {
  this.expr = expr;
  this.predicate = predicate;
}

FilterExpr.prototype.evaluate = function(ctx) {
  var nodes = this.expr.evaluate(ctx).nodeSetValue();
  for (var i = 0; i < this.predicate.length; ++i) {
    var nodes0 = nodes;
    nodes = [];
    for (var j = 0; j < nodes0.length; ++j) {
      var n = nodes0[j];
      if (this.predicate[i].evaluate(ctx.clone(n, j, nodes0)).booleanValue()) {
        nodes.push(n);
      }
    }
  }

  return new NodeSetValue(nodes);
}

function UnaryMinusExpr(expr) {
  this.expr = expr;
}

UnaryMinusExpr.prototype.evaluate = function(ctx) {
  return new NumberValue(-this.expr.evaluate(ctx).numberValue());
};

function BinaryExpr(expr1, op, expr2) {
  this.expr1 = expr1;
  this.expr2 = expr2;
  this.op = op;
}

BinaryExpr.prototype.evaluate = function(ctx) {
  var ret;
  switch (this.op.value) {
    case 'or':
      ret = new BooleanValue(this.expr1.evaluate(ctx).booleanValue() ||
                             this.expr2.evaluate(ctx).booleanValue());
      break;

    case 'and':
      ret = new BooleanValue(this.expr1.evaluate(ctx).booleanValue() &&
                             this.expr2.evaluate(ctx).booleanValue());
      break;

    case '+':
      ret = new NumberValue(this.expr1.evaluate(ctx).numberValue() +
                            this.expr2.evaluate(ctx).numberValue());
      break;

    case '-':
      ret = new NumberValue(this.expr1.evaluate(ctx).numberValue() -
                            this.expr2.evaluate(ctx).numberValue());
      break;

    case '*':
      ret = new NumberValue(this.expr1.evaluate(ctx).numberValue() *
                            this.expr2.evaluate(ctx).numberValue());
      break;

    case 'mod':
      ret = new NumberValue(this.expr1.evaluate(ctx).numberValue() %
                            this.expr2.evaluate(ctx).numberValue());
      break;

    case 'div':
      ret = new NumberValue(this.expr1.evaluate(ctx).numberValue() /
                            this.expr2.evaluate(ctx).numberValue());
      break;

    case '=':
      ret = this.compare(ctx, function(x1, x2) { return x1 == x2; });
      break;

    case '!=':
      ret = this.compare(ctx, function(x1, x2) { return x1 != x2; });
      break;

    case '<':
      ret = this.compare(ctx, function(x1, x2) { return x1 < x2; });
      break;

    case '<=':
      ret = this.compare(ctx, function(x1, x2) { return x1 <= x2; });
      break;

    case '>':
      ret = this.compare(ctx, function(x1, x2) { return x1 > x2; });
      break;

    case '>=':
      ret = this.compare(ctx, function(x1, x2) { return x1 >= x2; });
      break;

    default:
      alert('BinaryExpr.evaluate: ' + this.op.value);
  }
  return ret;
};

BinaryExpr.prototype.compare = function(ctx, cmp) {
  var v1 = this.expr1.evaluate(ctx);
  var v2 = this.expr2.evaluate(ctx);

  var ret;
  if (v1.type == 'node-set' && v2.type == 'node-set') {
    var n1 = v1.nodeSetValue();
    var n2 = v2.nodeSetValue();
    ret = false;
    for (var i1 = 0; i1 < n1.length; ++i1) {
      for (var i2 = 0; i2 < n2.length; ++i2) {
        if (cmp(xmlValue(n1[i1]), xmlValue(n2[i2]))) {
          ret = true;
          // Break outer loop. Labels confuse the jscompiler and we
          // don't use them.
          i2 = n2.length;
          i1 = n1.length;
        }
      }
    }

  } else if (v1.type == 'node-set' || v2.type == 'node-set') {

    if (v1.type == 'number') {
      var s = v1.numberValue();
      var n = v2.nodeSetValue();

      ret = false;
      for (var i = 0;  i < n.length; ++i) {
        var nn = xmlValue(n[i]) - 0;
        if (cmp(s, nn)) {
          ret = true;
          break;
        }
      }

    } else if (v2.type == 'number') {
      var n = v1.nodeSetValue();
      var s = v2.numberValue();

      ret = false;
      for (var i = 0;  i < n.length; ++i) {
        var nn = xmlValue(n[i]) - 0;
        if (cmp(nn, s)) {
          ret = true;
          break;
        }
      }

    } else if (v1.type == 'string') {
      var s = v1.stringValue();
      var n = v2.nodeSetValue();

      ret = false;
      for (var i = 0;  i < n.length; ++i) {
        var nn = xmlValue(n[i]);
        if (cmp(s, nn)) {
          ret = true;
          break;
        }
      }

    } else if (v2.type == 'string') {
      var n = v1.nodeSetValue();
      var s = v2.stringValue();

      ret = false;
      for (var i = 0;  i < n.length; ++i) {
        var nn = xmlValue(n[i]);
        if (cmp(nn, s)) {
          ret = true;
          break;
        }
      }

    } else {
      ret = cmp(v1.booleanValue(), v2.booleanValue());
    }

  } else if (v1.type == 'boolean' || v2.type == 'boolean') {
    ret = cmp(v1.booleanValue(), v2.booleanValue());

  } else if (v1.type == 'number' || v2.type == 'number') {
    ret = cmp(v1.numberValue(), v2.numberValue());

  } else {
    ret = cmp(v1.stringValue(), v2.stringValue());
  }

  return new BooleanValue(ret);
}

function LiteralExpr(value) {
  this.value = value;
}

LiteralExpr.prototype.evaluate = function(ctx) {
  return new StringValue(this.value);
};

function NumberExpr(value) {
  this.value = value;
}

NumberExpr.prototype.evaluate = function(ctx) {
  return new NumberValue(this.value);
};

function VariableExpr(name) {
  this.name = name;
}

VariableExpr.prototype.evaluate = function(ctx) {
  return ctx.getVariable(this.name);
}

// Factory functions for semantic values (i.e. Expressions) of the
// productions in the grammar. When a production is matched to reduce
// the current parse state stack, the function is called with the
// semantic values of the matched elements as arguments, and returns
// another semantic value. The semantic value is a node of the parse
// tree, an expression object with an evaluate() method that evaluates the
// expression in an actual context. These factory functions are used
// in the specification of the grammar rules, below.

function makeTokenExpr(m) {
  return new TokenExpr(m);
}

function passExpr(e) {
  return e;
}

function makeLocationExpr1(slash, rel) {
  rel.absolute = true;
  return rel;
}

function makeLocationExpr2(dslash, rel) {
  rel.absolute = true;
  rel.prependStep(makeAbbrevStep(dslash.value));
  return rel;
}

function makeLocationExpr3(slash) {
  var ret = new LocationExpr();
  ret.appendStep(makeAbbrevStep('.'));
  ret.absolute = true;
  return ret;
}

function makeLocationExpr4(dslash) {
  var ret = new LocationExpr();
  ret.absolute = true;
  ret.appendStep(makeAbbrevStep(dslash.value));
  return ret;
}

function makeLocationExpr5(step) {
  var ret = new LocationExpr();
  ret.appendStep(step);
  return ret;
}

function makeLocationExpr6(rel, slash, step) {
  rel.appendStep(step);
  return rel;
}

function makeLocationExpr7(rel, dslash, step) {
  rel.appendStep(makeAbbrevStep(dslash.value));
  return rel;
}

function makeStepExpr1(dot) {
  return makeAbbrevStep(dot.value);
}

function makeStepExpr2(ddot) {
  return makeAbbrevStep(ddot.value);
}

function makeStepExpr3(axisname, axis, nodetest) {
  return new StepExpr(axisname.value, nodetest);
}

function makeStepExpr4(at, nodetest) {
  return new StepExpr('attribute', nodetest);
}

function makeStepExpr5(nodetest) {
  return new StepExpr('child', nodetest);
}

function makeStepExpr6(step, predicate) {
  step.appendPredicate(predicate);
  return step;
}

function makeAbbrevStep(abbrev) {
  switch (abbrev) {
  case '//':
    return new StepExpr('descendant-or-self', new NodeTestAny);

  case '.':
    return new StepExpr('self', new NodeTestAny);

  case '..':
    return new StepExpr('parent', new NodeTestAny);
  }
}

function makeNodeTestExpr1(asterisk) {
  return new NodeTestElementOrAttribute;
}

function makeNodeTestExpr2(ncname, colon, asterisk) {
  return new NodeTestNC(ncname.value);
}

function makeNodeTestExpr3(qname) {
  return new NodeTestName(qname.value);
}

function makeNodeTestExpr4(typeo, parenc) {
  var type = typeo.value.replace(/\s*\($/, '');
  switch(type) {
  case 'node':
    return new NodeTestAny;

  case 'text':
    return new NodeTestText;

  case 'comment':
    return new NodeTestComment;

  case 'processing-instruction':
    return new NodeTestPI('');
  }
}

function makeNodeTestExpr5(typeo, target, parenc) {
  var type = typeo.replace(/\s*\($/, '');
  if (type != 'processing-instruction') {
    throw type;
  }
  return new NodeTestPI(target.value);
}

function makePredicateExpr(pareno, expr, parenc) {
  return new PredicateExpr(expr);
}

function makePrimaryExpr(pareno, expr, parenc) {
  return expr;
}

function makeFunctionCallExpr1(name, pareno, parenc) {
  return new FunctionCallExpr(name);
}

function makeFunctionCallExpr2(name, pareno, arg1, args, parenc) {
  var ret = new FunctionCallExpr(name);
  ret.appendArg(arg1);
  for (var i = 0; i < args.length; ++i) {
    ret.appendArg(args[i]);
  }
  return ret;
}

function makeArgumentExpr(comma, expr) {
  return expr;
}

function makeUnionExpr(expr1, pipe, expr2) {
  return new UnionExpr(expr1, expr2);
}

function makePathExpr1(filter, slash, rel) {
  return new PathExpr(filter, rel);
}

function makePathExpr2(filter, dslash, rel) {
  rel.prependStep(makeAbbrevStep(dslash.value));
  return new PathExpr(filter, rel);
}

function makeFilterExpr(expr, predicates) {
  if (predicates.length > 0) {
    return new FilterExpr(expr, predicates);
  } else {
    return expr;
  }
}

function makeUnaryMinusExpr(minus, expr) {
  return new UnaryMinusExpr(expr);
}

function makeBinaryExpr(expr1, op, expr2) {
  return new BinaryExpr(expr1, op, expr2);
}

function makeLiteralExpr(token) {
  // remove quotes from the parsed value:
  var value = token.value.substring(1, token.value.length - 1);
  return new LiteralExpr(value);
}

function makeNumberExpr(token) {
  return new NumberExpr(token.value);
}

function makeVariableReference(dollar, name) {
  return new VariableExpr(name.value);
}

// Used before parsing for optimization of common simple cases. See
// the begin of xpathParse() for which they are.
function makeSimpleExpr(expr) {
  if (expr.charAt(0) == '$') {
    return new VariableExpr(expr.substr(1));
  } else if (expr.charAt(0) == '@') {
    var a = new NodeTestName(expr.substr(1));
    var b = new StepExpr('attribute', a);
    var c = new LocationExpr();
    c.appendStep(b);
    return c;
  } else if (expr.match(/^[0-9]+$/)) {
    return new NumberExpr(expr);
  } else {
    var a = new NodeTestName(expr);
    var b = new StepExpr('child', a);
    var c = new LocationExpr();
    c.appendStep(b);
    return c;
  }
}

function makeSimpleExpr2(expr) {
  var steps = stringSplit(expr, '/');
  var c = new LocationExpr();
  for (var i = 0; i < steps.length; ++i) {
    var a = new NodeTestName(steps[i]);
    var b = new StepExpr('child', a);
    c.appendStep(b);
  }
  return c;
}

// The axes of XPath expressions.

var xpathAxis = {
  ANCESTOR_OR_SELF: 'ancestor-or-self',
  ANCESTOR: 'ancestor',
  ATTRIBUTE: 'attribute',
  CHILD: 'child',
  DESCENDANT_OR_SELF: 'descendant-or-self',
  DESCENDANT: 'descendant',
  FOLLOWING_SIBLING: 'following-sibling',
  FOLLOWING: 'following',
  NAMESPACE: 'namespace',
  PARENT: 'parent',
  PRECEDING_SIBLING: 'preceding-sibling',
  PRECEDING: 'preceding',
  SELF: 'self'
};

var xpathAxesRe = [
    xpathAxis.ANCESTOR_OR_SELF,
    xpathAxis.ANCESTOR,
    xpathAxis.ATTRIBUTE,
    xpathAxis.CHILD,
    xpathAxis.DESCENDANT_OR_SELF,
    xpathAxis.DESCENDANT,
    xpathAxis.FOLLOWING_SIBLING,
    xpathAxis.FOLLOWING,
    xpathAxis.NAMESPACE,
    xpathAxis.PARENT,
    xpathAxis.PRECEDING_SIBLING,
    xpathAxis.PRECEDING,
    xpathAxis.SELF
].join('|');


// The tokens of the language. The label property is just used for
// generating debug output. The prec property is the precedence used
// for shift/reduce resolution. Default precedence is 0 as a lookahead
// token and 2 on the stack. TODO(mesch): this is certainly not
// necessary and too complicated. Simplify this!

// NOTE: tabular formatting is the big exception, but here it should
// be OK.

var TOK_PIPE =   { label: "|",   prec:   17, re: new RegExp("^\\|") };
var TOK_DSLASH = { label: "//",  prec:   19, re: new RegExp("^//")  };
var TOK_SLASH =  { label: "/",   prec:   30, re: new RegExp("^/")   };
var TOK_AXIS =   { label: "::",  prec:   20, re: new RegExp("^::")  };
var TOK_COLON =  { label: ":",   prec: 1000, re: new RegExp("^:")  };
var TOK_AXISNAME = { label: "[axis]", re: new RegExp('^(' + xpathAxesRe + ')') };
var TOK_PARENO = { label: "(",   prec:   34, re: new RegExp("^\\(") };
var TOK_PARENC = { label: ")",               re: new RegExp("^\\)") };
var TOK_DDOT =   { label: "..",  prec:   34, re: new RegExp("^\\.\\.") };
var TOK_DOT =    { label: ".",   prec:   34, re: new RegExp("^\\.") };
var TOK_AT =     { label: "@",   prec:   34, re: new RegExp("^@")   };

var TOK_COMMA =  { label: ",",               re: new RegExp("^,") };

var TOK_OR =     { label: "or",  prec:   10, re: new RegExp("^or\\b") };
var TOK_AND =    { label: "and", prec:   11, re: new RegExp("^and\\b") };
var TOK_EQ =     { label: "=",   prec:   12, re: new RegExp("^=")   };
var TOK_NEQ =    { label: "!=",  prec:   12, re: new RegExp("^!=")  };
var TOK_GE =     { label: ">=",  prec:   13, re: new RegExp("^>=")  };
var TOK_GT =     { label: ">",   prec:   13, re: new RegExp("^>")   };
var TOK_LE =     { label: "<=",  prec:   13, re: new RegExp("^<=")  };
var TOK_LT =     { label: "<",   prec:   13, re: new RegExp("^<")   };
var TOK_PLUS =   { label: "+",   prec:   14, re: new RegExp("^\\+"), left: true };
var TOK_MINUS =  { label: "-",   prec:   14, re: new RegExp("^\\-"), left: true };
var TOK_DIV =    { label: "div", prec:   15, re: new RegExp("^div\\b"), left: true };
var TOK_MOD =    { label: "mod", prec:   15, re: new RegExp("^mod\\b"), left: true };

var TOK_BRACKO = { label: "[",   prec:   32, re: new RegExp("^\\[") };
var TOK_BRACKC = { label: "]",               re: new RegExp("^\\]") };
var TOK_DOLLAR = { label: "$",               re: new RegExp("^\\$") };

var TOK_NCNAME = { label: "[ncname]", re: new RegExp('^' + XML_NC_NAME) };

var TOK_ASTERISK = { label: "*", prec: 15, re: new RegExp("^\\*"), left: true };
var TOK_LITERALQ = { label: "[litq]", prec: 20, re: new RegExp("^'[^\\']*'") };
var TOK_LITERALQQ = {
  label: "[litqq]",
  prec: 20,
  re: new RegExp('^"[^\\"]*"')
};

var TOK_NUMBER  = {
  label: "[number]",
  prec: 35,
  re: new RegExp('^\\d+(\\.\\d*)?') };

var TOK_QNAME = {
  label: "[qname]",
  re: new RegExp('^(' + XML_NC_NAME + ':)?' + XML_NC_NAME)
};

var TOK_NODEO = {
  label: "[nodetest-start]",
  re: new RegExp('^(processing-instruction|comment|text|node)\\(')
};

// The table of the tokens of our grammar, used by the lexer: first
// column the tag, second column a regexp to recognize it in the
// input, third column the precedence of the token, fourth column a
// factory function for the semantic value of the token.
//
// NOTE: order of this list is important, because the first match
// counts. Cf. DDOT and DOT, and AXIS and COLON.

var xpathTokenRules = [
    TOK_DSLASH,
    TOK_SLASH,
    TOK_DDOT,
    TOK_DOT,
    TOK_AXIS,
    TOK_COLON,
    TOK_AXISNAME,
    TOK_NODEO,
    TOK_PARENO,
    TOK_PARENC,
    TOK_BRACKO,
    TOK_BRACKC,
    TOK_AT,
    TOK_COMMA,
    TOK_OR,
    TOK_AND,
    TOK_NEQ,
    TOK_EQ,
    TOK_GE,
    TOK_GT,
    TOK_LE,
    TOK_LT,
    TOK_PLUS,
    TOK_MINUS,
    TOK_ASTERISK,
    TOK_PIPE,
    TOK_MOD,
    TOK_DIV,
    TOK_LITERALQ,
    TOK_LITERALQQ,
    TOK_NUMBER,
    TOK_QNAME,
    TOK_NCNAME,
    TOK_DOLLAR
];

// All the nonterminals of the grammar. The nonterminal objects are
// identified by object identity; the labels are used in the debug
// output only.
var XPathLocationPath = { label: "LocationPath" };
var XPathRelativeLocationPath = { label: "RelativeLocationPath" };
var XPathAbsoluteLocationPath = { label: "AbsoluteLocationPath" };
var XPathStep = { label: "Step" };
var XPathNodeTest = { label: "NodeTest" };
var XPathPredicate = { label: "Predicate" };
var XPathLiteral = { label: "Literal" };
var XPathExpr = { label: "Expr" };
var XPathPrimaryExpr = { label: "PrimaryExpr" };
var XPathVariableReference = { label: "Variablereference" };
var XPathNumber = { label: "Number" };
var XPathFunctionCall = { label: "FunctionCall" };
var XPathArgumentRemainder = { label: "ArgumentRemainder" };
var XPathPathExpr = { label: "PathExpr" };
var XPathUnionExpr = { label: "UnionExpr" };
var XPathFilterExpr = { label: "FilterExpr" };
var XPathDigits = { label: "Digits" };

var xpathNonTerminals = [
    XPathLocationPath,
    XPathRelativeLocationPath,
    XPathAbsoluteLocationPath,
    XPathStep,
    XPathNodeTest,
    XPathPredicate,
    XPathLiteral,
    XPathExpr,
    XPathPrimaryExpr,
    XPathVariableReference,
    XPathNumber,
    XPathFunctionCall,
    XPathArgumentRemainder,
    XPathPathExpr,
    XPathUnionExpr,
    XPathFilterExpr,
    XPathDigits
];

// Quantifiers that are used in the productions of the grammar.
var Q_01 = { label: "?" };
var Q_MM = { label: "*" };
var Q_1M = { label: "+" };

// Tag for left associativity (right assoc is implied by undefined).
var ASSOC_LEFT = true;

// The productions of the grammar. Columns of the table:
//
// - target nonterminal,
// - pattern,
// - precedence,
// - semantic value factory
//
// The semantic value factory is a function that receives parse tree
// nodes from the stack frames of the matched symbols as arguments and
// returns an a node of the parse tree. The node is stored in the top
// stack frame along with the target object of the rule. The node in
// the parse tree is an expression object that has an evaluate() method
// and thus evaluates XPath expressions.
//
// The precedence is used to decide between reducing and shifting by
// comparing the precendence of the rule that is candidate for
// reducing with the precedence of the look ahead token. Precedence of
// -1 means that the precedence of the tokens in the pattern is used
// instead. TODO: It shouldn't be necessary to explicitly assign
// precedences to rules.

var xpathGrammarRules =
  [
   [ XPathLocationPath, [ XPathRelativeLocationPath ], 18,
     passExpr ],
   [ XPathLocationPath, [ XPathAbsoluteLocationPath ], 18,
     passExpr ],

   [ XPathAbsoluteLocationPath, [ TOK_SLASH, XPathRelativeLocationPath ], 18,
     makeLocationExpr1 ],
   [ XPathAbsoluteLocationPath, [ TOK_DSLASH, XPathRelativeLocationPath ], 18,
     makeLocationExpr2 ],

   [ XPathAbsoluteLocationPath, [ TOK_SLASH ], 0,
     makeLocationExpr3 ],
   [ XPathAbsoluteLocationPath, [ TOK_DSLASH ], 0,
     makeLocationExpr4 ],

   [ XPathRelativeLocationPath, [ XPathStep ], 31,
     makeLocationExpr5 ],
   [ XPathRelativeLocationPath,
     [ XPathRelativeLocationPath, TOK_SLASH, XPathStep ], 31,
     makeLocationExpr6 ],
   [ XPathRelativeLocationPath,
     [ XPathRelativeLocationPath, TOK_DSLASH, XPathStep ], 31,
     makeLocationExpr7 ],

   [ XPathStep, [ TOK_DOT ], 33,
     makeStepExpr1 ],
   [ XPathStep, [ TOK_DDOT ], 33,
     makeStepExpr2 ],
   [ XPathStep,
     [ TOK_AXISNAME, TOK_AXIS, XPathNodeTest ], 33,
     makeStepExpr3 ],
   [ XPathStep, [ TOK_AT, XPathNodeTest ], 33,
     makeStepExpr4 ],
   [ XPathStep, [ XPathNodeTest ], 33,
     makeStepExpr5 ],
   [ XPathStep, [ XPathStep, XPathPredicate ], 33,
     makeStepExpr6 ],

   [ XPathNodeTest, [ TOK_ASTERISK ], 33,
     makeNodeTestExpr1 ],
   [ XPathNodeTest, [ TOK_NCNAME, TOK_COLON, TOK_ASTERISK ], 33,
     makeNodeTestExpr2 ],
   [ XPathNodeTest, [ TOK_QNAME ], 33,
     makeNodeTestExpr3 ],
   [ XPathNodeTest, [ TOK_NODEO, TOK_PARENC ], 33,
     makeNodeTestExpr4 ],
   [ XPathNodeTest, [ TOK_NODEO, XPathLiteral, TOK_PARENC ], 33,
     makeNodeTestExpr5 ],

   [ XPathPredicate, [ TOK_BRACKO, XPathExpr, TOK_BRACKC ], 33,
     makePredicateExpr ],

   [ XPathPrimaryExpr, [ XPathVariableReference ], 33,
     passExpr ],
   [ XPathPrimaryExpr, [ TOK_PARENO, XPathExpr, TOK_PARENC ], 33,
     makePrimaryExpr ],
   [ XPathPrimaryExpr, [ XPathLiteral ], 30,
     passExpr ],
   [ XPathPrimaryExpr, [ XPathNumber ], 30,
     passExpr ],
   [ XPathPrimaryExpr, [ XPathFunctionCall ], 30,
     passExpr ],

   [ XPathFunctionCall, [ TOK_QNAME, TOK_PARENO, TOK_PARENC ], -1,
     makeFunctionCallExpr1 ],
   [ XPathFunctionCall,
     [ TOK_QNAME, TOK_PARENO, XPathExpr, XPathArgumentRemainder, Q_MM,
       TOK_PARENC ], -1,
     makeFunctionCallExpr2 ],
   [ XPathArgumentRemainder, [ TOK_COMMA, XPathExpr ], -1,
     makeArgumentExpr ],

   [ XPathUnionExpr, [ XPathPathExpr ], 20,
     passExpr ],
   [ XPathUnionExpr, [ XPathUnionExpr, TOK_PIPE, XPathPathExpr ], 20,
     makeUnionExpr ],

   [ XPathPathExpr, [ XPathLocationPath ], 20,
     passExpr ],
   [ XPathPathExpr, [ XPathFilterExpr ], 19,
     passExpr ],
   [ XPathPathExpr,
     [ XPathFilterExpr, TOK_SLASH, XPathRelativeLocationPath ], 20,
     makePathExpr1 ],
   [ XPathPathExpr,
     [ XPathFilterExpr, TOK_DSLASH, XPathRelativeLocationPath ], 20,
     makePathExpr2 ],

   [ XPathFilterExpr, [ XPathPrimaryExpr, XPathPredicate, Q_MM ], 20,
     makeFilterExpr ],

   [ XPathExpr, [ XPathPrimaryExpr ], 16,
     passExpr ],
   [ XPathExpr, [ XPathUnionExpr ], 16,
     passExpr ],

   [ XPathExpr, [ TOK_MINUS, XPathExpr ], -1,
     makeUnaryMinusExpr ],

   [ XPathExpr, [ XPathExpr, TOK_OR, XPathExpr ], -1,
     makeBinaryExpr ],
   [ XPathExpr, [ XPathExpr, TOK_AND, XPathExpr ], -1,
     makeBinaryExpr ],

   [ XPathExpr, [ XPathExpr, TOK_EQ, XPathExpr ], -1,
     makeBinaryExpr ],
   [ XPathExpr, [ XPathExpr, TOK_NEQ, XPathExpr ], -1,
     makeBinaryExpr ],

   [ XPathExpr, [ XPathExpr, TOK_LT, XPathExpr ], -1,
     makeBinaryExpr ],
   [ XPathExpr, [ XPathExpr, TOK_LE, XPathExpr ], -1,
     makeBinaryExpr ],
   [ XPathExpr, [ XPathExpr, TOK_GT, XPathExpr ], -1,
     makeBinaryExpr ],
   [ XPathExpr, [ XPathExpr, TOK_GE, XPathExpr ], -1,
     makeBinaryExpr ],

   [ XPathExpr, [ XPathExpr, TOK_PLUS, XPathExpr ], -1,
     makeBinaryExpr, ASSOC_LEFT ],
   [ XPathExpr, [ XPathExpr, TOK_MINUS, XPathExpr ], -1,
     makeBinaryExpr, ASSOC_LEFT ],

   [ XPathExpr, [ XPathExpr, TOK_ASTERISK, XPathExpr ], -1,
     makeBinaryExpr, ASSOC_LEFT ],
   [ XPathExpr, [ XPathExpr, TOK_DIV, XPathExpr ], -1,
     makeBinaryExpr, ASSOC_LEFT ],
   [ XPathExpr, [ XPathExpr, TOK_MOD, XPathExpr ], -1,
     makeBinaryExpr, ASSOC_LEFT ],

   [ XPathLiteral, [ TOK_LITERALQ ], -1,
     makeLiteralExpr ],
   [ XPathLiteral, [ TOK_LITERALQQ ], -1,
     makeLiteralExpr ],

   [ XPathNumber, [ TOK_NUMBER ], -1,
     makeNumberExpr ],

   [ XPathVariableReference, [ TOK_DOLLAR, TOK_QNAME ], 200,
     makeVariableReference ]
   ];

// That function computes some optimizations of the above data
// structures and will be called right here. It merely takes the
// counter variables out of the global scope.

var xpathRules = [];

function xpathParseInit() {
  if (xpathRules.length) {
    return;
  }

  // Some simple optimizations for the xpath expression parser: sort
  // grammar rules descending by length, so that the longest match is
  // first found.

  xpathGrammarRules.sort(function(a,b) {
    var la = a[1].length;
    var lb = b[1].length;
    if (la < lb) {
      return 1;
    } else if (la > lb) {
      return -1;
    } else {
      return 0;
    }
  });

  var k = 1;
  for (var i = 0; i < xpathNonTerminals.length; ++i) {
    xpathNonTerminals[i].key = k++;
  }

  for (i = 0; i < xpathTokenRules.length; ++i) {
    xpathTokenRules[i].key = k++;
  }

  xpathLog('XPath parse INIT: ' + k + ' rules');

  // Another slight optimization: sort the rules into bins according
  // to the last element (observing quantifiers), so we can restrict
  // the match against the stack to the subest of rules that match the
  // top of the stack.
  //
  // TODO(mesch): What we actually want is to compute states as in
  // bison, so that we don't have to do any explicit and iterated
  // match against the stack.

  function push_(array, position, element) {
    if (!array[position]) {
      array[position] = [];
    }
    array[position].push(element);
  }

  for (i = 0; i < xpathGrammarRules.length; ++i) {
    var rule = xpathGrammarRules[i];
    var pattern = rule[1];

    for (var j = pattern.length - 1; j >= 0; --j) {
      if (pattern[j] == Q_1M) {
        push_(xpathRules, pattern[j-1].key, rule);
        break;

      } else if (pattern[j] == Q_MM || pattern[j] == Q_01) {
        push_(xpathRules, pattern[j-1].key, rule);
        --j;

      } else {
        push_(xpathRules, pattern[j].key, rule);
        break;
      }
    }
  }

  xpathLog('XPath parse INIT: ' + xpathRules.length + ' rule bins');

  var sum = 0;
  mapExec(xpathRules, function(i) {
    if (i) {
      sum += i.length;
    }
  });

  xpathLog('XPath parse INIT: ' + (sum / xpathRules.length) +
           ' average bin size');
}

// Local utility functions that are used by the lexer or parser.

function xpathCollectDescendants(nodelist, node) {
  for (var n = node.firstChild; n; n = n.nextSibling) {
    nodelist.push(n);
    arguments.callee(nodelist, n);
  }
}

function xpathCollectDescendantsReverse(nodelist, node) {
  for (var n = node.lastChild; n; n = n.previousSibling) {
    nodelist.push(n);
    arguments.callee(nodelist, n);
  }
}


// The entry point for the library: match an expression against a DOM
// node. Returns an XPath value.
function xpathDomEval(expr, node) {
  var expr1 = xpathParse(expr);
  var ret = expr1.evaluate(new ExprContext(node));
  return ret;
}

// Utility function to sort a list of nodes. Used by xsltSort() and
// nxslSelect().
function xpathSort(input, sort) {
  if (sort.length == 0) {
    return;
  }

  var sortlist = [];

  for (var i = 0; i < input.contextSize(); ++i) {
    var node = input.nodelist[i];
    var sortitem = { node: node, key: [] };
    var context = input.clone(node, 0, [ node ]);

    for (var j = 0; j < sort.length; ++j) {
      var s = sort[j];
      var value = s.expr.evaluate(context);

      var evalue;
      if (s.type == 'text') {
        evalue = value.stringValue();
      } else if (s.type == 'number') {
        evalue = value.numberValue();
      }
      sortitem.key.push({ value: evalue, order: s.order });
    }

    // Make the sort stable by adding a lowest priority sort by
    // id. This is very convenient and furthermore required by the
    // spec ([XSLT] - Section 10 Sorting).
    sortitem.key.push({ value: i, order: 'ascending' });

    sortlist.push(sortitem);
  }

  sortlist.sort(xpathSortByKey);

  var nodes = [];
  for (var i = 0; i < sortlist.length; ++i) {
    nodes.push(sortlist[i].node);
  }
  input.nodelist = nodes;
  input.setNode(0);
}


// Sorts by all order criteria defined. According to the JavaScript
// spec ([ECMA] Section 11.8.5), the compare operators compare strings
// as strings and numbers as numbers.
//
// NOTE: In browsers which do not follow the spec, this breaks only in
// the case that numbers should be sorted as strings, which is very
// uncommon.
function xpathSortByKey(v1, v2) {
  // NOTE: Sort key vectors of different length never occur in
  // xsltSort.

  for (var i = 0; i < v1.key.length; ++i) {
    var o = v1.key[i].order == 'descending' ? -1 : 1;
    if (v1.key[i].value > v2.key[i].value) {
      return +1 * o;
    } else if (v1.key[i].value < v2.key[i].value) {
      return -1 * o;
    }
  }

  return 0;
}


// Parses and then evaluates the given XPath expression in the given
// input context. Notice that parsed xpath expressions are cached.
function xpathEval(select, context) {
  var expr = xpathParse(select);
  var ret = expr.evaluate(context);
  return ret;
}


// Copyright 2005, Google Inc.
// All Rights Reserved.
//
// Unit test for the XPath parser and engine.
//
// Author: Steffen Meschkat <mesch@google.com>
//         Junji Takagi <jtakagi@google.com>

var expr = [
    "@*",
    "@*|node()",
    "/descendant-or-self::div",
    "//div",
    "substring('12345', 0, 3)",
    "//title | //link",
    "$x//title",
    // "$x/title",  // TODO(mesch): parsing of this expression is broken
    "id('a')//title",
    "//*[@about]",
    "count(descendant::*)",
    "count(descendant::*) + count(ancestor::*)",
    "concat(substring-before(@image,'marker'),'icon',substring-after(@image,'marker'))",
    "@*|text()",
    "*|/",
    "source|destination",
    "$page != 'to' and $page != 'from'",
    "substring-after(icon/@image, '/mapfiles/marker')",
    "substring-before($str, $c)",
    "$page = 'from'",
    "segments/@time",
    "child::para",
    "child::*",
    "child::text()",
    "child::node()",
    "attribute::name",
    "attribute::*",
    "descendant::para",
    "ancestor::div",
    "ancestor-or-self::div",
    "descendant-or-self::para",
    "self::para",
    "child::chapter/descendant::para",
    "child::*/child::para",
    "/",
    "/descendant::para",
    "/descendant::olist/child::item",
    "child::para[position()=1]",
    "child::para[position()=last()]",
    "child::para[position()=last()-1]",
    "child::para[position()>1]",
    "following-sibling::chapter[position()=1]",
    "preceding-sibling::chapter[position()=1]",
    "/descendant::figure[position()=42]",
    "/child::doc/child::chapter[position()=5]/child::section[position()=2]",
    "child::para[attribute::type='warning']",
    "child::para[attribute::type='warning'][position()=5]",
    "child::para[position()=5][attribute::type='warning']",
    "child::chapter[child::title='Introduction']",
    "child::chapter[child::title]",
    "child::*[self::chapter or self::appendix]",
    "child::*[self::chapter or self::appendix][position()=last()]",
    "count(//*[id='u1']|//*[id='u2'])",
    "count(//*[id='u1']|//*[class='u'])",
    "count(//*[class='u']|//*[class='u'])",
    "count(//*[class='u']|//*[id='u1'])",

    // Axis expressions
    "count(//*[@id='self']/ancestor-or-self::*)",
    "count(//*[@id='self']/ancestor::*)",
    "count(//*[@id='self']/attribute::*)",
    "count(//*[@id='self']/child::*)",
    "count(//*[@id='self']/descendant-or-self::*)",
    "count(//*[@id='self']/descendant::*)",
    "count(//*[@id='self']/following-sibling::*)",
    "count(//*[@id='self']/following::*)",
    "//*[@id='self']/parent::*/@id",
    "count(//*[@id='self']/preceding-sibling::*)",
    "count(//*[@id='self']/preceding::*)",
    "//*[@id='self']/self::*/@id",

    // (Japanese)
    "/descendant-or-self::\u90e8\u5206",
    "//\u90e8\u5206",
    "substring('\uff11\uff12\uff13\uff14\uff15', 0, 3)",
    "//\u30bf\u30a4\u30c8\u30eb | //\u30ea\u30f3\u30af",
    "$\u8b0e//\u30bf\u30a4\u30c8\u30eb",
    "//*[@\u30c7\u30b9\u30c6\u30a3\u30cd\u30a4\u30b7\u30e7\u30f3]",
    "concat(substring-before(@\u30a4\u30e1\u30fc\u30b8,'\u76ee\u5370'),'\u30a2\u30a4\u30b3\u30f3',substring-after(@\u30a4\u30e1\u30fc\u30b8,'\u76ee\u5370'))",
    "\u30bd\u30fc\u30b9|\u30c7\u30b9\u30c6\u30a3\u30cd\u30a4\u30b7\u30e7\u30f3",
    "$\u30da\u30fc\u30b8 != '\u307e\u3067' and $\u30da\u30fc\u30b8 != '\u304b\u3089'",
    "substring-after(\u30a2\u30a4\u30b3\u30f3/@\u30a4\u30e1\u30fc\u30b8, '/\u5730\u56f3\u30d5\u30a1\u30a4\u30eb/\u76ee\u5370')",
    "substring-before($\u6587\u5b57\u5217, $\u6587\u5b57)",
    "$\u30da\u30fc\u30b8 = '\u304b\u3089'",
    "\u30bb\u30b0\u30e1\u30f3\u30c8/@\u6642\u523b",
    "child::\u6bb5\u843d",
    "attribute::\u540d\u524d",
    "descendant::\u6bb5\u843d",
    "ancestor::\u90e8\u5206",
    "ancestor-or-self::\u90e8\u5206",
    "descendant-or-self::\u6bb5\u843d",
    "self::\u6bb5\u843d",
    "child::\u7ae0/descendant::\u6bb5\u843d",
    "child::*/child::\u6bb5\u843d",
    "/descendant::\u6bb5\u843d",
    "/descendant::\u9806\u5e8f\u30ea\u30b9\u30c8/child::\u9805\u76ee",
    "child::\u6bb5\u843d[position()=1]",
    "child::\u6bb5\u843d[position()=last()]",
    "child::\u6bb5\u843d[position()=last()-1]",
    "child::\u6bb5\u843d[position()>1]",
    "following-sibling::\u7ae0[position()=1]",
    "preceding-sibling::\u7ae0[position()=1]",
    "/descendant::\u56f3\u8868[position()=42]",
    "/child::\u6587\u66f8/child::\u7ae0[position()=5]/child::\u7bc0[position()=2]",
    "child::\u6bb5\u843d[attribute::\u30bf\u30a4\u30d7='\u8b66\u544a']",
    "child::\u6bb5\u843d[attribute::\u30bf\u30a4\u30d7='\u8b66\u544a'][position()=5]",
    "child::\u6bb5\u843d[position()=5][attribute::\u30bf\u30a4\u30d7='\u8b66\u544a']",
    "child::\u7ae0[child::\u30bf\u30a4\u30c8\u30eb='\u306f\u3058\u3081\u306b']",
    "child::\u7ae0[child::\u30bf\u30a4\u30c8\u30eb]",
    "child::*[self::\u7ae0 or self::\u4ed8\u9332]",
    "child::*[self::\u7ae0 or self::\u4ed8\u9332][position()=last()]",

    // The following are all expressions that used to occur in google
    // maps XSLT templates.
    "$address",
    "$address=string(/page/user/defaultlocation)",
    "$count-of-snippet-of-url = 0",
    "$daddr",
    "$form",
    "$form = 'from'",
    "$form = 'to'",
    "$form='near'",
    "$home",
    "$i",
    "$i > $page and $i < $page + $range",
    "$i < $page and $i >= $page - $range",
    "$i < @max",
    "$i <= $page",
    "$i + 1",
    "$i = $page",
    "$i = 1",
    "$info = position() or (not($info) and position() = 1)",
    "$is-first-order",
    "$is-first-order and $snippets-exist",
    "$more",
    "$more > 0",
    "$near-point",
    "$page",
    "$page != 'from'",
    "$page != 'to'",
    "$page != 'to' and $page != 'from'",
    "$page > 1",
    "$page = 'basics'",
    "$page = 'details'",
    "$page = 'from'",
    "$page = 'to'",
    "$page='from'",
    "$page='to'",
    "$r >= 0.5",
    "$r >= 1",
    "$r - 0",
    "$r - 1",
    "$r - 2",
    "$r - 3",
    "$r - 4",
    "$saddr",
    "$sources",
    "$sources[position() < $details]",
    "$src",
    "$str",
    "\"'\"",
    "(//location[string(info/references/reference[1]/url)=string($current-url)]/info/references/reference[1])[1]",
    "(not($count-of-snippet-of-url = 0) and (position() = 1) or not($current-url = //locations/location[position() = $last-pos]//reference[1]/url))",
    "(not($info) and position() = 1) or $info = position()",
    ".",
    "../@arg0",
    "../@filterpng",
    "/page/@filterpng",
    "4",
    "@attribution",
    "@id",
    "@max > @num",
    "@meters > 16093",
    "@name",
    "@start div @num + 1",
    "@url",
    "ad",
    "address/line",
    "adsmessage",
    "attr",
    "boolean(location[@id='near'][icon/@image])",
    "bubble/node()",
    "calltoaction/node()",
    "category",
    "contains($str, $c)",
    "count(//location[string(info/references/reference[1]/url)=string($current-url)]//snippet)",
    "count(//snippet)",
    "count(attr)",
    "count(location)",
    "count(structured/source) > 1",
    "description/node()",
    "destination",
    "destinationAddress",
    "domain",
    "false()",
    "icon/@class != 'noicon'",
    "icon/@image",
    "info",
    "info/address/line",
    "info/distance",
    "info/distance and $near-point",
    "info/distance and info/phone and $near-point",
    "info/distance or info/phone",
    "info/panel/node()",
    "info/phone",
    "info/references/reference[1]",
    "info/references/reference[1]/snippet",
    "info/references/reference[1]/url",
    "info/title",
    "info/title/node()",
    "line",
    "location",
    "location[@id!='near']",
    "location[@id='near'][icon/@image]",
    "location[position() > $numlocations div 2]",
    "location[position() <= $numlocations div 2]",
    "locations",
    "locations/location",
    "near",
    "node()",
    "not($count-of-snippets = 0)",
    "not($form = 'from')",
    "not($form = 'near')",
    "not($form = 'to')",
    "not(../@page)",
    "not(structured/source)",
    "notice",
    "number(../@info)",
    "number(../@items)",
    "number(/page/@linewidth)",
    "page/ads",
    "page/directions",
    "page/error",
    "page/overlay",
    "page/overlay/locations/location",
    "page/refinements",
    "page/request/canonicalnear",
    "page/request/near",
    "page/request/query",
    "page/spelling/suggestion",
    "page/user/defaultlocation",
    "phone",
    "position()",
    "position() != 1",
    "position() != last()",
    "position() > 1",
    "position() < $details",
    "position()-1",
    "query",
    "references/@total",
    "references/reference",
    "references/reference/domain",
    "references/reference/url",
    "reviews/@positive div (reviews/@positive + reviews/@negative) * 5",
    "reviews/@positive div (reviews/@positive + reviews/@negative) * (5)",
    "reviews/@total",
    "reviews/@total > 1",
    "reviews/@total > 5",
    "reviews/@total = 1",
    "segments/@distance",
    "segments/@time",
    "segments/segment",
    "shorttitle/node()",
    "snippet",
    "snippet/node()",
    "source",
    "sourceAddress",
    "sourceAddress and destinationAddress",
    "string(../@daddr)",
    "string(../@form)",
    "string(../@page)",
    "string(../@saddr)",
    "string(info/title)",
    "string(page/request/canonicalnear) != ''",
    "string(page/request/near) != ''",
    "string-length($address) > $linewidth",
    "structured/@total - $details",
    "structured/source",
    "structured/source[@name]",
    "substring($address, 1, $linewidth - 3)",
    "substring-after($str, $c)",
    "substring-after(icon/@image, '/mapfiles/marker')",
    "substring-before($str, $c)",
    "tagline/node()",
    "targetedlocation",
    "title",
    "title/node()",
    "true()",
    "url",
    "visibleurl"
];

function testParse() {
  for (var i = 0; i < expr.length; ++i) {
    assert(expr[i], xpathParse(expr[i]));
  }
}

var numExpr = [
    /* number expressions */
    [ "1+1", 2 ],
    [ "floor( -3.1415 )", -4 ],
    [ "-5 mod -2", -1 ],
    [ "-5 mod 2", -1 ],
    [ "5 mod -2", 1 ],
    [ "5 mod 2", 1 ],
    [ "ceiling( 3.1415 )", 4.0 ],
    [ "floor( 3.1415 )", 3.0 ],
    [ "ceiling( -3.1415 )", -3.0 ],
    /* string expressions */
    [ "substring('12345', -42, 1 div 0)", "12345" ],
    [ "normalize-space( '  qwerty ' )", "qwerty" ],
    [ "contains('1234567890','9')", true ],
    [ "contains('1234567890','1')", true ],
    [ "'Hello World!'", 'Hello World!' ],
    [ "substring('12345', 1.5, 2.6)", "234" ],
    [ "substring('12345', 0, 3)", "12" ],
    /* string expressions (Japanese) */
    [ "substring('\u3042\u3044\u3046\u3048\u304a', -42, 1 div 0)",
      "\u3042\u3044\u3046\u3048\u304a" ],
    [ "normalize-space( '  \u3044\u308d\u306f\u306b\u307b\u3078\u3068 ' )",
      "\u3044\u308d\u306f\u306b\u307b\u3078\u3068" ],
    [ "contains('\u5357\u7121\u5999\u6cd5\u9023\u83ef\u7d4c','\u7d4c')",
      true ],
    [ "contains('\u5357\u7121\u5999\u6cd5\u9023\u83ef\u7d4c','\u5357')",
      true ],
    [ "'\u3053\u3093\u306b\u3061\u306f\u3001\u4e16\u754c\uff01'",
      '\u3053\u3093\u306b\u3061\u306f\u3001\u4e16\u754c\uff01' ],
    [ "substring('\uff11\uff12\uff13\uff14\uff15', 1.5, 2.6)",
      "\uff12\uff13\uff14" ],
    [ "substring('\uff11\uff12\uff13\uff14\uff15', 0, 3)",
      "\uff11\uff12" ],
    /* variables */
    [ "$foo", 'bar', { foo: 'bar' } ],
    [ "$foo", 100, { foo: 100 } ],
    [ "$foo", true, { foo: true } ],
    [ "$foo + 1", 101, { foo: 100 } ],
    /* variables (Japanese) */
    [ "$\u307b\u3052", '\u307b\u3048', { \u307b\u3052: '\u307b\u3048' } ],
    [ "$\u307b\u3052", 100, { \u307b\u3052: 100 } ],
    [ "$\u307b\u3052", true, { \u307b\u3052: true } ],
    [ "$\u307b\u3052 + 1", 101, { \u307b\u3052: 100 } ],
    /* functions */
    // function id() with string argument
    [ "count(id('test1'))", 1 ],
    // function id() with node-set argument
    [ "count(id(//*[@id='testid']))", 1 ],
    /* union expressions */
    [ "count(//*[@id='u1'])", 1 ],
    [ "count(//*[@class='u'])", 3 ],
    [ "count(//*[@id='u1']|//*[@id='u2'])", 2 ],
    [ "count(//*[@id='u1']|//*[@class='u'])", 3 ],
    [ "count(//*[@class='u']|//*[@class='u'])", 3 ],
    [ "count(//*[@class='u']|//*[@id='u1'])", 3 ]
];

function testEval() {
  for (var i = 0; i < numExpr.length; ++i) {
    var ctx = new ExprContext(document.body);
    var e = numExpr[i];
    if (e[2]) {
      for (var k in e[2]) {
        var v = e[2][k];
        if (typeof v == 'number') {
          ctx.setVariable(k, new NumberValue(v));
        } else if (typeof v == 'string') {
          ctx.setVariable(k, new StringValue(v));
        } else if (typeof v == 'boolean') {
          ctx.setVariable(k, new BooleanValue(v));
        }
      }
    }

    var result = xpathParse(e[0]).evaluate(ctx);
    if (typeof e[1] == 'number') {
      assertEquals(e[0], e[1], result.numberValue());
    } else if (typeof e[1] == 'string') {
      assertEquals(e[0], e[1], result.stringValue());
    } else if (typeof e[1] == 'boolean') {
      assertEquals(e[0], e[1], result.booleanValue());
    }
  }
}

// For the following axis expressions, we need full control over the
// entire document, so we cannot evaluate them against document.body,
// but use our own XML document here. We verify that they give the
// right results by counting the nodes in their result node sets. For
// the axes that contain only one node, we check that we found the
// right node using the id. For axes that contain elements, we only
// count the elements, so we don't have to worry about whitespace
// normalization for the text nodes.
var axisTests = [
    [ "count(//*[@id='self']/ancestor-or-self::*)", 3 ],
    [ "count(//*[@id='self']/ancestor::*)", 2 ],
    [ "count(//*[@id='self']/attribute::node())", 1 ],
    [ "count(//*[@id='self']/child::*)", 1 ],
    [ "count(//*[@id='self']/descendant-or-self::*)", 3 ],
    [ "count(//*[@id='self']/descendant::*)", 2 ],
    [ "count(//*[@id='self']/following-sibling::*)", 3 ],
    [ "count(//*[@id='self']/@*/following-sibling::*)", 0 ],
    [ "count(//*[@id='self']/following::*)", 4 ],
    [ "//*[@id='self']/parent::*/@id", "parent" ],
    [ "count(/parent::*)", 0 ],
    [ "count(//*[@id='self']/preceding-sibling::*)", 1 ],
    [ "count(//*[@id='self']/@*/preceding-sibling::*)", 0 ],
    [ "count(//*[@id='self']/preceding::*)", 2 ],
    [ "//*[@id='self']/self::*/@id", "self" ]
];

function testAxes() {
  var xml = [
      '<page>',
      ' <p></p>',
      ' <list id="parent">',
      '  <item></item>',
      '  <item id="self"><d><d></d></d></item>',
      '  <item></item>',
      '  <item></item>',
      '  <item></item>',
      ' </list>',
      ' <f></f>',
      '</page>'
  ].join("");
  var ctx = new ExprContext(xmlParse(xml));
  for (var i = 0; i < axisTests.length; ++i) {
    var e = axisTests[i];
    var result = xpathParse(e[0]).evaluate(ctx);
    if (typeof e[1] == 'number') {
      assertEquals(e[0], e[1], result.numberValue());
    } else if (typeof e[1] == 'string') {
      assertEquals(e[0], e[1], result.stringValue());
    } else if (typeof e[1] == 'boolean') {
      assertEquals(e[0], e[1], result.booleanValue());
    }
  }
}


function testAttributeAsterisk() {
  var ctx = new ExprContext(xmlParse('<x a="1" b="1"><y><z></z></y></x>'));
  var expr = xpathParse("count(/x/@*)");
  assertEquals(2, expr.evaluate(ctx).numberValue());
}


// eval an xpath expression to a single node
function evalNodeSet(expr, ctx) {
  var expr1 = xpathParse(expr);
  var e = expr1.evaluate(ctx);
  return e.nodeSetValue();
}

function testEvalDom() {
  var xml = [
      '<page>',
      '<request>',
      '<q>new york</q>',
      '</request>',
      '<location lat="100" lon="200"/>',
      '</page>'
  ].join('');

  doTestEvalDom(xml, 'page', 'location', 'lat', '100', 'lon', '200');
}

function testEvalDomJapanese() {
  var xml = [
      '<\u30da\u30fc\u30b8>',
      '<\u30ea\u30af\u30a8\u30b9\u30c8>',
      '<\u30af\u30a8\u30ea>\u6771\u4eac</\u30af\u30a8\u30ea>',
      '</\u30ea\u30af\u30a8\u30b9\u30c8>',
      '<\u4f4d\u7f6e \u7def\u5ea6="\u4e09\u5341\u4e94" ',
      "\u7d4c\u5ea6='\u767e\u56db\u5341'/>",
      '</\u30da\u30fc\u30b8>'
  ].join('');

  doTestEvalDom(xml, '\u30da\u30fc\u30b8', '\u4f4d\u7f6e',
                '\u7def\u5ea6', '\u4e09\u5341\u4e94',
                '\u7d4c\u5ea6', '\u767e\u56db\u5341');
}

function doTestEvalDom(xml, page, location, lat, latValue, lon, lonValue) {
  var slashPage = '/' + page;
  var slashPageLocationAtLat = '/' + page + '/' + location + '/@' + lat;
  var slashPageLocationAtLon = '/' + page + '/' + location + '/@' + lon;

  var ctx = new ExprContext(xmlParse(xml));
  var ctx1 = new ExprContext((new DOMParser).parseFromString(xml, 'text/xml'));

  var ns = evalNodeSet(page, ctx);
  assertEquals(page, ns.length, 1);

  ns = evalNodeSet(page, ctx1);
  assertEquals(page, ns.length, 1);

  ns = evalNodeSet(slashPage, ctx);
  assertEquals(slashPage, ns.length, 1);

  ns = evalNodeSet(slashPage, ctx1);
  assertEquals(slashPage, ns.length, 1);

  assertEquals('/', evalNodeSet('/', ctx).length, 1);
  assertEquals('/', evalNodeSet('/', ctx1).length, 1);

  assertEquals('/', evalNodeSet('/', ctx)[0].nodeName, '#document');
  assertEquals('/', evalNodeSet('/', ctx1)[0].nodeName, '#document');

  assertEquals(slashPage, evalNodeSet(slashPage, ctx)[0].nodeName, page);
  assertEquals(slashPage, evalNodeSet(slashPage, ctx1)[0].nodeName, page);

  var n = evalNodeSet(slashPageLocationAtLat, ctx)[0];
  assertEquals(slashPageLocationAtLat, n.nodeName, lat);
  assertEquals(slashPageLocationAtLat, n.nodeValue, latValue);

  n = evalNodeSet(slashPageLocationAtLat, ctx1)[0];
  assertEquals(slashPageLocationAtLat, n.nodeName, lat);
  assertEquals(slashPageLocationAtLat, n.nodeValue, latValue);

  var n = evalNodeSet(slashPageLocationAtLon, ctx)[0];
  assertEquals(slashPageLocationAtLon, n.nodeName, lon);
  assertEquals(slashPageLocationAtLon, n.nodeValue, lonValue);

  n = evalNodeSet(slashPageLocationAtLon, ctx1)[0];
  assertEquals(slashPageLocationAtLon, n.nodeName, lon);
  assertEquals(slashPageLocationAtLon, n.nodeValue, lonValue);
}


// Copyright 2005 Google Inc.
// All Rights Reserved
//
// Debug stuff for the XPath parser. Also used by XSLT.

TokenExpr.prototype.toString = function() {
  return this.value;
}

TokenExpr.prototype.parseTree = function(indent) {
  var ret = indent + '[token] ' + this.value + '\n';
  return ret;
}

LocationExpr.prototype.toString = function() {
  var ret = '';
  if (this.absolute) {
    ret += '/';
  }
  for (var i = 0; i < this.steps.length; ++i) {
    if (i > 0) {
      ret += '/';
    }
    ret += this.steps[i].toString();
  }
  return ret;
}

LocationExpr.prototype.parseTree = function(indent) {
  var ret = indent + '[location] ' + 
  (this.absolute ? 'absolute' : 'relative') + '\n';
  for (var i = 0; i < this.steps.length; ++i) {
    ret += this.steps[i].parseTree(indent + ' ');
  }  
  return ret;
}

StepExpr.prototype.toString = function() {
  var ret = this.axis + '::' + this.nodetest.toString();
  for (var i = 0; i < this.predicate.length; ++i) {
    ret += this.predicate[i].toString();
  }
  return ret;
}

StepExpr.prototype.parseTree = function(indent) {
  var ret = indent + '[step]\n' + 
  indent + ' [axis] ' + this.axis + '\n' + 
  this.nodetest.parseTree(indent + ' ');
  for (var i = 0; i < this.predicate.length; ++i) {
    ret += this.predicate[i].parseTree(indent + ' ');
  }  
  return ret;
}

NodeTestAny.prototype.toString = function() {
  return 'node()';
}

NodeTestAny.prototype.parseTree = function(indent) {
  return indent + '[nodetest] ' + this.toString() + '\n';
}

NodeTestElement.prototype.toString = function() {
  return '*';
}

NodeTestElement.prototype.parseTree = NodeTestAny.prototype.parseTree;

NodeTestText.prototype.toString = function() {
  return 'text()';
}

NodeTestText.prototype.parseTree = NodeTestAny.prototype.parseTree;

NodeTestComment.prototype.toString = function() {
  return 'comment()';
}

NodeTestComment.prototype.parseTree = NodeTestAny.prototype.parseTree;

NodeTestPI.prototype.toString = function() {
  return 'processing-instruction()';
}

NodeTestPI.prototype.parseTree = NodeTestAny.prototype.parseTree;

NodeTestNC.prototype.toString = function() {
  return this.nsprefix + ':*';
}

NodeTestNC.prototype.parseTree = NodeTestAny.prototype.parseTree;

NodeTestName.prototype.toString = function() {
  return this.name;
}

NodeTestName.prototype.parseTree = NodeTestAny.prototype.parseTree;

PredicateExpr.prototype.toString = function() {
  var ret = '[' + this.expr.toString() + ']';
  return ret;
}

PredicateExpr.prototype.parseTree = function(indent) {
  var ret = indent + '[predicate]\n' + this.expr.parseTree(indent + ' ');
  return ret;
}

FunctionCallExpr.prototype.toString = function() {
  var ret = this.name.value + '(';
  for (var i = 0; i < this.args.length; ++i) {
    if (i > 0) {
      ret += ', ';
    }
    ret += this.args[i].toString();
  }
  ret += ')';
  return ret;
}

FunctionCallExpr.prototype.parseTree = function(indent) {
  var ret = indent + '[function call] ' + this.name.value + '\n';
  for (var i = 0; i < this.args.length; ++i) {
    ret += this.args[i].parseTree(indent + ' ');
  }
  return ret;
}

UnionExpr.prototype.toString = function() {
  return this.expr1.toString() + ' | ' + this.expr2.toString();
}

UnionExpr.prototype.parseTree = function(indent) {
  var ret = indent + '[union]\n' + 
  this.expr1.parseTree(indent + ' ') + 
  this.expr2.parseTree(indent + ' ');
  return ret;
}

PathExpr.prototype.toString = function() {
  var ret = '{path: {' + this.filter.toString() + '} {' + this.rel.toString() + 
  '}}';
  return ret;
}

PathExpr.prototype.parseTree = function(indent) {
  var ret = indent + '[path]\n' + 
  indent + '- filter:\n' +
  this.filter.parseTree(indent + ' ') + 
  indent + '- location path:\n' +
  this.rel.parseTree(indent + ' ');
  return ret;
}

FilterExpr.prototype.toString = function() {
  var ret = this.expr.toString();
  for (var i = 0; i < this.predicate.length; ++i) {
    ret += this.predicate[i].toString();
  }
  return ret;
}

FilterExpr.prototype.parseTree = function(indent) {
  var ret = indent + '[filter]\n' + 
  indent + '- expr:\n' +
  this.expr.parseTree(indent + ' ');
  indent + '- predicates:\n';
  for (var i = 0; i < this.predicate.length; ++i) {
    ret += this.predicate[i].parseTree(indent + ' ');
  }
  return ret;
}

UnaryMinusExpr.prototype.toString = function() {
  return '-' + this.expr.toString();
}

UnaryMinusExpr.prototype.parseTree = function(indent) {
  return indent + '[unary] -\n' + this.expr.parseTree(indent + ' ');
}

BinaryExpr.prototype.toString = function() {
  return this.expr1.toString() + ' ' + this.op.value + ' ' + 
  this.expr2.toString();
}

BinaryExpr.prototype.parseTree = function(indent) {
  return indent + '[binary] ' + this.op.value + '\n' + 
  this.expr1.parseTree(indent + ' ') + 
  this.expr2.parseTree(indent + ' ');
}

LiteralExpr.prototype.toString = function() {
  return '"' + this.value + '"';
}

LiteralExpr.prototype.parseTree = function(indent) {
  return indent + '[literal] ' + this.toString() + '\n';
}

NumberExpr.prototype.toString = function() {
  return '' + this.value;
}

NumberExpr.prototype.parseTree = function(indent) {
  return indent + '[number] ' + this.toString() + '\n';
}

VariableExpr.prototype.toString = function() {
  return '$' + this.name;
}

VariableExpr.prototype.parseTree = function(indent) {
  return indent + '[variable] ' + this.toString() + '\n';
}

XNode.prototype.toString = function() {
  return this.nodeName;
}

ExprContext.prototype.toString = function() {
  return '[' + this.position + '/' + this.nodelist.length + '] ' + 
  this.node.nodeName;
}

function Value_toString() {
  return this.type + ': ' + this.value;
}

StringValue.prototype.toString = Value_toString;
NumberValue.prototype.toString = Value_toString;
BooleanValue.prototype.toString = Value_toString;
NodeSetValue.prototype.toString = Value_toString;


// Copyright 2005-2006 Google
//
// Author: Steffen Meschkat <mesch@google.com>
//
// A very simple logging facility, used in test/xpath.html.

var logging__ = true;

function Log() {};

Log.lines = [];

Log.write = function(s) {
  if (logging__) {
    this.lines.push(xmlEscapeText(s));
    this.show();
  }
};

// Writes the given XML with every tag on a new line.
Log.writeXML = function(xml) {
  if (logging__) {
    var s0 = xml.replace(/</g, '\n<');
    var s1 = xmlEscapeText(s0);
    var s2 = s1.replace(/\s*\n(\s|\n)*/g, '<br/>');
    this.lines.push(s2);
    this.show();
  }
}

// Writes without any escaping
Log.writeRaw = function(s) {
  if (logging__) {
    this.lines.push(s);
    this.show();
  }
}

Log.clear = function() {
  if (logging__) {
    var l = this.div();
    l.innerHTML = '';
    this.lines = [];
  }
}

Log.show = function() {
  var l = this.div();
  l.innerHTML += this.lines.join('<br/>') + '<br/>';
  this.lines = [];
  l.scrollTop = l.scrollHeight;
}

Log.div = function() {
  var l = document.getElementById('log');
  if (!l) {
    l = document.createElement('div');
    l.id = 'log';
    l.style.position = 'absolute';
    l.style.right = '5px';
    l.style.top = '5px';
    l.style.width = '250px';
    l.style.height = '150px';
    l.style.overflow = 'auto';
    l.style.backgroundColor = '#f0f0f0';
    l.style.border = '1px solid gray';
    l.style.fontSize = '10px';
    l.style.padding = '5px';
    document.body.appendChild(l);
  }
  return l;
}

// Reimplement the log functions from util.js to use the simple log.
function xpathLog(msg) {
  Log.write(msg);
};
function xsltLog(msg) {};
function xsltLogXml(msg) {};


// Copyright 2005 Google Inc.
// All Rights Reserved
//
// Tests for the XPath parser. To run the test, open the file from the
// file system. No server support is required.
//
//
// Author: Steffen Meschkat <mesch@google.com>

logging = true;
xpathdebug = true;

function load_expr() {
  var s = document.getElementById('s');
  for (var i = 0; i < expr.length; ++i) {
    var o = new Option(expr[i].replace(/&gt;/,'>').replace(/&lt;/,'<'));
    s.options[s.options.length] = o;
  }
  s.selectedIndex = 0;
}

function xpath_test(form) {
  Log.clear();
  try {
    var i = form.cases.selectedIndex;
    var options = form.cases.options;

    var text = options[i].value;
    Log.writeRaw('<tt><b>' + text + '</b></tt>');

    var expr = xpathParse(text);
    Log.writeRaw('<tt><b>' + text + '</b></tt>');
    Log.writeRaw('<pre>' + expr.parseTree('') + '</pre>');
    
    options[i].selected = false;
    if (i < options.length - 1) {
      options[i+1].selected = true;
    } else {
      options[0].selected = true;
    }
    
  } catch (e) {
    Log.write('EXCEPTION ' + e);
  }
}




node10.onload(null);



node11.onsubmit(null);

