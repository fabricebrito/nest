package org.esa.nest.dat.views.polarview;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.ui.product.ProductSceneImage;
import org.esa.beam.framework.ui.BasicView;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.ArrayList;
import java.text.DecimalFormat;

/**
 * NEST
 * User: lveci
 * Date: Dec 1, 2008
 */
public final class PolarView extends BasicView implements ActionListener, PopupMenuListener, MouseListener, MouseMotionListener {

    private final Product product;
    private MetadataElement spectraMetadataRoot = null;

    private final int numRecords;
    private final int recordLength;
    private int numDirBins;
    private int numWLBins;

    private float firstDirBins = 0;
    private float dirBinStep = 0;
    private float firstWLBin = 0;
    private float lastWLBin = 0;

    private ProductData.UTC zeroDopplerTime;
    private double minSpectrum = 0;
    private double maxSpectrum = 255;
    private double maxSpecDir = 0;
    private double maxSpecWL = 0;

    private double windSpeed = 0;
    private double windDirection = 0;
    private double sarWaveHeight = 0;
    private double sarAzShiftVar = 0;
    private double backscatter = 0;

    private int currentRecord = 0;

    private enum Unit { REAL, IMAGINARY, BOTH, AMPLITUDE, INTENSITY, MULTIPLIED }
    private final String[] unitTypes = new String[] { "Real", "Imaginary", "Both", "Amplitude", "Intensity", "Multiplied" };
    private enum WaveProductType { CROSS_SPECTRA, WAVE_SPECTRA }

    private final ControlPanel controlPanel;
    private final PolarCanvas graphView;
    private final ReadoutCanvas readoutView;
    private Unit graphUnit = Unit.REAL;
    private WaveProductType waveProductType = WaveProductType.WAVE_SPECTRA;
    private float spectrum[][] = null;

    public static final Color colourTable[] = (new Color[]{
            new Color(0, 0, 0), new Color(0, 0, 255), new Color(0, 255, 255),
            new Color(0, 255, 0), new Color(255, 255, 0), new Color(255, 0, 0)
    });
    private static final double rings[] = {50.0, 100.0, 200.0};
    private static final String ringTextStrings[] = {"200 (m)", "100 (m)", "50 (m)"};

    public PolarView(Product prod, ProductSceneImage sceneImage) {
        //super(sceneImage);
        product = prod;

        if(prod.getProductType().equals("ASA_WVW_2P"))
            waveProductType = WaveProductType.WAVE_SPECTRA;
        else if(prod.getProductType().equals("ASA_WVS_1P"))
            waveProductType = WaveProductType.CROSS_SPECTRA;

        getMetadata();

        final RasterDataNode[] rasters = sceneImage.getRasters();
        final RasterDataNode rasterNode = rasters[0];
        numRecords = rasterNode.getRasterHeight()-1;
        recordLength = rasterNode.getRasterWidth();

        graphView = new PolarCanvas();

        readoutView = new ReadoutCanvas();

        CreateContextMenu();
        addMouseListener(this);
        addMouseMotionListener(this);

        this.setLayout(new BorderLayout());

        controlPanel = new ControlPanel(this);
        this.add(controlPanel, BorderLayout.SOUTH);

        createPlot(currentRecord);
    }

    private void getMetadata() {
        final MetadataElement root = product.getMetadataRoot();
        final MetadataElement sph = root.getElement("SPH");
        numDirBins = sph.getAttributeInt("NUM_DIR_BINS", 0);
        numWLBins = sph.getAttributeInt("NUM_WL_BINS", 0);
        firstDirBins = (float) sph.getAttributeDouble("FIRST_DIR_BIN", 0);
        dirBinStep = (float) sph.getAttributeDouble("DIR_BIN_STEP", 0);
        firstWLBin = (float) sph.getAttributeDouble("FIRST_WL_BIN", 0);
        lastWLBin = (float) sph.getAttributeDouble("LAST_WL_BIN", 0);

        if(waveProductType == WaveProductType.WAVE_SPECTRA) {
            spectraMetadataRoot = root.getElement("OCEAN_WAVE_SPECTRA_MDS");
        } else {
            spectraMetadataRoot = root.getElement("CROSS_SPECTRA_MDS");
        }
    }

