/*
 * $Revision$ $Date$ This
 * file is part of *** M y C o R e *** See http://www.mycore.de/ for details.
 * This program is free software; you can use it, redistribute it and / or
 * modify it under the terms of the GNU General Public License (GPL) as
 * published by the Free Software Foundation; either version 2 of the License or
 * (at your option) any later version. This program is distributed in the hope
 * that it will be useful, but WITHOUT ANY WARRANTY; without even the implied
 * warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details. You should have received a copy of
 * the GNU General Public License along with this program, in a file called
 * gpl.txt or license.txt. If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA 02111-1307 USA
 */

package org.mycore.datamodel.common;

import java.io.File;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import org.jdom.Document;
import org.mycore.common.MCRConfiguration;
import org.mycore.common.MCRConfigurationException;
import org.mycore.common.MCRException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.datamodel.ifs2.MCRContent;
import org.mycore.datamodel.ifs2.MCRMetadataStore;
import org.mycore.datamodel.ifs2.MCRStore;
import org.mycore.datamodel.ifs2.MCRStoredMetadata;
import org.mycore.datamodel.metadata.MCRObjectID;

/**
 * @author Frank Lützenkirchen
 * @author Jens Kupferschmidt
 * @author Thomas Scheffler (yagee)
 */
public class MCRXMLTableManager {
    private static MCRXMLTableManager SINGLETON;

    public static synchronized MCRXMLTableManager instance() {
        if (SINGLETON == null)
            SINGLETON = new MCRXMLTableManager();
        return SINGLETON;
    }

    protected MCRXMLTableManager() {
        MCRConfiguration config = MCRConfiguration.instance();

        defaultClass = config.getString("MCR.Metadata.Store.DefaultClass", "org.mycore.datamodel.ifs2.MCRVersioningMetadataStore");

        String pattern = config.getString("MCR.Metadata.ObjectID.NumberPattern", "0000000000");
        defaultLayout = (pattern.length() - 4) + "-2-2";

        String base = config.getString("MCR.Metadata.Store.BaseDir");
        baseDir = new File(base);
        checkDir(baseDir, "base");

        svnBase = config.getString("MCR.Metadata.Store.SVNBase", null);
        if ((svnBase != null) && (svnBase.startsWith("file:///"))) {
            try {
                svnDir = new File(new URI(svnBase));
            } catch (URISyntaxException ex) {
                String msg = "Syntax error in MCR.Metadata.Store.SVNBase property: " + svnBase;
                throw new MCRConfigurationException(msg, ex);
            }
            checkDir(svnDir, "svn");
        }
    }

    private void checkDir(File dir, String type) {
        if (!dir.exists()) {
            try {
                boolean created = dir.mkdirs();
                if (!created) {
                    String msg = "Unable to create metadata store " + type + " directory " + dir.getAbsolutePath();
                    throw new MCRConfigurationException(msg);
                }
            } catch (Exception ex) {
                String msg = "Exception while creating metadata store " + type + " directory " + dir.getAbsolutePath();
                throw new MCRConfigurationException(msg, ex);
            }
        } else {
            if (!dir.canRead()) {
                String msg = "Metadata store " + type + " directory " + dir.getAbsolutePath() + " is not readable";
                throw new MCRConfigurationException(msg);
            }
            if (!dir.canWrite()) {
                String msg = "Metadata store " + type + " directory " + dir.getAbsolutePath() + " is not writeable";
                throw new MCRConfigurationException(msg);
            }
            if (!dir.isDirectory()) {
                String msg = "Metadata store " + type + " " + dir.getAbsolutePath() + " is a file, not a directory";
                throw new MCRConfigurationException(msg);
            }
        }
    }

    private String defaultClass;

    private String defaultLayout;

    private File baseDir;

    private File svnDir;

    private String svnBase;

