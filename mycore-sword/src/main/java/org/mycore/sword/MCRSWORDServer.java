/*
 * 
 * $Revision$ $Date$
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

package org.mycore.sword;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TimeZone;
import java.util.zip.Adler32;
import java.util.zip.CheckedInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.jdom2.Attribute;
import org.jdom2.Document;
import org.jdom2.Element;
import org.jdom2.JDOMException;
import org.jdom2.Namespace;
import org.jdom2.input.SAXBuilder;
import org.mycore.common.config.MCRConfiguration;
import org.mycore.frontend.servlets.MCRServletJob;
import org.purl.sword.atom.Author;
import org.purl.sword.atom.Generator;
import org.purl.sword.atom.Title;
import org.purl.sword.base.AtomDocumentRequest;
import org.purl.sword.base.AtomDocumentResponse;
import org.purl.sword.base.Collection;
import org.purl.sword.base.Deposit;
import org.purl.sword.base.DepositResponse;
import org.purl.sword.base.ErrorCodes;
import org.purl.sword.base.SWORDAuthenticationException;
import org.purl.sword.base.SWORDEntry;
import org.purl.sword.base.SWORDErrorDocument;
import org.purl.sword.base.SWORDErrorException;
import org.purl.sword.base.SWORDException;
import org.purl.sword.base.Service;
import org.purl.sword.base.ServiceDocument;
import org.purl.sword.base.ServiceDocumentRequest;
import org.purl.sword.base.Workspace;
import org.purl.sword.server.SWORDServer;

/**
 * <p>
 * The <code>MCRSWORDServer</code> is a base implementation of sword commons
 * {@link SWORDServer}. For a providing concrete implementation this class can
 * be used as base class or an own implementation of {@link SWORDServer} must be
 * given. An instance of this server is able to handle deposited files of
 * mime-type <code>application/zip</code> and validation of
 * <code>DSpace METS SIP Profile 1.0</code>.
 * </p>
 * <p>
 * If you want to use an instance of this class you have to provide an
 * implementation of {@link MCRSWORDIngester} inside mycores configuration with
 * full class path under the key <code>MCR.SWORD.ingester.class</code>. The
 * ingester should do handling of decompressed files and loading data into a
 * concrete object model. Also have a look at the documentation of methods
 * {@link #doDeposit(Deposit, MCRServletJob)} and
 * {@link #doServiceDocument(ServiceDocumentRequest, MCRServletJob)} what
 * details are done for you. Important is, that this implementation is not
 * capable of handling X-On-Behalf-Of requests.
 * </p>
 * <p>
 * For the use of an own implementation of a {@link SWORDServer} you have to
 * provide a class implementing this interface. Also there has to be a class
 * with full class path given inside mycores configuration under the key
 * <code>MCR.SWORD.server.class</code>.
 * </p>
 * 
 * @author Nils Verheyen
 * 
 */
public class MCRSWORDServer implements SWORDServer, MCRSWORDIngester {

    private static final Logger LOG = Logger.getLogger(MCRSWORDServer.class);

    /** Mime type for zip-files. */
    private static final String ZIP_MIME_TYPE = "application/zip";

    /**
     * A deposited file with METS profile must contain a <code>mets.xml</code>
     * as package descriptor.
     */
    private static final String METS_MANIFEST_FILENAME = "mets.xml";

    /**
     * When delivering a service document a package description must be given.
     * An instance of this class is able to validate only DSpace METS SIP
     * Profile 1.0.
     */
    private static final String METS_PACKAGING = "http://purl.org/net/sword-types/METSDSpaceSIP";

    /**
     * Inside a deposited file the mets.xml must contain this string as
     * <code>PROFILE</code>-attribute inside roots mets element.
     */
    static final String METS_PROFILE = "DSpace METS SIP Profile 1.0";

    /** Line separator of this system. */
    private static final String LINE_SEPARATOR = System.getProperty("line.separator");

    /** Counter counts op deposits. */
    private int counter;

    public AtomDocumentResponse doAtomDocument(AtomDocumentRequest adr, MCRServletJob job) throws SWORDAuthenticationException, SWORDErrorException,
            SWORDException {

        return new AtomDocumentResponse(HttpServletResponse.SC_OK);
    }

