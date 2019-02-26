# TeamCity Pipelines DSL
Kotlin DSL library for TeamCity pipelines

The library provides a number of extensions to simplify creating TeamCity build chains in Kotlin DSL. The main feature of the library is automatic setup of snapshot dependencies.

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

[![](https://jitpack.io/v/JetBrains/teamcity-pipelines-dsl.svg)](https://jitpack.io/#JetBrains/teamcity-pipelines-dsl)

## Examples

To compose the pipeline with the aid of TeamCity Pipelines DSL library you will be using three main terms: *sequence*, *parallel*, and *build*.

The main block of the pipeline is the **sequence**. Inside the sequence we may define *parallel* blocks and the individual *build* stages. Further on, the *parallel* blocks may include the individual *builds* but also *sequences*.

The following example demonstrates a simple sequence with three individual build configurations: 

```kotlin
//settings.kts

import jetbrains.buildServer.configs.kotlin.v2018_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2018_2.buildSteps.script
import jetbrains.buildServer.configs.kotlin.v2018_2.project
import jetbrains.buildServer.configs.kotlin.v2018_2.version

version = "2018.2"


project {
    sequence {
        build(Compile) {
            produces("application.jar")
        }
        build(Test) {
            requires(Compile, "application.jar")
            produces("test.reports.zip")
        }
        build(Package) {
            requires(Compile, "application.jar")
            produces("application.zip")
        }
    }
}

object Compile : BuildType({
    name = "Compile"
    //...
})

object Test : BuildType({
    name = "Test"
    //...
})

object Package : BuildType({
    name = "Package"
    //...
})

```
The snapshot dependencies are defined automatically: **Package** depends on **Test**, and **Test** depends on **Compile**.

```
    _________         ______         _________
   |         |       |      |       |         |              
   | Compile | ----> | Test | ----> | Package |  
   |_________|       |______|       |_________|   
```

The additional artifact dependencies are defined explicitly by using **requires(...)** and **produces(...)** functions.

If there's a need to parallelize part of the sequence we may use **parallel {}** block define the intent:
  
```kotlin
    sequence {
        build(Compile) {
            produces("application.jar")
        }
        parallel {
            build(Test1) {
                requires(Compile, "application.jar")
                produces("test.reports.zip")
            }
            build(Test2) {
                requires(Compile, "application.jar")
                produces("test.reports.zip")
            }
        }
        build(Package) {
            requires(Compile, "application.jar")
            produces("application.zip")
        }
    }
```  
All the build configurations declared in the *parallel* block will have a snapshot dependency on the last build configuration specified before the parallel block. In this case, Test1 and Test2 depend on Compile.

The first build configuration declared after the *parallel* block will have a snapshot dependency on all the build configrations declared in the parallel block. In this case, Package depends on Test1 and Test2.

```
                      _______
                     |       |
    _________        | Test1 |        _________
   |         | ----> |_______| ----> |         |             
   | Compile |        _______        | Package |  
   |_________| ----> |       | ----> |_________|
                     | Test2 |
                     |_______|      
```  
  
Even more, we can put a new sequence into the parallel block:

```kotlin
    sequence {
        build(Compile) {
            produces("application.jar")
        }
        parallel {
            build(Test) {
                requires(Compile, "application.jar")
                produces("test.reports.zip")
            }
            sequence {
                build(RunInspections) {
                   produces("inspection.reports.zip")
                }
                build(RunPerformanceTests) {
                   produces("perf.reports.zip")
                }
            }         
        }
        build(Package) {
            requires(Compile, "application.jar")
            produces("application.zip")
        }
    }   
```
The result is as follows:

```
                      _______
                     |       |
    _________        | Test  |                                _________
   |         | ----> |_______| ----------------------------> |         |             
   | Compile |        _______         ____________           | Package |  
   |_________| ----> | RunIn | ----> | RunPerform | -------> |_________|
                     | spect |       | anceTests  |
                     | ions  |       |____________|
                     |_______|      
```  