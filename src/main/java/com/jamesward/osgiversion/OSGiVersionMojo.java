package com.jamesward.osgiversion;

import java.util.ArrayList;
import java.util.List;

import org.apache.maven.model.Dependency;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Component;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.project.MavenProject;

@Mojo(name = "osgi-version")
public class OSGiVersionMojo extends AbstractMojo {

    public static final String MANIFEST_OSGI = "manifest.osgi";

    @Component
    public MavenProject project;

    public void execute() throws MojoExecutionException {

        try {
        	String manifest = getManifest();
            project.getProperties().setProperty(MANIFEST_OSGI, manifest);
            project.getModel().getProperties().setProperty(MANIFEST_OSGI, manifest);
        } catch (MalformedVersionException | MalformedSegmentException | MalformedQualifierException e) {
            throw new MojoExecutionException("Could not convert version to OSGi version", e);
        }
    }
    
    
    /**
     * @param dependencies a list of Maven dependencies defined for a project
     * @return an empty String if dependencies had size == 0, a String containing all dependencies separated by a comma, where each dependency has the form groupId;filter:="(artifactId=version.osgi)"
     * e.g., the maven coordinates org.test:testartifact.test:1.2.3 will be turned into org.test;filter:="(testartifact.test=1.2.3.0)"
     * @throws MalformedVersionException
     * @throws MalformedSegmentException
     * @throws MalformedQualifierException
     */
    public String calculateDependenciesString(List<Dependency> dependencies) throws MalformedVersionException, MalformedSegmentException, MalformedQualifierException {
    	List<String> result = new ArrayList<>();
    	
    	for (Dependency dependency : dependencies) {
    		// Create string for this dependency in the form:
    		// <groupId>;filter:="(<artifactId>=<version.osgi>)"
    		result.add(dependency.getGroupId() + ";filter:=\"(" + dependency.getArtifactId() + "=" + calculateVersion(dependency.getVersion()) + ")\"");
		}
    	
    	return String.join(",", result);
    }
    
    /**
     * @param mvnVersion
     * @return a version that is compliant tothe description of OSGi versions by https://osgi.org/specification/osgi.core/7.0.0/framework.module.html#i2655136
     */
    public String calculateVersion(String mvnVersion) throws MalformedVersionException, MalformedSegmentException, MalformedQualifierException {

        // default OSGi version
        String[] bndVersionSegments = {"0", "0", "0", "0"};

        if(mvnVersion == null || mvnVersion.length() == 0) {
            // malformed version -> error message?
            throw new MalformedVersionException(mvnVersion);
        }

        // remove all whitespaces
        mvnVersion = mvnVersion.replaceAll("\\s+", "");

        // get all segments, but at most 4
        String[] mvnVersionSegments = mvnVersion.split("\\.", 4);

        for (int i = 0; i < mvnVersionSegments.length; i++) {
            String mvnSegment = mvnVersionSegments[i];
            String[] numberAndRest = getNumberAndRest(mvnSegment);

            switch (numberAndRest.length) {
                case 0:
                    // no leading number found in this segment
                    if(i == 3) {
                        // it's ok, because it is the qualifier, which doesn't need to start with a number
                        bndVersionSegments[3] = formatQualifier(mvnSegment);
                    } else {
                        // malformed segment -> error message?
                        throw new MalformedSegmentException(mvnVersion, bndVersionSegments[3]);
                    }
                    break;
                case 1:
                    // it's a number. This is always acceptable
                    bndVersionSegments[i] = numberAndRest[0];
                    break;
                case 2:
                    // it's a number with something else as rest.
                    if(i == 3) {
                        // it's ok because it is the qualifier
                        bndVersionSegments[3] = formatQualifier(mvnSegment);
                    } else{
                        if(i == mvnVersionSegments.length - 1) {
                            // it's ok because it is the last segment of the maven version. The rest is used as qualifier
                            bndVersionSegments[i] = numberAndRest[0];
                            bndVersionSegments[3] = formatQualifier(numberAndRest[1]);
                        }
                        else {
                            // malformed segment -> error message?
                            throw new MalformedSegmentException(mvnVersion, bndVersionSegments[3]);
                        }
                    }
                    break;
                default:
                    // malformed return -> error message?
                    throw new MalformedVersionException(mvnVersion);
            }
        }
        
        if(bndVersionSegments[3].startsWith("-")) {
        	// remove leading dash from qualifier for readability
        	bndVersionSegments[3] = bndVersionSegments[3].replaceFirst("-", "");
        }
        
        return String.join(".", bndVersionSegments);
    }


