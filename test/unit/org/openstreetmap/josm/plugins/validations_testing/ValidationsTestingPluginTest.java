// License: GPL. For details, see LICENSE file.
package org.openstreetmap.josm.plugins.validations_testing;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.platform.commons.support.ReflectionSupport;
import org.openstreetmap.josm.data.validation.OsmValidator;
import org.openstreetmap.josm.plugins.PluginException;
import org.openstreetmap.josm.plugins.PluginInformation;
import org.openstreetmap.josm.testutils.JOSMTestRules;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Test method for {@link ValidationsTestingPlugin}
 *
 * @author Taylor Smock
 */
class ValidationsTestingPluginTest {
    @RegisterExtension
    static JOSMTestRules rules = new JOSMTestRules().preferences();
    /**
     * This is used to ignore known bad tests that are not added, or a test that
     * happens to be a "super" class.
     */
    private static final List<Class<? extends org.openstreetmap.josm.data.validation.Test>> ignoreList = Collections
            .emptyList();

    private static PluginInformation pluginInformation;

    @BeforeAll
    static void setup() throws PluginException {
        String manifest = "Plugin-Canloadatruntime: true\nAuthor: Random\nPlugin-Class: org.openstreetmap.josm.plugins.validations_testing.ValidationsTestingPlugin\nPlugin-Mainversion: 17084\n";
        pluginInformation = new PluginInformation(new ByteArrayInputStream(manifest.getBytes(StandardCharsets.UTF_8)),
                "validator_testing", null);
    }

    @SuppressWarnings("unchecked") // The class is checked (`isAssignableFrom`)
    @Test
    void ensureAllValidationsAreAdded() {
        List<Class<?>> classes = ReflectionSupport.findAllClassesInPackage(
                "org.openstreetmap.josm.plugins.validations_testing.data.validation.tests",
                clazz -> org.openstreetmap.josm.data.validation.Test.class.isAssignableFrom(clazz), name -> true);
        List<Class<? extends org.openstreetmap.josm.data.validation.Test>> tests = new ArrayList<>(
                classes.size() - ignoreList.size());
        for (Class<?> clazz : classes) {
            if (!ignoreList.contains(clazz)
                    && org.openstreetmap.josm.data.validation.Test.class.isAssignableFrom(clazz)) {
                tests.add((Class<? extends org.openstreetmap.josm.data.validation.Test>) clazz);
            }
        }
        // Ensure that the tests are not currently loaded
        for (Class<? extends org.openstreetmap.josm.data.validation.Test> test : tests) {
            OsmValidator.removeTest(test);
        }
        ValidationsTestingPlugin plugin = new ValidationsTestingPlugin(pluginInformation);
        for (Class<? extends org.openstreetmap.josm.data.validation.Test> test : tests) {
            assertTrue(OsmValidator.removeTest(test),
                    MessageFormat.format("{0} was not added", test.getCanonicalName()));
        }
        plugin.destroy();
        for (Class<? extends org.openstreetmap.josm.data.validation.Test> test : tests) {
            assertFalse(OsmValidator.removeTest(test));
        }

        plugin = new ValidationsTestingPlugin(pluginInformation);
        plugin.destroy();
        for (Class<? extends org.openstreetmap.josm.data.validation.Test> test : tests) {
            assertFalse(OsmValidator.removeTest(test));
        }
    }
}
