package org.esa.beam.framework.datamodel;

import junit.framework.TestCase;

import java.util.HashMap;

import org.esa.beam.framework.dataop.maptransf.Datum;

public class TiePointGridPointingTest extends TestCase {

    public void testRequiresGeoCoding() {
        final TiePointGrid latGrid = new TiePointGrid("lat", 2, 2, 0, 0, 1, 1, new float[]{1, 2, 3, 4});
        final TiePointGrid lonGrid = new TiePointGrid("lon", 2, 2, 0, 0, 1, 1, new float[]{1, 2, 3, 4});
        final TiePointGeoCoding geoCoding = new TiePointGeoCoding(latGrid, lonGrid, Datum.WGS_84);
        try {
            new TiePointGridPointing(geoCoding,
                                     null, null, null, null, null);
            // OK
        } catch (Exception e) {
            fail();
        }

        try {
            new TiePointGridPointing(null,
                                     null, null, null, null, null);
            fail();
        } catch (Exception e) {
            // OK
        }
    }

    public void testEqualsAndHashcode() {
        final TiePointGrid latGrid = new TiePointGrid("lat", 2, 2, 0, 0, 1, 1, new float[]{1, 2, 3, 4});
        final TiePointGrid lonGrid = new TiePointGrid("lon", 2, 2, 0, 0, 1, 1, new float[]{1, 2, 3, 4});
        final TiePointGrid lonGrid2 = new TiePointGrid("lon", 2, 2, 0, 0, 1, 1, new float[]{2, 3, 4, 5});

        final TiePointGeoCoding geoCoding1 = new TiePointGeoCoding(latGrid, lonGrid, Datum.WGS_84);
        final TiePointGeoCoding geoCoding2 = new TiePointGeoCoding(latGrid, lonGrid2, Datum.WGS_84);

        final TiePointGridPointing tiePointGridPointing1 = new TiePointGridPointing(geoCoding1,
                                                                                    null, null, null, null, null);
        final TiePointGridPointing tiePointGridPointing2 = new TiePointGridPointing(geoCoding1,
                                                                                    null, null, null, null, null);
        final TiePointGridPointing tiePointGridPointing3 = new TiePointGridPointing(geoCoding2,
                                                                                    null, null, null, null, null);

        // test equals
        assertTrue(tiePointGridPointing1.equals(tiePointGridPointing2));
        assertFalse(tiePointGridPointing1.equals(tiePointGridPointing3));

        // test hashCode
        assertTrue(tiePointGridPointing1.hashCode() == tiePointGridPointing2.hashCode());
        assertFalse(tiePointGridPointing1.hashCode() == tiePointGridPointing3.hashCode());

        // test usage as key in a map (ProductProjectionBuilder uses Pointings this way)
        HashMap map = new HashMap();
        map.put(tiePointGridPointing1, "Value");
        assertEquals("Value", map.get(tiePointGridPointing1));
        assertEquals("Value", map.get(tiePointGridPointing2));
        assertEquals(null, map.get(tiePointGridPointing3));
    }
}
