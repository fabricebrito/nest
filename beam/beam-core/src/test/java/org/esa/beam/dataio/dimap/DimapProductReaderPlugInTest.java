/*
 * $Id: DimapProductReaderPlugInTest.java,v 1.1 2009-04-28 14:39:33 lveci Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.dataio.dimap;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.esa.beam.GlobalTestConfig;
import org.esa.beam.framework.dataio.IllegalFileFormatException;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.datamodel.Product;

public class DimapProductReaderPlugInTest extends TestCase {

    private final static DimapProductReaderPlugIn _plugIn = new DimapProductReaderPlugIn();
    private ProductReader _productReader;

    public DimapProductReaderPlugInTest(String s) {
        super(s);
    }

    public static Test suite() {
        return new TestSuite(DimapProductReaderPlugInTest.class);
    }

    @Override
    protected void setUp() {
        _productReader = _plugIn.createReaderInstance();
    }

    @Override
    protected void tearDown() {
        _productReader = null;
    }

    public void testPlugInInfoQuery() {

        assertNotNull(_plugIn.getFormatNames());
        assertEquals(1, _plugIn.getFormatNames().length);
        assertEquals(DimapProductConstants.DIMAP_FORMAT_NAME, _plugIn.getFormatNames()[0]);

        assertNotNull(_plugIn.getInputTypes());
        assertEquals(2, _plugIn.getInputTypes().length);

        assertNotNull(_plugIn.getDescription(null));
    }

    public void testCanDecodeInput() {
        File file = GlobalTestConfig.getBeamTestDataOutputFile("DIMAP/test2.dim");
        try {
            file.getParentFile().mkdirs();
            file.createNewFile();
            final FileWriter writer = new FileWriter(file);
            writer.write("This file must contain the String '<Dimap_Document' to ensure this is a correct file format");
            writer.close();
            assertEquals(DecodeQualification.INTENDED, _plugIn.getDecodeQualification(file));
            assertEquals(DecodeQualification.INTENDED, _plugIn.getDecodeQualification(file.getPath()));
            if (!file.delete()) {
                file.deleteOnExit();
            }
            if (!file.getParentFile().delete()) {
                file.getParentFile().deleteOnExit();
            }
        } catch (IOException e) {
            System.out.println("DimapProductReaderPlugInTest: failed to create test file " + file.getPath());
        }
    }

    public void testCreatedProductReaderInstance() {
        assertNotNull(_productReader);
        assertEquals(true, _productReader instanceof DimapProductReader);
    }

    public void testReadProductNodes() {

        File inputFile = GlobalTestConfig.getBeamTestDataOutputFile("DIMAP/test2.dim");
        Product product = null;
        try {
            product = _productReader.readProductNodes(inputFile, null);
        } catch (IllegalFileFormatException e) {
            fail("unexpected IllegalFileFormatException: " + e.getMessage());
        } catch (IOException e) {
            // expected exception
        }
        assertNull(product);
    }
}
