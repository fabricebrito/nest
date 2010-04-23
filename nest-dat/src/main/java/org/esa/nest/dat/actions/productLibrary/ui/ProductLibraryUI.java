package org.esa.nest.dat.actions.productLibrary.ui;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.jidesoft.swing.JideSplitPane;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.BasicApp;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.nest.dat.DatContext;
import org.esa.nest.dat.actions.importbrowser.model.RepositoryScanner;
import org.esa.nest.dat.actions.productLibrary.model.ProductLibraryConfig;
import org.esa.nest.dat.dialogs.BatchGraphDialog;
import org.esa.nest.dat.toolviews.Projects.Project;
import org.esa.nest.db.ProductEntry;
import org.esa.nest.util.TestUtils;

import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

public class ProductLibraryUI {

    private static final String stopCommand = "stop";
    private static final String updateCommand = "update";
    private static final ImageIcon updateIcon = UIUtils.loadImageIcon("icons/Update24.gif");
    private static final ImageIcon updateRolloverIcon = ToolButtonFactory.createRolloverIcon(updateIcon);
    private static final ImageIcon stopIcon = UIUtils.loadImageIcon("icons/Stop24.gif");
    private static final ImageIcon stopRolloverIcon = ToolButtonFactory.createRolloverIcon(stopIcon);

    private JPanel mainPanel;
    private JComboBox repositoryListCombo;
    private JTable productEntryTable;
    private SortingDecorator sortedModel;

    private JLabel statusLabel;
    private JPanel progressPanel;
    private JButton openButton;
    private JButton addToProjectButton;
    private JButton openAllSelectedButton;
    private JButton batchProcessButton;
    private JButton removeButton;
    private JButton updateButton;

    private ProgressBarProgressMonitor progMon;
    private JProgressBar progressBar;
    private JPanel headerPanel;
    private File currentDirectory;
    private ProductOpenHandler openHandler;
    private final ProductLibraryConfig libConfig;
    private final String helpId;
    private JFrame mainFrame;

    private WorldMapUI worldMapUI = null;

    //private RepositoryTree repositoryTree = null;
    private DefaultMutableTreeNode rootNode = null;

    private DatabasePane dbPane;

    public ProductLibraryUI(final BasicApp basicApp, final String helpId) {
        libConfig = new ProductLibraryConfig(basicApp.getPreferences());
        this.helpId = helpId;
    }

    /**
     * Sets the ProductOpenHandler which handles the action when products should
     * be opened.
     *
     * @param handler The <code>ProductOpenHandler</code>, can be
     *                <code>null</code> to unset the handler.
     */
    public void setProductOpenHandler(final ProductOpenHandler handler) {
        openHandler = handler;
    }

    public synchronized JFrame getFrame() {
        if (mainFrame == null) {
            mainFrame = new JFrame("Product Library");
            mainFrame.setDefaultCloseOperation(JDialog.HIDE_ON_CLOSE);
            initUI();
            mainFrame.addComponentListener(new ComponentAdapter() {

                @Override
                public void componentHidden(final ComponentEvent e) {
                    if(progMon != null)
                        progMon.setCanceled(true);
                }
            });
            mainFrame.add(mainPanel);
            mainFrame.setBounds(libConfig.getWindowBounds());
            applyConfig(libConfig);
            mainFrame.addComponentListener(new ComponentAdapter() {

                @Override
                public void componentMoved(final ComponentEvent e) {
                    libConfig.setWindowBounds(e.getComponent().getBounds());
                }

                @Override
                public void componentResized(final ComponentEvent e) {
                    libConfig.setWindowBounds(e.getComponent().getBounds());
                }
            });
            setUIComponentsEnabled(repositoryListCombo.getItemCount() > 0);
        }
        return mainFrame;
    }

    private void applyConfig(final ProductLibraryConfig config) {
        final File[] baseDirList = config.getBaseDirs();
        for(File f : baseDirList) {
            repositoryListCombo.insertItemAt(f, repositoryListCombo.getItemCount());
        }
        if(baseDirList.length > 0)
            repositoryListCombo.setSelectedIndex(0);
    }

    private void performSelectAction() {
        updateStatusLabel();
        /*
            worldMapDataModel.setSelectedProduct(entry.getProduct());
                */
    }

    private void performOpenAction() {
        if (openHandler != null) {
            openHandler.openProducts(getSelectedFiles());
        }
    }

