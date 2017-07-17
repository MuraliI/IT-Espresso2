def call(String serviceName, String marathonJson, String serviceImplDir) {
    def gitCommitHash
    //def serviceName = 'helloworld'
    //def version = '1.0-SNAPSHOT'
    //def commitId = 'latest'
    def registryHost = 'rhldcmesboot711.na.rccl.com:5000'
    //def marathonJson = 'devops/helloworld.json'
    def marathonUrl = 'http://10.16.7.225:8080'
    def nexusUrl = "http://10.16.4.8:10118/repository/marathon-app-def"    
    def version = '1.0-SNAPSHOT' //to be replaced with version from pom file
    def buildTag = ""

    //switching on/offs
    def optionSonarQubeScan = true
    
    node {                
        def mvnHome = tool name: 'Maven 3.3.9', type: 'maven'
        buildTag = "build-${env.BUILD_ID}"
        stage('Preparation') { 
            // Get some code from a GitHub repository
            //checkout([$class: 'GitSCM', branches: [[name: gitBranch]], doGenerateSubmoduleConfigurations: false, extensions: [], submoduleCfg: [], userRemoteConfigs: [[credentialsId: gitCredentialsId
            //    , url: gitUrl]]])
            checkout scm
            sh 'git rev-parse HEAD > commit'
            gitCommitHash = readFile('commit').trim()                
            echo "Git Commit Hash ${env.GIT_COMMIT}"            
        }
        stage('SonarQube analysis') {
            if (optionSonarQubeScan) {
                withSonarQubeEnv('sonarqube') {
                // requires SonarQube Scanner for Maven 3.2+
                sh "'${mvnHome}/bin/mvn' org.sonarsource.scanner.maven:sonar-maven-plugin:3.2:sonar"
                }
            }
        }              
        stage('Build - Test') {
            sh "export MAVEN_OPTS=\"-Xmx4G -XX:MaxPermSize=0.7G\""
            //sh "'${mvnHome}/bin/mvn' clean install -P Integration,Performance"
			sh "'${mvnHome}/bin/mvn' clean install"
        }
        stage('Package and Publish') {
            //upload marathon file to Nexus
            sh "curl -v -u excalibur:excalibur --upload-file ${marathonJson} ${nexusUrl}/${serviceName}/${buildTag}/marathon.json"            

            dir(serviceImplDir) {
                sh "'${mvnHome}/bin/mvn' docker:build -P build"
            }
                        
            sh "docker tag ${serviceName}:latest ${registryHost}/${serviceName}:${gitCommitHash}"
            sh "docker tag ${serviceName}:latest ${registryHost}/${serviceName}:${buildTag}"
            sh "docker tag ${serviceName}:latest ${registryHost}/${serviceName}:latest"
            
            sh "docker push ${registryHost}/${serviceName}:${buildTag}"
            
            sh "docker rmi ${serviceName}:latest"
            sh "docker rmi ${registryHost}/${serviceName}:${gitCommitHash}"
            sh "docker rmi ${registryHost}/${serviceName}:${buildTag}"
            sh "docker rmi ${registryHost}/${serviceName}:latest"
        }
        stage('Dev Deploy') {
            echo "Deploy using Marathon..."
            marathon appid: '', credentialsId: '', docker: "${registryHost}/${serviceName}:${buildTag}", filename: marathonJson, url: marathonUrl
        }
        currentBuild.description = "Image info: ${registryHost}/${serviceName}:${buildTag} <a href='${JENKINS_URL}/job/${JOB_NAME}-promote/parambuild?imageTag=${registryHost}/${serviceName}:${buildTag}&environment=test&locations=all'>Promote this job</a>"
    }

}
