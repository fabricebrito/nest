/*
 * $Id: AbstractProductReader.java,v 1.1 2009-04-28 14:39:32 lveci Exp $
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
package org.esa.beam.framework.dataio;


import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.TreeNode;

import java.io.File;
import java.io.IOException;

/**
 * The <code>AbstractProductReader</code>  class can be used as a base class for new product reader implementations. The
 * only two methods which clients must implement are <code>readProductNodes()</code> and <code>readBandData</code>
 * methods.
 *
 * @author Norman Fomferra
 * @version $Revision: 1.1 $ $Date: 2009-04-28 14:39:32 $
 * @see #readProductNodes
 * @see #readBandRasterData
 */
public abstract class AbstractProductReader implements ProductReader {

    /**
     * The reader plug-in responsible for creating this reader.
     */
    private final ProductReaderPlugIn _readerPlugIn;

    /**
     * The input source
     */
    private Object _input;

    /**
     * The spectral and spatial subset definition used to read from the original data source.
     */
    private ProductSubsetDef _subsetDef;

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    protected AbstractProductReader(ProductReaderPlugIn readerPlugIn) {
        _readerPlugIn = readerPlugIn;
    }

    /**
     * Returns the plug-in which created this product reader.
     *
     * @return the product reader plug-in, should never be <code>null</code>
     */
    public ProductReaderPlugIn getReaderPlugIn() {
        return _readerPlugIn;
    }

    /**
     * Retrives the current output destination object. The return value might be <code>null</code> if the
     * <code>setInput()</code> method has not been called so far.
     */
    public Object getInput() {
        return _input;
    }

    /**
     * Checks if this reader ignores metadata or not.
     *
     * @return <code>true</code> if so
     */
    public boolean isMetadataIgnored() {
        boolean ignoreMetadata = false;
        if (_subsetDef != null) {
            ignoreMetadata = _subsetDef.isIgnoreMetadata();
        }
        return ignoreMetadata;
    }

    /**
     * Returns the subset information with which this data product is read from its physical source.
     *
     * @return the subset information, can be <code>null</code>
     */
    public ProductSubsetDef getSubsetDef() {
        return _subsetDef;
    }

    /**
     * Sets the subset information. This implemetation is protected to overwrite in the inherided class to ensure that
     * the subset information cannot be set from the <code>readProductNodes</code> method.
     *
     * @param subsetDef the subset definition
     */
    protected void setSubsetDef(ProductSubsetDef subsetDef) {
        _subsetDef = subsetDef;
    }


    /**
     * Tests whether or not a product node (a band, a tie-point grid or metadata element) with the given name is
     * accepted with respect to the optional spectral band subset. All accepted nodes will be part of the product read.
     *
     * @param name the node name
     * @return <code>true</code> if so
     */
    public boolean isNodeAccepted(String name) {
        return getSubsetDef() == null || getSubsetDef().isNodeAccepted(name);
    }

    /**
     * Reads the nodes of a data product and returns an in-memory representation of it.
     * <p/>
     * <p> The given subset info can be used to specify spatial and spectral portions of the original proudct. If the
     * subset is omitted, the complete product is read in.
     * <p/>
     * <p> Whether the band data - the actual pixel values - is read in immediately or later when pixels are requested,
     * is up to the implementation.
     *
     * @param input     an object representing a valid output for this product reader, might be a
     *                  <code>ImageInputStream</code> or other <code>Object</code> to use for future decoding.
     * @param subsetDef a spectral or spatial subset (or both) of the product. If <code>null</code>, the entire product
     *                  is read in
     * @throws IllegalArgumentException   if input type is not supported (see {@link ProductReaderPlugIn#getInputTypes()}).
     * @throws IOException                if an I/O error occurs
     * @throws IllegalFileFormatException if the file format is unknown.
     */
    public Product readProductNodes(Object input,
                                    ProductSubsetDef subsetDef) throws IOException {
        // (nf, 26.09.2007) removed (input == null) check, null inputs (= no sources) shall be allowed
        if (input != null && !isInstanceOfValidInputType(input)) {
            throw new IllegalArgumentException("invalid input source: " + input);
        }
        _input = input;
        setSubsetDef(subsetDef);
        final Product product = readProductNodesImpl();
        product.setModified(false);
        // (nf, 26.09.2007) added following snippet
        if (product.getProductReader() == null) {
            product.setProductReader(this);
        }
        return product;
    }

