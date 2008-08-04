The ACL Editor
--------------

Features: 
	- manipulating the ACL permissions (MCRAccess), this include 
	  editing existing permissions, creating and deleting permissions
	- filtering permissions
	- creating, editing, deleting the ACL rules (MCRAccessRule)
	 - embedding reduced editors into webpages (not implemented)
		
Implementation:
	- in this version we are using Javascript to manipulate the HTML
	- changing values will be catch by a JS-Function
	- submit send everything to the servlet
	- filter for chaged values and synchronize with the DB
	
Mapping editor:
	- creating new mapping: send directly to the servlet, parsing ObjId, AcPool, Rid -> DB 
	- only rules are changeable
	- change Rid: JS setChanged -> append "changed$" to tag ID -> submit to servlet -> filter for "changed$"
					-> extract ObjId, AcPool from tag ID, get RID from parameter mapping -> DB

	
BUGS:
	- maybe some bugs in JavaScript
	- scrolling in dropdown box (Rid) is slow
	- permEditor: safe -> filter gets lost 
	
TODO:
	- fixing Bugs
	- embedded modus (improvements)
	- security checking
	- validation of changes (removing rules which are in use, override existing mappings/ rules)
	- batch processing (change Rid on mappings)
	- wrapping long page in mapping editor
	- removing old ACL-Editor (Editor Framework version)
	- Grouping of long AC-Lists realised by Java not by XSL, like now. 
	   -- the problem with XSL is that big XML's (e.g 10000 tags) have to be 
	      fully loaded as JDOM. This will take memory and speed as well.
	- nothing new