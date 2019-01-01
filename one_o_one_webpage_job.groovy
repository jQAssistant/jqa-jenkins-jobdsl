
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

    jdk('JDK 8')
    mavenInstallation('Maven 3.5.0')

    // Environment variable 101_HOME must be defined in Jenkins
    goals("clean install deploy -DwebsitePath=${101_HOME}")

    publishers {
        mailer('dirk.mahler@buschmais.com,o.b.fischer@swe-blog.net', true, true)
    }
}
