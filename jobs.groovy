String[] modules = ["jqa-uber-parent"]
modules.each {
  def jobName = "${it}-continuous"
  mavenJob(jobName) {
    scm {
      git('git://github.com/buschmais/jqa-uber-parent.git') {
        branches("master")
      }
    }
    triggers {
      scm('H/15 * * * *')
    }
    mavenInstallation('Maven 3.2.5')
    goals('clean install')
  }
}
