
def String gitUrl = "git@github.com:buschmais/jqassistant-101.git";
def String jobName = "101-webpage-ManagedBuild";

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

        timerTrigger {
            spec('H H(0-7) * * *')
        }
    }

    mavenInstallation('Maven 3.2.5')
    goals("clean install")

    publishers {
        mailer('dirk.mahler@buschmais.com,o.b.fischer@swe-blog.net', true, true)
    }
}
