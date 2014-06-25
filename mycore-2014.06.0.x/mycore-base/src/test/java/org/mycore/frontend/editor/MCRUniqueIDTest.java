package org.mycore.frontend.editor;

import static org.hamcrest.CoreMatchers.*;
import static org.junit.Assert.*;
import org.junit.Test;

public class MCRUniqueIDTest {

    @Test
    public void testIDNotNull() {
        String id = MCRUniqueID.buildID();
        assertNotNull(id);
    }

    @Test
    public void testIDNotEqual() {
        String firstID = MCRUniqueID.buildID();
        String secondID = MCRUniqueID.buildID();
        assertThat(firstID, not(equalTo(secondID)));
    }
}