    /**
     * Provides an implementation of the <code>readProductNodes</code> interface method. Clients implementing this
     * method can be sure that the input object and eventually the subset information has already been set.
     * <p/>
     * <p>This method is called as a last step in the <code>readProductNodes(input, subsetInfo)</code> method.
     *
     * @return a new product instance
     * @throws IOException if an I/O error occurs
     */
    protected abstract Product readProductNodesImpl() throws IOException;

    /**
     * Reads raster data from the data source specified by the given destination band into the given in-memory buffer
     * and region.
     * <p/>
     * <p>For a complete description, please refer to the {@link ProductReader#readBandRasterData(org.esa.beam.framework.datamodel.Band,int,int,int,int,org.esa.beam.framework.datamodel.ProductData,com.bc.ceres.core.ProgressMonitor)}  interface definition}
     * of this method.
     * <p/>
     * <p>The <code>AbstractProductReader</code> implements this method using the <i>Template Method</i> pattern. The
     * template method in this case is the abstract method to which the call is delegated after an optional spatial
     * subset given by {@link #getSubsetDef()} has been applied to the input parameters.
     *
     * @param destBand    the destination band which identifies the data source from which to read the sample values
     * @param destOffsetX the X-offset in the band's raster co-ordinates
     * @param destOffsetY the Y-offset in the band's raster co-ordinates
     * @param destWidth   the width of region to be read given in the band's raster co-ordinates
     * @param destHeight  the height of region to be read given in the band's raster co-ordinates
     * @param destBuffer  the destination buffer which receives the sample values to be read
     * @param pm          a monitor to inform the user about progress
     * @throws IOException              if an I/O error occurs
     * @throws IllegalArgumentException if the number of elements destination buffer not equals <code>destWidth *
     *                                  destHeight</code> or the destination region is out of the band's raster
     * @see #readBandRasterDataImpl(int,int,int,int,int,int,org.esa.beam.framework.datamodel.Band,int,int,int,int,org.esa.beam.framework.datamodel.ProductData,com.bc.ceres.core.ProgressMonitor)
     * @see #getSubsetDef()
     * @see ProductReader#readBandRasterData(org.esa.beam.framework.datamodel.Band,int,int,int,int,org.esa.beam.framework.datamodel.ProductData,com.bc.ceres.core.ProgressMonitor)
     * @see org.esa.beam.framework.datamodel.Band#getRasterWidth()
     * @see org.esa.beam.framework.datamodel.Band#getRasterHeight()
     */
    public void readBandRasterData(Band destBand,
                                   int destOffsetX,
                                   int destOffsetY,
                                   int destWidth,
                                   int destHeight,
                                   ProductData destBuffer, ProgressMonitor pm) throws IOException {

        Guardian.assertNotNull("destBand", destBand);
        Guardian.assertNotNull("destBuffer", destBuffer);

        if (destBuffer.getNumElems() < destWidth * destHeight) {
            throw new IllegalArgumentException("destination buffer too small");
        }
        if (destBuffer.getNumElems() > destWidth * destHeight) {
            throw new IllegalArgumentException("destination buffer too big");
        }

        int sourceOffsetX = 0;
        int sourceOffsetY = 0;
        int sourceStepX = 1;
        int sourceStepY = 1;
        if (getSubsetDef() != null) {
            sourceStepX = getSubsetDef().getSubSamplingX();
            sourceStepY = getSubsetDef().getSubSamplingY();
            if (getSubsetDef().getRegion() != null) {
                sourceOffsetX = getSubsetDef().getRegion().x;
                sourceOffsetY = getSubsetDef().getRegion().y;
            }
        }
        sourceOffsetX += sourceStepX * destOffsetX;
        sourceOffsetY += sourceStepY * destOffsetY;
        int sourceWidth = sourceStepX * (destWidth - 1) + 1;
        int sourceHeight = sourceStepY * (destHeight - 1) + 1;

        readBandRasterDataImpl(sourceOffsetX,
                sourceOffsetY,
                sourceWidth,
                sourceHeight,
                sourceStepX,
                sourceStepY,
                destBand,
                destOffsetX,
                destOffsetY,
                destWidth,
                destHeight,
                destBuffer, pm);
    }

