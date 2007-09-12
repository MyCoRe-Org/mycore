The ACL Editor
--------------

Features: 
	- manipulating the ACL permissions (MCRAccess), this include 
	  editing existing permissions, creating and deleting permissions
	- filtering permissions
	- creating, editing, deleting the ACL rules (MCRAccessRule)
	 - embedding reduced editors into webpages (not implemented)
		
Implementation:
	- in this version the ACL editor is "Editor-Framework" centric, this means
	  the editors are using Editor-Framework xml
	- the MCRACLEditorServlet is responsible for delivering ACL data from DB as XML to
	  to the editors. On the other side the editors send their data to the servlet for processing
	- MCRACLHIBAccess is the connection to the DB
	- MCRACLXMLProcessing "convert" the data from DB to xml and vice versa
	
BUGS:
	- deleting rules is possible, even it is still in use
	- using more than one editor in the same webpage causing problems
	
TODO:
	- fixing Bugs
	- embedded modus
	- some security checking
	- validation of the inputs