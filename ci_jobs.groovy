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
        'archivers-plugin'
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
    addJob(gitUrl, module, 'it', '-U clean deploy -P IT', continuousJob)
    queue(continuousJob)
}

modulesWithSimpleBuild.each {
    def module = it
    def gitUrl = "git://github.com/buschmais/jqa-${module}"
    def continuousJob = addJob(gitUrl, module, 'ci', '-U clean deploy')
    queue(continuousJob)
}

def String[] allModules = modulesWithIT + modulesWithSimpleBuild;


allModules.each {
    def module = it
    def gitUrl = "git://github.com/buschmais/jqa-${module}"
    def continuousJob = addJob(gitUrl, module, 'val',
                               '-U -DskipTests -DskipITs -Djqassistant.severity=info clean install')
    queue(continuousJob)
}



listView('CI Jobs') {
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
            numToKeep(20)
        }
        localRepository(LocalRepositoryLocation.LOCAL_TO_WORKSPACE)
        providedSettings('Maven Settings')
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
        mavenOpts('-Dmaven.test.failure.ignore=false');
        publishers {
            mailer('dirk.mahler@buschmais.com,o.b.fischer@swe-blog.net', true, true)
        }
    }
}