    /**
     * The template method which is called by the method after an optional spatial subset has been applied to the input
     * parameters.
     * <p/>
     * <p>The destination band, buffer and region parameters are exactly the ones passed to the original  call. Since
     * the <code>destOffsetX</code> and <code>destOffsetY</code> parameters are already taken into acount in the
     * <code>sourceOffsetX</code> and <code>sourceOffsetY</code> parameters, an implementor of this method is free to
     * ignore them.
     *
     * @param sourceOffsetX the absolute X-offset in source raster co-ordinates
     * @param sourceOffsetY the absolute Y-offset in source raster co-ordinates
     * @param sourceWidth   the width of region providing samples to be read given in source raster co-ordinates
     * @param sourceHeight  the height of region providing samples to be read given in source raster co-ordinates
     * @param sourceStepX   the sub-sampling in X direction within the region providing samples to be read
     * @param sourceStepY   the sub-sampling in Y direction within the region providing samples to be read
     * @param destBand      the destination band which identifies the data source from which to read the sample values
     * @param destOffsetX   the X-offset in the band's raster co-ordinates
     * @param destOffsetY   the Y-offset in the band's raster co-ordinates
     * @param destWidth     the width of region to be read given in the band's raster co-ordinates
     * @param destHeight    the height of region to be read given in the band's raster co-ordinates
     * @param destBuffer    the destination buffer which receives the sample values to be read
     * @param pm            a monitor to inform the user about progress
     * @throws IOException if an I/O error occurs
     * @see #readBandRasterData
     * @see #getSubsetDef
     */
    protected abstract void readBandRasterDataImpl(int sourceOffsetX,
                                                   int sourceOffsetY,
                                                   int sourceWidth,
                                                   int sourceHeight,
                                                   int sourceStepX,
                                                   int sourceStepY,
                                                   Band destBand,
                                                   int destOffsetX,
                                                   int destOffsetY,
                                                   int destWidth,
                                                   int destHeight,
                                                   ProductData destBuffer,
                                                   ProgressMonitor pm) throws IOException;

    /**
     * Closes the access to all currently opened resources such as file input streams and all resources of this children
     * directly owned by this reader. Its primary use is to allow the garbage collector to perform a vanilla job.
     * <p/>
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>close()</code> are undefined.
     * <p/>
     * <p>Overrides of this method should always call <code>super.close();</code> after disposing this instance.
     *
     * @throws IOException if an I/O error occurs
     */
    public void close() throws IOException {
        Debug.trace("AbstractProductReader.close(): " + toString());
        _input = null;
        _subsetDef = null;
    }

    /**
     * Retrieves a set of TreeNode objects that represent the physical product structure as stored on the harddrive.
     * The tree consisty of:
     * - a root node (the one returned) pointing to the directory that CONTAINS the product
     * - any number of nested children that compose the product.
     * Each TreeNod is configured as follows:
     * - id: contains a string representation of the path. For the root node, this is the
     * absolute path to the parent of the file returned by Product.getFileLocation().
     * For all subsequent nodes, the node name.
     * - content: each node stores as content a java.io.File object that physically defines the node.
     * <p/>
     * The method returns null when a TreeNode can not be assembled (i.e. in-memory product, created from stream ...)
     *
     * @return the root TreeNode or null
     */
    public TreeNode<File> getProductComponents() {
        final Object input = getInput();
        final File inputFile;
        if (input instanceof File) {
            inputFile = (File) input;
        } else if (input instanceof String) {
            inputFile = new File((String) input);
        } else {
            return null;
        }

        final File parent = inputFile.getParentFile();
        final TreeNode<File> result = new TreeNode<File>(parent.getName());
        result.setContent(parent);

        final TreeNode<File> productFile = new TreeNode<File>(inputFile.getName());
        productFile.setContent(inputFile);
        result.addChild(productFile);
        return result;
    }

