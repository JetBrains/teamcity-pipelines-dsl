# TeamCity Pipelines DSL
Kotlin DSL library for TeamCity pipelines

The library provides a number of extensions to simplify creating TeamCity build chains in Kotlin DSL.

## Usage

1. Add jitpack repository to your .teamcity/pom.xml  

```xml
    <repositories>
      <repository>
        <id>jitpack.io</id>
        <url>https://jitpack.io</url>
      </repository>
    </repository>
```


2. Add the library as a dependency in .teamcity/pom.xml

```xml
   <dependency>
	 <groupId>com.github.JetBrains</groupId>
	 <artifactId>teamcity-pipelines-dsl</artifactId>
	 <version>[tag]</version>
   </dependency>
```


Enjoy!

[![](https://jitpack.io/v/JetBrains/teamcity-pipelines-dsl.svg)](https://jitpack.io/#JetBrains/teamcity-pipelines-dsl)