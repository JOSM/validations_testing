// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.validations_testing;

import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.data.validation.Test;
import org.openstreetmap.josm.plugins.Plugin;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.plugins.validations_testing.data.validation.tests.Maxspeed;
import org.openstreetmap.josm.tools.Destroyable;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * The entry point for generic validations
 *
 * @author Taylor Smock
 */
public class ValidationsTestingPlugin extends Plugin implements Destroyable {
    /** This collection contains tests to be added to the OsmValidator */
    private static final List<Class<? extends Test>> TESTS = Collections
            .unmodifiableList(Arrays.asList(Maxspeed.class));

    /**
     * Create a new plugin instance
     *
     * @param info The information for the plugin
     */
    public ValidationsTestingPlugin(PluginInformation info) {
        super(info);
        TESTS.forEach(OsmValidator::addTest);
    }

    @Override
    public void destroy() {
        TESTS.forEach(OsmValidator::removeTest);
    }
}
