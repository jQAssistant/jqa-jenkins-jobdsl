/*
 * Skript to generate the release jobs in Jenkins for jQAssistant.
 */

@groovy.transform.Canonical
class Project {
    String id = "";
    String name = "";
    String repoName = "";
    boolean needsITJob = true;

}

def Project[] projects = [
  ["jqa-ueber-parent", "jQA Ueber Parent", "https://github.com/buschmais/jqa-uber-parent.git", false],
  ["jqa-own-constraints", "jQA Own Constraints", "https://github.com/buschmais/jqa-own-constraints", false],  
  ["jqa-core-framework", "jQA Core Framework", "https://github.com/buschmais/jqa-core-framework.git", true],
  ["jqa-plugin-parent", "jQA Plugin Parent", "https://github.com/buschmais/jqa-plugin-parent.git", false],
  ["jqa-asciidoctor-utils", "jQA Asciidoctor Utilities", "https://github.com/buschmais/jqa-asciidoctor-utilities.git", false],
  ["jqa-plugin-common", "jQA Plugin Common", "https://github.com/buschmais/jqa-plugin-common.git", true],
  ["jqa-java-plugin", "jQA Java Plugin", "https://github.com/buschmais/jqa-java-plugin.git", true],
  ["jqa-java8-plugin", "jQA Java 8 Plugin", "https://github.com/buschmais/jqa-java8-plugin.git", true],
  ["jqa-xml-plugin", "jQA XML Plugin", "https://github.com/buschmais/jqa-xml-plugin.git", true],
  ["jqa-testng-plugin", "jQA TestNG Plugin", "https://github.com/buschmais/jqa-testng-plugin.git", true],
  ["jqa-yaml-plugin", "jQA YAML Plugin", "https://github.com/buschmais/jqa-yaml-plugin.git", true],
  ["jqa-jaxrs-plugin", "jQA JAX RS Plugin", "https://github.com/buschmais/jqa-jaxrs-plugin.git", true],
  ["jqa-jpa2-plugin", "jQA JPA 2 Plugin", "https://github.com/buschmais/jqa-jpa2-plugin.git", true],
  ["jqa-cdi-plugin", "jQA CDI Plugin", "https://github.com/buschmais/jqa-cdi-plugin.git", true],
  ["jqa-ejb3-plugin", "jQA EJB 3 Plugin", "https://github.com/buschmais/jqa-ejb3-plugin.git", true],
  ["jqa-maven3-plugin", "jQA Maven 3 Plugin", "https://github.com/buschmais/jqa-maven3-plugin.git", true],
  ["jqa-junit-plugin", "jQA JUnit Plugin", "https://github.com/buschmais/jqa-junit-plugin.git", true],
  ["jqa-m2repo-plugin", "jQA M2 Repository Plugin", "https://github.com/buschmais/jqa-m2repo-plugin.git", true],
  ["jqa-tycho-plugin", "jQA Tycho Plugin", "https://github.com/buschmais/jqa-tycho-plugin.git", true],
  ["jqa-osgi-plugin", "jQA OSGi Plugin", "https://github.com/buschmais/jqa-osgi-plugin.git", true],
  ["jqa-rdbms-plugin", "jQA RDBMS Plugin", "https://github.com/buschmais/jqa-rdbms-plugin.git", true],
  ["jqa-graphml-plugin", "jQA GraphML Plugin", "https://github.com/buschmais/jqa-graphml-plugin.git", true],
  ["jqa-javaee6-plugin", "jQA Java EE 6 Plugin", "https://github.com/buschmais/jqa-javaee6-plugin.git", true],
  ["jqa-distribution-specification", "jQA Distribution Specification Plugin", "https://github.com/buschmais/jqa-distribution-specification.git", true],
  ["jqa-maven-plugin", "jQA Plugin for Maven", "https://github.com/buschmais/jqa-maven-plugin.git", true],
  ["jqa-neo4j-backend", "jQA Neo4j Backend", "https://github.com/buschmais/jqa-neo4j-backend.git", true]
]

projects.each {
    def project = it;

    createChain(project, true);
    createChain(project, false);
}


def createChain(Project project, boolean manual) {
  
    def jobName = "rel-${project.id}" + (manual ? "-manual" : "-automatic") + "-ManagedBuild";
    def displayName = (manual ? "Manual " : "Automatic ") +"Release ${project.name}";
    def gitHubProject = "buschmais/${project.repoName}";

  
	mavenJob(jobName) {
      //displayName(displayName);
      
      mavenInstallation('Maven 3.2.5')

      
      parameters {
        if (manual) {	    
  	      stringParam('relVersion');        
          stringParam('nextDevVersion');   
          stringParam('branch', 'master');
        }
        
        booleanParam('dryRun', true);
      }
      
        
      logRotator {
        numToKeep(15)
        artifactNumToKeep(1)
      }
      
      wrappers {
        sshAgent('GitHub');
        
        configFileProvider {
          managedFiles {
            configFile {
              fileId('5e7b65a4-252e-4878-9aed-b8fc196e2545');
              variable('MAVEN_SETTINGS_FILE');
              }
          }
        }
        
        credentialsBinding {
          zipFile('GPG_HOME_DIR', 'gpg.zip (GPG)')
        }
      }
        
      scm {
        git {
          remote {
            url(project.repoName); 
            // Credentials stehen für GitHub
            credentials('eca671a9-32f0-42a5-8608-c0749b4b2390');
          }
          
          branch('master')

          extensions {
            localBranch('${branch}');
            wipeOutWorkspace();
      	      }
        }
      }
        
      preBuildSteps {                
        maven {          
          mavenInstallation('Maven 3.2.5')
          goals('release:clean')
          goals('install') 
          properties(skipTests: true)          
        }
        
        maven {
          mavenInstallation('Maven 3.2.5')
          goals('clean')
          goals('install') 
          
          if (project.needsITJob) {
             goals('-P IT')
          }
   
        }
      }
      
      mavenInstallation('Maven 3.2.5')
          
      if (!manual) {
            goals('--batch-mode');
			goals('--settings "$MAVEN_SETTINGS_FILE"');
            goals('clean');
            goals('release:prepare release:perform');

            goals('-DautoVersionSubmodules');
            goals('-Darguments="-Dgpg.homedir=\'$GPG_HOME_DIR\'"');
            
      } else {
            goals('--batch-mode');
            goals('--settings "$MAVEN_SETTINGS_FILE"');            
            goals('clean');
			goals('release:prepare release:perform');
        
            goals('-DautoVersionSubmodules');
            goals('-Darguments="-Dgpg.homedir=\'$GPG_HOME_DIR\'"');
        
            goals('-DreleaseVersion=${relVersion} -Dtag=${relVersion}');
            goals('-DdevelopmentVersion=${nextDevVersion} -DdryRun=${dryRun}');
            
      }

      }
  
}

listView('jQA Release Jobs') {
    jobs {
        regex("rel-.+")
    }
    columns {
        status()
        // weather() Explizit nicht gewollt. Beim Deployment kommt es manchmal
        //           zu Fehlern, die dann händisch korrigiert werden.
        //           Daher ist uns nur wichtig, ob das letzte Deployment
        //           erfolgreich war oder nicht.
        name()
        lastSuccess()
        lastFailure()
        lastDuration()
        buildButton()
    }
}
