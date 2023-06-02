/**
 * Jenkins Job DSL for jQAssistant Contrib/Plugin projects
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
gitCredentials = 'GitHub'

// XO
defineJobs('buschmais', new Project(repository: 'extended-objects'))

// jQA Contrib
def contribJobs = [
        new Project(repository: 'jqassistant-contrib-common'),
        new Project(repository: 'sonar-jqassistant-plugin'),
        new Project(repository: 'jqassistant-asciidoctorj-extensions')
]
contribJobs.each {
    defineJobs('jqassistant-contrib', it)
}
defineListView(contribJobs, 'jQAssistant Contrib');

// jQA Plugin
def pluginJobs = [
        new Project(repository: 'jqassistant-apoc-plugin'),
        new Project(repository: 'jqassistant-asciidoc-report-plugin'),
        new Project(repository: 'jqassistant-c4-plugin'),        
        new Project(repository: 'jqassistant-context-mapper-plugin'),
        new Project(repository: 'jqassistant-docker-plugin'),
        new Project(repository: 'jqassistant-github-plugin'),
        new Project(repository: 'jqassistant-graphml-plugin'),
        new Project(repository: 'jqassistant-graphql-plugin'),
        new Project(repository: 'jqassistant-jee-plugin'),
        new Project(repository: 'jqassistant-jmolecules-plugin'),
        new Project(repository: 'jqassistant-mapstruct-plugin'),
        new Project(repository: 'jqassistant-m2repo-plugin'),
        new Project(repository: 'jqassistant-npm-plugin'),
        new Project(repository: 'jqassistant-openapi-plugin', sonarTargetBranch: 'main'),
        new Project(repository: 'jqassistant-plantuml-report-plugin'),
        new Project(repository: 'jqassistant-plugin-common'),
        new Project(repository: 'jqassistant-rdbms-plugin'),
        new Project(repository: 'jqassistant-spring-plugin'),
        new Project(repository: 'jqassistant-xmi-plugin')
]
pluginJobs.each {
    defineJobs('jqassistant-plugin', it)
}
defineListView(pluginJobs, 'jQAssistant Plugin');

// definitions and functions

// Jobs
class Project {
    // The name of the GitHub repository
    String repository
    // JDK
    String jdk = 'JDK 11'
    // if true a sonar analysis will be triggered for each ci/release build
    boolean runSonar = true
    // the target branch for sonar target branch analysis
    String sonarTargetBranch
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
                extensions {
                    localBranch()
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
        def mavenGoal = (GIT_BRANCH == 'origin/main' || GIT_BRANCH == 'origin/master') ? 'deploy' : 'verify'
        if (project.runSonar) {
                goals('clean ' + mavenGoal + ' -PIT,sonar -Dsonar.branch.name=$GIT_BRANCH')
        } else {
            goals("clean ${mavenGoal} -PIT")
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
        localRepository(LocalRepositoryLocation.LOCAL_TO_EXECUTOR)
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

def defineListView(listViewJobs, viewName) {
    listView(viewName) {
        jobs {
            names(listViewJobs.collect { it.repository + "-ci" } as String[])
        }
        columns {
            status()
            weather()
            name()
            lastSuccess()
            lastFailure()
            lastDuration()
            buildButton()
        }
    }
}
