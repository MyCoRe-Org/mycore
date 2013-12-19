/**
 * 
 */
package org.mycore.handle;

import java.util.zip.CRC32;
import java.util.zip.Checksum;

import org.mycore.services.urn.MCRIConcordanceTable;


/**
 * Class provides functionality for calculating the checksum of a {@link MCRHandle}. 
 * 
 * @author shermann
 */
public class MCRGbvHandleChecksumProvider {

    /**
     * Calculates the checksum of the given handle. 
     * 
     * @param handle
     * @return
     */
    public int checksum(MCRHandle handle) {
        String combine = MCRGbvHandleProvider.URN_PREFIX + handle.getLocalName();

        byte[] bytes = combine.getBytes();

        Checksum chk = new CRC32();
        chk.update(bytes, 0, bytes.length);

        return Math.abs((int) chk.getValue() % 10);
    }

    /**
     * Calculates the checksum of the given handle as described in {@link http://www.persistent-identifier.de/?link=316}
     * 
     * @param handle
     * @return the single digit checksum for this handle
     */
    public int checksum2(MCRHandle handle) {
        String handleAsURN = MCRGbvHandleProvider.URN_PREFIX + handle.getLocalName();

        /*
         * get the String representation of this urn and split it. Every single
         * part of the string is one element in the array
         */
        String[] urn = handleAsURN.split("");

        /* Convert the String into an integer representation */
        StringBuilder sourceURNConverted = new StringBuilder();
        for (int i = 1; i < urn.length; i++) {
            sourceURNConverted.append(getIntegerAlias(urn[i]));
        }
        /* Split the string again to calculate the product sum */
        urn = sourceURNConverted.toString().split("");
        int productSum = 0;
        for (int i = 1; i < urn.length; i++) {
            productSum += i * Integer.parseInt(urn[i]);
        }
        /*
         * calculation of the ratio, dividing the productSum by the last element
         * of the converted urn
         */
        int q = productSum / Integer.parseInt(urn[urn.length - 1]);
        String[] arr = String.valueOf(q).split("");
        return Integer.valueOf(arr[arr.length - 1]);
    }

    /**
     * Returns the integer value for a given String
     * 
     * @see MCRIConcordanceTable
     * @throws IllegalArgumentException
     *             when the given String is null or its size does not equals 1
     */
    private int getIntegerAlias(String s) throws IllegalArgumentException {
        if (s == null || s.length() != 1)
            throw new IllegalArgumentException("Invalid String specified: " + s);
        if (s.equalsIgnoreCase(MCRIConcordanceTable.A))
            return MCRIConcordanceTable._A;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.B))
            return MCRIConcordanceTable._B;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.C))
            return MCRIConcordanceTable._C;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.D))
            return MCRIConcordanceTable._D;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.E))
            return MCRIConcordanceTable._E;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.F))
            return MCRIConcordanceTable._F;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.G))
            return MCRIConcordanceTable._G;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.H))
            return MCRIConcordanceTable._H;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.I))
            return MCRIConcordanceTable._I;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.J))
            return MCRIConcordanceTable._J;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.K))
            return MCRIConcordanceTable._K;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.L))
            return MCRIConcordanceTable._L;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.M))
            return MCRIConcordanceTable._M;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.N))
            return MCRIConcordanceTable._N;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.O))
            return MCRIConcordanceTable._O;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.P))
            return MCRIConcordanceTable._P;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.Q))
            return MCRIConcordanceTable._Q;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.R))
            return MCRIConcordanceTable._R;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.S))
            return MCRIConcordanceTable._S;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.T))
            return MCRIConcordanceTable._T;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.U))
            return MCRIConcordanceTable._U;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.V))
            return MCRIConcordanceTable._V;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.W))
            return MCRIConcordanceTable._W;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.X))
            return MCRIConcordanceTable._X;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.Y))
            return MCRIConcordanceTable._Y;
        if (s.equalsIgnoreCase(MCRIConcordanceTable.Z))
            return MCRIConcordanceTable._Z;
        if (s.equalsIgnoreCase(":"))
            return MCRIConcordanceTable._COLON;
        if (s.equalsIgnoreCase("-"))
            return MCRIConcordanceTable._MINUS;
        if (s.equalsIgnoreCase("0"))
            return MCRIConcordanceTable._0;
        if (s.equalsIgnoreCase("1"))
            return MCRIConcordanceTable._1;
        if (s.equalsIgnoreCase("2"))
            return MCRIConcordanceTable._2;
        if (s.equalsIgnoreCase("3"))
            return MCRIConcordanceTable._3;
        if (s.equalsIgnoreCase("4"))
            return MCRIConcordanceTable._4;
        if (s.equalsIgnoreCase("5"))
            return MCRIConcordanceTable._5;
        if (s.equalsIgnoreCase("6"))
            return MCRIConcordanceTable._6;
        if (s.equalsIgnoreCase("7"))
            return MCRIConcordanceTable._7;
        if (s.equalsIgnoreCase("8"))
            return MCRIConcordanceTable._8;
        if (s.equalsIgnoreCase("9"))
            return MCRIConcordanceTable._9;
        throw new IllegalArgumentException("Invalid String specified: " + s);
    }
}
