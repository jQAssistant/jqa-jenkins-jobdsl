/**
 * Jenkins Job DSL for jQAssistant Tooling/Plugin projects
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
maven = 'Maven 3.9'
mavenSettings = 'oss-maven-settings'
mavenOptsJdk17 = '--add-opens java.base/java.lang=ALL-UNNAMED --add-opens java.base/java.nio=ALL-UNNAMED'
gitCredentials = 'GitHub'

// XO
defineJobs('buschmais', new Project(repository: 'extended-objects'))

// jQA Tooling
def toolingJobs = [
        new Project(repository: 'jqassistant-tooling-common'),
        new Project(repository: 'sonar-jqassistant-plugin'),
        new Project(repository: 'jqassistant-asciidoctorj-extensions')
]
toolingJobs.each {
    defineJobs('jqassistant-tooling', it)
}
defineListView(toolingJobs, 'jQAssistant Tooling');

// jQA Plugin
def pluginJobs = [
        new Project(repository: 'jqassistant-apoc-plugin'),
        new Project(repository: 'jqassistant-asciidoc-report-plugin'),        
        new Project(repository: 'jqassistant-cyclonedx-plugin'),
        new Project(repository: 'jqassistant-github-plugin'),
        new Project(repository: 'jqassistant-graphql-plugin'),
        new Project(repository: 'jqassistant-jee-plugin'),
        new Project(repository: 'jqassistant-mapstruct-plugin'),
        new Project(repository: 'jqassistant-m2repo-plugin'),
        new Project(repository: 'jqassistant-nexusiq-plugin'),
        new Project(repository: 'jqassistant-npm-plugin'),
        new Project(repository: 'jqassistant-plantuml-report-plugin'),
        new Project(repository: 'jqassistant-rdbms-plugin'),        
        new Project(repository: 'jqassistant-xmi-plugin')
/*
        Migrated to GitHub actions:
        new Project(repository: 'jqassistant-c4-plugin'),
        new Project(repository: 'jqassistant-context-mapper-plugin'),
        new Project(repository: 'jqassistant-docker-plugin'),
        new Project(repository: 'jqassistant-graphml-plugin'),
        new Project(repository: 'jqassistant-jmolecules-plugin'),
        new Project(repository: 'jqassistant-openapi-plugin', sonarTargetBranch: 'main'),
        new Project(repository: 'jqassistant-plugin-common'),
        new Project(repository: 'jqassistant-spring-plugin'),        
        new Project(repository: 'jqassistant-typescript-plugin'),
*/
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
    String jdk = 'JDK 17'
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
        mavenOpts(mavenOptsJdk17);
        def mavenGoal = (GIT_BRANCH == 'origin/main' || GIT_BRANCH == 'origin/master') ? 'deploy' : 'verify'
        def options = '-PIT'
        if (project.runSonar) {
            options = options + ',sonar -Dsonar.branch.name=$GIT_LOCAL_BRANCH'
        }
        goals('clean ' + mavenGoal + ' ' + options)
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
        mavenOpts(mavenOptsJdk17);
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