    /**
     * When using this class as {@link SWORDServer} a service document with only
     * one collection is delivered. The following details can be configured
     * through MyCoRe:
     * <dl>
     * <dt>MCR.SWORD.workspace.title</dt>
     * <dd>&lt;atom:title&gt; of used workspace</dd>
     * <dt>MCR.SWORD.default.collection.title</dt>
     * <dd>&lt;atom:title&gt; of used collection</dd>
     * <dt>MCR.SWORD.default.collection.policy</dt>
     * <dd>&lt;sword:collectionPolicy&gt; of used collection. Used for a
     * human-readable description of collection policy. Include either a text
     * description or a URI.</dd>
     * <dt>MCR.SWORD.default.collection.treatment</dt>
     * <dd>&lt;sword:treatment&gt; of used collection. Used for a human-readable
     * statement about what treatment the deposited resource will receive.</dd>
     * <dt>MCR.SWORD.default.collection.abstract</dt>
     * <dd>&lt;dcterms:abstract&gt; of used collection. The use of a Dublin Core
     * dcterms:abstract element containing a description of the Collection is
     * RECOMMENDED.</dd>
     * <dt>MCR.SWORD.max.uploaded.file.size</dt>
     * <dd>&lt;sword:maxUploadSize&gt; of used workspace. May be included to
     * indicate the maximum size (in kB) of package that can be uploaded to the
     * SWORD service. <b>ATTENTION!</b> Inside the configuration max upload size
     * must be given in bytes. Inside the service document the max upload size
     * is given in kBytes.</dd>
     * </dl>
     * Some information is set to default by an instance of this class. We only
     * accept <code>application/zip</code> files, the accepted packaging is
     * DSpace METS SIP 1.0, the service version is 1.3 and a mediation is not
     * allowed.
     * 
     * @see <a
     *      href="http://www.swordapp.org/docs/sword-profile-1.3.html#b.8">Service
     *      Documents</a>
     */
    public ServiceDocument doServiceDocument(ServiceDocumentRequest sdr, MCRServletJob job) throws SWORDAuthenticationException, SWORDErrorException,
            SWORDException {

        // Allow users to force the throwing of a SWORD error exception by
        // setting the OBO user to 'error'
        if ("error".equals(sdr.getOnBehalfOf())) {
            // Throw the error exception
            throw new SWORDErrorException(ErrorCodes.MEDIATION_NOT_ALLOWED, "Mediated deposits not allowed");
        }

        // Create and return a dummy ServiceDocument
        ServiceDocument document = new ServiceDocument();
        Service service = new Service("1.3", true, true);
        service.setMaxUploadSize(MCRSWORDUtils.getMaxUploadSize() / 1024);
        document.setService(service);

        Workspace workspace = new Workspace();
        workspace.setTitle(MCRConfiguration.instance().getString("MCR.SWORD.workspace.title"));
        Collection collection = new Collection();
        collection.setTitle(MCRConfiguration.instance().getString("MCR.SWORD.default.collection.title"));
        collection.addAccepts(ZIP_MIME_TYPE);
        collection.addAcceptPackaging(METS_PACKAGING, 1.0f);
        collection.setCollectionPolicy(MCRConfiguration.instance().getString("MCR.SWORD.default.collection.policy"));
        collection.setTreatment(MCRConfiguration.instance().getString("MCR.SWORD.default.collection.treatment"));
        collection.setAbstract(MCRConfiguration.instance().getString("MCR.SWORD.default.collection.abstract"));
        collection.setMediation(false);
        collection.setLocation(sdr.getLocation());
        workspace.addCollection(collection);
        service.addWorkspace(workspace);

        return document;
    }

    /**
     * Base implementation for performing a deposit on this application. This
     * method uses a templating mechanism, to perform the deposit. The following
     * methods are used in order:
     * <dl>
     * <dt>1. {@link #validateDeposit(Deposit)}</dt>
     * <dd>When overriding be sure to throw a {@link SWORDErrorException}, so a
     * {@link SWORDErrorDocument} is delivered to the client</dd>
     * <dt>2. {@link #persistDeposit(Deposit, SWORDEntry, StringBuffer, MCRServletJob)}</dt>
     * <dd>When overriding use given {@link SWORDEntry} and {@link StringBuffer}
     * for providing verbose info and document response data.</dd>
     * <dt>3. {@link #createDepositResponse(StringBuffer, Deposit, SWORDEntry)}</dt>
     * <dd>When overriding provide a proper {@link DepositResponse}</dd>
     * </dl>
     */
    public DepositResponse doDeposit(Deposit deposit, MCRServletJob job) throws SWORDAuthenticationException, SWORDErrorException, SWORDException {

        LOG.info("performing deposit for user: " + deposit.getUsername());
        
        SWORDEntry swordEntry = new SWORDEntry();
        StringBuffer verboseInfo = new StringBuffer("Deposit info: ");

        // Check this is a collection that takes obo deposits, else thrown an
        // error
        validateDeposit(deposit);

        // persist files and created data objects for deposit
        persistDeposit(deposit, swordEntry, verboseInfo, job);

        // create response
        DepositResponse response = createDepositResponse(verboseInfo, deposit, swordEntry);
        
        LOG.info("deposit for user: " + deposit.getUsername() + " performed");

        return response;
    }

