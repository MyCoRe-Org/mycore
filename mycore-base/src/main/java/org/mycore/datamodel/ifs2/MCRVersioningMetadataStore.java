/*
 * $Revision$
 * $Date$
 *
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
 *
 * This program is free software; you can use it, redistribute it
 * and / or modify it under the terms of the GNU General Public License
 * (GPL) as published by the Free Software Foundation; either version 2
 * of the License or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program, in a file called gpl.txt or license.txt.
 * If not, write to the Free Software Foundation Inc.,
 * 59 Temple Place - Suite 330, Boston, MA  02111-1307 USA
 */

package org.mycore.datamodel.ifs2;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Iterator;

import org.apache.commons.vfs2.FileObject;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jdom2.JDOMException;
import org.mycore.common.MCRPersistenceException;
import org.mycore.common.MCRSessionMgr;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.common.config.MCRConfigurationException;
import org.mycore.common.content.MCRContent;
import org.tmatesoft.svn.core.SVNCancelException;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.auth.BasicAuthenticationManager;
import org.tmatesoft.svn.core.auth.ISVNAuthenticationManager;
import org.tmatesoft.svn.core.auth.SVNAuthentication;
import org.tmatesoft.svn.core.auth.SVNUserNameAuthentication;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.io.SVNRepository;
import org.tmatesoft.svn.core.io.SVNRepositoryFactory;
import org.tmatesoft.svn.core.wc.SVNEvent;
import org.tmatesoft.svn.core.wc.admin.ISVNAdminEventHandler;
import org.tmatesoft.svn.core.wc.admin.SVNAdminClient;
import org.tmatesoft.svn.core.wc.admin.SVNAdminEvent;

/**
 * Stores metadata objects both in a local filesystem structure and in a
 * Subversion repository. Changes can be tracked and restored. To enable
 * versioning, configure the repository URL, for example
 *
 * MCR.IFS2.Store.DocPortal_document.SVNRepositoryURL=file:///foo/svnroot/
 *
 * @author Frank Lützenkirchen
 */
public class MCRVersioningMetadataStore extends MCRMetadataStore {

    protected final static Logger LOGGER = LogManager.getLogger(MCRVersioningMetadataStore.class);

    protected SVNURL repURL;

    protected final static boolean SYNC_LAST_MODIFIED_ON_SVN_COMMIT = MCRConfiguration.instance().getBoolean(
        "MCR.IFS2.SyncLastModifiedOnSVNCommit", true);

    static {
        FSRepositoryFactory.setup();
    }

    @Override
    protected void init(String type) {
        super.init(type);
        setupSVN(type);
    }

    @Override
    protected void init(MCRStoreConfig config) {
        super.init(config);
        setupSVN(config.getID());
    }

    private void setupSVN(String type) {
        URI repositoryURI;
        String repositoryURIString = MCRConfiguration.instance()
            .getString("MCR.IFS2.Store." + type + ".SVNRepositoryURL");
        try {
            repositoryURI = new URI(repositoryURIString);
        } catch (URISyntaxException e) {
            String msg = "Syntax error in MCR.IFS2.Store." + type + ".SVNRepositoryURL property: "
                + repositoryURIString;
            throw new MCRConfigurationException(msg, e);
        }
        try {
            LOGGER.info("Versioning metadata store " + type + " repository URL: " + repositoryURI);
            repURL = SVNURL.create(repositoryURI.getScheme(), repositoryURI.getUserInfo(), repositoryURI.getHost(),
                repositoryURI.getPort(), repositoryURI.getPath(), true);
            LOGGER.info("repURL: " + repURL);
            File dir = new File(repURL.getPath());
            if (!dir.exists() || (dir.isDirectory() && dir.list().length == 0)) {
                LOGGER.info("Repository does not exist, creating new SVN repository at " + repositoryURI);
                repURL = SVNRepositoryFactory.createLocalRepository(dir, true, false);
            }
        } catch (SVNException ex) {
            String msg = "Error initializing SVN repository at URL " + repositoryURI;
            throw new MCRConfigurationException(msg, ex);
        }
    }

    /**
     * When metadata is saved, this results in SVN commit. If the property
     * MCR.IFS2.SyncLastModifiedOnSVNCommit=true (which is default), the
     * last modified date of the metadata file in the store will be set to the exactly
     * same timestamp as the SVN commit. Due to permission restrictions on Linux systems,
     * this may fail, so you can disable that behaviour.
     *
     * @return true, if last modified of file should be same as timestamp of SVN commit
     */
    public static boolean shouldSyncLastModifiedOnSVNCommit() {
        return SYNC_LAST_MODIFIED_ON_SVN_COMMIT;
    }

