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
    </repositories>
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

To compose the pipeline with the aid of TeamCity Pipelines DSL library you will be using three main terms: **sequence**, **parallel**, and **build**.

The main block of the pipeline is the **sequence**. Inside the sequence we may define *parallel* blocks and the individual *build* stages. Further on, the *parallel* blocks may include the individual *builds* but also *sequences*.

### Simple sequence

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

### The minimal diamond 

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
  
### Combining different blocks  
  
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

### Inline build definitions

There's actually an alternative form of the build() function that allows specifying the build configuration inline:


```kotlin
    sequence {
        val compile = build {
            id("Compile")
            name = "Compile"
            
            //alternatively, we could use produces() function here
            artifactRules = "target/application.jar"
         
            vcs {
              root(ApplicationVcs)
            }
            
            steps {
               maven {
                  goals = "clean package"
               }
            } 
        }
        val test = build {
            id("Test")
            name = "Test"
            
            vcs {
               root(ApplicationTestsVcs)
            }
            
            steps {
               // do something here..
            }
            
            // requires(...) is an alternative to 
            // dependencies { at
            //   artifact(compile) {
            //     artifactRules = ..
            //   }
            // }
            requires(compile, "application.jar")
            
        }
    }   
``` 

In the example above, we define two build configurations -- Compile and Test -- and register those in the sequence, meaning that Test build configuration will have a snapshot dependency on Compile.

### Arbitrary dependencies

The **sequence** is an abstraction that allows you to specify the dependencies in build configurations depending on the order in which they are declared within the sequence.  

However, sometimes it might be needed to create a dependency on a build configuration that is defined outside of the sequence. In this case, it is possible to use **dependsOn** method within a block 


```kotlin
    build(OtherBuild)
     
    sequence {
        build(Compile) {
            produces("application.jar")
        }
        parallel {
            dependsOn(SomeOtherConfiguration)
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

The **dependsOn** method invoked in a parallel block adds a dependency to every stage in that block. 
Hence, Test and RunInspections build configuration will end up having a dependency on OtherBuild:

```  
   ________________
  |                |
  |  OtherBuild    |                 _______
  |________________|--------------->|       |
        |          _________        | Test  |                                _________
        |         |         | ----> |_______| ----------------------------> |         |             
        |         | Compile |        _______         ____________           | Package |  
        |         |_________| ----> | RunIn | ----> | RunPerform | -------> |_________|
        |                           | spect |       | anceTests  |
        --------------------------->| ions  |       |____________|
                                    |_______|      
```

You can also use dependsOn to declare a dependency for a build configuration on a sequence (or a parallel block):


```kotlin
    var seq = Sequence() 
    
    sequence {
        build(Compile) {
            produces("application.jar")
        }
        parallel {
            build(Test) {
                requires(Compile, "application.jar")
                produces("test.reports.zip")
            }
            seq = sequence { // <--------- assigning a nested sequence 
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

    build(OtherBuild){
        dependsOn(seq) // <----------- depends on nested sequence
    }
``` 

The result is as follows:

```
                      _______
                     |       |
    _________        | Test  |                                _________          ____________
   |         | ----> |_______| ----------------------------> |         |        |            |     
   | Compile |        _______         ____________           | Package |        | OtherBuild |
   |_________| ----> | RunIn | ----> | RunPerform | -------> |_________|   ---->|____________|
                     | spect |       | anceTests  |                       |
                     | ions  |       |____________| ----------------------
                     |_______|      
```

### Snapshot dependency settings

Snapshot dependencies include various settings. For instance, we might choose to run the build on the same agent as the dependency.

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
            dependencySettings {
               runOnSameAgent = true // <--- 'Test' & 'RunInspections' will run on the same agent as 'Compile'
            }
        }
        build(Package) {
            requires(Compile, "application.jar")
            produces("application.zip")
        }
    }   
``` 

Also, the **dependsOn** method allows adding a block with the snapshot dependency settings:

```kotlin
    val other = build { id("other")}

    sequence {
        build(Compile) {
            produces("application.jar")
        }
        parallel {
            build(Test) {
                dependsOn(other){
                   runOnSameAgent = true // <--- 'Test' will run on the same agent as 'other'
                }
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

The **dependsOn** method can be used for the individual bulid configuration as well as for *parallel* and *sequence* blocks:

```kotlin
    val other = build {id("other")}

    sequence {
        build(Compile) {
            produces("application.jar")
        }
        parallel {
            dependsOn(other){
                runOnSameAgent = true // <--- 'Test' and 'RunInspections' will both run on the same agent as 'other'
            }
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

