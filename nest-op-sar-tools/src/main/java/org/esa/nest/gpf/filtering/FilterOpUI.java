package org.esa.nest.gpf.filtering;

import org.esa.beam.framework.gpf.ui.UIValidation;
import org.esa.beam.framework.gpf.ui.BaseOperatorUI;
import org.esa.beam.framework.ui.AppContext;

import javax.swing.*;
import javax.swing.tree.TreeSelectionModel;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import javax.swing.tree.DefaultTreeCellRenderer;

import java.util.Map;
import java.util.Enumeration;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: lveci
 * Date: Feb 12, 2008
 * Time: 1:52:49 PM
 * To change this template use File | Settings | File Templates.
 */
public class FilterOpUI extends BaseOperatorUI {

    private JTree tree = null;
    private DefaultMutableTreeNode root = null;

    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        JComponent panel = createPanel();
        initParameters();

        return panel;
    }

    public void initParameters() {
        String filterName = (String)paramMap.get("selectedFilterName");
        if(filterName != null) {
            setSelectedFilter(filterName);    
        }
    }

    public UIValidation validateParameters() {

        return new UIValidation(getSelectedFilter(tree) != null, "Filter not selected");
    }

    public void updateParameters() {
        FilterOperator.Filter filter = getSelectedFilter(tree);
        if(filter != null)
            paramMap.put("selectedFilterName", filter.toString());
    }

    private static DefaultMutableTreeNode findItem(DefaultMutableTreeNode parentItem, String filterName) {
        if(!parentItem.isLeaf()) {
            final Enumeration enumeration = parentItem.children();
            while (enumeration.hasMoreElements()) {
                final DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) enumeration.nextElement();
                DefaultMutableTreeNode found = findItem(treeNode, filterName);
                if (found != null)
                    return found;
            }
        }

        if(parentItem.toString().equals(filterName))
                return parentItem;
        return null;
    }

    private JComponent createPanel() {
        tree = createTree();
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        final JScrollPane treeView = new JScrollPane(tree);

        final JPanel contentPane = new JPanel(new BorderLayout(4, 4));
        contentPane.add(new JLabel("Filters:"), BorderLayout.NORTH);
        contentPane.add(treeView, BorderLayout.CENTER);

        return contentPane;
    }

    private JTree createTree() {
        root = new DefaultMutableTreeNode("@root");

        root.add(createNodes("Detect Lines", FilterOperator.LINE_DETECTION_FILTERS));
        root.add(createNodes("Detect Gradients (Emboss)", FilterOperator.GRADIENT_DETECTION_FILTERS));
        root.add(createNodes("Smooth and Blurr", FilterOperator.SMOOTHING_FILTERS));
        root.add(createNodes("Sharpen", FilterOperator.SHARPENING_FILTERS));
        root.add(createNodes("Enhance Discontinuities", FilterOperator.LAPLACIAN_FILTERS));
        root.add(createNodes("Non-Linear Filters", FilterOperator.NON_LINEAR_FILTERS));
        final JTree tree = new JTree(root);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new MyDefaultTreeCellRenderer());
        tree.putClientProperty("JTree.lineStyle", "Angled");
        expandAll(tree);
        return tree;
    }

    protected JTree getTree() {
        return tree;
    }

    protected void setSelectedFilter(String filterName) {
        DefaultMutableTreeNode item = findItem(root, filterName);
        if(item != null) {
            tree.setSelectionPath(new TreePath(item.getPath()));
        }
    }

    protected static FilterOperator.Filter getSelectedFilter(final JTree tree) {
        final TreePath selectionPath = tree.getSelectionPath();
        if (selectionPath == null) {
            return null;
        }

        final Object[] path = selectionPath.getPath();
        if (path != null && path.length > 0) {
            final DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) path[path.length - 1];
            if (treeNode.getUserObject() instanceof FilterOperator.Filter) {
                return (FilterOperator.Filter) treeNode.getUserObject();

            }
        }
        return null;
    }


    private static DefaultMutableTreeNode createNodes(String categoryName, FilterOperator.Filter[] filters) {

        DefaultMutableTreeNode category = new DefaultMutableTreeNode(categoryName);

        for (FilterOperator.Filter filter : filters) {
            DefaultMutableTreeNode item = new DefaultMutableTreeNode(filter);
            category.add(item);
        }

        return category;
    }


    private static void expandAll(JTree tree) {
        DefaultMutableTreeNode actNode = (DefaultMutableTreeNode) tree.getModel().getRoot();
        while (actNode != null) {
            if (!actNode.isLeaf()) {
                final TreePath actPath = new TreePath(actNode.getPath());
                tree.expandRow(tree.getRowForPath(actPath));
            }
            actNode = actNode.getNextNode();
        }
    }

    private static class MyDefaultTreeCellRenderer extends DefaultTreeCellRenderer {

        private Font _plainFont;
        private Font _boldFont;

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            final JLabel c = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row,
                                                                         hasFocus);
            if (_plainFont == null) {
                _plainFont = c.getFont().deriveFont(Font.PLAIN);
                _boldFont = c.getFont().deriveFont(Font.BOLD);
            }
            c.setFont(leaf ? _plainFont : _boldFont);
            c.setIcon(null);
            return c;
        }
    }


}