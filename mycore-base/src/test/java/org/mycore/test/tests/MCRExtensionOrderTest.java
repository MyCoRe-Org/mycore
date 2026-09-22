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

package org.mycore.test.tests;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.mycore.test.MCRMetadataExtension;
import org.mycore.test.MCRTestExtension;
import org.mycore.test.MyCoReTest;

/**
 * Every MyCoRe extension builds on the configuration that {@link MCRTestExtension} sets up, in {@code beforeAll}
 * as well as in {@code beforeEach}. JUnit calls the extensions in declaration order, so {@link MyCoReTest} has to
 * be declared first. {@link MCRTestExtension#requireInitialized} turns a violation into a readable error instead
 * of a failure somewhere deep inside MyCoRe.
 */
public class MCRExtensionOrderTest {

    @Test
    public void testExtensionBeforeMyCoReTestIsRejected() {
        IllegalStateException exception = assertThrows(IllegalStateException.class,
            () -> MCRTestExtension.requireInitialized(contextInitializedBy(null), MCRMetadataExtension.class));

        assertTrue(exception.getMessage().contains(MyCoReTest.class.getSimpleName()),
            "The error should name the annotation to add, but was: " + exception.getMessage());
        assertTrue(exception.getMessage().contains(MCRMetadataExtension.class.getSimpleName()),
            "The error should name the extension that is out of order, but was: " + exception.getMessage());
    }

    @Test
    public void testExtensionAfterMyCoReTestIsAccepted() {
        assertDoesNotThrow(
            () -> MCRTestExtension.requireInitialized(contextInitializedBy(Boolean.TRUE), MCRMetadataExtension.class));
    }

    /**
     * @param initialized what {@link MCRTestExtension} has left in its store, {@code null} before it has run
     */
    private static ExtensionContext contextInitializedBy(Boolean initialized) {
        ExtensionContext.Store store = mock(ExtensionContext.Store.class);
        when(store.get("initialized", Boolean.class)).thenReturn(initialized);

        ExtensionContext context = mock(ExtensionContext.class);
        when(context.getStore(MCRTestExtension.NAMESPACE)).thenReturn(store);
        when(context.getRequiredTestClass()).thenAnswer(invocation -> MCRExtensionOrderTest.class);
        return context;
    }

}
