
ReadMe for ftp.jar and ftp.zip
------------------------------

Parts of MyCoRe use classes for FTP client functionality. 
These classes are bundled in the file ftp.jar in this directory.
This is a slightly modified version of the Java FTP client 
library from Enterprise Distributed Technologies Ltd. 
The library is distributed under GNU Lesser General Public License.
The file ftp.jar contains the compiled class files and must
be placed in the CLASSPATH. ftp.zip contains all original and
modified sources, including JavaDoc documentation for the modifications, 
build files and so on. In ftp.zip, you will find more information
on the original package and its license.

The modification is that I added two methods in FTPClient.java to allow
FTP get/put to/from an InputStream or OutputStream instead of just
files on the filesystem.

The rest of this readme is the original readme.txt file from
Enterprise Distributed Technologies Ltd.

--------------------------------------------------------------------

/**
 *
 *  Java FTP client library.
 *  
 *  Copyright (C) 2000  Enterprise Distributed Technologies Ltd
 *  
 *  www.enterprisedt.com
 *  
 *  This library is free software; you can redistribute it and/or
 *  modify it under the terms of the GNU Lesser General Public
 *  License as published by the Free Software Foundation; either
 *  version 2.1 of the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *  Lesser General Public License for more details.
 *  
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the Free Software
 *  Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *  
 *  Bug fixes, suggestions and comments should be sent to:
 *  
 *  bruceb@cryptsoft.com
 *  
 *  or by snail mail to:
 *  
 *  Bruce P. Blackshaw
 *  53 Wakehurst Road
 *  London SW11 6DB
 *  United Kingdom
 * 
 *  Change Log:  
 *
 *	  $Log$
 *	  Revision 1.1  2002/06/04 07:50:36  mcrfluet
 *	  Added a readme file that describes the contents and license of ftp.jar and
 *	  ftp.zip
 *	
 *	
 */
 
 1. General
 
 As this package is released under the GNU Lesser General Public License, it can be
 freely embedded in commercial or non-commercial code. Read LICENSE.TXT for details
 of this license, or visit www.gnu.org.
 
 Please email any bug fixes, suggestions and comments to bruceb@cryptsoft.com. I will
 do my best to make regular releases of this library and incorporate fixes ASAP.
 
 I've tested this software on Solaris 2.6 and Windows 95 and Windows 2000 clients so far.
 
 I plan to make regular releases to enhance functionality and regression tests.
 
 The key class you'll want to use is the FTPClient class. See FTPClientTest for a 
 simple example of its use.
 
 2. Acknowledgements
 
 I am grateful to Peter van der Linden for making available his t.java FTP client class.
 I have not copied this class but have drawn heavily on its ideas. Make sure you visit
 his excellent Java FAQ, at www.afu.com/javafaq.html.

 I am also grateful to the very many people who have sent in suggestions, comments and bug fixes.
 
 Bruce Blackshaw
 London, UK.
 
 
 
 
