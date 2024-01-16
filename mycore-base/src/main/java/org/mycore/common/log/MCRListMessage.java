package org.mycore.common.log;

import java.util.List;

/**
 * A {@link MCRListMessage} is a list-like data structure that holds string values in
 * its entries that can be rendered as a comprehensible representation of that list.
 * <p>
 * Intended to create log messages that reveal linear data.
 * <p>
 * Example output:
 * <pre>
 * ├─ Foo: foo
 * ├─ Bar: bar
 * └─ Baz: baz
 * </pre>
 */
public final class MCRListMessage {

    private final MCRTreeMessage message = new MCRTreeMessage();

    public void add(String key, String value) {
        message.add(key, value);
    }

    public String logMessage(String introduction) {
        return message.logMessage(introduction);
    }

    public List<String> listLines() {
        return message.treeLines();
    }

}