    /**
     * Validates the content given in target deposit. Only the deposit itself is
     * validated not the file inside the deposit.
     * 
     * @param deposit
     *            the deposit to validate
     * @throws SWORDErrorException
     *             thrown if invalid information is given inside the deposit
     */
    protected void validateDeposit(Deposit deposit) throws SWORDErrorException {

        if (deposit.getOnBehalfOf() != null && !deposit.getOnBehalfOf().isEmpty()) {
            LOG.error(String.format("throwing error: mediated user (%1$s)", deposit.getOnBehalfOf()));
            throw new SWORDErrorException(ErrorCodes.MEDIATION_NOT_ALLOWED, "Mediated deposit not allowed");
        }
        if (!METS_PACKAGING.equals(deposit.getPackaging())) {
            LOG.error(String.format("throwing error: invalid packaging (%1$s)", deposit.getPackaging()));
            throw new SWORDErrorException(ErrorCodes.ERROR_CONTENT, "Invalid packaging given");
        }
        if (!ZIP_MIME_TYPE.equals(deposit.getContentType())) {
            LOG.error(String.format("throwing error: invalid mime type (%1$s)", deposit.getContentType()));
            throw new SWORDErrorException(ErrorCodes.ERROR_CONTENT, "Invalid mime type given");
        }
    }

    /**
     * <p>
     * Decompresses the file inside the {@link Deposit} and performs the
     * deposit. Inside all files are decompressed into the temporary directory
     * and deleted after depositing is done. Also the search and validation for
     * the package descriptor, as mentioned in classes doc is done. After a
     * successful validation, the concrete {@link MCRSWORDIngester} is called to
     * ingest all data into directory structure, databases etc.
     * </p>
     * <p>
     * When overriding this method be sure to fill given response entry, so a
     * proper {@link DepositResponse} can be generated
     * </p>
     * 
     * @param deposit
     *            contains the deposit item
     * @throws SWORDException
     *             thrown if error occured during deposit
     * @throws SWORDErrorException
     *             thrown if content inside the deposit contains invalid data
     */
    protected void persistDeposit(Deposit deposit, SWORDEntry responseEntry, StringBuffer verboseInfo, MCRServletJob job) throws SWORDErrorException, SWORDException {

        // Get the filenames
        if (deposit.getFilename() != null) {
            verboseInfo.append(LINE_SEPARATOR + "filename: {" + deposit.getFilename() + "}");
        }
        if (deposit.getSlug() != null) {
            verboseInfo.append(LINE_SEPARATOR + "slug: {" + deposit.getSlug() + "}");
        }

        // package descriptor contains information about all other
        // files/directories inside the uploaded file
        try {

            decompressZipToTemp(deposit, verboseInfo);
            ingest(deposit, responseEntry, verboseInfo, job);

        } catch (IOException ioe) {
            throw new SWORDErrorException(ErrorCodes.ERROR_CONTENT, "Failed to open deposited zip file");
        } finally {
            // remove decompressed files
            Set<File> files = deposit.getExtractedFiles().keySet();
            for (File extractedFile : files) {
                LOG.debug("deleting file: " + extractedFile.getAbsolutePath());
                extractedFile.delete();
            }
        }
    }
    
    /**
     * Performs the deposit for a {@link #METS_PROFILE}. Inside ingest validation of the
     * package profile is done. If valid profile is given a specific implementation of
     * {@link MCRSWORDIngester} is used, which is able to handle the {@link #METS_PROFILE}. 
     */
    @Override
    public void ingest(Deposit deposit, SWORDEntry response, StringBuffer verboseInfo, MCRServletJob job) throws IOException, SWORDException,
            SWORDErrorException {

        File packageDescriptor = null;
        
        Map<File, String> extractedFiles = deposit.getExtractedFiles();
        for (File file : extractedFiles.keySet()) {
            if (extractedFiles.get(file).equals(METS_MANIFEST_FILENAME)) {
                packageDescriptor = file;
                break;
            }
        }
        
        if (packageDescriptor != null) {
            
            // check packaging of descriptor
            validatePackageProfile(packageDescriptor, extractedFiles);
            
            // set package descriptor, so ingester does not need to search for it
            deposit.setPackageDescriptor(packageDescriptor);
            
            // ingest
            MCRSWORDIngester ingester = (MCRSWORDIngester) MCRConfiguration.instance().getSingleInstanceOf("MCR.SWORD.ingester.class");
            ingester.ingest(deposit, response, verboseInfo, job);
        } else {
            verboseInfo.append(LINE_SEPARATOR + "package descriptor " + METS_MANIFEST_FILENAME + " not found.");
            throw new SWORDErrorException(ErrorCodes.ERROR_CONTENT, "Failed to open package description file");
        }
    }

