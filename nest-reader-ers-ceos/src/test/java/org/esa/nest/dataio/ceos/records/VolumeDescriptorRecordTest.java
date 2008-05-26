
package org.esa.nest.dataio.ceos.records;

import junit.framework.TestCase;
import org.esa.nest.dataio.ceos.CeosFileReader;
import org.esa.nest.dataio.ceos.CeosTestHelper;
import org.esa.nest.dataio.ceos.IllegalCeosFormatException;
import org.esa.beam.framework.datamodel.MetadataElement;

import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

public class VolumeDescriptorRecordTest extends TestCase {

    private MemoryCacheImageOutputStream _ios;
    private String _prefix;
    private CeosFileReader _reader;

    private static String format = "ers";
    private static String volume_desc_recordDefinitionFile = "volume_descriptor.xml";

    protected void setUp() throws Exception {
        final ByteArrayOutputStream os = new ByteArrayOutputStream(24);
        _ios = new MemoryCacheImageOutputStream(os);
        _prefix = "VolumeDescriptorRecordTest_prefix";
        _ios.writeBytes(_prefix);
        writeRecordData(_ios);
        _ios.writeBytes("VolumeDescriptorRecordTest_suffix"); // suffix
        _reader = new CeosFileReader(_ios);
    }

    public void testInit_SimpleConstructor() throws IOException,
                                                    IllegalCeosFormatException {
        _reader.seek(_prefix.length());
        final BaseRecord record = new BaseRecord(_reader, -1, format, volume_desc_recordDefinitionFile);

        assertRecord(record);
    }

    public void testInit() throws IOException,
                                  IllegalCeosFormatException {
        final BaseRecord record = new BaseRecord(_reader, _prefix.length(), format, volume_desc_recordDefinitionFile);

        assertRecord(record);
    }

    public void testAssignMetadata() throws IOException,
                                            IllegalCeosFormatException {
        final BaseRecord record = new BaseRecord(_reader, _prefix.length(), format, volume_desc_recordDefinitionFile);
        final MetadataElement volumeMetadata = new MetadataElement("VOLUME_DESCRIPTOR");

        record.assignMetadataTo(volumeMetadata, "suffix");

        assertEquals(26, volumeMetadata.getNumAttributes());
        assertEquals(0, volumeMetadata.getNumElements());
        assertMetadata(volumeMetadata);
    }

    private void assertMetadata(final MetadataElement elem) {
        BaseRecordTest.assertMetadata(elem);

        BaseRecordTest.assertStringAttribute(elem, "Ascii code character", "AB");
        BaseRecordTest.assertStringAttribute(elem, "Specification number", "abcdefghijkl");
        BaseRecordTest.assertStringAttribute(elem, "Specification revision number", "CD");
        BaseRecordTest.assertStringAttribute(elem, "Record format revision number", "EF");
        BaseRecordTest.assertStringAttribute(elem, "Software version number", "bcdefghijklm");
        BaseRecordTest.assertStringAttribute(elem, "Logical volume ID", "cdefghijklmnopqr");
        BaseRecordTest.assertStringAttribute(elem, "Volume set ID", "defghijklmnopqrs");
        BaseRecordTest.assertIntAttribute(elem, "Volume number of this volume descriptor record", 12);
        BaseRecordTest.assertIntAttribute(elem, "Number of first file following the volume directory file", 2345);
        BaseRecordTest.assertIntAttribute(elem, "Logical volume number in volume set", 3456);
        BaseRecordTest.assertStringAttribute(elem, "Logical volume preparation date", "efghijkl");
        BaseRecordTest.assertStringAttribute(elem, "Logical volume preparation time", "fghijklm");
        BaseRecordTest.assertStringAttribute(elem, "Logical volume preparation country", "ghijklmnopqr");
        BaseRecordTest.assertStringAttribute(elem, "Logical volume preparing agency", "hijklmno");
        BaseRecordTest.assertStringAttribute(elem, "Logical volume preparing facility", "ijklmnopqrst");
        BaseRecordTest.assertIntAttribute(elem, "Number of filepointer records", 4567);
        BaseRecordTest.assertIntAttribute(elem, "Number of records", 5678);
    }

