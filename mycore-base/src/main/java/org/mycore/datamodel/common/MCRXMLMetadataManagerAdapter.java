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
 * Provides an abstract class for persistence managers of MCRObject and MCRDerivate xml
 * metadata to extend, with methods to perform CRUD operations on object metadata.
 * <p>
 * The default xml metadata manager is {@link MCRDefaultXMLMetadataManager}. If you wish to use
 * another manager implementation instead, change the following property accordingly:
 * <p>
 * MCR.Metadata.Manager.Class=org.mycore.datamodel.common.MCRDefaultXMLMetadataManager
 * <p>
 * Xml metadata managers have a default class they will instantiate for every store.
 * If you wish to use a different default class, change the following property
 * accordingly. For example, when using the MCRDefaultXMLMetadataManager:
 * <p>
 * MCR.Metadata.Store.DefaultClass=org.mycore.datamodel.ifs2.MCRVersioningMetadataStore
 * <p>
 * The following directory will be used by xml metadata managers to keep up-to-date
 * store contents in. This directory will be created if it does not exist yet.
 * <p>
 * MCR.Metadata.Store.BaseDir=/path/to/metadata/dir
 * <p>
 * For each project and type, subdirectories will be created below this path,
 * for example %MCR.Metadata.Store.BaseDir%/DocPortal/document/.
 * <p>
 * If an SVN-based store is configured, then the following property will be used to
 * store and manage local SVN repositories:
 * <p>
 * MCR.Metadata.Store.SVNBase=file:///path/to/local/svndir/
 * <p>
 * It is also possible to change individual properties per project and object type
 * and overwrite the defaults, for example
 * <p>
 * MCR.IFS2.Store.Class=org.mycore.datamodel.ifs2.MCRVersioningMetadataStore
 * MCR.IFS2.Store.SVNRepositoryURL=file:///use/other/location/for/document/versions/
 * MCR.IFS2.Store.SlotLayout=2-2-2-2
 * <p>
 * See documentation of MCRStore, MCRMetadataStore and the MCRXMLMetadataManager
 * extensions (e.g. MCRDefaultXMLMetadataManager) for details.
 *
 * @author Christoph Neidahl (OPNA2608)
 * 
 * @deprecated Use {@link MCRXMLMetadataManager instead}.
 */
@Deprecated(forRemoval = true)
public interface MCRXMLMetadataManagerAdapter extends MCRXMLMetadataManager {

}
