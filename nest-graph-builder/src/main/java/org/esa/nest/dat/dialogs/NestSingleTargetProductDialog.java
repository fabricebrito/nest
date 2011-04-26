/*
 * Copyright (C) 2011 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.nest.dat.dialogs;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.selection.AbstractSelectionChangeListener;
import com.bc.ceres.swing.selection.Selection;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import com.bc.ceres.swing.selection.SelectionChangeListener;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductFilter;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.OperatorUI;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.internal.RasterDataNodeValues;
import org.esa.beam.framework.gpf.ui.SingleTargetProductDialog;
import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.framework.gpf.ui.TargetProductSelectorModel;
import org.esa.beam.framework.gpf.ui.UIValidation;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import java.awt.*;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public class NestSingleTargetProductDialog extends SingleTargetProductDialog {
    private final String operatorName;
    private List<SourceProductSelector> sourceProductSelectorList;
    private Map<Field, SourceProductSelector> sourceProductSelectorMap;
    private final Map<String, Object> parameterMap = new HashMap<String, Object>(17);
    private JTabbedPane form;
    private String targetProductNameSuffix;
    private final OperatorUI opUI;

    public NestSingleTargetProductDialog(String operatorName, AppContext appContext, String title, String helpID) {
        super(appContext, title, helpID);
        this.operatorName = operatorName;
        targetProductNameSuffix = "";

        final OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(operatorName);
        if (operatorSpi == null) {
            throw new IllegalArgumentException("operatorName " + operatorName);
        }

        opUI = operatorSpi.createOperatorUI();

        // Fetch source products
        initSourceProductSelectors(operatorSpi);
        if (sourceProductSelectorList.size() > 0) {
            setSourceProductSelectorLabels();
            setSourceProductSelectorToolTipTexts();
        }

        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTablePadding(3, 3);

        final JPanel ioParametersPanel = new JPanel(tableLayout);
        for (SourceProductSelector selector : sourceProductSelectorList) {
            ioParametersPanel.add(selector.createDefaultPanel());
        }
        ioParametersPanel.add(getTargetProductSelector().createDefaultPanel());
        ioParametersPanel.add(tableLayout.createVerticalSpacer());
        sourceProductSelectorList.get(0).addSelectionChangeListener(new AbstractSelectionChangeListener() {

            @Override
            public void selectionChanged(SelectionChangeEvent event) {
                final Product selectedProduct = (Product) event.getSelection().getSelectedValue();
                if(selectedProduct != null) {
                    final TargetProductSelectorModel targetProductSelectorModel = getTargetProductSelector().getModel();
                    targetProductSelectorModel.setProductName(selectedProduct.getName() + getTargetProductNameSuffix());
                    opUI.setSourceProducts(new Product[] { selectedProduct });
                }
            }
        });

        this.form = new JTabbedPane();
        this.form.add("I/O Parameters", ioParametersPanel);

        final ParameterDescriptorFactory parameterDescriptorFactory = new ParameterDescriptorFactory();
        final PropertyContainer valueContainer = PropertyContainer.createMapBacked(parameterMap,
                                                operatorSpi.getOperatorClass(), parameterDescriptorFactory);

        valueContainer.setDefaultValues();

        if (valueContainer.getProperties().length > 0) {

            final JComponent paremetersPanel = opUI.CreateOpTab(operatorName, parameterMap, appContext);

            paremetersPanel.setBorder(new EmptyBorder(4, 4, 4, 4));
            this.form.add("Processing Parameters", new JScrollPane(paremetersPanel));

            for (final Field field : sourceProductSelectorMap.keySet()) {
                final SourceProductSelector sourceProductSelector = sourceProductSelectorMap.get(field);
                final String sourceAlias = field.getAnnotation(SourceProduct.class).alias();

                for (Property p : valueContainer.getProperties()) {
                    final PropertyDescriptor parameterDescriptor = p.getDescriptor();
                    final String sourceId = (String) parameterDescriptor.getAttribute("sourceId");
                    if (sourceId != null && (sourceId.equals(field.getName()) || sourceId.equals(sourceAlias))) {
                        final SelectionChangeListener valueSetUpdater = new ValueSetUpdater(parameterDescriptor);
                        sourceProductSelector.addSelectionChangeListener(valueSetUpdater);
                    }
                }
            }
        }

        getJDialog().setMinimumSize(new Dimension(500, 500));
    }

    private void initSourceProductSelectors(OperatorSpi operatorSpi) {
        sourceProductSelectorList = new ArrayList<SourceProductSelector>(3);
        sourceProductSelectorMap = new HashMap<Field, SourceProductSelector>(3);
        final Field[] fields = operatorSpi.getOperatorClass().getDeclaredFields();
        for (Field field : fields) {
            final SourceProduct annot = field.getAnnotation(SourceProduct.class);
            if (annot != null) {
                final ProductFilter productFilter = new AnnotatedSourceProductFilter(annot);
                SourceProductSelector sourceProductSelector = new SourceProductSelector(getAppContext());
                sourceProductSelector.setProductFilter(productFilter);
                sourceProductSelectorList.add(sourceProductSelector);
                sourceProductSelectorMap.put(field, sourceProductSelector);
            }
        }
    }

    private void setSourceProductSelectorLabels() {
        for (Field field : sourceProductSelectorMap.keySet()) {
            final SourceProductSelector selector = sourceProductSelectorMap.get(field);
            String label = null;
            final SourceProduct annot = field.getAnnotation(SourceProduct.class);
            if (!annot.label().isEmpty()) {
                label = annot.label();
            }
            if (label == null && !annot.alias().isEmpty()) {
                label = annot.alias();
            }
            if (label == null) {
                String name = field.getName();
                if (!annot.alias().isEmpty()) {
                    name = annot.alias();
                }
                label = PropertyDescriptor.createDisplayName(name);
            }
            if (!label.endsWith(":")) {
                label += ":";
            }
            selector.getProductNameLabel().setText(label);
        }
    }

    private void setSourceProductSelectorToolTipTexts() {
        for (Field field : sourceProductSelectorMap.keySet()) {
            final SourceProductSelector selector = sourceProductSelectorMap.get(field);

            final SourceProduct annot = field.getAnnotation(SourceProduct.class);
            final String description = annot.description();
            if (!description.isEmpty()) {
                selector.getProductNameComboBox().setToolTipText(description);
            }
        }
    }

    public void setIcon(final ImageIcon ico) {
        if(ico == null) return;
        this.getJDialog().setIconImage(ico.getImage());
    }

    @Override
    public int show() {
        initSourceProductSelectors();
        setContent(form);
        return super.show();
    }

    @Override
    public void hide() {
        releaseSourceProductSelectors();
        super.hide();
    }

    @Override
    protected Product createTargetProduct() throws Exception {
        if(validateUI()) {
            opUI.updateParameters();

            final HashMap<String, Product> sourceProducts = createSourceProductsMap();
            return GPF.createProduct(operatorName, parameterMap, sourceProducts);
        }
        return null;
    }

    private boolean validateUI() {
        final UIValidation validation = opUI.validateParameters();
        if(validation.getState() == UIValidation.State.WARNING) {
            final String msg = "Warning: "+validation.getMsg()+
                    "\n\nWould you like to continue?";
            return VisatApp.getApp().showQuestionDialog(msg, null) == 0;
        } else if(validation.getState() == UIValidation.State.ERROR) {
            final String msg = "Error: "+validation.getMsg();
            VisatApp.getApp().showErrorDialog(msg);
            return false;
        }
        return true;
    }

    private void initSourceProductSelectors() {
        for (SourceProductSelector sourceProductSelector : sourceProductSelectorList) {
            sourceProductSelector.initProducts();
        }
    }

    private void releaseSourceProductSelectors() {
        for (SourceProductSelector sourceProductSelector : sourceProductSelectorList) {
            sourceProductSelector.releaseProducts();
        }
    }

    private HashMap<String, Product> createSourceProductsMap() {
        final HashMap<String, Product> sourceProducts = new HashMap<String, Product>(8);
        for (Field field : sourceProductSelectorMap.keySet()) {
            final SourceProductSelector selector = sourceProductSelectorMap.get(field);
            String key = field.getName();
            final SourceProduct annot = field.getAnnotation(SourceProduct.class);
            if (!annot.alias().isEmpty()) {
                key = annot.alias();
            }
            sourceProducts.put(key, selector.getSelectedProduct());
        }
        return sourceProducts;
    }

    String getTargetProductNameSuffix() {
        return targetProductNameSuffix;
    }

    public void setTargetProductNameSuffix(String suffix) {
        targetProductNameSuffix = suffix;
    }

    private static class AnnotatedSourceProductFilter implements ProductFilter {

        private final SourceProduct annot;

        public AnnotatedSourceProductFilter(SourceProduct annot) {
            this.annot = annot;
        }

        public boolean accept(Product product) {

            if (!annot.type().isEmpty() && !product.getProductType().matches(annot.type())) {
                return false;
            }

            for (String bandName : annot.bands()) {
                if (!product.containsBand(bandName)) {
                    return false;
                }
            }

            return true;
        }
    }

    private static class ValueSetUpdater extends AbstractSelectionChangeListener {

        private final PropertyDescriptor propertyDescriptor;

        private ValueSetUpdater(PropertyDescriptor propertyDescriptor) {
            this.propertyDescriptor = propertyDescriptor;
        }

        @Override
        public void selectionChanged(SelectionChangeEvent event) {
            Selection selection = event.getSelection();
            String[] values = new String[0];
            if (selection != null) {
                final Product selectedProduct = (Product) selection.getSelectedValue();
                if (selectedProduct != null) {
                    Object object = propertyDescriptor.getAttribute(RasterDataNodeValues.ATTRIBUTE_NAME);
                    if (object != null) {
                        Class<? extends RasterDataNode> rasterDataNodeType = (Class<? extends RasterDataNode>) object;
                        boolean includeEmptyValue = !propertyDescriptor.isNotNull() && !propertyDescriptor.getType().isArray();
                        values = RasterDataNodeValues.getNames(selectedProduct, rasterDataNodeType, includeEmptyValue);
                    }
                }
            }
            propertyDescriptor.setValueSet(new ValueSet(values));
        }
    }
}