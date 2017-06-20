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
        values = new Vector<String>();
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
            out.println(sb.toString());
        }
    }
}
