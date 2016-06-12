String[] modules = [
        'uber-parent',
        'core-framework'
]

modules.each {
    def module = it
    def gitUrl = "git://github.com/buschmais/jqa-${module}"
    def continuousJob = addJob(gitUrl, module, 'continuous', 'clean verify')
    addJob(gitUrl, module, 'integration', 'clean deploy -PintegrationTest', continuousJob)
    queue(continuousJob)
}

def addJob(gitUrl, module, suffix, mavenGoals, upstreamJob = null) {
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
            if (upstreamJob) {
                upstream(upstreamJob.name, 'SUCCESS')
            } else {
                snapshotDependencies(true)
            }
        }
        mavenInstallation('Maven 3.2.5')
        goals(mavenGoals)
        publishers {
            mailer('dirk.mahler@buschmais.com,o.b.fischer@swe-blog.net', true, true)
        }
    }
}
