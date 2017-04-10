# MyCoRe-Mets

## MyCoRe-Mets-Editor

The MyCoRe-Mets-Editor can be started with the URL

    http://my.repository/rsc/mets/editor/start/$derivate_id$

I a user opens a Mets-Document, then it will be locked, so no other user can access it. The lock is bound to the user session.

## MCRUpdateMetsOnDerivateChangeEventHandler

The MCRUpdateMetsOnDerivateChangeEventHandler synchronizes the files of the derivate with the mets file. 
If you add new files, they will be added to the mets file and if you delete files, they will be deleted from the mets
 file.
 
The event handler also handles some alto-file linking e.g. if you have a file named MyCoRe.tiff then the event handler 
searches for a existing MyCoRe.xml in the alto folder in the root of the derivate.
  
This event handler is enabled by default, but can cause serious performance performance problems when you import large 
amount of files.
  


