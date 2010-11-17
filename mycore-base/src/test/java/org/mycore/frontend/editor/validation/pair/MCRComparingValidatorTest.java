package org.mycore.frontend.editor.validation.pair;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public abstract class MCRComparingValidatorTest extends MCRPairValidatorTest {

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