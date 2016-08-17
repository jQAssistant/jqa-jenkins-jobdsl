def String[] modulesWithIT = [
        'core-framework',
        'plugin-common',
        'java-plugin',
        'xml-plugin',
        'yaml-plugin',
        'junit-plugin',
        'java8-plugin',
        'jpa2-plugin',
        'jaxrs-plugin',
        'cdi-plugin',
        'ejb3-plugin',
        'javaee6-plugin',
        'testng-plugin',
        'osgi-plugin',
        'maven3-plugin',
        'tycho-plugin',
        'rdbms-plugin',
        'm2repo-plugin',
        'graphml-plugin',
        'neo4j-backend',
        'maven-plugin',
        'commandline-tool'
]

def String[] modulesWithSimpleBuild = [
        'asciidoctor-utilities',
        'uber-parent',
        'own-constraints',
        'plugin-parent',
        'distribution-specification'
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
