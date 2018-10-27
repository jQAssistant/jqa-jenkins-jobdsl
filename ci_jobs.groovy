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
    def gitUrl = "git://github.com/buschmais/jqa-${module}"
    def continuousJob = addJob(gitUrl, module, 'ci', 'clean verify')
    addJob(gitUrl, module, 'it', 'clean deploy -PIT -Djqassistant.failOnSeverity=INFO', continuousJob)
//    queue(continuousJob)
}

modulesWithSimpleBuild.each {
    def module = it
    def gitUrl = "git://github.com/buschmais/jqa-${module}"
    def continuousJob = addJob(gitUrl, module, 'ci', 'clean deploy')
//    queue(continuousJob)
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

def addJob(gitUrl, module, suffix, mavenGoals, upstreamJob = null, disableJob = false) {
    def jobName = "jqa-${module}-${suffix}-ManagedBuild"
    mavenJob(jobName) {
        logRotator {
            numToKeep(20)
        }
	// Leider gerade kein Platz auf den Server dafür,
	// das jede Chain ihr eigenes Repository nutzen kann
	// Wichtige wäre das jedoch, um zu verhindern daß
	// Builds failen, weil andere gerade neue Versionen
	// von Artefakten in das lokale, gemeinsame Repositoy,
	// schreiben,
	// Oliver B. Fischer, 2017-04-12
        // localRepository(LocalRepositoryLocation.LOCAL_TO_EXECUTOR)
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
                    spec('H H(0-7) * * 7') // trigger timer build only once a week on sundays
                }
            }
        }
        mavenInstallation('Maven 3.2.5')
        goals(mavenGoals)
        mavenOpts('-Dmaven.test.failure.ignore=false');
        publishers {
            mailer('dirk.mahler@buschmais.com,o.b.fischer@swe-blog.net', true, true)
        }
        if (disableJob) {
            disabled()
        }
    }
}
