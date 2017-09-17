
def String gitUrl = "https://github.com/buschmais/jqassistant-101.git";
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
    goals("clean install deploy")
    // You must replace this dummy path with the real one
    // after generation of the job.
    // I didn't find a way to set a default property for this
    // in Jenkins.
    // Oliver B. Fischer, 2017-09-17
    mavenOpts('-DwebsitePath=/IDontWantToHaveTheRealPathInAPublicRepository');

    publishers {
        mailer('dirk.mahler@buschmais.com,o.b.fischer@swe-blog.net', true, true)
    }
}
