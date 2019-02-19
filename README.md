# TeamCity Pipelines DSL
Kotlin DSL library for TeamCity pipelines

The library provides a number of extensions to simplify creating TeamCity build chains in Kotlin DSL.

## Usage

1. Add jitpack repository to your .teamcity/pom.xml  


    &lt;repositories&gt;
      &lt;repository&gt;
        &lt;id&gt;jitpack.io&lt;/id&gt;
        &lt;url&gt;https://jitpack.io&lt;/url&gt;
      &lt;/repository&gt;
    &lt;/repository&gt;


2. Add the library as a dependency in .teamcity/pom.xml


	&lt;dependency&gt;
	    &lt;groupId&gt;com.github.JetBrains&lt;/groupId&gt;
	    &lt;artifactId&gt;teamcity-pipelines-dsl&lt;/artifactId&gt;
	    &lt;version&gt;[tag]&lt;/version&gt;
	&lt;/dependency&gt;


Enjoy!

[![](https://jitpack.io/v/JetBrains/teamcity-pipelines-dsl.svg)](https://jitpack.io/#JetBrains/teamcity-pipelines-dsl)