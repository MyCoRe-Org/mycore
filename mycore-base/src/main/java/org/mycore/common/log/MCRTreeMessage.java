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

    public void add(String key, String value) {
        entries.add(new LineEntry(key, value));
    }

    public void add(String key, MCRTreeMessage description) {
        entries.add(new NestedEntry(key, description));
    }

    public String logMessage(String introduction) {
        List<String> treeLines = treeLines();
        String separator = System.lineSeparator();
        return treeLines.isEmpty() ? introduction : introduction + separator + String.join(separator, treeLines);
    }

    public List<String> treeLines() {
        LinkedList<String> lines = new LinkedList<>();
        treeLines(new LinkedList<>(), lines);
        return lines;
    }

    private void treeLines(List<Indent> prefix, List<String> lines) {
        prefix.add(Indent.LEAD);
        for (int i = 0, n = entries.size(); i < n; i++) {
            Entry entry = entries.get(i);
            if (i == n - 1) {
                prefix.remove(prefix.size() - 1);
                prefix.add(Indent.LAST);
            }
            entry.treeLines(prefix, lines);
        }
        prefix.remove(prefix.size() - 1);
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
            for (int i = 0, n = prefix.size(); i < n; i++) {
                Indent indent = prefix.get(i);
                String symbol = i == n - 1 ? indent.lastSymbol : indent.leadSymbol;
                builder.append(symbol);
            }
            return builder.toString();
        }

    }

    private static class LineEntry extends Entry {

        private final String key;

        private final String value;

        private LineEntry(String key, String value) {
            this.key = Objects.requireNonNull(key);
            this.value = Objects.requireNonNull(value);
        }

        @Override
        public void treeLines(List<Indent> prefix, List<String> lines) {
            lines.add(prefixString(prefix) + key + ": " + value);
        }

    }

    private static class NestedEntry extends Entry {

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
