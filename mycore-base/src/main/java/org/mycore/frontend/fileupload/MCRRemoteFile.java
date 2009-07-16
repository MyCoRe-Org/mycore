package org.mycore.frontend.fileupload;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.StringTokenizer;

public class MCRRemoteFile extends File {
    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private String remoteServletURL;
    private String path;

    File f;

    public MCRRemoteFile(String url, String pathname) {
        super("");
        this.remoteServletURL = url;
        this.path = pathname;

    }

    private String callServlet(Map paramMap) {
        String paramStr = createParamString(paramMap);
        String inputLine = null;
        StringBuffer strBuffer = new StringBuffer();

        try {
            URL servletURL = new URL(remoteServletURL + paramStr);
            URLConnection connection = servletURL.openConnection();

            connection.setUseCaches(false);
            BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

            while ((inputLine = in.readLine()) != null)
                strBuffer.append(inputLine);
            in.close();

        } catch (MalformedURLException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

        return strBuffer.toString();
    }

    private String createParamString(Map paramMap) {
        StringBuffer strBuffer = new StringBuffer();

        for (Iterator iterator = paramMap.keySet().iterator(); iterator.hasNext();) {
            String key = (String) iterator.next();
            String value = (String) paramMap.get(key);

            String symb = strBuffer.indexOf("?") < 0 ? "?" : "&";

            strBuffer.append(symb + key + "=" + value);
        }
        return strBuffer.toString();
    }

    private boolean remoteCheck(String remoteMethod) {
        HashMap paramMap = new HashMap();
        paramMap.put("pathname", path);
        paramMap.put("method", remoteMethod);

        boolean remoteCheck = Boolean.parseBoolean(callServlet(paramMap));
        return remoteCheck;
    }

    /*public boolean canExecute() {
        return remoteCheck("canExecute");
    }

    public boolean canRead() {
        return remoteCheck("canRead");
    }

    public boolean canWrite() {
        return remoteCheck("canWrite");
    }

    public int compareTo(File pathname) {
        return f.compareTo(pathname);
    }

    public boolean createNewFile() throws IOException {
        return f.createNewFile();
    }

    public boolean delete() {
        return f.delete();
    }

    public void deleteOnExit() {
        f.deleteOnExit();
    }

    public boolean equals(Object obj) {
        return f.equals(obj);
    }

    public boolean exists() {
        boolean exists = remoteCheck("exists");
        return exists;
    }

    public File getAbsoluteFile() {
        return f.getAbsoluteFile();
    }

    public String getAbsolutePath() {
        return f.getAbsolutePath();
    }

    public File getCanonicalFile() throws IOException {
        String canonPath = getCanonicalPath();
        return f.getCanonicalFile();
    }

    public String getCanonicalPath() throws IOException {
        return remoteDo("getCanonicalPath");
    }

    private String remoteDo(String method) {
        HashMap paramMap = new HashMap();
        paramMap.put("pathname", path);
        paramMap.put("method", method);

        return callServlet(paramMap);
    }

    public long getFreeSpace() {
        return f.getFreeSpace();
    }

    public String getName() {
        return f.getName();
    }

    public String getParent() {
        return f.getParent();
    }

    public File getParentFile() {
        return f.getParentFile();
    }

    public String getPath() {
        return this.path;
    }

    public long getTotalSpace() {
        return f.getTotalSpace();
    }

    public long getUsableSpace() {
        return f.getUsableSpace();
    }

    public int hashCode() {
        return f.hashCode();
    }

    public boolean isAbsolute() {
        return remoteCheck("isAbsolute");
    }

    public boolean isDirectory() {
        return remoteCheck("isDirectory");
    }

    public boolean isFile() {
        return remoteCheck("isFile");
    }

    public boolean isHidden() {
        return remoteCheck("isHidden");
    }

    public long lastModified() {
        return f.lastModified();
    }

    public long length() {
        return f.length();
    }

    public String[] list() {
        HashMap paramMap = new HashMap();
        paramMap.put("pathname", path);
        paramMap.put("method", "list");

        String fileList = callServlet(paramMap);

        return fileList.split("\n");
    }

    public String[] list(FilenameFilter filter) {
        return f.list(filter);
    }

    public File[] listFiles() {
        return f.listFiles();
    }

    public File[] listFiles(FileFilter filter) {
        return f.listFiles(filter);
    }

    public File[] listFiles(FilenameFilter filter) {
        return f.listFiles(filter);
    }

    public boolean mkdir() {
        return f.mkdir();
    }

    public boolean mkdirs() {
        return f.mkdirs();
    }

    public boolean renameTo(File dest) {
        return f.renameTo(dest);
    }

    public boolean setExecutable(boolean executable, boolean ownerOnly) {
        return f.setExecutable(executable, ownerOnly);
    }

    public boolean setExecutable(boolean executable) {
        return f.setExecutable(executable);
    }

    public boolean setLastModified(long time) {
        return f.setLastModified(time);
    }

    public boolean setReadable(boolean readable, boolean ownerOnly) {
        return f.setReadable(readable, ownerOnly);
    }

    public boolean setReadable(boolean readable) {
        return f.setReadable(readable);
    }

    public boolean setReadOnly() {
        return f.setReadOnly();
    }

    public boolean setWritable(boolean writable, boolean ownerOnly) {
        return f.setWritable(writable, ownerOnly);
    }

    public boolean setWritable(boolean writable) {
        return f.setWritable(writable);
    }

    public String toString() {
        return f.toString();
    }

    public URI toURI() {
        return f.toURI();
    }

    public URL toURL() throws MalformedURLException {
        return f.toURL();
    }*/

}
