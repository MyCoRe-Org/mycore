package org.mycore.frontend.cli;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.MessageFormat;
import java.util.Map;

import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerFactoryConfigurationError;
import javax.xml.transform.sax.SAXTransformerFactory;
import javax.xml.transform.sax.TransformerHandler;
import javax.xml.transform.stream.StreamResult;

import org.apache.log4j.Logger;
import org.hibernate.Criteria;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.mycore.backend.filesystem.MCRCStoreVFS;
import org.mycore.backend.hibernate.MCRHIBConnection;
import org.mycore.backend.hibernate.tables.MCRFSNODES;
import org.mycore.common.MCRConfiguration;
import org.mycore.datamodel.ifs.MCRContentStore;
import org.mycore.datamodel.ifs.MCRContentStoreFactory;
import org.mycore.frontend.cli.annotation.MCRCommand;
import org.xml.sax.SAXException;
import org.xml.sax.ext.Attributes2Impl;

public class MCRIFSCommands {
    private static Logger LOGGER = Logger.getLogger(MCRIFSCommands.class);

    private static abstract class FSNodeChecker {
        public abstract String getName();

        public abstract boolean checkNode(MCRFSNODES node, File localFile, Attributes2Impl atts);

        void addBaseAttributes(MCRFSNODES node, Attributes2Impl atts) {
            atts.clear();
            atts.addAttribute(nsURI, ATT_SIZE, ATT_SIZE, ATT_TYPE, Long.toString(node.getSize()));
            atts.addAttribute(nsURI, ATT_MD5, ATT_MD5, ATT_TYPE, node.getMd5());
            atts.addAttribute(nsURI, ATT_STORAGEID, ATT_STORAGEID, ATT_TYPE, node.getStorageid());
            atts.addAttribute(nsURI, ATT_OWNER, ATT_OWNER, ATT_TYPE, node.getOwner());
            atts.addAttribute(nsURI, ATT_NAME, ATT_NAME, ATT_TYPE, node.getName());
        }

        final static String nsURI = "";

        final static String ATT_TYPE = "CDATA";

        final static String ATT_STORAGEID = "storageid";

        final static String ATT_OWNER = "owner";

        final static String ATT_NAME = "fileName";

        final static String ATT_MD5 = "md5";

        final static String ATT_SIZE = "size";

    }

    private static class LocalFileExistChecker extends FSNodeChecker {
        @Override
        public String getName() {
            return "missing";
        }

        @Override
        public boolean checkNode(MCRFSNODES node, File localFile, Attributes2Impl atts) {
            if (localFile.exists()) {
                return true;
            }
            LOGGER.warn("File is missing: " + localFile);
            addBaseAttributes(node, atts);
            return false;
        }

    }

    private static final class MD5Checker extends LocalFileExistChecker {
        @Override
        public String getName() {
            return "md5";
        }

        @Override
        public boolean checkNode(MCRFSNODES node, File localFile, Attributes2Impl atts) {
            if (!super.checkNode(node, localFile, atts)) {
                atts.addAttribute(nsURI, super.getName(), super.getName(), ATT_TYPE, "true");
                return false;
            }
            addBaseAttributes(node, atts);
            if (localFile.length() != node.getSize()) {
                LOGGER.warn("File size does not match for file: " + localFile);
                atts.addAttribute(nsURI, "actualSize", "actualSize", ATT_TYPE, Long.toString(localFile.length()));
                return false;
            }
            //we can check MD5Sum
            FileInputStream fileInputStream;
            try {
                fileInputStream = new FileInputStream(localFile);
            } catch (FileNotFoundException e1) {
                //should not happen as we check it before
                LOGGER.warn(e1);
                return false;
            }
            byte[] digest;
            try {
                byte[] buffer = new byte[4096];
                MessageDigest md5Digest = MessageDigest.getInstance("MD5");
                int numRead;
                do {
                    numRead = fileInputStream.read(buffer);
                    if (numRead > 0) {
                        md5Digest.update(buffer, 0, numRead);
                    }
                } while (numRead != -1);
                digest = md5Digest.digest();
            } catch (NoSuchAlgorithmException e) {
                LOGGER.error(e);
                return false;
            } catch (IOException e) {
                LOGGER.error(e);
                return false;
            } finally {
                try {
                    fileInputStream.close();
                } catch (IOException e) {
                    LOGGER.warn("Could not clode file handle: " + localFile);
                }
            }
            StringBuilder md5SumBuilder = new StringBuilder();
            for (byte b : digest) {
                md5SumBuilder.append(Integer.toString((b & 0xff) + 0x100, 16).substring(1));
            }
            String md5Sum = md5SumBuilder.toString();
            if (md5Sum.equals(node.getMd5())) {
                return true;
            }
            LOGGER.warn("MD5 sum does not match for file: " + localFile);
            atts.addAttribute(nsURI, "actualMD5", "actualMD5", ATT_TYPE, md5Sum);
            return false;
        }
    }

