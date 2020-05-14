/*
Copyright 2020-2020 Cedric Liegeois

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.

    * Redistributions in binary form must reproduce the above
      copyright notice, this list of conditions and the following
      disclaimer in the documentation and/or other materials provided
      with the distribution.

    * Neither the name of the copyright holder nor the names of other
      contributors may be used to endorse or promote products derived
      from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT
LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR
A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
OWNER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE,
DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY
THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.FileCollection
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.testing.jacoco.tasks.JacocoReport

class CucumberExtension {
    String glue
}


/**
 * When applied to a project, adds tasks to run the cucumber test and produce a JaCoCo report.
 */
class Cucumber implements Plugin<Project> {

    private CucumberExtension extension

    void apply(Project project) {
        this.extension = project.extensions.create('cucumber', CucumberExtension)
        
        project.afterEvaluate { p -> doApply(p) }
    }
    
    private void doApply(Project project) {

        if (!project.pluginManager.hasPlugin('java-library')) {
            throw new GradleException('The Cucumber plugin requires the Java library plugin.')
        }
        
        /* test exclude all; use cucumberTest instead */
        project.getTasksByName('test', false).each {
            it -> it.exclude('**')
        }

        def buildDir = project.buildDir
        def compileTestJavaTask = project.getTasksByName('compileTestJava', false)

        def cucumberTestTask = project.task([type: JavaExec, dependsOn: compileTestJavaTask], 'cucumberTest') {
            group 'Verification'
            description 'Runs the cucumber tests.'

            outputs.upToDateWhen { false }

            def testSourceSet = getSourceSets(project).getByName(SourceSet.TEST_SOURCE_SET_NAME);

            classpath = testSourceSet.runtimeClasspath

            main = 'io.cucumber.core.cli.Main'

            def projectDir = project.projectDir

            jvmArgs("-Djava.util.logging.config.file=${projectDir}/src/test/resources/logging.properties") 

            doFirst {
                def tags = System.getProperty("tags")
                systemProperty 'io.omam.wire.testkit.tags', tags

                def features = this.extension.glue.replaceAll('\\.', '/')

                args = [
                    '--strict',
                    '--glue', extension.glue,
                    '--plugin', 'pretty',
                    '--plugin', 'json:' + "${buildDir}" + '/reports/cucumberTests/wire-tests.json',
                    '--tags', tags,
                    "${projectDir}/src/test/resources/${features}"
                ]
            }
        }

        if (project.hasProperty('enableJaCoCo')) {
            project.extensions.getByName('jacoco').applyTo(cucumberTestTask)
            cucumberTestTask.jacoco.excludes = ['*CastChannel*']
            project.task([type: JacocoReport], 'jacocoCucumberTestReport') {
                group 'Verification'
                description 'Generates code coverage report for the cucumberTest task.'

                outputs.upToDateWhen { false }

                def mainSourceSet = getSourceSets(project).getByName(SourceSet.MAIN_SOURCE_SET_NAME);
                classDirectories.from(mainSourceSet.output.classesDirs)
                sourceDirectories.from(mainSourceSet.java.srcDirs)
                def addClassDirs = findAdditionalClassDirectories(project).collect {
                    it.asFileTree.matching {
                        exclude '**/*CastChannel**'
                    }
                }
                additionalClassDirs.from(addClassDirs)
                additionalSourceDirs.from(findAdditionalSourceDirectories(project))
                executionData(project.files("${buildDir}/jacoco/cucumberTest.exec"))

                reports {
                    html.enabled = true
                    xml.enabled = true
                }
            }
        }
        
    }

    private Set<File> findAdditionalSourceDirectories(Project project) {
        return findProjects(project).collect { getSourceSets(it) }
                   .collect { it.getByName(SourceSet.MAIN_SOURCE_SET_NAME).java.srcDirs }
    }

    private List<FileCollection> findAdditionalClassDirectories(Project project) {
        return findProjects(project).collect { getSourceSets(it) }
                    .collect { it.getByName(SourceSet.MAIN_SOURCE_SET_NAME).output.classesDirs }
    }

    private Collection<ProjectDependency> findProjects(Project project) {
        return project.configurations.getByName('implementation')
                   .allDependencies.findAll { it instanceof ProjectDependency }
                   .collect { ((ProjectDependency) it).dependencyProject }
    }

    private SourceSetContainer getSourceSets(Project project) {
        def javaPluginConvention = project.getConvention().getPlugin(JavaPluginConvention.class);
        return javaPluginConvention.getSourceSets()    
    }

}
