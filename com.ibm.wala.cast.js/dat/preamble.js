// The following stuff has to be statically analyzed
// 1. readonly attributes
// 2. using setAttribute to register an event handler (this should be disallowed since it can be used like 'eval')
// 3. All methods properties assigned here are implicitly readonly
// 4. inheritance in this file is implemented by creating a new object for the prototype. Instead, the prototype object could be shared

// A combination of interfaces NodeList, NamedNodeMap, HTMLCollection
// implement a list of Nodes, accessible through names as well

dynamic_node = 0;
dom_nodes = new Array();

note_url = function noteURL(url) {
	// hook for analysis of Web pages
};

note_post_parameters = function notePostParameters(url) {
	// hook for analysis of Web pages
};

NamedNodeList = function NamedNodeList() {
	var maxLength = 10;
	var local = new Array(10);
	var counter = -1;

	var checkAndIncrease = function checkAndIncrease() {
		if(counter >= maxLength - 1) {
			maxLength += 10;
			var temp = new Array(maxLength);
			for(traverse = 0; traverse <= counter; traverse++) {
				temp[traverse] = local[traverse];
			}
			local = temp;
		}
	}

        this.get = function _get(index) {
                return local[ index ];
	}

	this.add = function add(elem) {
		checkAndIncrease();
		local[counter++] = elem;
	}

	this.getIndex = function getIndex(elem) {
		for(var traverse = 0; traverse <= counter; traverse++) {
			if(local[traverse] == elem) {
				return traverse;
			}
		}
		return -1;
	}

	this.remove = function remove(elem) {
		var found = getIndex(elem);
		if(found > -1) {
			for(traverse = found; traverse < counter; traverse++) {
				local[traverse] = local[traverse+1];
			}
			counter--;
		}
	}

	this.replace = function replace(newElem, oldElem) {
		var found = getIndex(oldElem);
		if(found > -1) {
			local[found] = newElem;
		}
	}

	this.insertBefore = function insertBefore(newElem, oldElem) {
		var found = getIndex(oldElem);
		if(found > -1) {
			checkAndIncrease();
			var prev = newElem;
			for(var traverse = counter + 1; traverse > found; traverse--) {
				local[traverse] = local[traverse-1];
			}
			local[found] = newElem;
		}
	}

	this.collect = function collect(predicate, result) {
          for(var traverse = 0; traverse <= counter; traverse++) {
            local[traverse].collect(predicate, result);
          }
        }
}

DOMNode = function DOMNode() { // An impostor for the Node class
	this.childNodes = new NamedNodeList();
	this.insertBefore = function insertBefore(newChild, refChild) {
				this.childNodes.insertBefore(newChild, refChild);
			}
	this.replaceChild = function replaceChild(newChild, oldChild) {
				this.childNodes.replace(newChild, oldChild);
			}
	this.removeChild = function removeChild(oldChild) {
				this.childNodes.remove(oldChild);
			}
	this.appendChild = function appendChild(newChild) {
				this.childNodes.add(newChild);
				newChild.parentNode = this;
			}
	this.hasChildNodes = function hasChildNodes() {
				return this.childNodes.hasElements();
			}
	
	this.ownerDocument = document;
	this.ownerWindow = window;
	this.ownerWindow.XMLHttpRequest = XMLHttpRequest;

	this.collect = function collect(predicate, result) {
          if (predicate(this)) {
            result.push(this);
          }
          this.childNodes.collect(predicate, result);
        }
}

DOMDocument = function DOMDocument() {
	this.temp = DOMNode;
	this.temp();

	this.createElement = function createElement(name) {
		// TODO : to be implemented accurately
		var toReturn = new DOMHTMLGenericElement(name);
		return toReturn;
	}

        this.getElementById = function getElementById(id) {
          var result = new Array();
          this.collect(function check_id(x) { return x.id == id; }, result);
          return result[0];
	}
	
	this.write = function write_to_dom (stuff) {
		
	};
}

HTMLBody = function HTMLBody(){
	this.innerHTML = new String();
}

DOMHTMLDocument = function DOMHTMLDocument() {
	this.temp = DOMDocument;
	this.temp();
	this.URL = new String();
	this.body = new HTMLBody();
	this.forms = new Array();
}


Location = function Location(){
	this.host = new String();
	this.hostname = new String();
	this.href = new String();
	this.search = new String();
	this.protocol = new String();
	this.pathname = new String();
	this.toString = function Location_to_string(){
		return new String();
	}
}