    @MCRCommand(syntax = "generate md5sum files in directory {0}", help = "writes md5sum files for every content store in directory {0}")
    public static void writeMD5SumFile(String targetDirectory) throws IOException {
        File targetDir = getDirectory(targetDirectory);
        Session session = MCRHIBConnection.instance().getSession();
        Criteria criteria = session.createCriteria(MCRFSNODES.class);
        criteria.addOrder(Order.asc("storeid"));
        criteria.addOrder(Order.asc("storageid"));
        criteria.add(Restrictions.eq("type", "F"));
        ScrollableResults fsnodes = criteria.scroll(ScrollMode.FORWARD_ONLY);
        Map<String, MCRContentStore> availableStores = MCRContentStoreFactory.getAvailableStores();
        String currentStoreId = null;
        MCRContentStore currentStore = null;
        File currentStoreBaseDir = null;
        FileWriter fw = null;
        String nameOfProject = MCRConfiguration.instance().getString("MCR.NameOfProject", "MyCoRe");
        try {
            while (fsnodes.next()) {
                MCRFSNODES fsNode = (MCRFSNODES) fsnodes.get(0);
                String storeID = fsNode.getStoreid();
                String storageID = fsNode.getStorageid();
                String md5 = fsNode.getMd5();
                session.evict(fsNode);
                if (!storeID.equals(currentStoreId)) {
                    //initialize current store
                    currentStoreId = storeID;
                    currentStore = availableStores.get(storeID);
                    if (fw != null) {
                        fw.close();
                    }
                    File outputFile = new File(targetDir, MessageFormat.format("{0}-{1}.md5", nameOfProject, storeID));
                    LOGGER.info("Writing to file: " + outputFile.getAbsolutePath());
                    fw = new FileWriter(outputFile);
                    if (currentStore instanceof MCRCStoreVFS) {
                        try {
                            currentStoreBaseDir = ((MCRCStoreVFS) currentStore).getBaseDir();
                        } catch (Exception e) {
                            LOGGER.warn("Could not get baseDir of store: " + storeID, e);
                            currentStoreBaseDir = null;
                        }
                    } else {
                        currentStoreBaseDir = null;
                    }
                }
                String path = currentStoreBaseDir != null ? new File(currentStoreBaseDir, storageID).getAbsolutePath() : storageID;
                //current store initialized
                String line = MessageFormat.format("{0}  {1}\n", md5, path);
                fw.write(line);
            }
        } finally {
            if (fw != null) {
                try {
                    fw.close();
                } catch (IOException e1) {
                    LOGGER.warn("Error while closing file.", e1);
                }
            }
            session.clear();
        }
    }

    @MCRCommand(syntax = "generate missing file report in directory {0}", help = "Writes XML report about missing files in directory {0}")
    public static void writeMissingFileReport(String targetDirectory) throws IOException, SAXException, TransformerConfigurationException {
        File targetDir = getDirectory(targetDirectory);
        FSNodeChecker checker = new LocalFileExistChecker();
        writeReport(targetDir, checker);
    }

    @MCRCommand(syntax = "generate md5 file report in directory {0}", help = "Writes XML report about failed md5 checks in directory {0}")
    public static void writeFileMD5Report(String targetDirectory) throws IOException, SAXException, TransformerConfigurationException {
        File targetDir = getDirectory(targetDirectory);
        FSNodeChecker checker = new MD5Checker();
        writeReport(targetDir, checker);
    }