    private File[] getSelectedFiles() {
        final int[] selectedRows = productEntryTable.getSelectedRows();
        final File[] selectedFiles = new File[selectedRows.length];
        for (int i = 0; i < selectedRows.length; i++) {
            final Object entry = productEntryTable.getValueAt(selectedRows[i], 0);
            if(entry instanceof ProductEntry) {
                selectedFiles[i] = ((ProductEntry)entry).getFile();
            }
        }
        return selectedFiles;
    }

    private JPopupMenu createPopup() {
        final String homeUrl = System.getProperty("nest.home", ".");
        final File graphPath = new File(homeUrl, File.separator + "graphs");

        final JPopupMenu popup = new JPopupMenu();
        if(graphPath.exists()) {
            createGraphMenu(popup, graphPath);
        }
        return popup;
    }

    private void createGraphMenu(final JPopupMenu menu, final File path) {
        final File[] filesList = path.listFiles();
        if(filesList == null || filesList.length == 0) return;

        for (final File file : filesList) {
            final String name = file.getName();
            if(file.isDirectory() && !file.isHidden() && !name.equalsIgnoreCase("internal")) {
                //final JPopupMenu subMenu = new JPopupMenu(name);
                //menu.add(subMenu);
                createGraphMenu(menu, file);
            } else if(name.toLowerCase().endsWith(".xml")) {
                final JMenuItem item = new JMenuItem(name.substring(0, name.indexOf(".xml")));
                item.addActionListener(new ActionListener() {

                    public void actionPerformed(final ActionEvent e) {
                        batchProcess(getSelectedFiles(), file);
                    }
                });
                menu.add(item);
            }
        }
    }

    private static void batchProcess(final File[] fileList, final File graphFile) {
        final BatchGraphDialog batchDlg = new BatchGraphDialog(new DatContext(""),
                "Batch Processing", "batchProcessing");
        batchDlg.setInputFiles(fileList);
        if(graphFile != null) {
            batchDlg.LoadGraphFile(graphFile);
        }
        batchDlg.show();
    }

    private void addRepository() {
        final File baseDir = promptForRepositoryBaseDir();
        if (baseDir == null) {
            return;
        }

        final int answer = JOptionPane.showOptionDialog(mainPanel,
                                                        "Search directory recursively?", "Add Repository",
                                                        JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
                                                        null, null);
        boolean doRecursive = false;
        if (answer == JOptionPane.YES_OPTION) {
            doRecursive = true;
        }

        libConfig.addBaseDir(baseDir);
        final int index = repositoryListCombo.getItemCount();
        repositoryListCombo.insertItemAt(baseDir, index);
        repositoryListCombo.setSelectedIndex(index);
        setUIComponentsEnabled(repositoryListCombo.getItemCount() > 0);

        updateRepostitory(baseDir, doRecursive);
    }

    private void updateRepostitory(final File baseDir, final boolean doRecursive) {
        if(baseDir == null) return;
        progMon = new ProgressBarProgressMonitor(progressBar, statusLabel);
        final SwingWorker repositoryCollector = new RepositoryCollector(baseDir, doRecursive, progMon);
        repositoryCollector.execute();
    }

   /* private void removeRepository(final Repository repository) {
        if (repository != null) {
            repositoryManager.removeRepository(repository);
            final boolean repositoryListIsNotEmpty = repositoryManager.getNumRepositories() > 0;
            if (repositoryListIsNotEmpty) {
                final Repository firstElement = repositoryManager.getRepository(0);
                if(firstElement != null)
                    repositoryListCombo.setSelectedItem(firstElement);
            }
            setUIComponentsEnabled(repositoryListIsNotEmpty);
        }
    }       */

    private void setUIComponentsEnabled(final boolean enable) {
        openButton.setEnabled(enable);
        addToProjectButton.setEnabled(enable);
        openAllSelectedButton.setEnabled(enable);
        batchProcessButton.setEnabled(enable);
        removeButton.setEnabled(enable);
        updateButton.setEnabled(enable);
        repositoryListCombo.setEnabled(enable);
    }

    private void toggleUpdateButton(final String command) {
        if (command.equals(stopCommand)) {
            updateButton.setIcon(stopIcon);
            updateButton.setRolloverIcon(stopRolloverIcon);
            updateButton.setActionCommand(stopCommand);
        } else {
            updateButton.setIcon(updateIcon);
            updateButton.setRolloverIcon(updateRolloverIcon);
            updateButton.setActionCommand(updateCommand);
        }
    }

