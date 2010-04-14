
package org.esa.nest.dat.actions.productLibrary.model.dataprovider;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.util.ProductUtils;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.db.ProductDB;
import org.esa.nest.db.ProductEntry;
import org.esa.nest.util.ResourceUtils;

import javax.media.jai.JAI;
import javax.media.jai.PlanarImage;
import javax.media.jai.RasterFactory;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.SubsampleAverageDescriptor;
import javax.swing.*;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableColumn;
import java.awt.*;
import java.awt.image.*;
import java.io.File;
import java.io.IOException;
import java.util.Comparator;

public class QuicklookProvider implements DataProvider {

    private final static Comparator quickLookComparator = new QuickLookComparator();
    private final int maxWidth;
    private TableColumn quickLookColumn;
    private final File dbSystemFolder;

    private static final String QUICKLOOK_PREFIX = "QL_";
    private static final String QUICKLOOK_EXT = ".jpg";

    public QuicklookProvider(final int maxWidth) throws IOException {
        this.maxWidth = maxWidth;
        this.dbSystemFolder = ProductDB.instance().getDBSystemDir();
    }

    public boolean mustCreateData(final ProductEntry entry) {
        final File quickLookFile = getQuickLookFile(dbSystemFolder, entry.getId());
        return !quickLookFile.exists() || quickLookFile.length() == 0;
    }

    public void createData(final ProductEntry entry) throws IOException {
      /*  try {
            final Product product = entry.getProduct();
            final File storageDir = repository.getStorageDir();
            final File quickLookFile = getQuickLookFile(storageDir, entry.getProductFile().getName());
            quickLookFile.createNewFile();
            final BufferedImage bufferedImage = createQuickLook(product);

            if(isComplex(product)) {
                ImageIO.write(average(bufferedImage), "JPG", quickLookFile);
            } else {   // detected
                ImageIO.write(bufferedImage, "JPG", quickLookFile);
            }
            //ImageIO.write(downSampleImage(bufferedImage), "JPG", quickLookFile);
        } catch(Exception e) {
            System.out.println("Quicklook create data failed :"+e.getMessage());
            throw new IOException(e);
        }     */
    }

    private static boolean isComplex(Product product) {
        final MetadataElement root = product.getMetadataRoot();
        if(root != null) {
            final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
            if(absRoot != null && absRoot.getAttributeString(AbstractMetadata.SAMPLE_TYPE, "").equals("COMPLEX"))
                return true;
        }
        return false;
    }

    static BufferedImage averageWithJAI(BufferedImage image) {
        final int width = 8;
        final int height = 8;
        final RenderedOp rendered = JAI.create("boxfilter", image,
                                 width, height,
                                 width/2, height/2);

        return rendered.getAsBufferedImage();
    }

    static RenderedImage downSampleImage(RenderedImage image) {

        final int size = 4;
        final double scaleX = 1.0 / size;
        final double scaleY = 1.0 / size;
        return SubsampleAverageDescriptor.create(image, scaleX, scaleY, null);
    }

    private static BufferedImage average(BufferedImage image) {

        final int rangeFactor = 4;
        final int azimuthFactor = 4;
        final int rangeAzimuth = rangeFactor * azimuthFactor;
        final Raster raster = image.getData();

        final int w = image.getWidth() / rangeFactor;
        final int h = image.getHeight() / azimuthFactor;
        int index = 0;
        final byte[] data = new byte[w*h];

        for (int ty = 0; ty < h; ++ty) {
            final int yStart = ty * azimuthFactor;
            final int yEnd = yStart + azimuthFactor;

            for (int tx = 0; tx < w; ++tx) {
                final int xStart = tx * rangeFactor;
                final int xEnd = xStart + rangeFactor;

                double meanValue = 0.0;
                for (int y = yStart; y < yEnd; ++y) {
                    for (int x = xStart; x < xEnd; ++x) {

                        meanValue += raster.getSample(x, y, 0);
                    }
                }
                meanValue /= rangeAzimuth;

                data[index++] = (byte)meanValue;
            }
        }

        return createRenderedImage(data, w, h, raster);
    }
    
