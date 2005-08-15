package wcms.util;

import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class HashCipher {

	/** Creates a new instance of HashCipher */
	public HashCipher() {
	}

	public static final void main(String args[]) throws IOException,
			NoSuchAlgorithmException, ArrayIndexOutOfBoundsException {
		try {
			//System.out.println(crypt(args[0]));
		} catch (ArrayIndexOutOfBoundsException aioobe) {
			//System.out.println("Usage: java wcms.util.HashCipher
			// <\"PasswordString\">");
			//System.out.println("Example: java wcms.util.HashCipher \"tESt
			// &58y\"");
		}
	}

	public static String crypt(String string) throws IOException,
			NoSuchAlgorithmException {
		byte[] buffer = new byte[12];
		//DigestInputStream dis = new DigestInputStream(new
		// InputStream.getInstance());
		buffer = string.getBytes();
		String outstr = new String(hash(buffer));
		return outstr;
	}

	private static String hash(byte[] buffer) throws NoSuchAlgorithmException {
		String s = "";
		MessageDigest md = MessageDigest.getInstance("MD5");
		md.update(buffer);
		buffer = md.digest(buffer);
		for (int i = 0; i < buffer.length; i++) {
			s += Integer.toHexString(buffer[i] & 0xFF);
			//System.out.print( (s.length() == 1 ) ? "0"+s : s );
		}
		return s;
	}
}
