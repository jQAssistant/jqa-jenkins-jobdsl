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
 * - "GPG: ZIP-file containing GPG pubring and secring for signing the Maven artifacts
 *
 * Tool installations:
 * - "JDK 1.8"
 * - "Maven 3.6.0"
 */
jdk = 'JDK 1.8'
maven = 'Maven 3.6.0'
mavenSettings = 'oss-maven-settings'
gitCredentials = 'GitHub'
gpgCredentials = 'GPG'

// jQAssistant Contribution Jobs
class ContribJob {
    String module
    String name
}
ContribJob[] contribJobs = [
        new ContribJob(module: 'jqassistant-contrib-common', name: 'jqa-contrib-common'),
        new ContribJob(module: 'jqassistant-asciidoc-report-plugin', name: 'jqa-asciidoc-report-plugin'),
        new ContribJob(module: 'jqassistant-plantuml-rule-plugin', name: 'jqa-plantuml-rule-plugin'),
        new ContribJob(module: 'jqassistant-test-impact-analysis-plugin', name: 'jqa-test-impact-analysis-plugin'),
        new ContribJob(module: 'sonar-jqassistant-plugin', name: 'sonar-jqassistant-plugin')
]
contribJobs.each {
    ci('jqassistant-contrib', it.module, it.name)
    release('jqassistant-contrib', it.module, it.name)
}

// XO Jobs
ci('buschmais', 'extended-objects', 'xo')
release('buschmais', 'extended-objects', 'xo')

// Defines a CI job
def ci(organization, module, jobName) {
    def gitUrl = "https://github.com/${organization}/${module}.git"
    job = mavenJob(jobName + '-ci') {
        logRotator {
            numToKeep(10)
        }
        scm {
            git {
                remote {
                    url(gitUrl)
                    credentials(gitCredentials)
                }
                branches('*/master')
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
        goals('clean deploy')
        mavenOpts('-Dmaven.test.failure.ignore=false')
        publishers {
            mailer('dirk.mahler@buschmais.com', true, true)
        }
    }
}

// Defines a Release job
def release(organization, module, jobName) {
    def gitUrl = "https://github.com/${organization}/${module}.git"
    job = mavenJob(jobName + '-rel') {
        parameters {
            booleanParam('DryRun', false, '')
            stringParam('ReleaseVersion', '', 'The version to release and to be used as tag.')
            stringParam('DevelopmentVersion', '', 'The next development version.')
        }
        wrappers {
            configFiles {
                file(mavenSettings) {
                    variable('MAVEN_SETTINGS')
                }
            }
            sshAgent(gitCredentials)
            credentialsBinding {
                zipFile('GPG_HOME_DIR', gpgCredentials)
            }
            preBuildCleanup()
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
                branch('*/master')
                extensions {
                    localBranch 'master'
                }
            }
        }
        jdk(jdk)
        mavenInstallation(maven)
        providedSettings(mavenSettings)
//      goals('release:prepare release:perform -s "$MAVEN_SETTINGS" -DautoVersionSubmodules -DreleaseVersion=${ReleaseVersion} -Dtag=${ReleaseVersion} -DdevelopmentVersion=${DevelopmentVersion} -DdryRun=${DryRun} -Darguments="-Dgpg.homedir=\'$GPG_HOME_DIR\'"')
        goals('release:prepare release:perform -s "$MAVEN_SETTINGS" -DautoVersionSubmodules -DreleaseVersion=${ReleaseVersion} -Dtag=${ReleaseVersion} -DdevelopmentVersion=${DevelopmentVersion} -DdryRun=${DryRun}"')
        mavenOpts('-Dmaven.test.failure.ignore=false')
        publishers {
            mailer('dirk.mahler@buschmais.com', true, true)
        }
    }
}
