String[] modules = ['jqa-uber-parent']
modules.each {
    def jobName = '${it}-continuous'
    def gitUrl = 'git://github.com/buschmais/${it}'
    mavenJob(jobName) {
        logRotator {
            numToKeep(5)
        }
        customConfigFile('Maven Settings')
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
