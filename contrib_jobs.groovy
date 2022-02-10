/**
 * Jenkins Job DSL for jQAssistant Contrib projects
 *
 * Plugins:
 * - Job DSL plugin
 * - Maven Integration plugin
 * - Config File Provider plugin providing 'oss-maven-settings' containing server settings for deploying to sonatype.org
 * - SSH Agent plugin (GitHub authentication)
 * - Git Parameter plugin
 *
 * Credentials:
 * - "GitHub": Private SSH Key
 *
 * Tool installations:
 * - "Maven 3.6"
 */
maven = 'Maven 3.6'
mavenSettings = 'oss-maven-settings'

// Jobs
class Project {
    // The name of the GitHub repository
    String repository
    // JDK
    String jdk = 'JDK 11'
    // if true a sonar analysis will be triggered for each ci/release build
    boolean runSonar = true
}

// XO
defineJobs('buschmais', new Project(repository: 'extended-objects'))

// jQA Contrib
[
        new Project(repository: 'jqassistant-apoc-plugin'),
        new Project(repository: 'jqassistant-c4-plugin'),
        new Project(repository: 'jqassistant-context-mapper-plugin'),
        new Project(repository: 'jqassistant-contrib-common'),
        new Project(repository: 'jqassistant-dashboard-plugin'),
        new Project(repository: 'jqassistant-docker-plugin'),
        new Project(repository: 'jqassistant-graph-algorithms-plugin'),
        new Project(repository: 'jqassistant-hcl-plugin'),
        new Project(repository: 'jqassistant-java-ddd-plugin'),
        new Project(repository: 'jqassistant-java-metrics-plugin'),
        new Project(repository: 'jqassistant-jmolecules-plugin'),
        new Project(repository: 'jqassistant-plantuml-rule-plugin'),
        new Project(repository: 'jqassistant-test-impact-analysis-plugin'),
        new Project(repository: 'jqassistant-wordcloud-report-plugin'),
        new Project(repository: 'jqassistant-xmi-plugin'),
        new Project(repository: 'sonar-jqassistant-plugin')
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
    def jobName = project.repository + '-ci'
    job = mavenJob(jobName) {
        authorization {
            permission('hudson.model.Item.Discover', 'anonymous')
            permission('hudson.model.Item.Read', 'anonymous')
            permission('hudson.model.Item.Workspace', 'anonymous')
        }
        lockableResources(project.repository)
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
        jdk(project.jdk)
        mavenInstallation(maven)
        providedSettings(mavenSettings)
        if (project.runSonar) {
            goals('clean deploy -PIT,sonar')
        } else {
            goals('clean deploy -PIT')
        }
        fingerprintingDisabled()
        publishers {
            mailer('dirk.mahler@buschmais.com', true, true)
        }
        wrappers {
            credentialsBinding {
                string('SONARCLOUD_LOGIN', 'SonarCloud')
            }
            timeout {
                absolute(minutes = 60)
            }
        }
    }
}

// Defines a Release job
def release(organization, project) {
    def gitUrl = "https://github.com/${organization}/${project.repository}.git"
    def jobName = project.repository + '-rel'
    job = mavenJob(jobName) {
        authorization {
            permission('hudson.model.Item.Discover', 'anonymous')
            permission('hudson.model.Item.Read', 'anonymous')
            permission('hudson.model.Item.Workspace', 'anonymous')
        }
        lockableResources(project.repository)
        parameters {
            gitParam('Branch') {
                description('The branch to build the release from.')
                type('BRANCH')
                defaultValue('origin/master')
            }
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
            credentialsBinding {
                string('SONARCLOUD_LOGIN', 'SonarCloud')
            }
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
                branch('${Branch}')
                extensions {
                    localBranch()
                }
            }
        }
        jdk(project.jdk)
        mavenInstallation(maven)
        providedSettings(mavenSettings)
        goals('release:prepare release:perform -s "$MAVEN_SETTINGS" -DautoVersionSubmodules -DreleaseVersion=${ReleaseVersion} -Dtag=${ReleaseVersion} -DdevelopmentVersion=${DevelopmentVersion} -DdryRun=${DryRun}"')
        fingerprintingDisabled()
        publishers {
            mailer('dirk.mahler@buschmais.com', true, true)
        }
    }
}