    /**
     *
     * @param segment a String in the form of a leading number part and anything else as rest, e.g. 123 or 123asdf.
     * @return a String array 'result' with length 0 if segment has no leading number, 1 if segment is a number (result[0] == segment), 2 if segment has a leading number and anything else as rest (result[0] == number, result[1] == rest)
     */
    private String[] getNumberAndRest(String segment) {
        String pattern = "(\\d+)(.*)";
        if(segment.matches(pattern)) {
            String number = segment.replaceAll(pattern, "$1");
            String rest = segment.replaceAll(pattern, "$2");
            if(rest.length() == 0) {
                return new String[] {number};
            }
            return new String[]{number, rest};
        } else {
            return new String[0];
        }
    }


    /**
     * @param qualifier an arbitrary String
     * @return a String that only contains a-z, A-z, 0-9, -, _
     */
    private String formatQualifier(String qualifier) throws MalformedQualifierException {
        String wellFormedQualifier = qualifier.replaceAll("[^\\w_\\-]", "");
        if(wellFormedQualifier.length() != qualifier.length()) {
            // warning?
            throw new MalformedQualifierException(qualifier);
        }
        return wellFormedQualifier;
    }
    
    /**
     * @return A string in the form of </br>
     * Bundle-SymbolicName: ${project.groupId}.${project.artifactId} </br>
     * Bundle-Version: ${version.osgi} </br>
     * Bundle-Description: ${project.description} </br>
     * -resourceonly:true </br>
     * WebJars-Resource:\ </br>
     * 	/META-INF/resources/webjars/${project.artifactId}/${project.version},\ </br>
     * 	/webjars-requirejs.js </br>
     * Provide-Capability: ${project.groupId};${project.artifactId}:List<String>=${version.osgi} </br>
     * Require-Capability: ${dependencies.osgi} </br>
     * @throws MalformedVersionException
     * @throws MalformedSegmentException
     * @throws MalformedQualifierException
     */
    public String getManifest() throws MalformedVersionException, MalformedSegmentException, MalformedQualifierException {
    	String osgiVersion = calculateVersion(project.getVersion());
    	String osgiDependencies = calculateDependenciesString(project.getDependencies());
    	
    	StringBuilder sb = new StringBuilder();
    	sb.append("Bundle-SymbolicName: " + project.getGroupId() + "." + project.getArtifactId() + "\n");
    	sb.append("Bundle-Version:	" + osgiVersion + "\n");
    	sb.append("-resourceonly:true\n");
    	sb.append("WebJars-Resource:\\\n");
    	sb.append("/META-INF/resources/webjars/" + project.getArtifactId() + "/" + osgiVersion + ",\\\n");
    	sb.append("/webjars-requirejs.js\n");
    	sb.append("Provide-Capability: " + project.getGroupId() + ";" + project.getArtifactId() + ":List<String>=" + osgiVersion + "\n");
    	
    	if(osgiDependencies.length() != 0)
    		sb.append("Require-Capability: " + osgiDependencies);
    	return sb.toString();
    }

    public class MalformedVersionException extends Exception {
        private String version;

        public MalformedVersionException(String version) {
            this.version = version;
        }

        @Override
        public String getMessage() {
            return "Malformed version '" + version + "'";
        }
    }

    public class MalformedSegmentException extends Exception {
        private String version;
        private String segment;

        public MalformedSegmentException(String version, String segment) {
            this.version = version;
            this.segment = segment;
        }

        @Override
        public String getMessage() {
            return "Malformed segment for version '" + version + "' at segment '" + segment + "'";
        }
    }

    public class MalformedQualifierException extends Exception {
        private String qualifier;

        public MalformedQualifierException(String qualifier) {
            this.qualifier = qualifier;
        }

        @Override
        public String getMessage() {
            return "Malformed qualifier '" + qualifier + "'";
        }
    }

}