    private synchronized MCRMetadataStore getStore(String project, String type) {
        String prefix = "MCR.IFS2.Store." + project + "_" + type + ".";
        MCRConfiguration config = MCRConfiguration.instance();

        String forceXML = config.getString(prefix + "ForceXML", null);
        if (forceXML == null) // store not configured yet
        {
            config.set(prefix + "ForceXML", true);

            String clazz = config.getString(prefix + "Class", null);
            if (clazz == null) {
                config.set(prefix + "Class", defaultClass);
                clazz = defaultClass;
            }

            if (clazz.equals("org.mycore.datamodel.ifs2.MCRVersioningMetadataStore")) {
                String svnURL = config.getString(prefix + "SVNRepositoryURL", null);
                if (svnURL == null) {
                    config.set(prefix + "SVNRepositoryURL", svnBase + "/" + project + "/" + type);

                    File projectDir = new File(svnDir, project);
                    if (!projectDir.exists())
                        projectDir.mkdirs();
                }
            }

            String slotLayout = config.getString(prefix + "SlotLayout", null);
            if (slotLayout == null)
                config.set(prefix + "SlotLayout", defaultLayout);

            File projectDir = new File(baseDir, project);
            if (!projectDir.exists())
                projectDir.mkdir();

            File typeDir = new File(projectDir, type);
            if (!typeDir.exists())
                typeDir.mkdir();

            config.set(prefix + "BaseDir", typeDir.getAbsolutePath());
        }

        return MCRMetadataStore.getStore(project + "_" + type);
    }

    private MCRMetadataStore getStore(String base) {
        String[] split = base.split("_");
        return getStore(split[0], split[1]);
    }

    private MCRMetadataStore getStore(MCRObjectID mcrid) {
        return getStore(mcrid.getProjectId(), mcrid.getTypeId());
    }

    public MCRStoredMetadata create(MCRObjectID mcrid, Document xml, Date lastModified) {
        try {
            return create(mcrid, MCRContent.readFrom(xml), lastModified);
        } catch (Exception ex) {
            if (ex instanceof MCRException)
                throw (MCRException) ex;
            String msg = "Exception while storing XML metadata of mcrobject " + mcrid.getId();
            throw new MCRPersistenceException(msg, ex);
        }
    }

    public MCRStoredMetadata create(MCRObjectID mcrid, byte[] xml, Date lastModified) {
        try {
            return create(mcrid, MCRContent.readFrom(xml), lastModified);
        } catch (Exception ex) {
            if (ex instanceof MCRException)
                throw (MCRException) ex;
            String msg = "Exception while storing XML metadata of mcrobject " + mcrid.getId();
            throw new MCRPersistenceException(msg, ex);
        }
    }

    public MCRStoredMetadata create(MCRObjectID mcrid, MCRContent xml, Date lastModified) {
        try {
            MCRStoredMetadata sm = getStore(mcrid).create(xml, mcrid.getNumberAsInteger());
            sm.setLastModified(lastModified);
            MCRConfiguration.instance().systemModified();
            return sm;
        } catch (Exception ex) {
            if (ex instanceof MCRException)
                throw (MCRException) ex;
            String msg = "Exception while storing XML metadata of mcrobject " + mcrid.getId();
            throw new MCRPersistenceException(msg, ex);
        }
    }

    public void delete(String mcrid) {
        delete(new MCRObjectID(mcrid));
    }

    public void delete(MCRObjectID mcrid) {
        try {
            if (exists(mcrid))
                getStore(mcrid).delete(mcrid.getNumberAsInteger());
        } catch (Exception ex) {
            if (ex instanceof MCRException)
                throw (MCRException) ex;
            String msg = "Exception while deleting XML metadata of mcrobject " + mcrid.getId();
            throw new MCRPersistenceException(msg, ex);
        }
        MCRConfiguration.instance().systemModified();
    }

    public MCRStoredMetadata update(MCRObjectID mcrid, Document xml, Date lastModified) {
        try {
            return update(mcrid, MCRContent.readFrom(xml), lastModified);
        } catch (Exception ex) {
            if (ex instanceof MCRException)
                throw (MCRException) ex;
            String msg = "Exception while updating XML metadata of mcrobject " + mcrid.getId();
            throw new MCRPersistenceException(msg, ex);
        }
    }

    public MCRStoredMetadata update(MCRObjectID mcrid, byte[] xml, Date lastModified) {
        try {
            return update(mcrid, MCRContent.readFrom(xml), lastModified);
        } catch (Exception ex) {
            if (ex instanceof MCRException)
                throw (MCRException) ex;
            String msg = "Exception while updating XML metadata of mcrobject " + mcrid.getId();
            throw new MCRPersistenceException(msg, ex);
        }
    }

