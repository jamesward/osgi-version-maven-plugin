package com.jamesward.osgiversion;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.testing.stubs.MavenProjectStub;

public class ProjectStub extends MavenProjectStub {

    public ProjectStub() {
        setGroupId("foo");
        setArtifactId("bar");
        setVersion("1.2.3-alpha1");
        setName("foobar");
        
        Dependency dependency = new Dependency();
        dependency.setGroupId("foobar");
        dependency.setArtifactId("barfoo");
        dependency.setVersion("2.1.4.0");
        
        List<Dependency> dependencies = new ArrayList<>();
        dependencies.add(dependency);
        
        setDependencies(dependencies);
    }

}
