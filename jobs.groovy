String[] modules = ['jqa-uber-parent']
modules.each {
    def module = it
    def jobName = "${module}-continuous"
    def gitUrl = "git://github.com/buschmais/${module}"
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
        }
        mavenInstallation('Maven 3.2.5')
        goals('clean install')
    }
}