    private static void writeReport(File targetDir, FSNodeChecker checker) throws TransformerFactoryConfigurationError, SAXException, IOException,
        FileNotFoundException, TransformerConfigurationException {
        Session session = MCRHIBConnection.instance().getSession();
        Criteria criteria = session.createCriteria(MCRFSNODES.class);
        criteria.addOrder(Order.asc("storeid"));
        criteria.addOrder(Order.asc("owner"));
        criteria.addOrder(Order.asc("name"));
        criteria.add(Restrictions.eq("type", "F"));
        ScrollableResults fsnodes = criteria.scroll(ScrollMode.FORWARD_ONLY);
        Map<String, MCRContentStore> availableStores = MCRContentStoreFactory.getAvailableStores();
        String currentStoreId = null;
        MCRContentStore currentStore = null;
        File currentStoreBaseDir = null;
        StreamResult streamResult = null;
        String nameOfProject = MCRConfiguration.instance().getString("MCR.NameOfProject", "MyCoRe");
        SAXTransformerFactory tf = (SAXTransformerFactory) SAXTransformerFactory.newInstance();
        TransformerHandler th = null;
        Attributes2Impl atts = new Attributes2Impl();
        final String rootName = checker.getName();
        final String elementName = "file";
        final String ATT_BASEDIR = "basedir";
        final String nsURI = "";
        final String ATT_TYPE = "CDATA";
        String owner = null;

        try {
            while (fsnodes.next()) {
                MCRFSNODES fsNode = (MCRFSNODES) fsnodes.get(0);
                String storeID = fsNode.getStoreid();
                String storageID = fsNode.getStorageid();
                session.evict(fsNode);
                if (!storeID.equals(currentStoreId)) {
                    //initialize current store
                    currentStoreId = storeID;
                    currentStore = availableStores.get(storeID);
                    if (th != null) {
                        th.endElement(nsURI, rootName, rootName);
                        th.endDocument();
                        OutputStream outputStream = streamResult.getOutputStream();
                        if (outputStream != null) {
                            outputStream.close();
                        }
                    }
                    File outputFile = new File(targetDir, MessageFormat.format("{0}-{1}-{2}.xml", nameOfProject, storeID, rootName));
                    streamResult = new StreamResult(new FileOutputStream(outputFile));
                    th = tf.newTransformerHandler();
                    Transformer serializer = th.getTransformer();
                    serializer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
                    serializer.setOutputProperty(OutputKeys.INDENT, "yes");
                    th.setResult(streamResult);
                    LOGGER.info("Writing to file: " + outputFile.getAbsolutePath());
                    th.startDocument();
                    atts.clear();
                    atts.addAttribute(nsURI, "project", "project", ATT_TYPE, nameOfProject);
                    if (currentStore instanceof MCRCStoreVFS) {
                        try {
                            currentStoreBaseDir = ((MCRCStoreVFS) currentStore).getBaseDir();
                            atts.addAttribute(nsURI, ATT_BASEDIR, ATT_BASEDIR, ATT_TYPE, currentStoreBaseDir.getAbsolutePath());
                        } catch (Exception e) {
                            LOGGER.warn("Could not get baseDir of store: " + storeID, e);
                            currentStoreBaseDir = null;
                        }
                    } else {
                        currentStoreBaseDir = null;
                    }
                    th.startElement(nsURI, rootName, rootName, atts);
                }
                if (currentStoreBaseDir == null) {
                    continue;
                }
                if (!fsNode.getOwner().equals(owner)) {
                    owner = fsNode.getOwner();
                    LOGGER.info("Checking owner/derivate: " + owner);
                }
                File f = new File(currentStoreBaseDir, storageID);
                if (!checker.checkNode(fsNode, f, atts)) {
                    th.startElement(nsURI, elementName, elementName, atts);
                    th.endElement(nsURI, elementName, elementName);
                }
            }
        } finally {
            session.clear();
            if (th != null) {
                try {
                    th.endElement(nsURI, rootName, rootName);
                    th.endDocument();
                    OutputStream outputStream = streamResult.getOutputStream();
                    if (outputStream != null) {
                        outputStream.close();
                    }
                } catch (IOException e1) {
                    LOGGER.warn("Error while closing file.", e1);
                }
            }
        }
    }

    /**
     * @param targetDirectory
     * @return
     */
    static File getDirectory(String targetDirectory) {
        File targetDir = new File(targetDirectory);
        if (!targetDir.isDirectory()) {
            throw new IllegalArgumentException("Target directory " + targetDir.getAbsolutePath() + " is not a directory.");
        }
        return targetDir;
    }

}
