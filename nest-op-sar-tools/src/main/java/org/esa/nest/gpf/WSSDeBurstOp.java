package org.esa.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;

import java.awt.*;
import java.util.*;

/**
 * De-Burst a WSS product
 */
@OperatorMetadata(alias = "WSS-Deburst",
        category = "SAR Tools",
        description="Merges the bursts of an ASAR WSS product")
public class WSSDeBurstOp extends Operator {

    @SourceProduct(alias="source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
            sourceProductId="source", label="Source Band")
    String[] sourceBandNames;

    @Parameter(defaultValue = "true", label="Produce Intensities Only")
    private boolean produceIntensitiesOnly = true;
    @Parameter(defaultValue = "true", label="Mean Average Intensities")
    private boolean average = false;

    private Vector<Integer> startLine = new Vector<Integer>(5);
    private static final double zeroThreshold = 1000;
    private static final double zeroThresholdSmall = 500;
    private final Vector<LineTime[]> lineTimeList = new Vector<LineTime[]>(5);

    private int numberOfDatasets = 0;
    private transient Map<Band, ComplexBand> bandMap;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public WSSDeBurstOp() {
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.beam.framework.datamodel.Product} annotated with the
     * {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        // check product type 
        if (!sourceProduct.getProductType().equals("ASA_WSS_1P")) {
            throw new OperatorException("Source product is not an ASA_WSS_1P");
        }

        bandMap = new HashMap<Band, ComplexBand>(5);
        final int targetHeight = (int) (sourceProduct.getSceneRasterHeight() / 2.9);

        targetProduct = new Product(sourceProduct.getName() + "_deburst",
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                targetHeight);

        if (sourceBandNames == null || sourceBandNames.length == 0) {
            final Band[] bands = sourceProduct.getBands();
            final ArrayList<String> bandNameList = new ArrayList<String>(sourceProduct.getNumBands());
            for (Band band : bands) {
                bandNameList.add(band.getName());
            }
            sourceBandNames = bandNameList.toArray(new String[bandNameList.size()]);
        }

        final Band[] sourceBands = new Band[sourceBandNames.length];
        for (int i = 0; i < sourceBandNames.length; i++) {
            final String sourceBandName = sourceBandNames[i];
            final Band sourceBand = sourceProduct.getBand(sourceBandName);
            if (sourceBand == null) {
                throw new OperatorException("Source band not found: " + sourceBandName);
            }
            sourceBands[i] = sourceBand;
        }

        if((sourceBands.length & 1) != 0) {
            throw new OperatorException("Please select correct real, imaginary band pairs");
        }

        for (int i = 0, count = 1; i < sourceBands.length - 1; i += 2, ++count) {

            final String SSnum = sourceBands[i].getName().substring(2);
            if(!sourceBands[i].getUnit().equals("real") ||
               !sourceBands[i+1].getUnit().equals("imaginary") ||
               !SSnum.equals(sourceBands[i+1].getName().substring(2)))
                    throw new OperatorException("Please select correct real, imaginary band pairs");

            final String countStr = "_SS" + SSnum;

            if (produceIntensitiesOnly) {
                final Band tgtBand = targetProduct.addBand("Intensity" + countStr, ProductData.TYPE_FLOAT32);
                tgtBand.setUnit("intensity");
                bandMap.put(tgtBand, new ComplexBand(sourceBands[i], sourceBands[i+1]));
            } else {
                final Band trgI = targetProduct.addBand('i' +countStr, sourceBands[i].getDataType());
                trgI.setUnit("real");
                final Band trgQ = targetProduct.addBand('q' +countStr, sourceBands[i+1].getDataType());
                trgQ.setUnit("imagainary");
                bandMap.put(trgI, new ComplexBand(sourceBands[i], sourceBands[i+1]));
                createVirtualIntensityBand(targetProduct, trgI, trgQ, countStr);
                createVirtualPhaseBand(targetProduct, trgI, trgQ, countStr);
            }
            ++numberOfDatasets;
        }

        copyMetaData(sourceProduct.getMetadataRoot(), targetProduct.getMetadataRoot());
        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);
        ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
    }

    private static void copyMetaData(final MetadataElement source, final MetadataElement target) {
        for (final MetadataElement element : source.getElements()) {
            if (!element.getName().equals("Image Record"))
                target.addElement(element.createDeepClone());
        }
        for (final MetadataAttribute attribute : source.getAttributes()) {
            target.addAttribute(attribute.createDeepClone());
        }
    }

    private static void createVirtualIntensityBand(final Product product,
                                                   final Band bandI, final  Band bandQ, final String countStr) {
        final String expression = bandI.getName() + " * " + bandI.getName() + " + " +
                bandQ.getName() + " * " + bandQ.getName();

        final VirtualBand band = new VirtualBand("Intensity" + countStr,
                ProductData.TYPE_FLOAT32,
                product.getSceneRasterWidth(),
                product.getSceneRasterHeight(),
                expression);
        band.setSynthetic(true);
        band.setUnit("intensity");
        band.setDescription("Intensity from complex data");
        product.addBand(band);
    }

    private static void createVirtualPhaseBand(final Product product,
                                                 final Band bandI, final Band bandQ, final String countStr) {
        final String expression = "atan2("+bandQ.getName()+ ',' +bandI.getName()+ ')';

        final VirtualBand virtBand = new VirtualBand("Phase" + countStr,
                ProductData.TYPE_FLOAT32,
                product.getSceneRasterWidth(),
                product.getSceneRasterHeight(),
                expression);
        virtBand.setSynthetic(true);
        virtBand.setUnit("phase");
        virtBand.setDescription("Phase from complex data");
        product.addBand(virtBand);
    }

    /**
     * Called by the framework in order to compute the stack of tiles for the given target bands.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTiles The current tiles to be computed for each target band.
     * @param pm          A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          if an error occurs during computation of the target rasters.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

        Tile targetTileI = null, targetTileQ = null, targetTileIntensity = null;

        //System.out.println("targetRect " + targetRectangle.x + " " + targetRectangle.y + " w "
        //        + targetRectangle.width + " h " + targetRectangle.height);

        try {

            pm.beginTask("de-bursting WSS product", numberOfDatasets);
            for (int index = 0, cplxIndex = 0; index < numberOfDatasets; ++index, cplxIndex += 4) {

                Band srcBandI, srcBandQ;
                if (produceIntensitiesOnly) {
                    final Band tgtBand = targetProduct.getBandAt(index);
                    targetTileIntensity = targetTiles.get(tgtBand);
                    final ComplexBand cBand = bandMap.get(tgtBand);
                    srcBandI = cBand.i;
                    srcBandQ = cBand.q;
                } else {
                    final Band tgtBandI = targetProduct.getBandAt(cplxIndex);
                    final Band tgtBandQ = targetProduct.getBandAt(cplxIndex + 1);
                    targetTileI = targetTiles.get(tgtBandI);
                    targetTileQ = targetTiles.get(tgtBandQ);
                    final ComplexBand cBand = bandMap.get(tgtBandI);
                    srcBandI = sourceProduct.getBand(cBand.i.getName());
                    srcBandQ = sourceProduct.getBand(cBand.q.getName());
                    // skip virtual intensity band
                }

                final int rasterWidth = srcBandI.getRasterWidth();
                final int srcRectX = Math.min(targetRectangle.x, rasterWidth);
                final int srcRectWidth = Math.min(targetRectangle.width, rasterWidth - srcRectX);
                if (srcRectWidth < 1) continue;

                try {
                    //synchronized(this) {
                        if (lineTimeList.size() <= index) {
                            sortLineTimes(sourceProduct, srcBandI);
                        } else {
                            //LineTime[] lineTimes = lineTimeList.get(index);
                            //for (LineTime lt : lineTimes) {
                            //    lt.visited = false;
                            //}
                        }
                   // }
                } catch (Exception e) {
                    System.out.print("getImageRecord " + e.toString());
                    System.out.println();
                }

                final LineTime[] lineTimes = lineTimeList.get(index);

                int targetLine =0;
                final int maxX = targetRectangle.x + targetRectangle.width;
                
                //final double threshold = 0.000000139;
                final double threshold = 0.000000135;

                int startLineForBand = startLine.get(index);
                int i = startLineForBand;
                while (i < lineTimes.length) {
                    if (lineTimes[i].visited) {
                        ++i;
                        continue;
                    }

                    final Vector<Integer> burstLines = new Vector<Integer>(4);
                    burstLines.add(lineTimes[i].line);
                    lineTimes[i].visited = true;
                    startLineForBand = i;

                    int j = i + 1;
                    while (j < lineTimes.length && j < i + 10 && burstLines.size() < 3) {
                        if (lineTimes[j].visited) {
                            ++j;
                            continue;
                        }

                        final double diff = lineTimes[j].time - lineTimes[i].time;
                        if (diff < threshold) {
                            burstLines.add(lineTimes[j].line);
                            lineTimes[j].visited = true;
                        }
                        ++j;
                    }
                    ++i;

                    if (!burstLines.isEmpty()) {

                        final boolean ok = deburstTile(burstLines, targetLine, targetRectangle.x, maxX, srcBandI, srcBandQ,
                                targetTileI, targetTileQ, targetTileIntensity, pm);
                        if(ok)
                            ++targetLine;
                    }

                    if(targetLine >= targetRectangle.height)
                        break;
                }
                startLine.set(index, startLineForBand);

                pm.worked(1);
            }
            pm.done();

        } catch (Exception e) {
            System.out.print("WSSDeburst.computeTileStack " + e.toString());
        }
    }

    private void sortLineTimes(final Product srcProduct, final Band srcBand) {
        final MetadataElement imgRecElem = srcProduct.getMetadataRoot().getElement("Image Record");
        final MetadataElement bandElem = imgRecElem.getElement(srcBand.getName());

        final MetadataAttribute attrib = bandElem.getAttribute("t");
        final double[] timeData = (double[])attrib.getData().getElems();
        final LineTime[] lineTimes = new LineTime[timeData.length];
        lineTimeList.add(lineTimes);
        startLine.add(0);

        for(int y=0; y < timeData.length; ++y) {
            lineTimes[y] = new LineTime(y, timeData[y]);
        }

        Arrays.sort(lineTimes, new LineTimeComparator());
    }

    private boolean deburstTile(final Vector<Integer> burstLines, final int targetLine, final int startX, final int endX,
                              final Band srcBandI, final Band srcBandQ,
                              final Tile targetTileI, final Tile targetTileQ,
                              final Tile targetTileIntensity, final ProgressMonitor pm) {

        final Integer[] burstLineList = new Integer[burstLines.size()];
        burstLines.toArray(burstLineList);
        final int rowLength = endX - startX;
        final double[] peakLine = new double[rowLength];
        final double[] peakLineI = new double[rowLength];
        final double[] peakLineQ = new double[rowLength];
        final double[] sumLine = new double[rowLength];
        final int[] avgTotals = new int[rowLength];
        Arrays.fill(peakLine, -Float.MAX_VALUE);
        Arrays.fill(sumLine, 0.0);
        Arrays.fill(avgTotals, 0);

        double Ival, Qval, intensity;

        final Vector<short[]> srcDataListI = new Vector<short[]>(3);
        final Vector<short[]> srcDataListQ = new Vector<short[]>(3);

        try {
            // get all burst lines
            getBurstLines(burstLineList, srcBandI, srcBandQ, startX, endX, srcDataListI, srcDataListQ, pm);

            if(srcDataListI.isEmpty())
                return false;

            final int dataListSize = srcDataListI.size();
            // for all x peakpick or average from the bursts
            for (int x = startX, i = 0; x < endX; ++x, ++i) {

                for(int j=0; j < dataListSize; ++j) {

                    final short[] srcDataI = srcDataListI.get(j);
                    final short[] srcDataQ = srcDataListQ.get(j);

                    Ival = srcDataI[i];
                    Qval = srcDataQ[i];

                    intensity = (Ival * Ival) + (Qval * Qval);
                    if (intensity > peakLine[i]) {
                        peakLine[i] = intensity;
                        peakLineI[i] = Ival;
                        peakLineQ[i] = Qval;
                    }

                    if (average) {
                        // average with neighbours
                        if(!isInvalid(Ival, Qval, zeroThresholdSmall)) {
                            sumLine[i] += intensity;
                            avgTotals[i] += 1;
                        } if(i > 0) {
                            addToAverage(i, srcDataI[i-1], srcDataQ[i-1], sumLine, avgTotals);
                        } if(i < srcDataI.length-1) {
                            addToAverage(i, srcDataI[i+1], srcDataQ[i+1], sumLine, avgTotals);
                        }
                    }
                }

                if(average && avgTotals[i] > 1)
                    sumLine[i] /= avgTotals[i];
            }

            if (produceIntensitiesOnly) {
                final ProductData rawData = targetTileIntensity.getRawSamples();
                final int stride = targetLine * targetTileIntensity.getWidth();

                if (average) {

                    for (int x = startX, i = 0; x < endX; ++x, ++i) {
                        rawData.setElemDoubleAt(stride + x, sumLine[i]);
                    }
                } else {
                    for (int x = startX, i = 0; x < endX; ++x, ++i) {
                        if(peakLine[i] == -Float.MAX_VALUE) {
                            peakLine[i] = 0;
                            //System.out.println("uninitPeak " + i + " at " + targetLine);
                        }
                        rawData.setElemDoubleAt(stride + x, peakLine[i]);
                    }
                }
            } else {
                final ProductData rawDataI = targetTileI.getRawSamples();
                final ProductData rawDataQ = targetTileQ.getRawSamples();
                final int stride = targetLine * targetTileI.getWidth();

                for (int x = startX, i = 0; x < endX; ++x, ++i) {
                    if(peakLineI[i] == -Float.MAX_VALUE) {
                        peakLineI[i] = 0;
                        peakLineQ[i] = 0;
                    }
                    rawDataI.setElemDoubleAt(stride + x, peakLineI[i]);
                    rawDataQ.setElemDoubleAt(stride + x, peakLineQ[i]);
                }
            }
            return true;

        } catch (Exception e) {
            System.out.println("deburstTile " + e.toString());
        }
        return false;
    }

    private static void getBurstLines(final Integer[] burstLineList, final Band srcBandI, final Band srcBandQ,
                               final int startX, final int endX,
                               final Vector<short[]> srcDataListI, final Vector<short[]> srcDataListQ,
                               final ProgressMonitor pm) {
        Tile sourceRasterI, sourceRasterQ;
        final int srcBandHeight = srcBandI.getRasterHeight() - 1;
        final int srcBandWidth = srcBandI.getRasterWidth() - 1;

        for (Integer y : burstLineList) {
            if (y > srcBandHeight) continue;

            final Rectangle sourceRectangle = new Rectangle(startX, y, endX, 1);
            sourceRasterI = getSourceTile(srcBandI, sourceRectangle, pm);
            final short[] srcDataI = (short[]) sourceRasterI.getRawSamples().getElems();
            sourceRasterQ = getSourceTile(srcBandQ, sourceRectangle, pm);
            final short[] srcDataQ = (short[]) sourceRasterQ.getRawSamples().getElems();

            int invalidCount = 0;
            int total = 0;
            final int max = Math.min(srcBandWidth, srcDataI.length);
            for(int i=500; i < max; i+= 50) {
                if(isInvalid(srcDataI[i], srcDataQ[i], zeroThreshold))
                    ++invalidCount;
                ++total;
            }
            if(invalidCount / (float)total > 0.4)  {
                //System.out.println("skipping " + y);
                continue;
            }

            srcDataListI.add(srcDataI);
            srcDataListQ.add(srcDataQ);
        }
    }

    private static void addToAverage(int i, double Ival, double Qval,
                                     final double[] sumLine, final int[] avgTotals) {
        if(!isInvalid(Ival, Qval, zeroThresholdSmall)) {
            sumLine[i] += (Ival * Ival) + (Qval * Qval);
            avgTotals[i] += 1;
        }
    }

    private static boolean isInvalid(final double i, final double q, final double threshold) {
        return i > -threshold && i < threshold && q > -threshold && q < threshold;
    }

    private final static class LineTime {
        boolean visited = false;
        final int line;
        final double time;

        LineTime(final int y, final double utc) {
            line = y;
            time = utc;
        }
    }

    private final static class LineTimeComparator implements Comparator<LineTime> {
        public int compare(LineTime a, LineTime b) {
            if (a.time < b.time) return -1;
            else if (a.time > b.time) return 1;
            return 0;
        }
    }

    private final static class ComplexBand {
        final Band i;
        final Band q;
        public ComplexBand(final Band iBand, final Band qBand) {
            i = iBand;
            q = qBand;
        }
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(WSSDeBurstOp.class);
        }
    }
}