DOMWindow = function DOMWindow(){
	this.name = new String();
	this.open = function window_open(url, stuff) { 
		note_url(url); 
	};
}

DOJOObj = function DOJOObj(){
	this.moduleUrl = function module_url(str1, str2){
		return str1 + str2;
	}
}

// Creating the root Location object
var location = new Location();

// Creating the root document object
document = new DOMHTMLDocument();

// Creating the root window object
window = new DOMWindow();

document.location = location;
window.location = location;

window.document = document;
document.defaultView = window;
window.XMLHttpRequest = XMLHttpRequest;

var dojo = new DOJOObj();

DOMElement = function DOMElement() { // An impostor for the Element class
	// inherits from Node
	this.temp = DOMNode;
	this.temp();

	// The get/set/remove attribute methods cannot be run using 'onclick','onmouseover', 'on...' kind of arguments for name.
	// since that would be used as a workaround for eval

	this.getAttribute = function getAttribute(name) {
		return this[name];
	}
	this.setAttribute = function setAttribute(name, value) {
		this[name] = value;
	}

	this.removeAttribute = function removeAttribute(name) {
	        this[name] = undefined;
	}

    this.getElementsByTagName = function _getElementsByTagName(tagName) {
        var result = new Array();
        this.collect(function check_tag(x) { return x.name == tagName; }, result);
        return result;

    }

}

DOMHTMLElement = function DOMHTMLElement() { // An impostor for the HTMLElement class
	// inherits from Element
	this.temp = DOMElement;
	this.temp();

	// Set HTML Attribute Defaults
	this.id = null;
	this.title = null;
	this.lang = null;
	this.dir = null;
	this.className = null;
    
	// record new node in dom_nodes
	dom_nodes[dynamic_node++] = this;
	
    this.forms = new Array();
    this.formCount = 0;
}

// Just a hack until all HTML elements have corresponding constructors
DOMHTMLGenericElement = function DOMHTMLGenericElement(tagName) {
	// inherits from Element
	this.temp = DOMHTMLElement;
	this.temp();

	// Set just the tag name
	this.nodeName = tagName;
	this.nodeValue = null;
	
	// load 'src' if appropriate
	this.src.loadFile = String.prototype.loadFile;
	this.src.loadFile();
}

var formCount = 0;

DOMHTMLFormElement = function DOMHTMLFormElement() {
	// inherits from HTMLElement
	this.temp = DOMHTMLElement;
	this.temp();

        // add to 'forms' property
        document.forms[formCount++] = this;

	// Set Javascript properties
	this.nodeName = "FORM";
	this.elements = new NamedNodeList();
	this.length = function form_elt_length() {
		return this.elements.length;
	}
	this.submit = function form_elt_submit() {
	}
	this.reset = function form_elt_reset () {
	}

	// Set HTML Attribute Defaults
	this.name = null;
	this.acceptCharset = "UNKNOWN";
	this.action = null;
	this.enctype = "application/x-www-form-urlencoded";
	this.method = "get";
	this.target = null;
}

DOMHTMLTableElement = function DOMHTMLTableElement () {
	// inherits from HTMLElement
	this.temp = DOMHTMLElement;
	this.temp();

	this.rows = function table_elt_rows() {
	}	
}

XMLHttpRequest = function _XMLHttpRequest() {

	this.UNSENT = 0;
	this.OPENED = 1;
	this.HEADERS_RECEIVED = 2;
	this.LOADING = 3;
	this.DONE = 4;

	this.orsc_handler = function xhr_orsc_handler() {
		this.onreadystatechange();
	}
		  
	this.open = function xhr_open(method, url, async, user, password) {
		this.sUrl = url;
		note_url(url);
		this.orsc_handler();
	}

	this.setRequestHeader = function xhr_setRequestHeader(header, value) {
		
	}

	this.send = function xhr_send(data) {
		this.orsc_handler();
		note_post_parameters(data);
	}

	this.abort = function xhr_abort() {
		this.orsc_handler();
	}

	this.getResponseHeader = function xhr_getResponseHeader(header) {

	}	

	this.getAllResponseHeaders = function xhr_getAllResponseHeaders() {

	}

};

for(var n = 0; n < dom_nodes.length; n++) {
	dom_nodes[n].onload();
	dom_nodes[n].onreadystatechange();
}

