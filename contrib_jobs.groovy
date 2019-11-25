/**
 * Jenkins Job DSL for jQAssistant Contrib projects
 *
 * Plugins:
 * - Job DSL plugin
 * - Maven Integration plugin
 * - Config File Provider plugin providing 'oss-maven-settings' containing server settings for deploying to sonatype.org
 * - SSH Agent plugin (GitHub authentication)
 *
 * Credentials:
 * - "GitHub": Private SSH Key
 *
 * Tool installations:
 * - "JDK 1.8"
 * - "Maven 3.5.0"
 */
jdk = 'JDK 1.8'
maven = 'Maven 3.6'
mavenSettings = 'oss-maven-settings'
gitCredentials = 'GitHub'

// Jobs
class Project {
    // The name of the GitHub repository
    String repository
    String name
}

// XO
defineJobs('buschmais', new Project(repository: 'extended-objects', name: 'xo'))

// jQA Contrib
[
        new Project(repository: 'jqassistant-contrib-common', name: 'jqassistant-contrib-common'),
        new Project(repository: 'jqassistant-test-impact-analysis-plugin', name: 'jqassistant-test-impact-analysis-plugin'),
        new Project(repository: 'jqassistant-plantuml-rule-plugin', name: 'jqassistant-plantuml-rule-plugin'),
        new Project(repository: 'jqassistant-dashboard-plugin', name: 'jqassistant-dashboard-plugin'),
        new Project(repository: 'jqassistant-java-metrics-plugin', name: 'jqassistant-java-metrics-plugin'),
        new Project(repository: 'jqassistant-java-ddd-plugin', name: 'jqassistant-java-ddd-plugin'),
        new Project(repository: 'jqassistant-apoc-plugin', name: 'jqassistant-apoc-plugin'),
        new Project(repository: 'jqassistant-graph-algorithms-plugin', name: 'jqassistant-graph-algorithms-plugin'),
        new Project(repository: 'sonar-jqassistant-plugin', name: 'sonar-jqassistant-plugin')
].each {
    defineJobs('jqassistant-contrib', it)
}

def defineJobs(organization, project) {
    ci(organization, project)
    release(organization, project)
}

// Defines a CI job
def ci(organization, project) {
    def gitUrl = "https://github.com/${organization}/${project.repository}.git"
    def jobName = project.name + '-ci'
    job = mavenJob(jobName) {
        authorization {
            permission('hudson.model.Item.Discover', 'anonymous')
            permission('hudson.model.Item.Read', 'anonymous')
            permission('hudson.model.Item.Workspace', 'anonymous')
        }
        lockableResources(project.name)
        // Use a shared repo for enabling trigger on SNAPSHOT changes
        localRepository(LocalRepositoryLocation.LOCAL_TO_EXECUTOR)
        logRotator {
            numToKeep(10)
        }
        scm {
            git {
                remote {
                    url(gitUrl)
                    credentials(gitCredentials)
                }
                branches('refs/heads/master')
            }
        }
        triggers {
            scm('H/15 * * * *')
            snapshotDependencies(true)
            timerTrigger {
                // trigger timer build once a week on sundays
                spec('H H(0-7) * * 7')
            }
        }
        jdk(jdk)
        mavenInstallation(maven)
        providedSettings(mavenSettings)
        goals('clean deploy -PIT')
        mavenOpts('-Dmaven.test.failure.ignore=false')
        publishers {
            mailer('dirk.mahler@buschmais.com', true, true)
        }
        wrappers {
            timeout {
                absolute(minutes = 60)
            }
        }
    }
}

// Defines a Release job
def release(organization, project) {
    def gitUrl = "https://github.com/${organization}/${project.repository}.git"
    def jobName = project.name + '-rel'
    job = mavenJob(jobName) {
        authorization {
            permission('hudson.model.Item.Discover', 'anonymous')
            permission('hudson.model.Item.Read', 'anonymous')
            permission('hudson.model.Item.Workspace', 'anonymous')
        }
        lockableResources(project.name)
        parameters {
            stringParam('Branch', 'master', 'The branch to build the release from.')
            stringParam('ReleaseVersion', '', 'The version to release and to be used as tag.')
            stringParam('DevelopmentVersion', '', 'The next development version.')
            booleanParam('DryRun', false, '')
        }
        wrappers {
            configFiles {
                file(mavenSettings) {
                    variable('MAVEN_SETTINGS')
                }
            }
            sshAgent(gitCredentials)
            preBuildCleanup()
            timeout {
                absolute(minutes = 60)
            }
        }
        logRotator {
            numToKeep(10)
        }
        scm {
            git {
                remote {
                    url(gitUrl)
                    credentials(gitCredentials)
                }
                branch('refs/heads/${Branch}')
                extensions {
                    localBranch('${Branch}')
                }
            }
        }
        jdk(jdk)
        mavenInstallation(maven)
        providedSettings(mavenSettings)
        goals('release:prepare release:perform -s "$MAVEN_SETTINGS" -DautoVersionSubmodules -DreleaseVersion=${ReleaseVersion} -Dtag=${ReleaseVersion} -DdevelopmentVersion=${DevelopmentVersion} -DdryRun=${DryRun}"')
        mavenOpts('-Dmaven.test.failure.ignore=false')
        publishers {
            mailer('dirk.mahler@buschmais.com', true, true)
        }
    }
}
