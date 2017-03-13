def String jobName = "tck-jqa-compliance-test-ManagedBuild"
def String mavenGoals = "clean install"
def String gitUrl = "https://github.com/buschmais/jqa-compliance-tests.git"

mavenJob(jobName) {
    logRotator {
        numToKeep(20)
    }

    providedSettings('Maven Settings')

    scm {
        git(gitUrl) {
            branches('*/master')
        }
    }

    triggers {
        scm('H/15 * * * *')
        snapshotDependencies(false)
        timerTrigger {
            spec('H H(18-21) * * *')
        }
    }

    mavenInstallation('Maven 3.2.5')
    goals(mavenGoals)

    publishers {
        mailer('dirk.mahler@buschmais.com,o.b.fischer@swe-blog.net', true, true)
    }
}

listView('jQA TCK Jobs') {
    jobs {
        regex("tck-.+")
    }
    columns {
        status()
        // weather() Explizit nicht gewollt. Beim Deployment kommt es manchmal
        //           zu Fehlern, die dann händisch korrigiert werden.
        //           Daher ist uns nur wichtig, ob das letzte Deployment
        //           erfolgreich war oder nicht.
        name()
        lastSuccess()
        lastFailure()
        lastDuration()
        buildButton()
    }
}

