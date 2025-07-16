/*
 * This file is part of ***  M y C o R e  ***
 * See https://www.mycore.de/ for details.
 *
 * MyCoRe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * MyCoRe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with MyCoRe.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.mycore.common.log;

import java.util.LinkedList;
import java.util.List;
import java.util.Objects;

/**
 * A {@link MCRTreeMessage} is a tree-like data structure that holds string values in
 * its entries that can be rendered as a comprehensible representation of that tree.
 * <p>
 * Intended to create log messages that reveal (deeply) nested data.
 * <p>
 * Example output:
 * <pre>
 * ├─ Foo: foo
 * ├─ Bar:
 * │  ├─ 1: one
 * │  ├─ 2: two
 * │  └─ 3: three
 * └─ Baz:
 *    ├─ 1: one
 *    ├─ 2: two
 *    └─ 3: three
 * </pre>
 */
public final class MCRTreeMessage {

    private final List<Entry> entries = new LinkedList<>();

    public void add(String line) {
        entries.add(new LineEntry(line));
    }

    public void add(String key, String value) {
        entries.add(new LineEntry(key, value));
    }

    public void add(String key, MCRTreeMessage message) {
        entries.add(new NestedEntry(key, message));
    }

    public String logMessage(String introduction) {
        List<String> treeLines = treeLines();
        String separator = System.lineSeparator();
        return treeLines.isEmpty() ? introduction : introduction + separator + String.join(separator, treeLines);
    }

    public List<String> treeLines() {
        List<String> lines = new LinkedList<>();
        treeLines(new LinkedList<>(), lines);
        return lines;
    }

    private void treeLines(List<Indent> prefix, List<String> lines) {
        prefix.add(Indent.LEAD);
        int entriesCount = entries.size();
        for (int i = 0; i < entriesCount; i++) {
            Entry entry = entries.get(i);
            if (i == entriesCount - 1) {
                prefix.removeLast();
                prefix.add(Indent.LAST);
            }
            entry.treeLines(prefix, lines);
        }
        prefix.removeLast();
    }

    private enum Indent {

        LEAD("│  ", "├─ "),

        LAST("   ", "└─ ");

        private final String leadSymbol;

        private final String lastSymbol;

        Indent(String leadSymbol, String lastSymbol) {
            this.leadSymbol = leadSymbol;
            this.lastSymbol = lastSymbol;
        }

    }

    private static abstract class Entry {

        public abstract void treeLines(List<Indent> prefix, List<String> lines);

        protected String prefixString(List<Indent> prefix) {
            StringBuilder builder = new StringBuilder();
            int prefixCount = prefix.size();
            for (int i = 0; i < prefixCount; i++) {
                Indent indent = prefix.get(i);
                String symbol = i == prefixCount - 1 ? indent.lastSymbol : indent.leadSymbol;
                builder.append(symbol);
            }
            return builder.toString();
        }

    }

    private static final class LineEntry extends Entry {

        private final String line;

        private LineEntry(String line) {
            this.line = Objects.requireNonNull(line);
        }

        private LineEntry(String key, String value) {
            this(Objects.requireNonNull(key) + ": " + Objects.requireNonNull(value));
        }

        @Override
        public void treeLines(List<Indent> prefix, List<String> lines) {
            lines.add(prefixString(prefix) + line);
        }

    }

    private static final class NestedEntry extends Entry {

        private final String key;

        private final MCRTreeMessage description;

        private NestedEntry(String key, MCRTreeMessage description) {
            this.key = Objects.requireNonNull(key);
            this.description = Objects.requireNonNull(description);
        }

        @Override
        public void treeLines(List<Indent> prefix, List<String> lines) {
            lines.add(prefixString(prefix) + key + ":");
            description.treeLines(prefix, lines);
        }
    }

}
