// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.validations_testing.data.validation.tests;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openstreetmap.josm.command.Command;
import org.openstreetmap.josm.data.Bounds;
import org.openstreetmap.josm.data.DataSource;
import org.openstreetmap.josm.data.coor.LatLon;
import org.openstreetmap.josm.data.osm.DataSet;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.osm.OsmPrimitive;
import org.openstreetmap.josm.data.osm.Relation;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.NullProgressMonitor;
import org.openstreetmap.josm.spi.preferences.Config;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test class for {@link Maxspeed}
 *
 * @author Taylor Smock
 */
class MaxspeedTest {
    private static Maxspeed test;
    @RegisterExtension
    static JOSMTestRules rules = new JOSMTestRules().preferences();

    @BeforeAll
    static void setup() {
        test = new Maxspeed();
    }

    @BeforeEach
    void cleanUp() {
        test.clear();
        Config.getPref().putDouble("plugin.validations_testing.maxspeed.maxlength", 30);
        test.startTest(NullProgressMonitor.INSTANCE);
    }

    @Test
    void testHighwayNoConnections() {
        Way way = new Way();
        way.addNode(new Node(LatLon.ZERO));
        way.addNode(new Node(LatLon.ZERO));
        way.put("highway", "tertiary");
        way.put("name", "Test name");

        addToDataset(new DataSet(), Collections.singleton(way));

        test.visit(way);
        assertTrue(test.getErrors().isEmpty());
    }

    @Test
    void testHighwayOneConnection() {
        Way way1 = new Way();
        way1.addNode(new Node(LatLon.ZERO));
        way1.addNode(new Node(LatLon.ZERO));
        way1.put("highway", "tertiary");
        way1.put("name", "Test name");
        Way way2 = new Way();
        way2.addNode(way1.lastNode());
        way2.addNode(new Node(LatLon.ZERO));
        way2.put("highway", "tertiary");
        way2.put("maxspeed", "60");
        way2.put("name", "Test name");

        addToDataset(new DataSet(), Arrays.asList(way1, way2));

        test.visit(way1);
        assertTrue(test.getErrors().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(strings = { "name", "ref" })
    void testHighwayTwoConnectionsName(String key) {
        Way way1 = new Way();
        way1.addNode(new Node(LatLon.ZERO));
        way1.addNode(new Node(LatLon.ZERO));
        way1.put("highway", "tertiary");
        way1.put(key, "Test name");
        Way way2 = new Way();
        way2.addNode(way1.lastNode());
        way2.addNode(new Node(LatLon.ZERO));
        way2.put("highway", "tertiary");
        way2.put("maxspeed", "60");
        way2.put(key, "Test name");
        Way way3 = new Way();
        way3.addNode(new Node(LatLon.ZERO));
        way3.addNode(way1.firstNode());
        way3.put("highway", "tertiary");
        way3.put(key, "Test name");

        addToDataset(new DataSet(), Arrays.asList(way1, way2, way3));

        test.visit(way1);
        assertTrue(test.getErrors().isEmpty());

        way3.put("maxspeed", "60");
        test.visit(way1);
        assertEquals(1, test.getErrors().size());
        TestError testError = test.getErrors().iterator().next();
        assertEquals(1, testError.getPrimitives().size());
        assertSame(way1, testError.getPrimitives().iterator().next());
        assertEquals("Maxspeed has a blank spot with equal maxspeeds on either side", testError.getDescription());
        assertNotNull(testError.getFix());
        assertFalse(way1.hasKey("maxspeed"));
        Command fixCommand = testError.getFix();
        fixCommand.executeCommand();
        assertEquals("60", way1.get("maxspeed"));
        fixCommand.undoCommand();
        assertFalse(way1.hasKey("maxspeed"));

        way1.put(key, "Test name bad");
        test.clear();
        test.visit(way1);
        assertTrue(test.getErrors().isEmpty());

        // Check download area
        way1.put(key, way2.get("name"));
        // We need a node that is not new
        ("name".equals(key) ? way1.lastNode() : way1.firstNode()).setOsmId(1, 1);
        way1.getDataSet().addDataSource(new DataSource(new Bounds(1, 1, 2, 2), ""));
        test.clear();
        test.visit(way1);
        assertTrue(test.getErrors().isEmpty());
    }

    @ParameterizedTest
    @ValueSource(ints = { 0, 30, 60, 90 })
    void testLengths(final int distance) {
        String key = "name";
        Way way1 = new Way();
        // This gives a length of ~45
        way1.addNode(new Node(LatLon.ZERO));
        way1.addNode(new Node(new LatLon(0, 0.0004043)));
        way1.put("highway", "tertiary");
        way1.put(key, "Test name");
        Way way2 = new Way();
        way2.addNode(way1.lastNode());
        way2.addNode(new Node(LatLon.ZERO));
        way2.put("highway", "tertiary");
        way2.put("maxspeed", "60");
        way2.put(key, "Test name");
        Way way3 = new Way();
        way3.addNode(new Node(LatLon.ZERO));
        way3.addNode(way1.firstNode());
        way3.put("highway", "tertiary");
        way3.put("maxspeed", "60");
        way3.put(key, "Test name");
        addToDataset(new DataSet(), Arrays.asList(way1, way2, way3));

        Config.getPref().putDouble("plugin.validations_testing.maxspeed.maxlength", distance);
        test.startTest(NullProgressMonitor.INSTANCE);
        test.clear();
        test.visit(way1);
        assertEquals(1, test.getErrors().size());
        TestError testError = test.getErrors().get(0);
        if (45 < distance) {
            assertNotNull(testError.getFix());
        } else {
            assertNull(testError.getFix());
        }
    }

    /**
     * Robustly add primitives to a dataset
     *
     * @param dataSet    The dataset to add primitives to
     * @param primitives The primitives to add
     */
    private static void addToDataset(DataSet dataSet, Collection<OsmPrimitive> primitives) {
        for (OsmPrimitive primitive : primitives) {
            if (primitive instanceof Node && !dataSet.containsNode((Node) primitive)) {
                dataSet.addPrimitive(primitive);
            } else if (primitive instanceof Way && !dataSet.containsWay((Way) primitive)) {
                addToDataset(dataSet, new ArrayList<>(((Way) primitive).getNodes()));
                dataSet.addPrimitive(primitive);
            } else if (primitive instanceof Relation && !dataSet.containsRelation((Relation) primitive)) {
                addToDataset(dataSet, ((Relation) primitive).getMemberPrimitives());
                dataSet.addPrimitive(primitive);
            }
        }
    }
}