    private void getSpectraMetadata(int rec) {
        try {
            final String elemName = spectraMetadataRoot.getName()+'.'+(rec+1);
            final MetadataElement spectraMetadata = spectraMetadataRoot.getElement(elemName);

            zeroDopplerTime = spectraMetadata.getAttributeUTC("zero_doppler_time");
            maxSpecDir = spectraMetadata.getAttributeDouble("spec_max_dir", 0);
            maxSpecWL = spectraMetadata.getAttributeDouble("spec_max_wl", 0);

            if(waveProductType == WaveProductType.WAVE_SPECTRA) {
                minSpectrum = spectraMetadata.getAttributeDouble("min_spectrum", 0);
                maxSpectrum = spectraMetadata.getAttributeDouble("max_spectrum", 255);
                windSpeed = spectraMetadata.getAttributeDouble("wind_speed", 0);
                windDirection = spectraMetadata.getAttributeDouble("wind_direction", 0);
                sarWaveHeight = spectraMetadata.getAttributeDouble("SAR_wave_height", 0);
                sarAzShiftVar = spectraMetadata.getAttributeDouble("SAR_az_shift_var", 0);
                backscatter = spectraMetadata.getAttributeDouble("backscatter", 0);
            }
        } catch(Exception e) {
            System.out.println("Unable to get metadata for "+spectraMetadataRoot.getName());
        }

        DecimalFormat frmt = new DecimalFormat("0.0000");

        final ArrayList<String> metadataList = new ArrayList<String>(10);
        metadataList.add("Time: " + zeroDopplerTime.toString());
        metadataList.add("Direction of Spectrum Max: " + maxSpecDir + " deg");
        metadataList.add("Wavelength of Spectrum Max: " + frmt.format(maxSpecWL) + " m");

        if(waveProductType == WaveProductType.WAVE_SPECTRA) {
            metadataList.add("Min Spectrum: " + frmt.format(minSpectrum));
            metadataList.add("Max Spectrum: " + frmt.format(maxSpectrum));

            metadataList.add("Wind Speed: " + windSpeed + " m/s");
            metadataList.add("Wind Direction: " + windDirection + " deg");
            metadataList.add("SAR Swell Wave Height: " + frmt.format(sarWaveHeight) + " m");
            metadataList.add("SAR Azimuth Shift Var: " + frmt.format(sarAzShiftVar) + " m^2");
            metadataList.add("Backscatter: " + frmt.format(backscatter) + " dB");
        }

        readoutView.setMetadata(metadataList.toArray(new String[metadataList.size()]));
    }

