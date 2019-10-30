jdk = 'JDK 1.8'
maven = 'Maven 3.6'
mavenSettings = 'oss-maven-settings'

String gitUrl = "https://github.com/jqassistant/jqassistant-101.git";
String jobName = "101-webpage-ManagedBuild";

mavenJob(jobName) {
    logRotator {
        numToKeep(20)
    }

    providedSettings(mavenSettings)

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

    jdk(jdk)
    mavenInstallation(maven)

    // Environment variable 101_HOME must be defined in Jenkins
    goals('clean install deploy -DwebsitePath=$101_HOME')

    publishers {
        mailer('dirk.mahler@buschmais.com,o.b.fischer@swe-blog.net', true, true)
    }
}
