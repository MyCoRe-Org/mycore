package org.mycore.common.function;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.Optional;
import java.util.stream.Stream;

import org.junit.Test;

public class MCRThrowFunctionTest {

    @Test
    public void testToFunctionBiFunctionOfTERClassOfQsuperE() {
        Optional<String> findFirst = Stream.of("passed")
            .map(((MCRThrowFunction<String, String, IOException>) MCRThrowFunctionTest::failExceptionally)
                .toFunction(MCRThrowFunctionTest::mayhandleException, IOException.class))
            .findFirst();
        assertEquals("Junit test:passed", findFirst.get());
    }

    @Test(expected = IOException.class)
    public void testToFunction() throws IOException {
        try {
            Stream.of("passed").map(
                ((MCRThrowFunction<String, String, IOException>) MCRThrowFunctionTest::failExceptionally).toFunction())
                .findFirst();
        } catch (RuntimeException e) {
            if (e.getCause() != null && e.getCause() instanceof IOException) {
                throw (IOException) e.getCause();
            }
            throw e;
        }
    }

    public static String failExceptionally(String input) throws IOException {
        throw new IOException("Junit test");
    }

    public static String mayhandleException(String input, IOException e) {
        return e.getMessage() + ":" + input;
    }

}