    private File promptForRepositoryBaseDir() {
        final JFileChooser fileChooser = createDirectoryChooser();
        fileChooser.setCurrentDirectory(currentDirectory);
        final int response = fileChooser.showOpenDialog(mainPanel);
        currentDirectory = fileChooser.getCurrentDirectory();
        File selectedDir = fileChooser.getSelectedFile();
        if(selectedDir != null && selectedDir.isFile())
            selectedDir = selectedDir.getParentFile();
        if (response == JFileChooser.APPROVE_OPTION) {
            return selectedDir;
        }
        return null;
    }

    private static JFileChooser createDirectoryChooser() {
        final JFileChooser fileChooser = new JFileChooser();
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileFilter(new FileFilter() {

            @Override
            public boolean accept(final File f) {
                return true;//f.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Directories"; /* I18N */
            }
        });
        fileChooser.setDialogTitle("Select Directory"); /* I18N */
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
        fileChooser.setApproveButtonText("Select"); /* I18N */
        fileChooser.setApproveButtonMnemonic('S');
        return fileChooser;
    }

    private void initUI() {
        mainPanel = new JPanel(new BorderLayout(4, 4));
        final JPanel southPanel = new JPanel(new BorderLayout(4, 4));

        final JPanel northPanel = new JPanel(new BorderLayout(4, 4));
        repositoryListCombo = new JComboBox();
        setComponentName(repositoryListCombo, "repositoryListCombo");
        productEntryTable = new JTable();
        statusLabel = new JLabel("");
        progressPanel = new JPanel();
        openButton = new JButton();
        setComponentName(openButton, "openButton");
        removeButton = new JButton();
        setComponentName(removeButton, "removeButton");
        updateButton = new JButton();
        setComponentName(updateButton, "updateButton");
        progressBar = new JProgressBar();
        setComponentName(progressBar, "progressBar");
        headerPanel = new JPanel();

        final JPanel openPanel = new JPanel(new BorderLayout(4, 4));

        addToProjectButton = new JButton();
        setComponentName(addToProjectButton, "addToProject");
        addToProjectButton.setText("Import to Project");
        addToProjectButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                Project.instance().ImportFileList(getSelectedFiles());
            }
        });

        openAllSelectedButton = new JButton();
        setComponentName(openAllSelectedButton, "openAllSelectedButton");
        openAllSelectedButton.setText("Open Selected");
        openAllSelectedButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                performOpenAction();
            }
        });

        batchProcessButton = new JButton();
        setComponentName(batchProcessButton, "batchProcessButton");
        batchProcessButton.setText("Batch Process");
        batchProcessButton.setComponentPopupMenu(createPopup());
        batchProcessButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                batchProcess(getSelectedFiles(), null);
            }
        });

        openPanel.add(addToProjectButton, BorderLayout.WEST);
        openPanel.add(openAllSelectedButton, BorderLayout.CENTER);
        openPanel.add(batchProcessButton, BorderLayout.EAST);

        northPanel.add(headerPanel, BorderLayout.CENTER);
        southPanel.add(statusLabel, BorderLayout.CENTER);
        southPanel.add(openPanel, BorderLayout.WEST);
        southPanel.add(progressPanel, BorderLayout.EAST);

        mainPanel.add(northPanel, BorderLayout.NORTH);
        mainPanel.add(createCentrePanel(), BorderLayout.CENTER);
        mainPanel.add(southPanel, BorderLayout.SOUTH);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));

        progressBar = new JProgressBar();
        progressBar.setStringPainted(true);
        progressPanel.setLayout(new BorderLayout());
        progressPanel.add(progressBar);
        progressPanel.setVisible(false);

        productEntryTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        productEntryTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        productEntryTable.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(final MouseEvent e) {
                final int clickCount = e.getClickCount();
                if (clickCount == 2) {
                    performOpenAction();
                } else if(clickCount == 1) {
                    performSelectAction();
                }
            }
        });
        initHeaderPanel(headerPanel);
    }

    private JPanel createCentrePanel() {
        final JideSplitPane splitPane1H = new JideSplitPane(JideSplitPane.HORIZONTAL_SPLIT);

        final MyDatabaseQueryListener dbQueryListener = new MyDatabaseQueryListener();
        final JideSplitPane splitPane11 = new JideSplitPane(JideSplitPane.VERTICAL_SPLIT);
        dbPane = new DatabasePane();
        dbPane.addListener(dbQueryListener);
        splitPane11.add(dbPane);

        //splitPane11.add(createRepositoryTreeControl());
        worldMapUI = new WorldMapUI();
        worldMapUI.addListener(dbQueryListener);
        splitPane11.add(worldMapUI.getWorlMapPane());

        splitPane1H.add(splitPane11);
        splitPane1H.add(new JScrollPane(productEntryTable));

        return splitPane1H;
    }

    private void setComponentName(JComponent button, String name) {
        button.setName(getClass().getName() + name);
    }

   /* JComponent createRepositoryTreeControl() {
        final JScrollPane prjScrollPane = new JideScrollPane(createTree());
        prjScrollPane.setPreferredSize(new Dimension(320, 480));
        prjScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        prjScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);

        return prjScrollPane;
    }*/

    public void UpdateUI() {
        productEntryTable.updateUI();
        //updateWorldMap();
    }

  /*  private RepositoryTree createTree() {
        rootNode = new DefaultMutableTreeNode("");
        repositoryTree = new RepositoryTree(this);
        repositoryTree.populateTree(rootNode);
        repositoryTree.setRootVisible(false);
        repositoryTree.setShowsRootHandles(true);
        final DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) repositoryTree.getCellRenderer();
        renderer.setLeafIcon(IconsFactory.getImageIcon(ProductLibraryUI.class, "/org/esa/beam/resources/images/icons/RsBandAsSwath16.gif"));
        renderer.setClosedIcon(IconsFactory.getImageIcon(ProductLibraryUI.class, "/org/esa/beam/resources/images/icons/RsGroupClosed16.gif"));
        renderer.setOpenIcon(IconsFactory.getImageIcon(ProductLibraryUI.class, "/org/esa/beam/resources/images/icons/RsGroupOpen16.gif"));
        return repositoryTree;
    }    */

    private void initHeaderPanel(final JPanel headerBar) {
        headerBar.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        openButton = createToolButton(UIUtils.loadImageIcon("icons/Open24.gif"));
        openButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                performOpenAction();
            }
        });
        headerBar.add(openButton, gbc);

        updateButton = createToolButton(updateIcon);
        updateButton.setActionCommand(updateCommand);
        updateButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                if (e.getActionCommand().equals("stop")) {
                    updateButton.setEnabled(false);
                    mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    progMon.setCanceled(true);
                } else {
                    updateRepostitory((File)repositoryListCombo.getSelectedItem(), true);
                }
            }
        });
        headerBar.add(updateButton, gbc);

        headerBar.add(new JLabel("Repository:")); /* I18N */
        gbc.weightx = 99;
        headerBar.add(repositoryListCombo, gbc);
        gbc.weightx = 0;

        JButton _addButton = createToolButton(UIUtils.loadImageIcon("icons/Plus24.gif"));
        _addButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                addRepository();
            }
        });
        headerBar.add(_addButton, gbc);

        removeButton = createToolButton(UIUtils.loadImageIcon("icons/Minus24.gif"));
        removeButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                //removeRepository((Repository) repositoryListCombo.getSelectedItem());
            }
        });
        headerBar.add(removeButton, gbc);

        JButton helpButton = createToolButton(UIUtils.loadImageIcon("icons/Help24.gif"));
        setComponentName(helpButton, "helpButton");
        HelpSys.enableHelpOnButton(helpButton, helpId);
        headerBar.add(helpButton, gbc);
    }

    private JButton createToolButton(final ImageIcon icon) {
        final JButton button = (JButton) ToolButtonFactory.createButton(icon,
                                                                        false);
        button.setBackground(headerPanel.getBackground());
        return button;
    }

    private void updateStatusLabel() {
        String selectedText = "";
        final int selecteRows = productEntryTable.getSelectedRowCount();
        if(selecteRows > 0)
            selectedText = ", "+selecteRows+" Selected";
        statusLabel.setText(productEntryTable.getRowCount() + " Products"+ selectedText);
    }

    public void ShowRepository(final ProductEntry[] productEntryList) {
        final ProductEntryTableModel tableModel = new ProductEntryTableModel(productEntryList);
        sortedModel = new SortingDecorator(tableModel, productEntryTable.getTableHeader());
        productEntryTable.setModel(sortedModel);
        productEntryTable.setColumnModel(tableModel.getColumnModel());
        updateStatusLabel();
        worldMapUI.setProductEntryList(productEntryList);
    }

    /**
     * A {@link com.bc.ceres.core.ProgressMonitor} which uses a
     * Swing's {@link javax.swing.ProgressMonitor} to display progress.
     */
    private class ProgressBarProgressMonitor implements ProgressMonitor {

        private final JProgressBar progressBar;
        private final JLabel messageLabel;

        private double currentWork;
        private double totalWork;

        private int totalWorkUI;
        private int currentWorkUI;
        private int lastWorkUI;
        private boolean cancelRequested;

        public ProgressBarProgressMonitor(JProgressBar progressBar, JLabel messageLabel) {
            this.progressBar = progressBar;
            this.messageLabel = messageLabel;
        }

        /**
         * Notifies that the main task is beginning.  This must only be called once
         * on a given progress monitor instance.
         *
         * @param name      the name (or description) of the main task
         * @param totalWork the total number of work units into which
         *                  the main task is been subdivided. If the value is <code>UNKNOWN</code>
         *                  the implementation is free to indicate progress in a way which
         *                  doesn't require the total number of work units in advance.
         */
        public void beginTask(String name, int totalWork) {
            Assert.notNull(name, "name");
            currentWork = 0.0;
            this.totalWork = totalWork;
            currentWorkUI = 0;
            lastWorkUI = 0;
            totalWorkUI = totalWork;
            if (messageLabel != null) {
                messageLabel.setText(name);
            }
            cancelRequested = false;
            setDescription(name);
            setVisibility(true);
            progressBar.setMaximum(totalWork);
            toggleUpdateButton(stopCommand);
        }

        /**
         * Notifies that the work is done; that is, either the main task is completed
         * or the user canceled it. This method may be called more than once
         * (implementations should be prepared to handle this case).
         */
        public void done() {
            runInUI(new Runnable() {
                public void run() {
                    if (progressBar != null) {
                        progressBar.setValue(progressBar.getMaximum());
                        setVisibility(false);
                        toggleUpdateButton(updateCommand);
                        updateButton.setEnabled(true);
                        mainPanel.setCursor(Cursor.getDefaultCursor());
                    }
                }
            });
        }

        /**
         * Internal method to handle scaling correctly. This method
         * must not be called by a client. Clients should
         * always use the method </code>worked(int)</code>.
         *
         * @param work the amount of work done
         */
        public void internalWorked(double work) {
            currentWork += work;
            currentWorkUI = (int) (totalWorkUI * currentWork / totalWork);
            if (currentWorkUI > lastWorkUI) {
                runInUI(new Runnable() {
                    public void run() {
                        if (progressBar != null) {
                            int progress = progressBar.getMinimum() + currentWorkUI;
                            progressBar.setValue(progress);
                            setVisibility(true);
                            toggleUpdateButton(stopCommand);
                        }
                        lastWorkUI = currentWorkUI;
                    }
                });
            }
        }

        /**
         * Returns whether cancelation of current operation has been requested.
         * Long-running operations should poll to see if cancelation
         * has been requested.
         *
         * @return <code>true</code> if cancellation has been requested,
         *         and <code>false</code> otherwise
         *
         * @see #setCanceled(boolean)
         */
        public boolean isCanceled() {
            return cancelRequested;
        }

        /**
         * Sets the cancel state to the given value.
         *
         * @param canceled <code>true</code> indicates that cancelation has
         *                 been requested (but not necessarily acknowledged);
         *                 <code>false</code> clears this flag
         *
         * @see #isCanceled()
         */
        public void setCanceled(boolean canceled) {
            cancelRequested = canceled;
            if (canceled) {
                done();
            }
        }

        /**
         * Sets the task name to the given value. This method is used to
         * restore the task label after a nested operation was executed.
         * Normally there is no need for clients to call this method.
         *
         * @param name the name (or description) of the main task
         *
         * @see #beginTask(String, int)
         */
        public void setTaskName(final String name) {
            runInUI(new Runnable() {
                public void run() {
                    if (messageLabel != null) {
                        messageLabel.setText(name);
                    }
                }
            });
        }

        /**
         * Notifies that a subtask of the main task is beginning.
         * Subtasks are optional; the main task might not have subtasks.
         *
         * @param name the name (or description) of the subtask
         */
        public void setSubTaskName(final String name) {
            setVisibility(true);
            messageLabel.setText(name);
            toggleUpdateButton(stopCommand);
        }

        /**
         * Notifies that a given number of work unit of the main task
         * has been completed. Note that this amount represents an
         * installment, as opposed to a cumulative amount of work done
         * to date.
         *
         * @param work the number of work units just completed
         */
        public void worked(int work) {
            internalWorked(work);
        }

        ////////////////////////////////////////////////////////////////////////
        // Stuff to be performed in Swing's event-dispatching thread

        private void runInUI(Runnable task) {
            if (SwingUtilities.isEventDispatchThread()) {
                task.run();
            } else {
                SwingUtilities.invokeLater(task);
            }
        }

        private void setDescription(final String description) {
            statusLabel.setText(description);
        }

        private void setVisibility(final boolean visible) {
            progressPanel.setVisible(visible);
            statusLabel.setVisible(visible);
        }
    }

    private class MyDatabaseQueryListener implements DatabaseQueryListener {

        public void notifyNewProductEntryListAvailable() {
            ShowRepository(dbPane.getProductEntryList());
        }

        public void notifyNewMapSelectionAvailable() {
            dbPane.setSelectionRect(worldMapUI.getSelectionBox());
        }
    }


    /**
     * Should be implemented for handling opening {@link org.esa.beam.framework.datamodel.Product} files.
     */
    public static interface ProductOpenHandler {

        /**
         * Implemetation should open the given files as {@link org.esa.beam.framework.datamodel.Product}s.
         *
         * @param productFiles the files to open.
         */
        public void openProducts(File[] productFiles);
    }

    private class RepositoryCollector extends SwingWorker {

        private final File baseDir;
        private final boolean doRecursive;
        private final ProgressMonitor pm;


        public RepositoryCollector(final File baseDir, final boolean doRecursive, final ProgressMonitor pm) {
            this.pm = pm;
            this.baseDir = baseDir;
            this.doRecursive = doRecursive;
        }

        @Override
        protected Boolean doInBackground() throws Exception {
            final ArrayList<File> dirList = new ArrayList<File>();
            dirList.add(baseDir);
            if (doRecursive) {
                final File[] subDirs = collectAllSubDirs(baseDir);
                dirList.addAll(Arrays.asList(subDirs));
            }

            final ArrayList<File> fileList = new ArrayList<File>();
            for(File file : dirList) {
                fileList.addAll(Arrays.asList(file.listFiles()));
            }

            final int total = fileList.size();
            pm.beginTask("Scanning Files...", total);
            int i=1;
            try {
                for(File file : fileList) {
                    if(!file.isDirectory()) {
                        if(pm.isCanceled())
                            break;
                        if(TestUtils.isNotProduct(file))
                            continue;

                        if(dbPane.getDB().pathExistsInDB(file))
                            continue;

                        try {
                        final ProductReader reader = ProductIO.getProductReaderForFile(file);
                        if(reader != null) {
                            Product sourceProduct = null;
                            try {
                                sourceProduct = reader.readProductNodes(file, null);
                            } catch(Exception e) {
                                System.out.println("Unable to read "+file.getAbsolutePath()+"\n"+e.getMessage());
                            }
                            if(sourceProduct != null) {
                                //System.out.println("Adding "+file.getAbsolutePath());

                                dbPane.getDB().saveProduct(sourceProduct);
                                sourceProduct.dispose();
                            }
                        }
                        } catch(Exception e) {
                            System.out.println("Unable to scan "+file.getAbsolutePath()+"\n"+e.getMessage());
                        }
                    }
                    pm.setTaskName("Scanning Files... "+i+" of "+total);
                    pm.worked(1);
                    ++i;
                }
            } catch(Exception e) {
                System.out.println("Scanning Exception\n"+e.getMessage());
            } finally {
                pm.done();
            }
            return true;
        }

        @Override
        public void done() {

            dbPane.refresh();
        }

        private File[] collectAllSubDirs(final File dir) {
            final ArrayList<File> dirList = new ArrayList<File>();
            final RepositoryScanner.DirectoryFileFilter dirFilter = new RepositoryScanner.DirectoryFileFilter();

            final File[] subDirs = dir.listFiles(dirFilter);
            for (final File subDir : subDirs) {
                dirList.add(subDir);
                final File[] dirs = collectAllSubDirs(subDir);
                dirList.addAll(Arrays.asList(dirs));
            }
            return dirList.toArray(new File[dirList.size()]);
        }
    }
}