    /**
     * Checks if the given object is an instance of one of the valid input types for this product reader.
     *
     * @param input the input object passed to {@link #readProductNodes(Object,ProductSubsetDef)}
     * @return <code>true</code> if so
     * @see org.esa.beam.framework.dataio.ProductReaderPlugIn#getInputTypes()
     */
    protected boolean isInstanceOfValidInputType(Object input) {
        if (getReaderPlugIn() != null) {
            Class[] inputTypes = getReaderPlugIn().getInputTypes();
            for (Class inputType : inputTypes) {
                if (inputType.isInstance(input)) {
                    return true;
                }
            }
            return false;
        }
        return true;
    }

    /**
     * Used by the {@link #createTiePointGrid(String,int,int,float,float,float,float,float[]) createTiePointGrid} method in order to determine
     * the discontinuity mode for angle tie-point grids.
     * <p>The default implementation returns {@link TiePointGrid#DISCONT_AT_180} for
     * the names "lon", "long" or "longitude" ignoring letter case,
     * {@link TiePointGrid#DISCONT_NONE} otherwise.
     *
     * @param name the grid name
     * @return the discontinuity mode, always one of {@link TiePointGrid#DISCONT_NONE}, {@link TiePointGrid#DISCONT_AT_180} and {@link TiePointGrid#DISCONT_AT_360}.
     */
    protected int getGridDiscontinutity(String name) {
        if (isNameOfLongitudeGrid(name)) {
            return TiePointGrid.DISCONT_AT_180;
        }
        return TiePointGrid.DISCONT_NONE;
    }

    /**
     * Creates a tie point grid from the given properties.
     * <p>The method uses the {@link #getGridDiscontinutity(String)} method in order to
     * creater an appropriate angular tie-point grids.
     *
     * @param gridName     the grid name
     * @param gridWidth    the grid's raster width
     * @param gridHeight   the grid's raster height
     * @param offsetX      the grid origin's X-coordinate in pixel units
     * @param offsetY      the grid origin's Y-coordinate in pixel units
     * @param subSamplingX the grid subsampling's X in pixel units
     * @param subSamplingY the grid subsampling's Y in pixel units
     * @param tiePoints    the tie-points
     * @return the tie-point grid instance, never null
     */
    protected TiePointGrid createTiePointGrid(String gridName,
                                              int gridWidth,
                                              int gridHeight,
                                              float offsetX,
                                              float offsetY,
                                              float subSamplingX,
                                              float subSamplingY,
                                              float[] tiePoints) {
        final int gridDiscontinutity = getGridDiscontinutity(gridName);
        if (gridDiscontinutity != 0) {
            Debug.trace("creating tie-point grid '" + gridName +
                    "' with discontinuity at " + gridDiscontinutity +
                    " degree");
        }
        return new TiePointGrid(gridName,
                gridWidth,
                gridHeight,
                offsetX,
                offsetY,
                subSamplingX,
                subSamplingY,
                tiePoints,
                gridDiscontinutity);
    }

    /**
     * Returns a string representation of the reader.
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return getClass().getName() + "[input=" + _input + "]";
    }

    private static boolean isNameOfLongitudeGrid(String name) {
        return name.equalsIgnoreCase("lon") ||
                name.equalsIgnoreCase("long") ||
                name.equalsIgnoreCase("longitude");
    }
}
