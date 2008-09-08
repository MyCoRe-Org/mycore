package org.mycore.frontend.fileupload;

import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.mycore.frontend.servlets.MCRServlet;
import org.mycore.frontend.servlets.MCRServletJob;

public class MCRRemoteFileServlet extends MCRServlet {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    private File f;

    public void doGetPost(MCRServletJob job) throws Exception {
        HttpServletRequest request = job.getRequest();
        HttpServletResponse response = job.getResponse();
        String mode = request.getParameter("method");
        String pathname = request.getParameter("pathname");

        if (pathname != null && !pathname.equals("")) {
            f = new File(pathname);

            /*if (mode.equals("exists")) {
                output(response, exists());
            } else if (mode.equals("isAbsolute")) {
                output(response, isAbsolute());
            } else if (mode.equals("isDirectory")) {
                output(response, isDirectory());
            } else if (mode.equals("isFile")) {
                output(response, isFile());
            } else if (mode.equals("isHidden")) {
                output(response, isHidden());
            } else if (mode.equals("canExecute")) {
                output(response, canExecute());
            } else if (mode.equals("canRead")) {
                output(response, canRead());
            } else if (mode.equals("canWrite")) {
                output(response, canWrite());
            } else if (mode.equals("list")) {
                output(response, list());
            } else if (mode.equals("canExecute")) {
                boolean canExecute = f.canExecute();
                output(response, String.valueOf(canExecute));
            }*/
        } else
            output(response, "Error!");
    }

    private void output(HttpServletResponse response, String string) throws IOException {
        PrintWriter out = response.getWriter();
        out.println(string);
        out.close();
    }

    /*private String canExecute() {
        return String.valueOf(f.canExecute());
    }

    private String canRead() {
        return String.valueOf(f.canRead());
    }

    private String canWrite() {
        return String.valueOf(f.canWrite());
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

    private String exists() {
        return String.valueOf(f.exists());
    }

    public File getAbsoluteFile() {
        return f.getAbsoluteFile();
    }

    public String getAbsolutePath() {
        return f.getAbsolutePath();
    }

    public File getCanonicalFile() throws IOException {
        return f.getCanonicalFile();
    }

    public String getCanonicalPath() throws IOException {
        return f.getCanonicalPath();
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
        return f.getPath();
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

    private String isAbsolute() {
        return String.valueOf(f.isAbsolute());
    }

    private String isDirectory() {
        return String.valueOf(f.isDirectory());
    }

    private String isFile() {
        return String.valueOf(f.isFile());
    }

    private String isHidden() {
        return String.valueOf(f.isHidden());
    }

    public long lastModified() {
        return f.lastModified();
    }

    public long length() {
        return f.length();
    }

    private String list() {
        String[] fileList = f.list();
        StringBuffer strBuffer = new StringBuffer();

        for (String string : fileList) {
            strBuffer.append(string + "\n");
        }

        return strBuffer.toString();
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
