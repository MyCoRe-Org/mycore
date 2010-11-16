package org.mycore.frontend.editor.validation;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.Before;

public class MCRComparingValidatorTest extends MCRPairValidatorTest {

    private static final String emptyValue = "";

    private static final String lowerValue = "5";

    private static final String higherValue = "6";

    @Before
    public void setup() {
        validator = new MCRIntegerPairValidator();
    }

    @Test
    public void testHasRequiredProperties() {
        validator.setProperty("type", "integer");
        validator.setProperty("operator", "=");
        assertTrue(validator.hasRequiredProperties());
    }

    @Test
    public void testIncompleteInput() {
        assertTrue(validator.isValidPair(lowerValue, null));
        assertTrue(validator.isValidPair(null, lowerValue));
        assertTrue(validator.isValidPair(lowerValue, emptyValue));
        assertTrue(validator.isValidPair(emptyValue, lowerValue));
        assertTrue(validator.isValidPair(emptyValue, emptyValue));
        assertTrue(validator.isValidPair(null, null));
    }

    @Test
    public void testIllegalOperator() {
        validator.setProperty("operator", "?");
        assertFalse(validator.isValidPair(lowerValue, lowerValue));
        assertFalse(validator.isValidPair(lowerValue, higherValue));
        assertFalse(validator.isValidPair(higherValue, lowerValue));
    }

    @Test
    public void testEquals() {
        validator.setProperty("operator", "=");
        assertTrue(validator.isValidPair(lowerValue, lowerValue));
        assertFalse(validator.isValidPair(lowerValue, higherValue));
    }

    @Test
    public void testNotEquals() {
        validator.setProperty("operator", "!=");
        assertFalse(validator.isValidPair(lowerValue, lowerValue));
        assertTrue(validator.isValidPair(lowerValue, higherValue));
    }

    @Test
    public void testLowerThan() {
        validator.setProperty("operator", "<");
        assertTrue(validator.isValidPair(lowerValue, higherValue));
        assertFalse(validator.isValidPair(lowerValue, lowerValue));
        assertFalse(validator.isValidPair(higherValue, lowerValue));
    }

    @Test
    public void testGreaterThan() {
        validator.setProperty("operator", ">");
        assertTrue(validator.isValidPair(higherValue, lowerValue));
        assertFalse(validator.isValidPair(higherValue, higherValue));
        assertFalse(validator.isValidPair(lowerValue, higherValue));
    }

    @Test
    public void testLowerOrEqual() {
        validator.setProperty("operator", "<=");
        assertTrue(validator.isValidPair(lowerValue, higherValue));
        assertTrue(validator.isValidPair(lowerValue, lowerValue));
        assertFalse(validator.isValidPair(higherValue, lowerValue));
    }

    @Test
    public void testGreaterOrEqual() {
        validator.setProperty("operator", ">=");
        assertTrue(validator.isValidPair(higherValue, lowerValue));
        assertTrue(validator.isValidPair(higherValue, higherValue));
        assertFalse(validator.isValidPair(lowerValue, higherValue));
    }
}