    public MCRStoredMetadata update(MCRObjectID mcrid, MCRContent xml, Date lastModified) {
        if (!exists(mcrid)) {
            String msg = "Object to update does not exist: " + mcrid;
            throw new MCRPersistenceException(msg);
        }

        try {
            MCRStoredMetadata sm = getStore(mcrid).retrieve(mcrid.getNumberAsInteger());
            sm.update(xml);
            sm.setLastModified(lastModified);
            MCRConfiguration.instance().systemModified();
            return sm;
        } catch (Exception ex) {
            if (ex instanceof MCRException)
                throw (MCRException) ex;
            String msg = "Exception while updating XML metadata of mcrobject " + mcrid.getId();
            throw new MCRPersistenceException(msg, ex);
        }
    }

    public Document retrieveXML(MCRObjectID mcrid) {
        try {
            return retrieveStoredMetadata(mcrid).getMetadata().asXML();
        } catch (Exception ex) {
            if (ex instanceof MCRException)
                throw (MCRException) ex;
            String msg = "Exception while retrieving XML metadata of mcrobject " + mcrid.getId();
            throw new MCRPersistenceException(msg, ex);
        }
    }

    public byte[] retrieveBLOB(MCRObjectID mcrid) {
        try {
            return retrieveStoredMetadata(mcrid).getMetadata().asByteArray();
        } catch (Exception ex) {
            if (ex instanceof MCRException)
                throw (MCRException) ex;
            String msg = "Exception while retrieving XML metadata of mcrobject " + mcrid.getId();
            throw new MCRPersistenceException(msg, ex);
        }
    }

    public MCRStoredMetadata retrieveStoredMetadata(MCRObjectID mcrid) {
        try {
            return getStore(mcrid).retrieve(mcrid.getNumberAsInteger());
        } catch (Exception ex) {
            if (ex instanceof MCRException)
                throw (MCRException) ex;
            String msg = "Exception while retrieving XML metadata of mcrobject " + mcrid.getId();
            throw new MCRPersistenceException(msg, ex);
        }
    }

    /**
     * This method returns the highest stored ID number for a given MCRObjectID
     * base, or 0 if no object is stored for this type and project.
     * 
     * @param project
     *            the project ID part of the MCRObjectID base
     * @param type
     *            the type ID part of the MCRObjectID base
     * @exception MCRPersistenceException
     *                if a persistence problem is occurred
     * @return the highest stored ID number as a String
     */
    public int getHighestStoredID(String project, String type) {
        return getStore(project, type).getHighestStoredID();
    }

    public boolean exists(MCRObjectID mcrid) {
        try {
            return getStore(mcrid).exists(mcrid.getNumberAsInteger());
        } catch (Exception ex) {
            if (ex instanceof MCRException)
                throw (MCRException) ex;
            String msg = "Exception while checking existence of mcrobject " + mcrid.getId();
            throw new MCRPersistenceException(msg, ex);
        }
    }

    public List<String> listIDsForBase(String base) {
        MCRMetadataStore store = getStore(base);
        List<String> list = new ArrayList<String>();
        Iterator<Integer> it = store.listIDs(MCRStore.ASCENDING);
        MCRObjectID oid = new MCRObjectID(base + "_1");
        while (it.hasNext()) {
            oid.setNumber(it.next());
            list.add(oid.getId());
        }
        return list;
    }

    public List<String> listIDsOfType(String type) {
        List<String> list = new ArrayList<String>();
        for (File fProject : baseDir.listFiles()) {
            String project = fProject.getName();
            for (File fType : fProject.listFiles()) {
                if (!type.equals(fType.getName()))
                    continue;
                String base = project + "_" + type;
                list.addAll(listIDsForBase(base));
            }
        }
        return list;
    }

    public List<String> listIDs() {
        List<String> list = new ArrayList<String>();
        for (File fProject : baseDir.listFiles()) {
            String project = fProject.getName();
            for (File fType : fProject.listFiles()) {
                String type = fType.getName();
                String base = project + "_" + type;
                list.addAll(listIDsForBase(base));
            }
        }
        return list;
    }

    public long getLastModified() {
        return MCRConfiguration.instance().getSystemLastModified();
    }
}
