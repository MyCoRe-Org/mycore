/*
 * This file is part of ***  M y C o R e  ***
 * See http://www.mycore.de/ for details.
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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Function;

/**
 * A {@link MCRTableMessage} is a table-like data structure that can be rendered as a two-dimensional string
 * representation of that table.
 * <p>
 * Intended to create log messages that reveal the construction of tabular data.
 * <p>
 * Example output:
 * <pre>
 * │ Arabic │ Roman │ Englich      │
 * ├────────┼───────┼──────────────┤
 * │ 0      │       │ zero         │
 * │ 1      │ I     │ one          │
 * │ 23     │ XXIII │ twenty-three │
 * │ 42     │ XLII  │ forty-two    │
 * </pre>
 */
public final class MCRTableMessage<T> {

    private final List<Column<? super T>> columns;

    private final List<T> rows = new LinkedList<>();

    @SafeVarargs
    public MCRTableMessage(Column<? super T>... columns) {
        this(Arrays.asList(columns));
    }

    public MCRTableMessage(List<Column<? super T>> columns) {
        this.columns = new ArrayList<>(Objects.requireNonNull(columns));
        this.columns.forEach(Objects::requireNonNull);
    }

    public String logMessage(String introduction) {
        List<String> tableLines = tableLines();
        String separator = System.lineSeparator();
        return columns.isEmpty() ? introduction : introduction + separator + String.join(separator, tableLines);
    }

    public void add(T row) {
        rows.add(Objects.requireNonNull(row));
    }

    public List<String> tableLines() {
        LinkedList<String> lines = new LinkedList<>();

        int[] lengths = new int[columns.size()];
        String[] names = new String[columns.size()];
        String[][] values = new String[rows.size()][columns.size()];

        addNames(names, lengths);
        addValues(values, lengths);

        lines.add(rowLine(names, lengths));
        lines.add(separatorLine(lengths));
        for (int r = 0; r < rows.size(); r++) {
            lines.add(rowLine(values[r], lengths));
        }

        return lines;
    }

    private void addNames(String[] names, int[] lengths) {
        for (int c = 0; c < columns.size(); c++) {
            Column<? super T> column = columns.get(c);
            String name = column.name;
            lengths[c] = name.length();
            names[c] = name;
        }
    }

    private void addValues(String[][] values, int[] lengths) {
        for (int r = 0; r < rows.size(); r++) {
            T row = rows.get(r);
            for (int c = 0; c < columns.size(); c++) {
                Column<? super T> column = columns.get(c);
                String value = stringValue(column, row);
                lengths[c] = Math.max(lengths[c], value.length());
                values[r][c] = value;
            }
        }
    }

    private String stringValue(Column<? super T> column, T row) {
        return stringValue(column.mapper.apply(row));
    }

    private String stringValue(Object value) {
        if (value == null) {
            return "";
        } else if (value instanceof String string) {
            return string;
        } else if (value instanceof Optional<?> optional) {
            return optional.map(this::stringValue).orElse("");
        } else {
            return value.toString();
        }
    }

    private String rowLine(String[] values, int[] lengths) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < values.length; i++) {
            String value = values[i];
            builder.append('│');
            builder.append(' ');
            builder.append(value);
            builder.append(" ".repeat(lengths[i] - value.length()));
            builder.append(' ');
        }
        builder.append('│');
        return builder.toString();
    }

    private String separatorLine(int[] lengths) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < lengths.length; i++) {
            builder.append(i == 0 ? '├' : '┼');
            builder.append('─');
            builder.append("─".repeat(lengths[i]));
            builder.append('─');

        }
        builder.append('┤');
        return builder.toString();
    }

    public record Column<T>(String name, Function<T, Object> mapper) {

        public Column(String name, Function<T, Object> mapper) {
            this.name = Objects.requireNonNull(name);
            this.mapper = Objects.requireNonNull(mapper);
        }

    }

}
