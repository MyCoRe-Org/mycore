# MyCoRe Pandoc Integration
## Prerequisites
* Install [Pandoc](https://www.pandoc.org/) Version 2.19 or higher (for binary releases, see [https://github.com/jgm/pandoc/releases](https://github.com/jgm/pandoc/releases))

## Customization (Optional)
* Pandoc can be extended via [custom readers](https://pandoc.org/custom-readers.html) and [custom writers](https://pandoc.org/custom-writers.html) which can be written in [Lua](https://www.lua.org/)
* This module provides some basic Lua scripts (in src/main/resources/lua). Since all Lua scripts need to be available to the external command pandoc via the environment variable [LUA_PATH](https://www.lua.org/pil/8.1.html), the respective resources must
  * either be provided in the source tree of your application (in src/main/resources/lua)
  * or be extracted from the jar-file of this module via the following plugin
```xml
<plugin>
    <groupId>org.apache.maven.plugins</groupId>
    <artifactId>maven-dependency-plugin</artifactId>
    <executions>
        <execution>
            <goals>
                <goal>unpack</goal>
            </goals>
            <phase>process-resources</phase>
            <configuration>
                <artifactItems>
                    <artifactItem>
                        <groupId>org.mycore</groupId>
                        <artifactId>mycore-pandoc</artifactId>
                        <version>${mycore.version}</version>
                        <type>jar</type>
                        <overWrite>false</overWrite>
                        <outputDirectory>${project.build.directory}/${project.build.finalName}/WEB-INF/classes</outputDirectory>
                        <includes>lua/*.*</includes>
                    </artifactItem>
                </artifactItems>
            </configuration>
        </execution>
    </executions>
</plugin>
```
