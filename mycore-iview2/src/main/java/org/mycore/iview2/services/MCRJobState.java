package org.mycore.iview2.services;

/**
 * Represents the status of tiling jobs
 * @author Thomas Scheffler (yagee)
 *
 */
public enum MCRJobState {
    /**
     * job is added to tiling queue
     */
    NEW('n'),
    /**
     * job is currently being processed by an image tiler
     */
    PROCESSING('p'),
    /**
     * image tiling process is complete
     */
    FINISHED('f'),
    /**
     * image tiling process is complete
     */
    ERROR('e');

    private char status;

    private MCRJobState(char status) {
        this.status = status;
    }

    /**
     * returns character representing the status
     * @return
     * <table>
     *  <caption>returned characters depending on current state</caption>
     *  <tr>
     *   <th>{@link #NEW}</th>
     *   <td>'n'</td>
     *  </tr>
     *  <tr>
     *   <th>{@link #PROCESSING}</th>
     *   <td>'p'</td>
     *  </tr>
     *  <tr>
     *   <th>{@link #FINISHED}</th>
     *   <td>'f'</td>
     *  </tr>
     *  <tr>
     *   <th>{@link #ERROR}</th>
     *   <td>'e'</td>
     *  </tr>
     * </table>
     */
    public char toChar() {
        return status;
    }
}
