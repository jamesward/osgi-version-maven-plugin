# OSGi Version Maven Plugin

This plugin makes a new property *${version.osgi}* available to your Maven project.

## How to use it ?

Simply add this plugin to your project/build/plugins POM element :
```xml
<plugin>
     <groupId>com.jamesward</groupId>
     <artifactId>osgi-version-maven-plugin</artifactId>
     <version>0.1</version>
     <executions>
         <execution>
             <phase>initialize</phase>
             <goals><goal>osgi-version</goal></goals>
         </execution>
     </executions>
</plugin>
```

A new property *${version.osgi}* will be exposed.


## Dev Info

Run tests: `./mvnw test`

