package com.jamesward.osgiversion;

import org.apache.maven.plugin.testing.AbstractMojoTestCase;

import java.io.File;

public class UnsnapshotMojoTest extends AbstractMojoTestCase {

    protected void setUp() throws Exception {
        super.setUp();
    }

    public void testCalculateVersion() throws Exception {
        OSGiVersionMojo osgiVersionMojo = new OSGiVersionMojo();

        try {
            osgiVersionMojo.calculateVersion("1.2.3.4-_asdfasdf&/");
        } catch (OSGiVersionMojo.MalformedQualifierException ignored) { }

        try {
            osgiVersionMojo.calculateVersion(".asdf..33öjkhadsf");
        } catch (OSGiVersionMojo.MalformedSegmentException ignored) { }

        try {
            osgiVersionMojo.calculateVersion("1.asdf.3.asd.asdf.sdf");
        } catch (OSGiVersionMojo.MalformedSegmentException ignored) { }

        try {
            osgiVersionMojo.calculateVersion("");
        } catch (OSGiVersionMojo.MalformedVersionException ignored) { }

        assertEquals("1.2.3.beta1-2", osgiVersionMojo.calculateVersion("1.2.3-beta1-2"));
        assertEquals("1.2.0.test", osgiVersionMojo.calculateVersion("1.2-test"));
        assertEquals("1.2.0.SNAPSHOT", osgiVersionMojo.calculateVersion("1.2-SNAPSHOT"));
    }

    public void testPlugin() throws Exception {
        File testPom = getTestFile("src/test/resources/pom.xml");
        assertNotNull(testPom);
        assertTrue(testPom.exists());

        OSGiVersionMojo mojo = (OSGiVersionMojo) lookupMojo("osgi-version", testPom);
        assertNotNull(mojo);

        mojo.execute();
       
        String expected = 
        		"Bundle-Version: 1.2.3.alpha-1\n"
        	   + "Bundle-Description: \n"
        	   + "-resourceonly:true\n"
        	   + "WebJars-Resource:\\\n"
        	   + "/META-INF/resources/webjars/bar/1.2.3-alpha-1,\\\n"
        	   + "/webjars-requirejs.js\n"
        	   + "Provide-Capability: foo;bar:List<String>=1.2.3.alpha-1\n"
        	   + "Require-Capability: foobar;filter:=\"(barfoo=2.1.4.0)\"";

        assertEquals(expected, mojo.project.getModel().getProperties().getProperty(OSGiVersionMojo.MANIFEST_OSGI));
    }

}
