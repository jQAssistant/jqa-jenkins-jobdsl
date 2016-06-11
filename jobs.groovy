String[] modules = [
        'uber-parent',
        'core-framework'
]
modules.each {
    def module = it
    def jobName = "jqa-${module}-continuous"
    def gitUrl = "git://github.com/buschmais/jqa-${module}"
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
        goals('clean install')
        publishers {
            mailer('dirk.mahler@buschmais.com,o.b.fischer@swe-blog.net', true, true)
        }
    }
}
