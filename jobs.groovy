String[] modules = [
        'uber-parent',
        'core-framework'
]

modules.each {
    def module = it
    def gitUrl = "git://github.com/buschmais/jqa-${module}"
    def jobName = addJob(gitUrl, module, 'continuous', 'clean verify')
    addJob(gitUrl, module, 'integration', 'clean install -PintegrationTest')
    queue(jobName)
}

def addJob(gitUrl, module, suffix, mavenGoals) {
    def jobName = "managed-jqa-${module}-${suffix}"
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
            scm('H/15 * * * *')
            snapshotDependencies(true)
        }
        mavenInstallation('Maven 3.2.5')
        goals(mavenGoals)
        publishers {
            mailer('dirk.mahler@buschmais.com,o.b.fischer@swe-blog.net', true, true)
        }
    }
    return jobName
}
