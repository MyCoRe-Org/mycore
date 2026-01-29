/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.datamodel.common;

/**
 * Manages persistence of MCRObject and MCRDerivate xml metadata.
 * Provides methods to create, retrieve, update and delete object metadata
 * using IFS2 MCRMetadataStore instances.
 * <p>
 * For configuration, at least the following properties must be set:
 * <p>
 * MCR.Metadata.Store.BaseDir=/path/to/metadata/dir
 * MCR.Metadata.Store.SVNBase=file:///path/to/local/svndir/
 * <p>
 * Both directories will be created if they do not exist yet.
 * For each project and type, a subdirectory will be created,
 * for example %MCR.Metadata.Store.BaseDir%/DocPortal/document/.
 * <p>
 * The default IFS2 store is MCRVersioningMetadataStore, which
 * versions metadata using SVN in local repositories below SVNBase.
 * If you do not want versioning and would like to have better
 * performance, change the following property to
 * <p>
 * MCR.Metadata.Store.DefaultClass=org.mycore.datamodel.ifs2.MCRMetadataStore
 * <p>
 * It is also possible to change individual properties per project and object type
 * and overwrite the defaults, for example
 * <p>
 * MCR.IFS2.Store.Class=org.mycore.datamodel.ifs2.MCRVersioningMetadataStore
 * MCR.IFS2.Store.SVNRepositoryURL=file:///use/other/location/for/document/versions/
 * MCR.IFS2.Store.SlotLayout=2-2-2-2
 * <p>
 * See documentation of MCRStore and MCRMetadataStore for details.
 *
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 * 
 * @deprecated Use {@link MCRDefaultXMLMetadataManager} instead.
 */
@Deprecated(forRemoval = true)
public class MCRDefaultXMLMetadataManagerAdapter extends MCRDefaultXMLMetadataManager {

}
