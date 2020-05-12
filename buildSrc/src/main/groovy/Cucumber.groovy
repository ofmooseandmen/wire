import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.SourceSet
import org.gradle.testing.jacoco.tasks.JacocoReport

/**
 * When applied to a project, adds tasks to run the cucumber test and produce a JaCoCo report.
 */
class Cucumber implements Plugin<Project> {

    void apply(Project project) {

        project.pluginManager.apply(JavaLibraryPlugin.class)

        /* test exclude all; use cucumberTest instead */
        project.getTasksByName('test', false).each {
            it -> it.exclude('**')
        }

        def buildDir = project.buildDir
        def javaPluginConvention = project.getConvention().getPlugin(JavaPluginConvention.class);
        def sourceSets = javaPluginConvention.getSourceSets()
        def compileTestJavaTask = project.getTasksByName('compileTestJava', false)
        
        def cucumberTestTask = project.task([type: JavaExec, dependsOn: compileTestJavaTask], 'cucumberTest') {
            group 'Verification'
            description 'Runs the cucumber tests.'

            outputs.upToDateWhen { false }

            def convention = project.getConvention().getPlugin(JavaPluginConvention.class);
            def testSourceSet = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME);

            classpath = testSourceSet.runtimeClasspath

            main = 'io.cucumber.core.cli.Main'

            def projectDir = project.projectDir

            jvmArgs("-Djava.util.logging.config.file=${projectDir}/src/test/resources/logging.properties") 
    
            doFirst {
                def tags = System.getProperty("tags")
                systemProperty 'io.omam.wire.testkit.tags', tags
                args = [
                    '--strict',
                    '--glue', 'io.omam.wire',
                    '--plugin', 'pretty',
                    '--plugin', 'json:' + "${buildDir}" + '/reports/cucumberTests/wire-tests.json',
                    '--tags', tags,
                    "${projectDir}/src/test/resources/io/"
                ]
            }
        }

        if (project.hasProperty('enableJaCoCo')) {
            project.extensions.getByName('jacoco').applyTo(cucumberTestTask)

            project.task([type: JacocoReport, dependsOn: cucumberTestTask], 'jacocoCucumberTestReport') {
                group 'Verification'
                description 'Generates code coverage report for the cucumberTest task.'

                outputs.upToDateWhen { false }
        
                def mainSourceSet = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
                classDirectories.from(mainSourceSet.output.classesDirs)
                sourceDirectories.from(mainSourceSet.java.srcDirs)

                executionData(project.files("${buildDir}/jacoco/cucumberTest.exec"))

                reports {
                    html.enabled = true
                    xml.enabled = true
                }
            }
        }
    }
}
