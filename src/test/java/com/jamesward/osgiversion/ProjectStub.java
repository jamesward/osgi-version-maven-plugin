package com.jamesward.osgiversion;

import org.apache.maven.plugin.testing.stubs.MavenProjectStub;

public class ProjectStub extends MavenProjectStub {

    public ProjectStub() {
        setGroupId("foo");
        setArtifactId("bar");
        setVersion("1.2.3-alpha1");
        setName("foobar");
    }

}
