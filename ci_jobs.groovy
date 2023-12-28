jdk = 'JDK 17'
maven = 'Maven 3.9'
mavenSettings = 'oss-maven-settings'
gitCredentials = 'GitHub'

String[] modulesWithIT = [
        'commandline-tool',
        'core-framework',
        'java-plugin',
        'junit-plugin',
        'json-plugin',
        'maven-plugin',
        'maven3-plugin',
        'neo4j-backend',
        'plugin-common',
        'xml-plugin',
        'yaml2-plugin'
]

String[] modulesWithSimpleBuild = [
        'bill-of-materials',
        'checkstyle-config',
        'asciidoctor-utilities',
        'distribution-specification',
        'manual',
        'own-constraints',
        'plugin-parent',
        'uber-parent'
]

modulesWithIT.each {
    def module = it
    def gitUrl = "https://github.com/jqassistant/jqa-${module}"
    def continuousJob = addJob(gitUrl, module, 'ci', 'clean verify -Djqassistant.skip')
    addJob(gitUrl, module, 'it', 'clean deploy -PIT -Djqassistant.failOnSeverity=MINOR', continuousJob)
}

modulesWithSimpleBuild.each {
    def module = it
    def gitUrl = "https://github.com/jqassistant/jqa-${module}"
    addJob(gitUrl, module, 'ci', 'clean deploy -Djqassistant.failOnSeverity=MINOR')
}

listView('jQAssistant') {
    jobs {
        regex("jqa-.+")
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

def addJob(gitUrl, module, suffix, mavenGoals, upstreamJob = null, disableJob = false, queueJob = false) {
    def jobName = "jqa-${module}-${suffix}-ManagedBuild"
    job = mavenJob(jobName) {
        authorization {
            permission('hudson.model.Item.Discover', 'anonymous')
            permission('hudson.model.Item.Read', 'anonymous')
            permission('hudson.model.Item.Workspace', 'anonymous')
        }
        logRotator {
            numToKeep(10)
        }
        // Use a shared repo for enabling trigger on SNAPSHOT changes
        localRepository(LocalRepositoryLocation.LOCAL_TO_EXECUTOR)
        wrappers {
            timeout {
                absolute(minutes = 90)
            }
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
            if (upstreamJob) {
                upstream(upstreamJob.name, 'SUCCESS')
            } else {
                scm('H/15 * * * *')
                snapshotDependencies(true)
                timerTrigger {
                    // trigger timer build once a week on sundays
                    spec('H H(7-10) * * 7')
                }
            }
        }
        jdk(jdk)
        mavenInstallation(maven)
        providedSettings(mavenSettings)
        mavenOpts('-Dmaven.test.failure.ignore=false');
        goals(mavenGoals)
        fingerprintingDisabled()
        publishers {
            mailer('dirk.mahler@buschmais.com,o.b.fischer@swe-blog.net', true, true)
        }
        if (disableJob) {
            disabled()
        }
    }
    if (queueJob) {
        queue(job)
    }
    return job
}
