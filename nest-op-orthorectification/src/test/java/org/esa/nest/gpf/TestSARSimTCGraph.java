package org.esa.nest.gpf;

import junit.framework.TestCase;
import org.esa.nest.dat.plugins.graphbuilder.GraphExecuter;
import org.esa.nest.dat.plugins.graphbuilder.GraphNode;
import org.esa.nest.dat.SARSimTerrainCorrectionDialog;
import org.esa.nest.util.TestUtils;
import org.esa.beam.framework.gpf.graph.GraphException;
import com.bc.ceres.core.ProgressMonitor;

import java.io.File;


/**
 * Unit test for SARSim TC Graph
 */
public class TestSARSimTCGraph extends TestCase {


    @Override
    protected void setUp() throws Exception {

    }

    @Override
    protected void tearDown() throws Exception {

    }

    public void testCreateGraph() throws GraphException {
      /*  final GraphExecuter graphEx = new GraphExecuter();
        graphEx.loadGraph(graphFile, true);

        graphEx.InitGraph();

        graphEx.executeGraph(ProgressMonitor.NULL);   */
    }

    public void testDialog() {
        //SARSimTerrainCorrectionDialog dialog = new SARSimTerrainCorrectionDialog(new TestUtils.MockAppContext(),
        //        "SAR Sim Terrain Correction", "SARSimGeocodingOp");

    }

}