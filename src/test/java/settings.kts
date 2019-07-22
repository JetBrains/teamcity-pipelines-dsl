import jetbrains.buildServer.configs.kotlin.v2018_2.BuildType
import jetbrains.buildServer.configs.kotlin.v2018_2.Project
import jetbrains.buildServer.configs.kotlin.v2018_2.project
import jetbrains.buildServer.configs.kotlin.v2018_2.version


/*
   sequence    := (stage)+
   stage    := (serial|parallel)+
   parallel := (serial|sequence)+
   serial   := build
 */

version = "2018.2"

project {

    val otherBuild = build {
        id("OtherBuild")
    }

    val sequence = sequence {
        build {
            id("blah blah blah")
        }
        build {
            id("nom nom nom")
        }
    }


    sequence {
        build(Compile) {
            produces("application.jar")
        }
        parallel {
            dependsOn(sequence)

            build(Test1)

            build {
                id("asdasd")
            }

            sequence {
                dependsOn(Compile)
                build(Test2)
                build(Test3)
            }

        }
        build(Package) {
            requires(Compile, "application.jar")
            produces("application.zip")
        }
        build(Publish) {
            requires(Package, "application.zip")
        }
    }

    build {
        id("YetAnotherBuild")
        dependsOn(Test)
        dependsOn(sequence)
    }

    println()
}

object Compile : BuildType({
    name = "Compile"

})

object Test : BuildType({
    name = "Test"
})

object Test1 : BuildType({
    name = "Test1"
})

object Test2 : BuildType({
    name = "Test2"
})

object Test3 : BuildType({
    name = "Test3"
})

object Test4 : BuildType({
    name = "Test4"
})

object Package : BuildType({
    name = "Package"
})

object Publish : BuildType({
    name = "Publish"
})

object Deploy : BuildType({
    name = "Deploy"
})