    /**
     * <p>
     * After a successful deposit, a deposit response document must be
     * generated, which is delivered to the client.
     * </p>
     * <p>
     * The resposne can be configured through mycores configuration with the
     * following parameters:
     * <dl>
     * <dt>MCR.SWORD.generator.content</dt>
     * <dd>The generator element is best described in <a
     * href="http://tools.ietf.org/html/rfc4287#section-4.2.4">IETFs
     * documentation</a></dd>
     * <dt>MCR.SWORD.generator.version</dt>
     * <dd>See previous mentioned documentation</dd>
     * <dt>MCR.SWORD.treatment.noop.persisted</dt>
     * <dd>&lt;sword:treatment&gt; contains treatment for a performed noop
     * deposit.</dd>
     * <dt>MCR.SWORD.treatment.persisted</dt>
     * <dd>&lt;sword:treatment&gt; contains treatment for a performed deposit.</dd>
     * </dl>
     * Treatment must be present and contain either a human-readable statement
     * describing treatment the deposited resource has received or a URI that
     * dereferences to such a description.
     * </p>
     * <p>
     * If overridden be sure to set {@link HttpServletResponse#SC_CREATED} to returned
     * response if it was created.
     * </p>
     * 
     * @param verboseInfo
     *            the verbose info which can be used to add info to clients
     *            request
     * @param deposit
     *            the {@link Deposit} to perform
     * @param swordEntry
     *            the {@link SWORDEntry} which should be filled
     * @see <a href="http://www.ukoln.ac.uk/repositories/digirep/index/SWORD_APP_Profile_1.2#Response">Example response</a>
     */
    protected DepositResponse createDepositResponse(StringBuffer verboseInfo, Deposit deposit, SWORDEntry swordEntry) {

        String username = deposit.getUsername();

        // Handle the deposit
        if (!deposit.isNoOp()) {
            counter++;
        }
        DepositResponse dr = new DepositResponse(Deposit.CREATED);

        Title t = new Title();
        t.setContent("SWORD Server Deposit: #" + counter + " of file: " + deposit.getFilename());
        swordEntry.setTitle(t);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'");
        TimeZone utc = TimeZone.getTimeZone("UTC");
        sdf.setTimeZone(utc);
        String milliFormat = sdf.format(new Date());
        swordEntry.setUpdated(milliFormat);

        Author a = new Author();
        if (username != null) {
            a.setName(username);
        } else {
            a.setName("unknown");
        }
        swordEntry.addAuthors(a);

        Generator generator = new Generator();
        generator.setContent(MCRConfiguration.instance().getString("MCR.SWORD.generator.content"));
        generator.setVersion(MCRConfiguration.instance().getString("MCR.SWORD.generator.version"));
        swordEntry.setGenerator(generator);

        String treatment = deposit.isNoOp() ? "MCR.SWORD.treatment.noop.persisted" : "MCR.SWORD.treatment.persisted";
        swordEntry.setTreatment(MCRConfiguration.instance().getString(treatment));

        if (deposit.isVerbose()) {
            swordEntry.setVerboseDescription(verboseInfo.toString());
        }

        swordEntry.setNoOp(deposit.isNoOp());

        dr.setEntry(swordEntry);

        dr.setHttpResponse(HttpServletResponse.SC_CREATED);

        return dr;
    }

