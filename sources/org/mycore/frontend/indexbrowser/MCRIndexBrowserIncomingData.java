package org.mycore.frontend.indexbrowser;

/**
 * Contains all incoming data from the web browser.
 *
 * @author Matthias Eichner
 */
public class MCRIndexBrowserIncomingData {

    private String index;
    private int from = 0;
    private int to = Integer.MAX_VALUE - 10;
    private StringBuffer path;
    private String search;
    private String mode;
    private boolean init;

    public MCRIndexBrowserIncomingData(String search, String mode, String index, String fromTo, String init) {
        set(search, mode, index, fromTo, init);
    }

    public void set(String search, String mode, String index, String fromTo, String init) {
        this.search = search;
        this.mode = mode;
        this.index = index;
        this.path = new StringBuffer(this.index);
        this.path.append("/");
        if (fromTo != null && fromTo.length() > 0) {
            String from = fromTo.substring(0, fromTo.indexOf("-"));
            String to = fromTo.substring(fromTo.indexOf("-") + 1);
            this.from = Integer.parseInt(from);
            this.to = Integer.parseInt(to);
            updatePath();
        }
        this.init = Boolean.parseBoolean(init);
    }

    private void updatePath() {
        path.append(this.from);
        path.append("-");
        path.append(this.to);
        path.append("/");
    }
    
    public String getIndex() {
        return index;
    }
    public int getFrom() {
        return from;
    }
    public int getTo() {
        return to;
    }
    public String getPath() {
        return path.toString();
    }
    public String getSearch() {
        return search;
    }
    public String getMode() {
        return mode;
    }
    public boolean isInit() {
        return init;
    }
}