    private void createPlot(int rec) {

        spectrum = getSpectrum(0, rec, graphUnit != Unit.IMAGINARY);
        getSpectraMetadata(rec);

        float minValue = 0;//Float.MAX_VALUE;
        float maxValue = 255;//Float.MIN_VALUE;

        // complex data
        if (graphUnit == Unit.AMPLITUDE || graphUnit == Unit.INTENSITY || graphUnit == Unit.BOTH || graphUnit == Unit.MULTIPLIED) {
            final float imagSpectrum[][] = getSpectrum(1, rec, false);
            minValue = Float.MAX_VALUE;
            maxValue = Float.MIN_VALUE;
            final int halfCircle = spectrum.length / 2;
            for (int i = 0; i < spectrum.length; i++) {
                for (int j = 0; j < spectrum[0].length; j++) {
                    final float rS = spectrum[i][j];
                    final float iS = imagSpectrum[i][j];
                    float v = rS;
                    if (graphUnit == Unit.BOTH) {
                        if (i >= halfCircle)
                            v = iS;
                    } else if (graphUnit == Unit.MULTIPLIED) {
                        if (sign(rS) == sign(iS))
                            v *= iS;
                        else
                            v = 0.0F;
                    } else {
                        if (sign(rS) == sign(iS))
                            v = rS * rS + iS * iS;
                        else
                            v = 0.0F;
                        if (graphUnit == Unit.INTENSITY)
                            v = (float) Math.sqrt(v);
                    }
                    spectrum[i][j] = v;
                    minValue = Math.min(minValue, v);
                    maxValue = Math.max(maxValue, v);
                }
            }
        }

        final float thFirst;
        double logr;
        final float rStep = (float) (Math.log(lastWLBin) - Math.log(firstWLBin)) / (float) (numWLBins - 1);
        logr = Math.log(firstWLBin) - (rStep / 2.0);
        final double colourRange[] = {(double) minValue, (double) maxValue};
        final double radialRange[] = {0.0, 333.33333333333};
        final float thStep;

        if(waveProductType == WaveProductType.WAVE_SPECTRA) {
            thFirst = firstDirBins - 5f;
            thStep = dirBinStep;
        } else {
            thFirst = firstDirBins + 5f;
            thStep = -dirBinStep;
        }

        final int nWl = spectrum[0].length;
        final float radii[] = new float[nWl + 1];
        for (int j = 0; j <= nWl; j++) {
            radii[j] = (float) (10000.0 / Math.exp(logr));
            logr += rStep;
        }

        final PolarData data = new PolarData(spectrum, 90f + thFirst, thStep, radii);

        graphView.getColourAxis().setDataRange(colourRange);
        graphView.getRadialAxis().setAutoRange(false);
        graphView.getRadialAxis().setDataRange(radialRange);
        graphView.getRadialAxis().setRange(radialRange[0], radialRange[1], 4);
        graphView.getRadialAxis().setTitle("Wavelength (m)");
        graphView.setRings(rings, null);
        data.setColorScale(ColourScale.newCustomScale(colourRange));
        graphView.setData(data);

        controlPanel.updateControls();
        repaint();
    }

    private float[][] getSpectrum(int imageNum, int rec, boolean getReal) {

        float[] dataset;
        try {
            final RasterDataNode rasterNode = product.getBandAt(imageNum);
            rasterNode.loadRasterData();
            dataset = new float[recordLength];
            rasterNode.getPixels(0, rec, recordLength, 1, dataset);

        } catch (IOException e) {
            System.out.println(e.getMessage());
            return null;
        }

        final int Nd2 = numDirBins / 2;
        final float minValue = 0;
        final float maxValue = 255;
        final float scale = (maxValue - minValue) / 255F;
        final float spectrum[][] = new float[numDirBins][numWLBins];
        int index = 0;

        if(waveProductType == WaveProductType.WAVE_SPECTRA) {
            for (int i = 0; i < numDirBins; i++) {
                for (int j = 0; j < numWLBins; j++) {
                    spectrum[i][j] = dataset[index++] * scale + minValue;
                }
            }
        } else {
            for (int i = 0; i < Nd2; i++) {
                for (int j = 0; j < numWLBins; j++) {
                    spectrum[i][j] = dataset[index++] * scale + minValue;
                }
            }

            if (getReal) {
                for (int i = 0; i < Nd2; i++) {
                    System.arraycopy(spectrum[i], 0, spectrum[i + Nd2], 0, numWLBins);
                }
            } else {
                for (int i = 0; i < Nd2; i++) {
                    for (int j = 0; j < numWLBins; j++) {
                        spectrum[i + Nd2][j] = -spectrum[i][j];
                    }
                }
            }
        }
        return spectrum;
    }

    private static int sign(float f) {
        return f < 0.0F ? -1 : 1;
    }

    /**
     * Paints the panel component
     *
     * @param g The Graphics
     */
    @Override
    protected void paintComponent(java.awt.Graphics g) {
        super.paintComponent(g);

        ((Graphics2D) g).setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                RenderingHints.VALUE_ANTIALIAS_ON);

        graphView.setSize(getWidth(), getHeight());
        graphView.paint(g);

