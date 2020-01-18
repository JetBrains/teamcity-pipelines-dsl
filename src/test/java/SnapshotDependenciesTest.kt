/*
 * Copyright 2000-2020 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import jetbrains.buildServer.configs.kotlin.v2018_2.*
import org.junit.Assert.*
import org.junit.Test
import java.lang.AssertionError


class SnapshotDependenciesTest {

    private fun BuildType.dependencyId(index: Int) =
        dependencies.items[index].buildTypeId.id?.value


    @Test
    fun simpleSequence() {
        //region given for simpleSequence
        val a = BuildType { id("A") }
        val b = BuildType { id("B") }
        val c = BuildType { id("C") }
        //endregion

        val project = Project {
            sequence {
                build(a)
                build(b)
                build(c)
            }
        }

        //region assertions for simpleSequence
        assertEquals(3, project.buildTypes.size)

        assertEquals(0, a.dependencies.items.size)
        assertEquals(1, b.dependencies.items.size)
        assertEquals(1, c.dependencies.items.size)

        assertEquals("A", b.dependencyId(0))
        assertEquals("B", c.dependencyId(0))
        //endregion
    }

    @Test
    fun minimalDiamond() {
        //region given for minimalDiamond
        val a = BuildType { id("A") }
        val b = BuildType { id("B") }
        val c = BuildType { id("C") }
        val d = BuildType { id("D") }
        //endregion

        val project = Project {
            sequence {
                build(a)
                parallel {
                    build(b)
                    build(c)
                }
                build(d)
            }
        }

        //region assertions for minimalDiamond
        assertEquals(4, project.buildTypes.size)

        assertEquals(0, a.dependencies.items.size)
        assertEquals(1, b.dependencies.items.size)
        assertEquals(1, c.dependencies.items.size)
        assertEquals(2, d.dependencies.items.size)

        assertEquals("A", b.dependencyId(0))
        assertEquals("A", c.dependencyId(0))

        assertEquals("B", d.dependencyId(0))
        assertEquals("C", d.dependencyId(1))
        //endregion
    }

    @Test
    fun sequenceInParallel() {
        //region given for sequenceInParallel
        val a = BuildType { id("A") }
        val b = BuildType { id("B") }
        val c = BuildType { id("C") }
        val d = BuildType { id("D") }
        val e = BuildType { id("E") }
        //endregion

        val project = Project {
            sequence {
                build(a)
                parallel {
                    build(b)
                    sequence {
                        build(c)
                        build(d)
                    }
                }
                build(e)
            }
        }

        //region assertions for sequenceInParallel
        assertEquals(5, project.buildTypes.size)

        assertEquals(0, a.dependencies.items.size)
        assertEquals(1, b.dependencies.items.size)
        assertEquals(1, c.dependencies.items.size)
        assertEquals(1, d.dependencies.items.size)
        assertEquals(2, e.dependencies.items.size)

        assertEquals("A", b.dependencyId(0))
        assertEquals("A", c.dependencyId(0))

        assertEquals("C", d.dependencyId(0))

        assertEquals("B", e.dependencyId(0))
        assertEquals("D", e.dependencyId(1))
        //endregion
    }

    @Test
    fun outOfSequenceDependency() {
        //region given for outOfSequenceDependency
        val a = BuildType { id("A") }
        val b = BuildType { id("B") }
        val c = BuildType { id("C") }
        val d = BuildType { id("D") }
        val e = BuildType { id("E") }
        val f = BuildType { id("F") }
        //endregion

        val project = Project {

            build(f) // 'f' does not belong to sequence

            sequence {
                build(a)
                parallel {
                    build(b) {
                        dependsOn(f)
                    }
                    sequence {
                        build(c)
                        build(d)
                    }
                }
                build(e)
            }
        }

        //region assertions for outOfSequenceDependency
        assertEquals(6, project.buildTypes.size)

        assertEquals(0, a.dependencies.items.size)
        assertEquals(2, b.dependencies.items.size)
        assertEquals(1, c.dependencies.items.size)
        assertEquals(1, d.dependencies.items.size)
        assertEquals(2, e.dependencies.items.size)

        assertEquals("F", b.dependencyId(0))
        assertEquals("A", b.dependencyId(1))

        assertEquals("A", c.dependencyId(0))

        assertEquals("C", d.dependencyId(0))

        assertEquals("B", e.dependencyId(0))
        assertEquals("D", e.dependencyId(1))
        //endregion
    }


    @Test
    fun parallelDependsOnParallel() {
        //region given for parallelDependsOnParallel
        val a = BuildType { id("A") }
        val b = BuildType { id("B") }
        val c = BuildType { id("C") }
        val d = BuildType { id("D") }
        val e = BuildType { id("E") }
        val f = BuildType { id("F") }
        //endregion

        val project = Project {
            sequence {
                build(a)
                parallel {
                    build(b)
                    build(c)
                }
                parallel {
                    build(d)
                    build(e)
                }
                build(f)
            }
        }

        //region assertions for parallelDependsOnParallel
        assertEquals(6, project.buildTypes.size)

        assertEquals(0, a.dependencies.items.size)
        assertEquals(1, b.dependencies.items.size)
        assertEquals(1, c.dependencies.items.size)
        assertEquals(2, d.dependencies.items.size)
        assertEquals(2, e.dependencies.items.size)
        assertEquals(2, f.dependencies.items.size)

        assertEquals("A", b.dependencyId(0))
        assertEquals("A", c.dependencyId(0))

        assertEquals("B", d.dependencyId(0))
        assertEquals("C", d.dependencyId(1))

        assertEquals("B", e.dependencyId(0))
        assertEquals("C", e.dependencyId(1))

        assertEquals("D", f.dependencyId(0))
        assertEquals("E", f.dependencyId(1))
        //endregion
    }

    @Test
    fun simpleDependencySettings() {

        //region given for simpleDependencySettings
        val a = BuildType { id("A") }
        val b = BuildType { id("B") }

        val settings: SnapshotDependency.() -> Unit = {
            runOnSameAgent = true
            onDependencyCancel = FailureAction.IGNORE
            reuseBuilds = ReuseBuilds.NO
        }
        //endregion

        val project = Project {
            sequence {
                build(a)
                sequence {
                    build(b)
                    dependencySettings(settings)
                }
            }
        }

        //region assertions for simpleDependencySettings
        assertEquals(2, project.buildTypes.size)

        assertEquals(0, a.dependencies.items.size)
        assertEquals(1, b.dependencies.items.size)

        assertEquals("A", b.dependencyId(0))

        val expected = SnapshotDependency().apply(settings)
        val actual = b.dependencies.items[0].snapshot
        assertEquals(expected.runOnSameAgent, actual!!.runOnSameAgent)
        assertEquals(expected.onDependencyCancel, actual.onDependencyCancel)
        assertEquals(expected.onDependencyFailure, actual.onDependencyFailure)
        //endregion
    }

    @Test
    fun nestedSequenceSettings() {
        //region given for dependsOnWithSettings
        val a = BuildType { id("A") }
        val b = BuildType { id("B") }
        val c = BuildType { id("C") }
        val d = BuildType { id("D") }
        val e = BuildType { id("E") }
        val settings: SnapshotDependency.() -> Unit = {
            runOnSameAgent = true
            onDependencyCancel = FailureAction.IGNORE
            reuseBuilds = ReuseBuilds.NO
        }
        //endregion

        val project = Project {
            sequence {
                build(a)
                parallel {
                    build(b)
                    sequence {
                        build(c)
                        build(d)
                    }
                    dependencySettings(settings)
                }
                build(e)
            }
        }

        //region assertions for nestedSequenceSettings
        assertEquals(5, project.buildTypes.size)

        assertEquals(0, a.dependencies.items.size)
        assertEquals(1, b.dependencies.items.size)
        assertEquals(1, c.dependencies.items.size)
        assertEquals(1, d.dependencies.items.size)
        assertEquals(2, e.dependencies.items.size)

        val expected = SnapshotDependency().apply(settings)
        val actualForB = b.dependencies.items[0].snapshot
        assertEquals(expected.runOnSameAgent, actualForB!!.runOnSameAgent)
        assertEquals(expected.onDependencyCancel, actualForB.onDependencyCancel)
        assertEquals(expected.onDependencyFailure, actualForB.onDependencyFailure)

        val actualForC = c.dependencies.items[0].snapshot
        assertEquals(expected.runOnSameAgent, actualForC!!.runOnSameAgent)
        assertEquals(expected.onDependencyCancel, actualForC.onDependencyCancel)
        assertEquals(expected.onDependencyFailure, actualForC.onDependencyFailure)

        //TODO: The following fails because the settings are only applied to the fan-ins of the parallel block
//        val actualForD = d.dependencies.items[0].snapshot
//        assertEquals(expected.runOnSameAgent, actualForD!!.runOnSameAgent)
//        assertEquals(expected.onDependencyCancel, actualForD.onDependencyCancel)
//        assertEquals(expected.onDependencyFailure, actualForD.onDependencyFailure)
        //endregion
    }

    @Test
    fun sequenceDependencies() {

        //region given for sequenceDependencies
        val a = BuildType { id("A") }
        val b = BuildType { id("B") }
        val c = BuildType { id("C") }

        val settings: SnapshotDependency.() -> Unit = {
            runOnSameAgent = true
            onDependencyCancel = FailureAction.IGNORE
            reuseBuilds = ReuseBuilds.NO
        }
        //endregion

        val project = Project {
            val s1 = sequence { build(a) }
            val s2 = sequence { build(b) }

            val s3 = sequence {
                dependsOn(s1, settings)
                dependsOn(s2)
                build(c)
            }
        }

        //region assertions for sequenceDependencies
        assertEquals(3, project.buildTypes.size)
        assertEquals(0, a.dependencies.items.size)
        assertEquals(0, b.dependencies.items.size)
        assertEquals(2, c.dependencies.items.size)

        val expected = SnapshotDependency().apply(settings)
        val cDependsOnA = c.dependencies.items[0].snapshot
        assertEquals(expected.runOnSameAgent, cDependsOnA!!.runOnSameAgent)
        assertEquals(expected.onDependencyCancel, cDependsOnA.onDependencyCancel)
        assertEquals(expected.onDependencyFailure, cDependsOnA.onDependencyFailure)

        val cDepensdOnB = c.dependencies.items[1].snapshot
        val default = SnapshotDependency()
        assertEquals(default.runOnSameAgent, cDepensdOnB!!.runOnSameAgent)
        assertEquals(default.onDependencyCancel, cDepensdOnB.onDependencyCancel)
        assertEquals(default.onDependencyFailure, cDepensdOnB.onDependencyFailure)
        //endregion
    }

    @Test(expected = AssertionError::class)
    fun singleDependsOnParallelWithSettings() {
        var par: Parallel

        val Compile = BuildType { id("Compile") }
        val Test = BuildType { id("Test") }
        val RunInspections = BuildType { id("RunInspections") }
        val RunPerformanceTests = BuildType { id("RunPerformanceTests") }
        val Package = BuildType { id("Package") }

        val project = Project {
            sequence {
                build(Compile) {
                    produces("application.jar")
                }
                par = parallel {
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
                    dependsOn(par) {
                       onDependencyFailure = FailureAction.FAIL_TO_START // <--- don't want to Package if any build in parallel failed
                    }
                }
            }
        }

        //region assertions for sequenceDependencies
        assertEquals(5, project.buildTypes.size)
        assertEquals(3, Package.dependencies.items.size)
        assertNull(Package.dependencies.items[0].snapshot)
        assertNotNull(Package.dependencies.items[1].snapshot)
        assertNotNull(Package.dependencies.items[2].snapshot)

        val snapshot = Package.dependencies.items[1].snapshot
        assertEquals(FailureAction.FAIL_TO_START, snapshot?.onDependencyFailure)
        //endregion
    }
}