    /**
     * Decompresses zip file from given deposit to the temporary upload dir.
     * 
     * @param deposit
     *            object to deposit
     * @param files
     *            should contain an empty not null list, which ist filled during
     *            decompress with all decompressed files
     * @param verboseInfo
     *            should contain a not null <code>StringBuffer</code> where
     *            information can be added to
     * 
     * @throws IOException
     */
    private static void decompressZipToTemp(Deposit deposit, StringBuffer verboseInfo) throws IOException {

        byte[] buffer = new byte[1024];

        // upload is no multipart, so content can be read directly from body
        File file = deposit.getFile();
        ZipInputStream zip = new ZipInputStream(new BufferedInputStream(new CheckedInputStream(new FileInputStream(file), new Adler32())));
        ZipEntry ze;
        
        // configure temp dir
        String tempDirString = MCRConfiguration.instance().getString("MCR.SWORD.temp.upload.dir", System.getProperty("java.io.tmpdir"));
        File tempDir = new File(tempDirString);

        StringBuilder skippedDirectories = new StringBuilder(LINE_SEPARATOR + "skipped directories: {");
        StringBuilder decompressedFiles = new StringBuilder(LINE_SEPARATOR + "decompressed files: {");

        // all files/directories are written to temp directory
        while ((ze = zip.getNextEntry()) != null) {
            if (ze.isDirectory()) {
                skippedDirectories.append(LINE_SEPARATOR + ze.getName());
                continue;
            }

            String filename = ze.getName();
            LOG.debug("zip entry name: " + filename);
            decompressedFiles.append(LINE_SEPARATOR + ze.getName());

            // write temp file
            String filePrefix = filename.substring(0, filename.lastIndexOf("."));
            String fileSuffix = filename.substring(filename.lastIndexOf(".") + 1);
            File tempFile = File.createTempFile(filePrefix, fileSuffix, tempDir);
            FileOutputStream output = new FileOutputStream(tempFile);
            int bytesRead;
            while ((bytesRead = zip.read(buffer)) != -1) {
                output.write(buffer, 0, bytesRead);
            }
            output.close();
            zip.closeEntry();
            deposit.getExtractedFiles().put(tempFile, ze.getName());
        }
        skippedDirectories.append(LINE_SEPARATOR + "}");
        verboseInfo.append(skippedDirectories);

        decompressedFiles.append(LINE_SEPARATOR + "}");
        verboseInfo.append(decompressedFiles);

    }

    /**
     * Validates target mets package descriptor. The following details are
     * validated:
     * <ul>
     * <li><code>PROFILE</code> attribute of root element must match
     * {@link #METS_PROFILE}</li>
     * <li>All files given in mets fileSec must be present if a fileSec is
     * defined.</li>
     * </ul>
     * 
     * @param packageDescriptor
     *            contains the package descriptor in deposited file
     * @param extractedFiles
     *            contains a list with all decompressed files
     * @throws SWORDErrorException
     *             thrown if one of the previous details is invalid
     * @throws SWORDException
     *             thrown if a internal error occured
     */
    protected void validatePackageProfile(File packageDescriptor, Map<File, String> extractedFiles) throws SWORDErrorException, SWORDException {
        SAXBuilder parser = new SAXBuilder();
        try {
            Document document = parser.build(packageDescriptor);
            Element metsElement = document.getRootElement();
            Attribute profile = metsElement.getAttribute("PROFILE");
            if (profile == null || !METS_PROFILE.equals(profile.getValue())) {
                String profileValue = profile == null ? "null" : profile.getValue();
                LOG.error(String.format("throwing error: invalid profile (%1$s)", profileValue));
                throw new SWORDErrorException(ErrorCodes.ERROR_CONTENT, "Invalid profile inside package descriptor. PROFILE must me " + METS_PROFILE);
            }

            // check if listed files in descriptor are available inside the
            // deposit
            Element fileSec = metsElement.getChild("fileSec");
            List<String> originalFilenames = new ArrayList<String>();
            if (fileSec != null) {
                List<Element> fileDefinitions = fileSec.getChild("fileGrp").getChildren("file");
                for (Element file : fileDefinitions) {
                    Element flocat = file.getChild("FLocat");
                    if (flocat != null) {
                        String flocateValue = flocat.getAttributeValue("href", Namespace.getNamespace("xlink", "http://www.w3.org/1999/xlink"));
                        originalFilenames.add(flocateValue);
                    }
                }
            }

            List<String> missingFiles = new ArrayList<String>();
            for (String filename : originalFilenames) {
                String prefix = filename.substring(0, filename.lastIndexOf("."));
                String suffix = filename.substring(filename.lastIndexOf(".") + 1);

                for (File file : extractedFiles.keySet()) {
                    String originalFilename = extractedFiles.get(file);
                    if (!(originalFilename.startsWith(prefix) && originalFilename.endsWith(suffix))) {
                        missingFiles.add(originalFilename);
                    }
                }
            }
            if (!missingFiles.isEmpty()) {
                throw new SWORDErrorException(ErrorCodes.ERROR_CONTENT, "The following file(s) were not found inside zip: " + missingFiles);
            }
        } catch (JDOMException e) {
            throw new SWORDErrorException(ErrorCodes.ERROR_CONTENT, "invalid package descriptor given -> " + e.getMessage());
        } catch (IOException e) {
            throw new SWORDException("package descriptor could not be read");
        }
    }

}
