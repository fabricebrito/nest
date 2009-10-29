package org.esa.beam.dataio.modis.bandreader;

import com.bc.ceres.core.ProgressMonitor;
import ncsa.hdf.hdflib.HDFException;
import org.esa.beam.dataio.modis.hdf.IHDFAdapterForMocking;
import org.esa.beam.dataio.modis.hdf.lib.HDFTestCase;
import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.math.Range;

public class ModisUint8BandReaderTest extends HDFTestCase {

    public void testRead() throws ProductIOException, HDFException {
        setHdfMock(new IHDFAdapterForMocking() {
            byte value = -7;

            @Override
            public void SDreaddata(int sdsId, int[] start, int[] stride, int[] count, Object buffer)
                    throws HDFException {
                final byte[] bytes = (byte[]) buffer;
                for (int i = 0; i < bytes.length; i++) {
                    bytes[i] = value;
                    value += 2;
                }
            }
        });

        final ProductData buffer = new ProductData.UByte(12);
        final ModisUint8BandReader reader = new ModisUint8BandReader(3, 2, false);
        reader.setFillValue(0);
        reader.setValidRange(new Range(4, Byte.MAX_VALUE * 2 - 3));

        // Method under test
        reader.readBandData(0, 0, 4, 3, 1, 1, buffer, ProgressMonitor.NULL);

        final int[] expected = {249, 251, 0, 0, 0, 0, 5, 7, 9, 11, 13, 15};
        for (int i = 0; i < expected.length; i++) {
            assertEquals("false at index: " + i + "  ", expected[i], buffer.getElemIntAt(i));
        }
    }

    public void testHDFException() throws ProductIOException, HDFException {
        setHdfMock(new IHDFAdapterForMocking() {
            @Override
            public void SDreaddata(int sdsId, int[] start, int[] stride, int[] count, Object buffer)
                    throws HDFException {
                throw new HDFException("TestMessage");
            }
        });

        final int sdsId = 3;
        final ProductData buffer = new ProductData.UByte(12);
        final ModisUint8BandReader reader = new ModisUint8BandReader(sdsId, 2, false);

        try {
            // Method under test
            reader.readBandData(0, 0, 4, 3, 1, 1, buffer, ProgressMonitor.NULL);
            fail();
        } catch (HDFException e) {
            assertEquals("TestMessage", e.getMessage());
        } catch (Exception e) {
            fail();
        }
    }
}