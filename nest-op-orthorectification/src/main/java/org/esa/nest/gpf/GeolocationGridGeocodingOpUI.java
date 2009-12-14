package org.esa.nest.gpf;

import org.esa.beam.framework.dataop.dem.ElevationModelDescriptor;
import org.esa.beam.framework.dataop.dem.ElevationModelRegistry;
import org.esa.beam.framework.dataop.resamp.ResamplingFactory;
import org.esa.beam.framework.dataop.maptransf.MapProjection;
import org.esa.beam.framework.dataop.maptransf.MapProjectionRegistry;
import org.esa.beam.framework.gpf.ui.BaseOperatorUI;
import org.esa.beam.framework.gpf.ui.UIValidation;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.util.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.Map;
import java.util.Arrays;

/**
 * User interface for RangeDopplerGeocodingOp
 */
public class GeolocationGridGeocodingOpUI extends BaseOperatorUI {

    private final JList bandList = new JList();
    protected final JComboBox projectionName = new JComboBox();
    protected final JComboBox imgResamplingMethod = new JComboBox(new String[] {ResamplingFactory.NEAREST_NEIGHBOUR_NAME,
                                                                           ResamplingFactory.BILINEAR_INTERPOLATION_NAME,
                                                                           ResamplingFactory.CUBIC_CONVOLUTION_NAME});

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);

        final String[] projectionsValueSet = getProjectionsValueSet();
        Arrays.sort(projectionsValueSet);
        for(String name : projectionsValueSet) {
            projectionName.addItem(name);
        }

        final JComponent panel = createPanel();
        initParameters();

        return new JScrollPane(panel);
    }

    @Override
    public void initParameters() {

        OperatorUIUtils.initBandList(bandList, getBandNames());
        imgResamplingMethod.setSelectedItem(paramMap.get("imgResamplingMethod"));
        projectionName.setSelectedItem(paramMap.get("projectionName"));
    }

    @Override
    public UIValidation validateParameters() {

        return new UIValidation(true, "");
    }

    @Override
    public void updateParameters() {

        OperatorUIUtils.updateBandList(bandList, paramMap);
        paramMap.put("imgResamplingMethod", imgResamplingMethod.getSelectedItem());
        paramMap.put("projectionName", projectionName.getSelectedItem());
    }

    protected JComponent createPanel() {

        final JPanel contentPane = new JPanel();
        contentPane.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        contentPane.add(new JLabel("Source Bands:"), gbc);
        gbc.fill = GridBagConstraints.BOTH;
        gbc.gridx = 1;
        contentPane.add(new JScrollPane(bandList), gbc);

        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridx = 0;
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Image Resampling Method:", imgResamplingMethod);
        gbc.gridy++;
        DialogUtils.addComponent(contentPane, gbc, "Map Projection:", projectionName);

        return contentPane;
    }

    private static String[] getProjectionsValueSet() {
        final MapProjection[] projections = MapProjectionRegistry.getProjections();
        final String[] projectionsValueSet = new String[projections.length];
        for (int i = 0; i < projectionsValueSet.length; i++) {
            projectionsValueSet[i] = projections[i].getName();
        }
        return projectionsValueSet;
    }
}