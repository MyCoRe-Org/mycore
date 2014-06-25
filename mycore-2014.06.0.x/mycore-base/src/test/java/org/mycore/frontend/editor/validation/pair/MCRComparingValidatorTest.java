package org.mycore.frontend.editor.validation.pair;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;
import org.mycore.frontend.editor.validation.MCRValidatorTest;

public abstract class MCRComparingValidatorTest extends MCRValidatorTest {

    protected String emptyValue = "";

    protected String lowerValue;

    protected String higherValue;

    @Test
    public void testIncompleteInput() {
        assertTrue(validator.isValid(lowerValue, null));
        assertTrue(validator.isValid(null, lowerValue));
        assertTrue(validator.isValid(lowerValue, emptyValue));
        assertTrue(validator.isValid(emptyValue, lowerValue));
        assertTrue(validator.isValid(emptyValue, emptyValue));
        assertTrue(validator.isValid(null, null));
    }

    @Test
    public void testIllegalOperator() {
        validator.setProperty("operator", "?");
        assertFalse(validator.isValid(lowerValue, lowerValue));
        assertFalse(validator.isValid(lowerValue, higherValue));
        assertFalse(validator.isValid(higherValue, lowerValue));
    }

    @Test
    public void testEquals() {
        validator.setProperty("operator", "=");
        assertTrue(validator.isValid(lowerValue, lowerValue));
        assertFalse(validator.isValid(lowerValue, higherValue));
    }

    @Test
    public void testNotEquals() {
        validator.setProperty("operator", "!=");
        assertFalse(validator.isValid(lowerValue, lowerValue));
        assertTrue(validator.isValid(lowerValue, higherValue));
    }

    @Test
    public void testLowerThan() {
        validator.setProperty("operator", "<");
        assertTrue(validator.isValid(lowerValue, higherValue));
        assertFalse(validator.isValid(lowerValue, lowerValue));
        assertFalse(validator.isValid(higherValue, lowerValue));
    }

    @Test
    public void testGreaterThan() {
        validator.setProperty("operator", ">");
        assertTrue(validator.isValid(higherValue, lowerValue));
        assertFalse(validator.isValid(higherValue, higherValue));
        assertFalse(validator.isValid(lowerValue, higherValue));
    }

    @Test
    public void testLowerOrEqual() {
        validator.setProperty("operator", "<=");
        assertTrue(validator.isValid(lowerValue, higherValue));
        assertTrue(validator.isValid(lowerValue, lowerValue));
        assertFalse(validator.isValid(higherValue, lowerValue));
    }

    @Test
    public void testGreaterOrEqual() {
        validator.setProperty("operator", ">=");
        assertTrue(validator.isValid(higherValue, lowerValue));
        assertTrue(validator.isValid(higherValue, higherValue));
        assertFalse(validator.isValid(lowerValue, higherValue));
    }
}