    private static BufferedImage createRenderedImage(byte[] array, int w, int h, Raster raster) {

        // create rendered image with demension being width by height
        final SampleModel sm = RasterFactory.createBandedSampleModel(DataBuffer.TYPE_BYTE, w, h, 1);
        final ColorModel cm = PlanarImage.createColorModel(sm);
        final DataBufferByte dataBuffer = new DataBufferByte(array, array.length);
        final WritableRaster writeraster = RasterFactory.createWritableRaster(sm, dataBuffer, new Point(0,0));

        return new BufferedImage(cm, writeraster, cm.isAlphaPremultiplied(), null);
    }

    public Object getData(final ProductEntry entry) throws IOException {
     /*   final File storageDir = repository.getStorageDir();
        final File quickLookFile = getQuickLookFile(storageDir, entry.getProductFile().getName());
        BufferedImage bufferedImage = null;
        if (quickLookFile.canRead()) {
            final FileInputStream fis = new FileInputStream(quickLookFile);
            try {
                bufferedImage = ImageIO.read(fis);
            } finally {
                fis.close();
            }
        }
        final DataObject data = new DataObject();
        data.quickLook = bufferedImage;
        return data;        */
        return null;
    }

    /**
     * Returns the {@link java.util.Comparator} for the data provided by this <code>DataProvider</code>.
     *
     * @return the comparator.
     */
    public Comparator getComparator() {
        return quickLookComparator;
    }

    public void cleanUp(final ProductEntry entry) {
    /*    // nothing to do with propertyMap
        final File quickLookFile = getQuickLookFile(repository.getStorageDir(), entry.getProductFile().getName());
        if (quickLookFile != null && quickLookFile.exists()) {
            quickLookFile.delete();
        }   */
    }

    private BufferedImage createQuickLook(final Product product) throws IOException {
        final ProductSubsetDef productSubsetDef = new ProductSubsetDef("subset");
        int scaleFactor = Math.max(product.getSceneRasterWidth(), product.getSceneRasterHeight()) / maxWidth;
        if (scaleFactor < 1) {
            scaleFactor = 1;
        }
        productSubsetDef.setSubSampling(scaleFactor, scaleFactor);

        final String quicklookBandName = ProductUtils.findSuitableQuicklookBandName(product);
        final String expression = quicklookBandName + "==0 ? 0 : 10 * log10(abs("+quicklookBandName+"))";
        final VirtualBand virtBand = new VirtualBand("QuickLook",
                ProductData.TYPE_FLOAT32,
                product.getSceneRasterWidth(),
                product.getSceneRasterHeight(),
                expression);
        virtBand.setSynthetic(true);
        product.addBand(virtBand);
        
        final Product productSubset = product.createSubset(productSubsetDef, null, null);

        return ProductUtils.createColorIndexedImage(productSubset.getBand(virtBand.getName()), ProgressMonitor.NULL);
    }

    public TableColumn getTableColumn() {
        if (quickLookColumn == null) {
            quickLookColumn = new TableColumn();
            quickLookColumn.setHeaderValue("Quick Look");        /*I18N*/
            quickLookColumn.setPreferredWidth(200);
            quickLookColumn.setResizable(true);
            quickLookColumn.setCellRenderer(new QuickLookRenderer(200));
            quickLookColumn.setCellEditor(new QuickLookEditor());
        }
        return quickLookColumn;
    }

    private static File getQuickLookFile(final File storageDir, final int id) {
        return new File(storageDir, QUICKLOOK_PREFIX + id + QUICKLOOK_EXT);
    }

    private static class DataObject {

        public BufferedImage quickLook = null;
    }

    private static class QuickLookRenderer extends DefaultTableCellRenderer {

        private final int rowHeight;
        private JLabel tableComponent;

        public QuickLookRenderer(final int height) {
            rowHeight = height + 3;
        }

