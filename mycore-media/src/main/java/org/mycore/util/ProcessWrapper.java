package org.mycore.util;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * Wrapper for running processes.
 * @author Reuben Firmin
 */
public class ProcessWrapper {

    private BufferedStreamReader err;
    private BufferedStreamReader out;

    /**
     * Default constructor.
     */
    public ProcessWrapper() {
    }

    /**
     * Run the given command. Blocks until the command is finished, and returns the error/exit code from the command.
     * @param command The list of command & args, the first of which is expected to be the executable.
     *  E.g. "ls" "-ax" "/tmp".
     * @return The exit code. 0 indicates normal completion.
     */
    public final int runCommand(final String... command)
        throws IOException, InterruptedException
    {
        final ProcessBuilder pb = new ProcessBuilder(command);
        final Process p = pb.start();
        err = new BufferedStreamReader(p.getErrorStream());
        out = new BufferedStreamReader(p.getInputStream());
        final Thread t1 = new Thread(err);
        final Thread t2 = new Thread(out);
        t1.start();
        t2.start();
        // blocks until process is done
        return p.waitFor();
    }

    /**
     * The stdout from the command. Will throw NPE if runCommand isn't run first.
     * @return Never null
     */
    public final String getStdOut() {
        return out.getBuffer();
    }

    /**
     * The stdout from the command. Will throw NPE if runCommand isn't run first.
     * @return Never null
     */
    public final String getStdErr() {
        return err.getBuffer();
    }

    /**
     * Class to collate the output from a stream into a buffer. The buffer may be read at any time.
     * @author Reuben Firmin
     */
    private static class BufferedStreamReader implements Runnable {
        private InputStream is;
        private StringBuffer buffer = new StringBuffer();

        /**
         * The inputstream to read from.
         * @param is not null
         */
        public BufferedStreamReader(final InputStream is) {
            this.is = is;
        }

        /**
         * The output from the stream.
         * @return Never null
         */
        public final String getBuffer() {
            return buffer.toString();
        }

        /**
         * {@inheritDoc}
         */
        public final void run() {
            try {
                final BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                String data;
                while ((data = reader.readLine()) != null) {
                    buffer.append(data + "\n");
                }
                reader.close();
            } catch (IOException ioEx) {
                ioEx.printStackTrace();
            }
        }
    }
}