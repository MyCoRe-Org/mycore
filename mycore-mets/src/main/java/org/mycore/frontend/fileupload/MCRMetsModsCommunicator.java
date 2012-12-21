package org.mycore.frontend.fileupload;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.Socket;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Stack;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.Deflater;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class MCRMetsModsCommunicator {

    protected String url;
    protected String uid;
    protected final static int bufferSize = 65536; // 64 KByte

    public MCRMetsModsCommunicator(String url, String uploadId) {
        this.url = url;
        this.uid = uploadId;
    }

    public void uploadMets(String mets_string) {
        try {
            startUploadSession(mets_string.length());
            uploadFile("mets.xml", mets_string);
            endUploadSession();
        } catch (Exception e) {
            e.printStackTrace(System.out);
        }
    }

    protected long countTotalBytes(Vector files) {
        long total = 0;

        for (Object file : files) total += ((File) file).length();

        return total;
    }

    public void uploadFile(String path, String mets) throws Exception {
        byte[] metsbytes = mets.getBytes("UTF-8");
        System.out.println("--- Starting filetransfer ---");
        String md5 = buildMD5StringByString(metsbytes);
        System.out.println("MD5 checksum is " + md5);

        Hashtable<String, Object> request = new Hashtable<String, Object>();
        request.put("md5", md5);
        request.put("method", "uploadFile");
        request.put("path", path);
        request.put("length", String.valueOf(metsbytes.length));
        System.out.println("File length is " + metsbytes.length);

        System.out.println("Sending filename to server: " + path);

        String reply = (String) (send(request));
        System.out.println("Received reply from server: " + reply);

        if ("skip file".equals(reply)) {
            System.out.println("File skipped.");
            return;
        }

        StringTokenizer st = new StringTokenizer(reply, ":");
        String host = st.nextToken();
        int port = Integer.parseInt(st.nextToken());
        System.out.println("Server says we should connect to " + host + ":" + port);

        System.out.println("Trying to create client socket...");

        Socket socket = new Socket(host, port);
        socket.setReceiveBufferSize(Math.max(socket.getReceiveBufferSize(), bufferSize));
        socket.setSendBufferSize(Math.max(socket.getSendBufferSize(), bufferSize));

        System.out.println("Socket created, connected to server.");
        System.out.println("Socket send buffer size is " + socket.getSendBufferSize());

        ZipOutputStream zos = new ZipOutputStream(socket.getOutputStream());
        DataInputStream din = new DataInputStream(socket.getInputStream());

        // Large files like video already are compressed somehow
        zos.setLevel(Deflater.NO_COMPRESSION);

        ZipEntry ze = new ZipEntry(java.net.URLEncoder.encode(path, "UTF-8"));
        StringBuilder extra = new StringBuilder();
        extra.append(md5).append(" ").append(metsbytes.length).append(" ").append(uid);
        ze.setExtra(extra.toString().getBytes("UTF-8"));
        zos.putNextEntry(ze);

        int num = 0;
        long sended = 0;
        byte[] buffer = new byte[bufferSize];

        System.out.println("Starting to send file content...");

        ByteArrayInputStream bais = new ByteArrayInputStream(metsbytes);

        InputStream source = new BufferedInputStream(bais, buffer.length);

        long lastPing = System.currentTimeMillis();
        while ((num = source.read(buffer)) != -1) {

            zos.write(buffer, 0, num);
            sended += num;

            // Send a "ping" to MCRUploadServlet so that server keeps HTTP Session alive
            if ((System.currentTimeMillis() - lastPing) > 10000) {
                lastPing = System.currentTimeMillis();
                Hashtable<String, Object> ping = new Hashtable<String, Object>();
                ping.put("method", "ping");
                System.out.println("Sending ping to servlet...");
                String pong = (String) (send(ping));
                System.out.println("Server responded with " + pong);
            }
        }

        zos.closeEntry();
        zos.flush();
        System.out.println("Releasing file: mets.xml");
        source.close();
        System.out.println("Finished sending file content.");

        long numBytesStored = din.readLong();
        System.out.println("Server reports that " + numBytesStored + " bytes have been stored.");

        socket.close();

        System.out.println("Socket closed, file transfer successfully completed.");
    }

    /**
     * Creates a list of all files in the given directories
     * 
     * @param selectedFiles
     *            list of selected files or directories from filechooser
     */
    protected Vector[] listFiles(File[] selectedFiles) throws Exception {
        Vector[] list = new Vector[2];
        list[0] = new Vector();
        list[1] = new Vector();

        if ((null == selectedFiles) || (0 == selectedFiles.length)) {
            return list;
        }

        for (File selectedFile : selectedFiles) {
            File f = selectedFile;

            if (!f.exists()) {
                throw new FileNotFoundException("Datei oder Verzeichnis " + f.getPath() + " nicht gefunden!");
            }

            if (!f.canRead()) {
                throw new IOException("Datei oder Verzeichnis " + f.getPath() + " nicht lesbar!");
            }

            if (f.isFile()) {
                list[0].addElement(f);
                list[1].addElement(f.getName());
            } else {
                Stack dirStack = new Stack();
                Stack baseStack = new Stack();

                dirStack.push(f);
                baseStack.push(f.getName() + "/");

                while (!dirStack.empty()) {
                    File dir = (File) (dirStack.pop());
                    String base = (String) (baseStack.pop());

                    String[] files = dir.list();

                    for (String file : files) {
                        f = new File(dir, file);

                        if (f.isFile()) {
                            list[0].addElement(f);
                            list[1].addElement(base + file);
                        } else {
                            dirStack.push(f);
                            baseStack.push(base + file + "/");
                        }
                    }
                }
            }
        }

        return list;
    }

    protected void startUploadSession(int numFiles) throws IOException, MCRUploadException {
        Hashtable<String, Object> request = new Hashtable<String, Object>();
        request.put("method", "startUploadSession");
        request.put("numFiles", String.valueOf(numFiles));
        send(request);
    }

    protected void endUploadSession() throws IOException, MCRUploadException {
        Hashtable<String, Object> request = new Hashtable<String, Object>();
        request.put("method", "endUploadSession");
        send(request);
    }

    protected void cancelUploadSession() throws IOException, MCRUploadException {
        Hashtable<String, Object> request = new Hashtable<String, Object>();
        request.put("method", "cancelUploadSession");
        send(request);
    }

    protected Object send(Hashtable parameters) throws IOException, MCRUploadException {
        parameters.put("uploadId", uid);
        Hashtable response = getResponse(doPost(parameters));
        return response.get("return");
    }

    protected InputStream doPost(Hashtable parameters) throws IOException {
        String data = encodeParameters(parameters);
        String mime = "application/x-www-form-urlencoded";

        URLConnection connection = null;

        try {
            connection = new URL(url).openConnection();
        } catch (MalformedURLException ignored) {
            return null;
        } // will never happen if base URL is ok

        connection.setDoInput(true);
        connection.setDoOutput(true);
        connection.setUseCaches(false);
        connection.setDefaultUseCaches(false);
        connection.setRequestProperty("Content-type", mime);
        connection.setRequestProperty("Content-length", String.valueOf(data.length()));

        DataOutputStream out = new DataOutputStream(connection.getOutputStream());
        out.writeBytes(data);
        out.flush();
        out.close();

        return connection.getInputStream();
    }

    protected String encodeParameters(Hashtable parameters) {
        StringBuilder data = new StringBuilder();
        Enumeration e = parameters.keys();

        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            String value = (String) parameters.get(name);

            try {
                data.append(URLEncoder.encode(name, "UTF-8")).append("=").append(URLEncoder.encode(value, "UTF-8")).append("&");
            } catch (UnsupportedEncodingException ex) {
                System.out.println(ex.getClass().getName());
                System.out.println(ex.getMessage());
                throw new RuntimeException("Could not encode parameters");
            }
        }

        data.setLength(data.length() - 1);

        return data.toString();
    }

    protected Hashtable getResponse(InputStream is) throws IOException, MCRUploadException {
        DataInputStream dis = new DataInputStream(is);
        String mime = dis.readUTF();
        byte[] dummy = new byte[0];

        Hashtable response = new Hashtable();

        while (dis.read(dummy, 0, 0) != -1) {
            String key = dis.readUTF();
            String clname = dis.readUTF();
            Object value = null;

            if (clname.equals(String.class.getName())) {
                value = dis.readUTF();
            } else if (clname.equals(Integer.class.getName())) {
                value = dis.readInt();
            } else {
                value = dis.readUTF();
            }

            response.put(key, value);
        }

        if (mime.equals("upload/exception")) {
            String clname = (String) (response.get("clname"));
            String message = (String) (response.get("message"));
            String strace = (String) (response.get("strace"));
            throw new MCRUploadException(clname, message, strace);
        }

        return response;
    }

    /** Calculates the MD5 checksum of the given mets file * */
    protected String buildMD5StringByString(byte[] metsbytes) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("MD5");

        InputStream fis = new ByteArrayInputStream(metsbytes);
        BufferedInputStream bis = new BufferedInputStream(fis, bufferSize);
        DigestInputStream in = new DigestInputStream(bis, digest);

        byte[] buffer = new byte[bufferSize];

        while (in.read(buffer, 0, buffer.length) != -1)
            ;

        in.close();

        byte[] bytes = digest.digest();
        StringBuilder sb = new StringBuilder();

        for (byte aByte : bytes) {
            String sValue = "0" + Integer.toHexString(aByte);
            sb.append(sValue.substring(sValue.length() - 2));
        }

        return sb.toString();
    }

}