    private void writeRecordData(final MemoryCacheImageOutputStream ios) throws IOException {
        BaseRecordTest.writeRecordData(ios);

        ios.writeBytes("AB"); // asciiCodeCharacter // A2
        CeosTestHelper.writeBlanks(ios, 2); // blank
        ios.writeBytes("abcdefghijkl"); // specificationNumber //A12
        ios.writeBytes("CD"); // specificationRevisionNumer // A2
        ios.writeBytes("EF"); // recordFormatRevisionNumer // A2
        ios.writeBytes("bcdefghijklm"); // softwareVersionNumber // A12
        CeosTestHelper.writeBlanks(ios, 16); // blank
        ios.writeBytes("cdefghijklmnopqr"); // logicalVolumeID // A16
        ios.writeBytes("defghijklmnopqrs"); // volumeSetID // A16
        CeosTestHelper.writeBlanks(ios, 6); // blank
        ios.writeBytes("12"); // volumeNuberOfThisVolumeDescritorRecord // I2
        ios.writeBytes("2345"); // nuberOfFirstFileFollowingTheVolumeDirectoryFile // I4
        ios.writeBytes("3456"); // logicalVolumeNumberInVolumeSet // I4
        ios.writeBytes("3457"); // physicalVolumeNumberInVolumeSet // I4
        ios.writeBytes("efghijkl"); // logicalVolumePreparationDate // A8
        ios.writeBytes("fghijklm"); // logicalVolumePreparationTime // A8
        ios.writeBytes("ghijklmnopqr"); // logicalVolumePreparationCountry // A12
        ios.writeBytes("hijklmno"); // logicalVolumePreparingAgent // A8
        ios.writeBytes("ijklmnopqrst"); // logicalVolumePreparingFacility // A12
        ios.writeBytes("4567"); // numberOfFilepointerRecords // I4
        ios.writeBytes("5678"); // nuberOfRecords // I4
        ios.writeBytes("5"); // Total number of logical volume set // I4
        CeosTestHelper.writeBlanks(ios, 192); // blank
    }

    private void assertRecord(final BaseRecord record) throws IOException {
        BaseRecordTest.assertRecord(record);
        assertEquals(_prefix.length(), record.getStartPos());
        assertEquals(_prefix.length() + 357, _ios.getStreamPosition());

        assertEquals("AB", record.getAttributeString("Ascii code character"));
        assertEquals("abcdefghijkl", record.getAttributeString("Specification number"));
        assertEquals("CD", record.getAttributeString("Specification revision number"));
        assertEquals("EF", record.getAttributeString("Record format revision number"));
        assertEquals("bcdefghijklm", record.getAttributeString("Software version number"));
        assertEquals("cdefghijklmnopqr", record.getAttributeString("Logical volume ID"));
        assertEquals("defghijklmnopqrs", record.getAttributeString("Volume set ID"));
        assertEquals(12, record.getAttributeInt("Volume number of this volume descriptor record"));
        assertEquals(2345, record.getAttributeInt("Number of first file following the volume directory file"));
        assertEquals(3456, record.getAttributeInt("Logical volume number in volume set"));
        assertEquals("efghijkl", record.getAttributeString("Logical volume preparation date"));
        assertEquals("fghijklm", record.getAttributeString("Logical volume preparation time"));
        assertEquals("ghijklmnopqr", record.getAttributeString("Logical volume preparation country"));
        assertEquals("hijklmno", record.getAttributeString("Logical volume preparing agency"));
        assertEquals("ijklmnopqrst", record.getAttributeString("Logical volume preparing facility"));
        assertEquals(4567, record.getAttributeInt("Number of filepointer records"));
        assertEquals(5678, record.getAttributeInt("Number of records"));
    }
}
