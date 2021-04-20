// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.validations_testing.data.validation.tests;

import org.openstreetmap.josm.command.ChangePropertyCommand;
import org.openstreetmap.josm.data.osm.Way;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.data.validation.TestError;
import org.openstreetmap.josm.gui.progress.ProgressMonitor;

import java.util.List;
import java.util.Optional;
import java.util.regex.Pattern;

import static org.openstreetmap.josm.tools.I18n.tr;
import org.openstreetmap.josm.data.osm.Node;
import org.openstreetmap.josm.data.validation.Severity;
import org.openstreetmap.josm.spi.preferences.Config;

/**
 * Look for short ways that happen to be missing a maxspeed, when the maxspeed
 * is the same on both sides.
 *
 * @author Taylor Smock
 */
public class Maxspeed extends Test {
    private static final String HIGHWAY = "highway";
    private static final String NAME_TAG = "name";
    private static final String REF = "ref";
    private static final String MAX_SPEED = "maxspeed";
    /**
     * In order for the ignore list to work, each validator error must have a unique
     * identifier
     */
    // I add 1_000_000 to the intended code to avoid accidental duplications.
    private static final int MAX_SPEED_CODE = 1_000_000 + 4100;
    private static final int MAX_SPEED_BLANKSPOT = MAX_SPEED_CODE + 1;

    // Patterns are compiled ahead of time, as they can be very expensive
    private static final Pattern LINK_REGEX = Pattern.compile("^.*_link$");
    private static final Pattern HIGHWAY_REGEX = Pattern
            .compile("^(motorway|trunk|primary|secondary|tertiary|unclassified|residential|service|.*_link)$");

    private static final int MAXLENGTH = 30; // meters
    private double maxlength;

    public Maxspeed() {
        super(tr("Maxspeed consistency"), tr("Looks for short ways that have the same maxspeed on both sides"));
        this.maxlength = Config.getPref().getDouble("plugin.validations_testing.maxspeed.maxlength", MAXLENGTH);
    }

    @Override
    public void startTest(ProgressMonitor monitor) {
        super.startTest(monitor);
        // This ensures that if the user changes the max length between runs, we get the
        // updated max length
        this.maxlength = Config.getPref().getDouble("plugin.validations_testing.maxspeed.maxlength", MAXLENGTH);
    }

    @Override
    public void visit(Way way) {
        if (!way.isUsable() || (way.hasKey(HIGHWAY) && LINK_REGEX.matcher(way.get(HIGHWAY)).matches()))
            return;
        int connections = getNumberOfConnections(way, HIGHWAY, HIGHWAY_REGEX);
        if (connections == 2) {
            checkMaxspeedConsistency(way);
        }
    }

    /**
     * Get the number of connections for a way
     *
     * @param way   The way to check
     * @param key   The key to look at (required to be counted as a connection)
     * @param regex The regex for the key value to match (required to count as a
     *              connection)
     * @return The number of connections for the way
     */
    private static int getNumberOfConnections(Way way, String key, Pattern regex) {
        int returnValue = 0;
        if (way.firstNode().isOutsideDownloadArea() || way.lastNode().isOutsideDownloadArea())
            return -1;
        for (int i = 0; i < way.getNodesCount(); i++) {
            List<Way> refs = way.getNode(i).getParentWays();
            refs.remove(way);
            Optional<Way> connected = refs.stream().filter(wp -> wp.hasKey(key))
                    .filter(wp -> regex.matcher(wp.get(key)).matches()).findAny();
            if (connected.isPresent()) {
                returnValue++;
            }
        }
        return returnValue;
    }

    /**
     * Get the maxspeed of another way from a node
     *
     * @param way  The originating way
     * @param node The node
     * @return The maxspeed for another way (if it exists)
     */
    private static String getMaxspeedOther(Way way, Node node) {
        List<Way> refs = node.getParentWays();
        refs.remove(way);
        Optional<Way> nWay = refs.stream()
                .filter(tWay -> (tWay.hasKey(NAME_TAG) && tWay.get(NAME_TAG).equals(way.get(NAME_TAG)))
                        || (tWay.hasKey(REF) && tWay.get(REF).equals(way.get(REF))))
                .findAny();

        if (nWay.isPresent() && nWay.get().hasKey(MAX_SPEED)) {
            return nWay.get().get(MAX_SPEED);
        } else {
            return null;
        }
    }

    /**
     * Check the actual maxspeed consistency. This is what actually adds the error
     * to the list.
     *
     * @param way The way to check
     */
    private void checkMaxspeedConsistency(Way way) {
        String firstNodeMaxspeed = getMaxspeedOther(way, way.firstNode());
        String lastNodeMaxspeed = getMaxspeedOther(way, way.lastNode());
        boolean needsMaxspeed = !way.hasKey(MAX_SPEED);
        if (firstNodeMaxspeed != null && firstNodeMaxspeed.equals(lastNodeMaxspeed) && needsMaxspeed) {
            TestError.Builder testError = TestError.builder(this, Severity.WARNING, MAX_SPEED_BLANKSPOT)
                    .message(tr("testing"), tr("Maxspeed has a blank spot with equal maxspeeds on either side"))
                    .primitives(way);
            if (way.getLength() < this.maxlength) {
                testError.fix(() -> new ChangePropertyCommand(way, MAX_SPEED, firstNodeMaxspeed));
            }
            errors.add(testError.build());
        }
    }
}
