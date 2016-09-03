def String[] modulesWithIT = [
        'cdi-plugin',
        'commandline-tool',
        'core-framework',
        'ejb3-plugin',
        'graphml-plugin',
        'java8-plugin',
        'java-plugin',
        'javaee6-plugin',
        'junit-plugin',
        'jpa2-plugin',
        'jaxrs-plugin',
        'json-plugin',
        'm2repo-plugin',
        'maven-plugin',
        'maven3-plugin',
        'neo4j-backend',
        'osgi-plugin',
        'plugin-common',
        'rdbms-plugin',
        'spring-plugin',
        'testng-plugin',
        'tycho-plugin',
        'xml-plugin',
        'yaml-plugin',
]

def String[] modulesWithSimpleBuild = [
        'asciidoctor-utilities',
        'distribution-specification',
        'manual',
        'own-constraints',
        'plugin-parent',
        'uber-parent'
]

modulesWithIT.each {
    def module = it
    def gitUrl = "git://github.com/buschmais/jqa-${module}"
    def continuousJob = addJob(gitUrl, module, 'ci', 'clean verify')
    addJob(gitUrl, module, 'it', 'clean deploy -P IT', continuousJob)
    queue(continuousJob)
}

modulesWithSimpleBuild.each {
    def module = it
    def gitUrl = "git://github.com/buschmais/jqa-${module}"
    def continuousJob = addJob(gitUrl, module, 'ci', 'clean deploy')
    queue(continuousJob)
}

listView('Managed Jobs') {
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

def addJob(gitUrl, module, suffix, mavenGoals, upstreamJob = null) {
    def jobName = "jqa-${module}-${suffix}-ManagedBuild"
    mavenJob(jobName) {
        logRotator {
            numToKeep(5)
        }
        providedSettings('jQA')
        scm {
            git(gitUrl) {
                branches('*/master')
            }
        }
        triggers {
            if (upstreamJob) {
                upstream(upstreamJob.name, 'SUCCESS')
            } else {
                scm('H/15 * * * *')
                snapshotDependencies(true)
                timerTrigger {
                    spec('H H(0-7) * * *')
                }
            }
        }
        mavenInstallation('Maven 3.2.5')
        goals(mavenGoals)
        publishers {
            mailer('dirk.mahler@buschmais.com,o.b.fischer@swe-blog.net', true, true)
        }
    }
}
