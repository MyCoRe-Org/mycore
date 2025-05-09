package org.mycore.iview2.frontend;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.mycore.iview2.backend.MCRTileInfo;
import org.mycore.test.MyCoReTest;

@Deprecated
@MyCoReTest
class MCRTileServletTest {

    @ParameterizedTest
    @CsvSource({
        "/mir_derivate_00000001/dt_zs_155_jg1762_006.tif/imageinfo.xml, mir_derivate_00000001, " +
                "/dt_zs_155_jg1762_006.tif, imageinfo.xml",
        "/mir_derivate_00000001/dt_zs_155_jg1762_002.tif/4/3/0.jpg, mir_derivate_00000001, " +
                "/dt_zs_155_jg1762_002.tif, 4/3/0.jpg"
    })
    void getTileInfoParameterized(String pathInfo, String expectedDerivate, String expectedImagePath,
        String expectedTile) {
        MCRTileInfo tileInfo = MCRTileServlet.getTileInfo(pathInfo);
        assertEquals(expectedDerivate, tileInfo.derivate());
        assertEquals(expectedImagePath, tileInfo.imagePath());
        assertEquals(expectedTile, tileInfo.tile());
    }
}
