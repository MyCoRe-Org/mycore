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

package org.mycore.common;

import java.io.PrintStream;
import java.util.Vector;

public class Table {
    private int currentCol;

    private int[] colSize;

    private int columns;

    Vector<String> values;

    public Table(int columns) {
        currentCol = 0;
        this.columns = columns;
        colSize = new int[columns];
        values = new Vector<>();
    }

    public void addValue(String value) {
        if (value.length() > colSize[currentCol % columns]) {
            colSize[currentCol % columns] = value.length();
        }
        values.add(value);
        currentCol++;
    }

    public void print(PrintStream out) {
        int rows = values.size() / columns;
        StringBuilder sb = new StringBuilder();
        for (int r = 0; r < rows; r++) {
            sb.delete(0, sb.length() + 1);
            for (int c = 0; c < columns; c++) {
                String value = values.get(r * columns + c);
                for (int i = 0; i <= colSize[c] - value.length(); i++) {
                    sb.append(' ');
                }
                sb.append(value);
            }
            out.println(sb);
        }
    }
}