        @Override
        public Component getTableCellRendererComponent(final JTable table,
                                                       final Object value,
                                                       final boolean isSelected,
                                                       final boolean hasFocus,
                                                       final int row,
                                                       final int column) {
            if (tableComponent == null) {
                tableComponent = (JLabel) super.getTableCellRendererComponent(table,
                                                                              value,
                                                                              isSelected,
                                                                              hasFocus,
                                                                              row,
                                                                              column);
                tableComponent.setText("");
                tableComponent.setVerticalAlignment(SwingConstants.CENTER);
                tableComponent.setHorizontalAlignment(SwingConstants.CENTER);

            }

            setBackground(table, isSelected);

            if (value == null) {
                tableComponent.setIcon(null);
                tableComponent.setText("");
                return tableComponent;
            }
            final DataObject data;
            if (value instanceof DataObject) {
                data = (DataObject) value;
            } else {
                data = new DataObject();
            }

            if (data.quickLook != null) {
                final BufferedImage image = data.quickLook;
                final TableColumn tableColumn = table.getColumnModel().getColumn(column);
                int cellWidth = tableColumn.getWidth();
                int cellHeight = tableColumn.getWidth();
                if(image.getHeight() > image.getWidth())
                    cellWidth = -1;
                else
                    cellHeight = -1;
                tableComponent.setIcon(
                        new ImageIcon(image.getScaledInstance(cellWidth, cellHeight, BufferedImage.SCALE_FAST)));
                tableComponent.setText("");
                setTableRowHeight(table, row);
            } else {
                tableComponent.setIcon(null);
                tableComponent.setText("Not available!");
            }
            return tableComponent;
        }

        private void setBackground(final JTable table, final boolean isSelected) {
            Color backGroundColor = table.getBackground();
            if (isSelected) {
                backGroundColor = table.getSelectionBackground();
            }
            tableComponent.setBorder(BorderFactory.createLineBorder(backGroundColor, 3));
            tableComponent.setBackground(backGroundColor);
        }

        private void setTableRowHeight(final JTable table, final int row) {
            if (table.getRowHeight(row) < rowHeight) {
                table.setRowHeight(row, rowHeight);
            }
        }
    }

    public static class QuickLookEditor extends AbstractCellEditor implements TableCellEditor {

        private final JScrollPane scrollPane;

        public QuickLookEditor() {

            scrollPane = new JScrollPane();
            scrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
            scrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
            scrollPane.getViewport().setOpaque(false);
        }

        public Component getTableCellEditorComponent(final JTable table,
                                                     final Object value,
                                                     final boolean isSelected,
                                                     final int row,
                                                     final int column) {
            if (!(value instanceof DataObject)) {
                return null;
            }
            final BufferedImage image = ((DataObject) value).quickLook;
            if (image == null) {
                return null;
            }
            scrollPane.setViewportView(
                    new JLabel(new ImageIcon(image.getScaledInstance(-1, -1, BufferedImage.SCALE_AREA_AVERAGING))));
            final Color backgroundColor = table.getSelectionBackground();
            scrollPane.setBackground(backgroundColor);
            scrollPane.setBorder(BorderFactory.createLineBorder(backgroundColor, 3));

            return scrollPane;
        }

        public Object getCellEditorValue() {
            return null;
        }
    }


    private static class QuickLookComparator implements Comparator {

        public int compare(final Object o1, final Object o2) {
            if (o1 == o2) {
                return 0;
            }
            if (o1 == null) {
                return -1;
            } else if (o2 == null) {
                return 1;
            }

            final BufferedImage image1 = ((DataObject) o1).quickLook;
            final BufferedImage image2 = ((DataObject) o2).quickLook;

            if (image1 == null) {
                return -1;
            } else if (image2 == null) {
                return 1;
            }

            final Integer height1 = image1.getHeight();
            final Integer height2 = image2.getHeight();

            return height1.compareTo(height2);
        }
    }
}