        readoutView.paint(g);
    }

    private void CreateContextMenu() {
        final ImageIcon opIcon = null;

    }


    @Override
    public JPopupMenu createPopupMenu(Component component) {
        return null;
    }

    @Override
    public JPopupMenu createPopupMenu(MouseEvent event) {
        final JPopupMenu popup = new JPopupMenu();

        final JMenuItem itemNext = createMenuItem("Next");
        popup.add(itemNext);
        itemNext.setEnabled(currentRecord < numRecords);

        final JMenuItem itemPrev = createMenuItem("Previous");
        popup.add(itemPrev);
        itemPrev.setEnabled(currentRecord > 0);

        final JMenuItem itemColourScale = createMenuItem("Colour Scale");
        popup.add(itemColourScale);

        final JMenu unitMenu = new JMenu("Unit");
        popup.add(unitMenu);

        createCheckedMenuItem(unitTypes[Unit.REAL.ordinal()], unitMenu, graphUnit == Unit.REAL);
        createCheckedMenuItem(unitTypes[Unit.IMAGINARY.ordinal()], unitMenu, graphUnit == Unit.IMAGINARY);
        createCheckedMenuItem(unitTypes[Unit.BOTH.ordinal()], unitMenu, graphUnit == Unit.BOTH);
        createCheckedMenuItem(unitTypes[Unit.AMPLITUDE.ordinal()], unitMenu, graphUnit == Unit.AMPLITUDE);
        createCheckedMenuItem(unitTypes[Unit.INTENSITY.ordinal()], unitMenu, graphUnit == Unit.INTENSITY);
        createCheckedMenuItem(unitTypes[Unit.MULTIPLIED.ordinal()], unitMenu, graphUnit == Unit.MULTIPLIED);

        popup.setLabel("Justification");
        popup.setBorder(new BevelBorder(BevelBorder.RAISED));
        popup.addPopupMenuListener(this);
        popup.show(this, event.getX(), event.getY());

        return popup;
    }

    private JMenuItem createMenuItem(String name) {
        final JMenuItem item = new JMenuItem(name);
        item.setHorizontalTextPosition(JMenuItem.RIGHT);
        item.addActionListener(this);
        return item;
    }

    private JCheckBoxMenuItem createCheckedMenuItem(String name, JMenu parent, boolean state) {
        final JCheckBoxMenuItem item = new JCheckBoxMenuItem(name);
        item.setHorizontalTextPosition(JMenuItem.RIGHT);
        item.addActionListener(this);
        parent.add(item);
        return item;
    }

    /**
     * Handles menu item pressed events
     *
     * @param event the action event
     */
    public void actionPerformed(ActionEvent event) {

        if(event.getActionCommand().equals("Next")) {
            showNextPlot();
        } else if(event.getActionCommand().equals("Previous")) {
            showPreviousPlot();
        } else if(event.getActionCommand().equals("Colour Scale")) {
            callColourScaleDlg();  
        } else if(event.getActionCommand().equals("Real")) {
            graphUnit = Unit.REAL;
            createPlot(currentRecord);
        } else if(event.getActionCommand().equals("Imaginary")) {
            graphUnit = Unit.IMAGINARY;
            createPlot(currentRecord);
        } else if(event.getActionCommand().equals("Both")) {
            graphUnit = Unit.BOTH;
            createPlot(currentRecord);
        } else if(event.getActionCommand().equals("Amplitude")) {
            graphUnit = Unit.AMPLITUDE;
            createPlot(currentRecord);
        } else if(event.getActionCommand().equals("Intensity")) {
            graphUnit = Unit.INTENSITY;
            createPlot(currentRecord);
        } else if(event.getActionCommand().equals("Multiplied")) {
            graphUnit = Unit.MULTIPLIED;
            createPlot(currentRecord);
        }
    }

    int getCurrentRecord() {
        return currentRecord;
    }

    int getNumRecords() {
        return numRecords;
    }

    void showNextPlot() {
        createPlot(++currentRecord);
    }

    void showPreviousPlot() {
        createPlot(--currentRecord);
    }

    void showPlot(int record) {
        currentRecord = record;
        createPlot(currentRecord);
    }

    private void checkPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
            createPopupMenu(e);
        }
    }

    public void popupMenuWillBecomeVisible(PopupMenuEvent e) {
    }

    public void popupMenuWillBecomeInvisible(PopupMenuEvent e) {
    }

    public void popupMenuCanceled(PopupMenuEvent e) {
    }


    /**
     * Handle mouse pressed event
     *
     * @param e the mouse event
     */
    public void mousePressed(MouseEvent e) {
        checkPopup(e);

    }

    /**
     * Handle mouse clicked event
     *
     * @param e the mouse event
     */
    public void mouseClicked(MouseEvent e) {
        checkPopup(e);

        final Object src = e.getSource();
        if(src == graphView) {
            final Axis axis = graphView.selectAxis(e.getPoint());
            if(axis != null && axis == graphView.getColourAxis()) {
                //axisPanel = new AxisDialog(this);
                //axisPanel.show(axis, graphView, p);
            }
        }
    }

    public void mouseEntered(MouseEvent e) {
    }

    public void mouseExited(MouseEvent e) {
    }

    /**
     * Handle mouse released event
     *
     * @param e the mouse event
     */
    public void mouseReleased(MouseEvent e) {
        checkPopup(e);


        repaint();
    }

    /**
     * Handle mouse dragged event
     *
     * @param e the mouse event
     */
    public void mouseDragged(MouseEvent e) {

    }

    /**
     * Handle mouse moved event
     *
     * @param e the mouse event
     */
    public void mouseMoved(MouseEvent e) {
        updateReadout(e);
    }

    private void callColourScaleDlg() {
        final ColourScaleDialog dlg = new ColourScaleDialog(graphView.getColourAxis());
        dlg.show();
    }

    private void updateReadout(MouseEvent evt) {
        if(spectrum == null)
            return;
        final double rTh[] = graphView.getRTheta(evt.getPoint());
        if(rTh != null) {
            final float thFirst;
            final int thBin;
            final float thStep;
            final int element;
            final int direction;

            final float rStep = (float) (Math.log(lastWLBin) - Math.log(firstWLBin)) / (float) (numWLBins - 1);
            int wvBin = (int)(((rStep / 2.0 + Math.log(10000.0 / rTh[0])) - Math.log(firstWLBin)) / rStep);
            wvBin = Math.min(wvBin, spectrum[0].length - 1);
            final int wl = (int)Math.round(Math.exp((double)wvBin * rStep + Math.log(firstWLBin)));

            if(waveProductType == WaveProductType.CROSS_SPECTRA) {
                thFirst = firstDirBins - 5f;
                thStep = dirBinStep;
                thBin = (int)(((rTh[1] - (double)thFirst) % 360.0) / (double)thStep);
                element = (thBin % (spectrum.length / 2)) * spectrum[0].length + wvBin;
                direction = (int)((float)thBin * thStep + thStep / 2.0f + thFirst);
            } else {
                thFirst = firstDirBins + 5f;
                thStep = -dirBinStep;
                thBin = (int)((((360.0 - rTh[1]) + (double)thFirst) % 360.0) / (double)(-thStep));
                element = thBin * spectrum[0].length + wvBin;
                direction = (int)(-((float)thBin * thStep + thStep / 2.0f + thFirst));
            }             

            final ArrayList<String> readoutList = new ArrayList<String>(5);
            readoutList.add("Record: " + (currentRecord+1) + " of " + (numRecords+1));
            readoutList.add("Wavelength: " + wl + " m");
            readoutList.add("Direction: " + direction + " deg");
            readoutList.add("Bin: " + (thBin+1) + "," + (wvBin+1) + " Element: " + element);
            readoutList.add("Value: " + spectrum[thBin][wvBin]);

            readoutView.setReadout(readoutList.toArray(new String[readoutList.size()]));

            repaint();
            controlPanel.repaint();
        }
    }

}