    /**
     * Returns the SVN repository used to manage metadata versions in this
     * store.
     *
     * @return the SVN repository used to manage metadata versions in this
     *         store.
     */
    SVNRepository getRepository() throws SVNException {
        SVNRepository repository = SVNRepositoryFactory.create(repURL);
        String user = MCRSessionMgr.getCurrentSession().getUserInformation().getUserID();
        SVNAuthentication[] auth = new SVNAuthentication[] {
            SVNUserNameAuthentication.newInstance(user, false, repURL, false) };
        BasicAuthenticationManager authManager = new BasicAuthenticationManager(auth);
        repository.setAuthenticationManager(authManager);
        return repository;
    }

    /**
     * Returns the URL of the SVN repository used to manage metadata versions in
     * this store.
     *
     * @return the URL of the SVN repository used to manage metadata versions in
     *         this store.
     */
    SVNURL getRepositoryURL() {
        return repURL;
    }

    /**
     * Checks to local SVN repository for errors
     * @throws MCRPersistenceException if 'svn verify' fails
     */
    public void verify() throws MCRPersistenceException {
        String replURLStr = repURL.toString();
        if (!repURL.getProtocol().equals("file")) {
            LOGGER.warn("Cannot verify non local SVN repository '{}'.", replURLStr);
            return;
        }
        try {
            SVNRepository repository = getRepository();
            long latestRevision = repository.getLatestRevision();
            if (latestRevision == 0) {
                LOGGER.warn("Cannot verify SVN repository '{}' with no revisions.", replURLStr);
            }
            ISVNAuthenticationManager authenticationManager = repository.getAuthenticationManager();
            SVNAdminClient adminClient = new SVNAdminClient(authenticationManager, null);
            File repositoryRoot = new File(URI.create(replURLStr));
            adminClient.setEventHandler(new ISVNAdminEventHandler() {
                //if more than batchSize revisions print progress
                int batchSize = 100;

                @Override
                public void checkCancelled() throws SVNCancelException {
                }

                @Override
                public void handleEvent(SVNEvent event, double progress) throws SVNException {
                }

                @Override
                public void handleAdminEvent(SVNAdminEvent event, double progress) throws SVNException {
                    if (event.getMessage() != null) {
                        if (event.getRevision() % batchSize != 0 || event.getRevision() == 0) {
                            LOGGER.debug(event::getMessage);
                        } else {
                            LOGGER.info("{} ({}% done)", event.getMessage(),
                                (int) (event.getRevision() * 100.0 / latestRevision));
                        }
                    }
                }
            });
            adminClient.doVerify(repositoryRoot);
            LOGGER.info("Verified SVN repository '{}'.", replURLStr);
        } catch (Exception e) {
            throw new MCRPersistenceException("SVN repository contains errors and could not be verified: " + replURLStr,
                e);
        }
    }

    @Override
    public MCRVersionedMetadata create(MCRContent xml, int id) throws IOException, JDOMException {
        return (MCRVersionedMetadata) super.create(xml, id);
    }

    @Override
    public MCRVersionedMetadata create(MCRContent xml) throws IOException, JDOMException {
        return (MCRVersionedMetadata) super.create(xml);
    }

    /**
     * Returns the metadata stored under the given ID, or null. Note that this
     * metadata may not exist currently in the store, it may be a deleted
     * version, which can be restored then.
     *
     * @param id
     *            the ID of the XML document
     * @return the metadata stored under that ID, or null when there is no such
     *         metadata object
     */
    @Override
    public MCRVersionedMetadata retrieve(int id) throws IOException {
        if (exists(id)) {
            return (MCRVersionedMetadata) super.retrieve(id);
        } else {
            return new MCRVersionedMetadata(this, getSlot(id), id, super.forceDocType);
        }
    }

    /**
     * Updates all stored metadata to the latest revision in SVN
     */
    public void updateAll() throws Exception {
        for (Iterator<Integer> ids = listIDs(true); ids.hasNext();) {
            retrieve(ids.next()).update();
        }
    }

    @Override
    public void delete(int id) throws IOException {
        MCRVersionedMetadata vm = retrieve(id);
        vm.delete();
    }

    @Override
    protected MCRVersionedMetadata buildMetadataObject(FileObject fo, int id) {
        return new MCRVersionedMetadata(this, fo, id, super.forceDocType);
    }

}
