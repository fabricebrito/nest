package org.esa.nest.dataio.ceos.jers;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.nest.dataio.ceos.CeosFileReader;
import org.esa.nest.dataio.ceos.IllegalCeosFormatException;
import org.esa.nest.dataio.ceos.ers.ERSImageFDR;
import org.esa.nest.dataio.ceos.records.ImageRecord;
import org.esa.beam.framework.datamodel.ProductData;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/*
 * $Id: JERSImageFile.java,v 1.1 2008-05-26 19:32:11 lveci Exp $
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


/**
 * This class represents an image file of an Avnir-2 product.
 *
 * @author Marco Peters
 * @version $Revision: 1.1 $ $Date: 2008-05-26 19:32:11 $
 */
class JERSImageFile {

    private final ERSImageFDR _imageFDR;
    private final ImageRecord[] _imageRecords;
    private CeosFileReader _ceosReader;
    private final int _imageNumber;
    private int _imageRecordLength;
    private long _startPosImageRecords;

    private static String mission = "jers";
    private static String image_recordDefinitionFile = "image_file.xml";
    private static String image_recordDefinition = "image_record.xml";

    public JERSImageFile(final ImageInputStream imageStream) throws IOException,
                                                                      IllegalCeosFormatException {
        _ceosReader = new CeosFileReader(imageStream);
        _imageFDR = new ERSImageFDR(_ceosReader, -1, mission, image_recordDefinitionFile);
        _ceosReader.seek(_imageFDR.getAbsolutPosition(_imageFDR.getRecordLength()));
        _imageRecords = new ImageRecord[_imageFDR.getAttributeInt("Number of lines per data set")];
        _imageRecords[0] = new ImageRecord(_ceosReader, -1, mission, image_recordDefinition);
        _imageRecordLength = _imageRecords[0].getRecordLength();
        _startPosImageRecords = _imageRecords[0].getStartPos();
        _imageNumber = _imageRecords[0].getImageNumber();
    }

    public String getBandName() {
        return JERSConstants.BANDNAME_PREFIX + _imageNumber;
    }

    public String getBandDescription() {
        return String.format(JERSConstants.BAND_DESCRIPTION_FORMAT_STRING, new Object[]{_imageNumber});
    }

    public int getBandIndex() {
        return _imageNumber;
    }

    public int getRasterWidth() {
        return _imageFDR.getAttributeInt("Number of pixels per line per SAR channel");//getNumImagePixelsPerLine();
    }

    public int getRasterHeight() {
        return _imageFDR.getAttributeInt("Number of lines per data set");
    }

    public static String getGeophysicalUnit() {
        return JERSConstants.GEOPHYSICAL_UNIT;
    }

    public float getSpectralWavelength() {
        final int bandIndex = _imageNumber;

        switch (bandIndex) {
        case 1:
            return JERSConstants.WAVELENGTH_BAND_1;
        case 2:
            return JERSConstants.WAVELENGTH_BAND_2;
        case 3:
            return JERSConstants.WAVELENGTH_BAND_3;
        case 4:
            return JERSConstants.WAVELENGTH_BAND_4;
        default:
            return 0;
        }
    }

    public float getSpectralBandwidth() {
        final int bandIndex = _imageNumber;

        switch (bandIndex) {
        case 1:
            return JERSConstants.BANDWIDTH_BAND_1;
        case 2:
            return JERSConstants.BANDWIDTH_BAND_2;
        case 3:
            return JERSConstants.BANDWIDTH_BAND_3;
        case 4:
            return JERSConstants.BANDWIDTH_BAND_4;
        default:
            return 0;
        }
    }

 /*   public int getTotalMillisInDayOfLine(final int y) throws IOException,
                                                             IllegalCeosFormatException {
        return getImageRecord(y).getScanStartTimeMillisAtDay();
    }

    public int getMicrosecondsOfLine(final int y) throws IOException,
                                                         IllegalCeosFormatException {
        return getImageRecord(y).getScanStartTimeMicros();
    } */

    public void readBandRasterData(final int sourceOffsetX, final int sourceOffsetY,
                                   final int sourceWidth, final int sourceHeight,
                                   final int sourceStepX, final int sourceStepY,
                                   final int destOffsetX, final int destOffsetY,
                                   final int destWidth, final int destHeight,
                                   final ProductData destBuffer, ProgressMonitor pm) throws IOException,
                                                                                            IllegalCeosFormatException
    {
        final int sourceMaxY = sourceOffsetY + sourceHeight - 1;
        ImageRecord imageRecord;

        int x = sourceOffsetX * 2;

        pm.beginTask("Reading band '" + getBandName() + "'...", sourceMaxY - sourceOffsetY);
        try {
            final short[] srcLine = new short[sourceWidth];
            final short[] destLine = new short[destWidth];
            for (int y = sourceOffsetY; y <= sourceMaxY; y += sourceStepY) {
                if (pm.isCanceled()) {
                    break;
                }

                // Read source line
                if (_imageRecords[y] == null) {
                    _ceosReader.seek(_imageRecordLength * y + _startPosImageRecords);
                    _imageRecords[y] = new ImageRecord(_ceosReader, _ceosReader.getCurrentPos(), _imageRecords[0].getCeosDatabase());
                }
                imageRecord = _imageRecords[y];

                _ceosReader.seek(imageRecord.getImageDataStart() + x);
                _ceosReader.readB2(srcLine);

                // Copy source line into destination buffer
                final int currentLineIndex = (y - sourceOffsetY) * destWidth;
                if (sourceStepX == 1) {

                    System.arraycopy(srcLine, 0, destBuffer.getElems(), currentLineIndex, destWidth);
                } else {
                    copyLine(srcLine, destLine, sourceStepX);

                    System.arraycopy(destLine, 0, destBuffer.getElems(), currentLineIndex, destWidth);
                }

                pm.worked(1);
            }

        } finally {
            pm.done();
        }
    }

    private static void copyLine(final short[] srcLine, final short[] destLine,
                          final int sourceStepX) {
        for (int x = 0, i = 0; x < destLine.length; x++, i += sourceStepX) {
            destLine[x] = srcLine[i];
        }
    }

    public void close() throws IOException {
        _ceosReader.close();
        _ceosReader = null;
    }
}