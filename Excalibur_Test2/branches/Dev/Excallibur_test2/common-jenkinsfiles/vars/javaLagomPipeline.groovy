def call(String serviceName, String marathonJson) {
    def gitCommitHash
    //def serviceName = 'helloworld'
    //def version = '1.0-SNAPSHOT'
    //def commitId = 'latest'
    def registryHost = 'rhldcmesboot711.na.rccl.com:5000'
    //def marathonJson = 'devops/helloworld.json'
    def marathonUrl = 'http://10.16.7.225:8080'
    def nexusUrl = "http://10.16.4.8:10118/repository/marathon-app-def"
    def version = '1.0-SNAPSHOT' //to be replaced with version from pom file
    def cxHost = "https://iadcmxus03.cloudasc.net" //checkmarx host
    def buildTag = ""
    def optionSonarQubeScan = true
    def optionCheckmarxScan = true


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
        stage('Checkmax Scan') {
            if (optionCheckmarxScan) {
                     withCredentials([[$class: 'UsernamePasswordMultiBinding', credentialsId: 'checkmarx_userpass',
                            usernameVariable: 'cxUsername', passwordVariable: 'cxPassword']]) {
                        //available as an env variable, but will be masked if you try to print it out any which way
                        
                        withEnv(["CHECKMARX_USERNAME=${cxUsername}"
                                , "CHECKMARX_PASSWORD=${cxPassword}"
                                , "CHECKMARX_HOST=${cxHost}"]) {
                            def risk = sh script: "'/opt/cx/CxJenkinsPLPlugin/CxJenkinsPipeline.sh' WORKSPACE=$PWD JOB_NAME=$JOB_NAME BUILD_NUM=$BUILD_NUMBER",returnStatus:true;
                        }                        
                    }
            }
        }        
        stage('Build - Test') {
            sh "'${mvnHome}/bin/mvn' clean package"
        }
        stage('Generate Code Coverage') {
            sh "'${mvnHome}/bin/mvn' cobertura:cobertura -Dcobertura.report.formats=xml,html"
        }
        stage('SonarQube analysis') {
            if (optionSonarQubeScan) {
                withSonarQubeEnv('sonarqube') {
                // requires SonarQube Scanner for Maven 3.2+
                sh "'${mvnHome}/bin/mvn' org.sonarsource.scanner.maven:sonar-maven-plugin:3.2:sonar"
                }
            }
        }        
        stage('Package and Publish') {
            //upload marathon file to Nexus
            sh "curl -v -u excalibur:excalibur --upload-file ${marathonJson} ${nexusUrl}/${serviceName}/${buildTag}/marathon.json"
            
            sh "unzip  ${serviceName}-impl/target/${serviceName}-impl-${version}-standalone-bundle.zip -d ${serviceName}-impl/target/"
            sh "docker build -t ${serviceName}:${gitCommitHash} ./${serviceName}-impl"
            sh "docker tag ${serviceName}:${gitCommitHash} ${registryHost}/${serviceName}:${gitCommitHash}"
            sh "docker tag ${serviceName}:${gitCommitHash} ${registryHost}/${serviceName}:${buildTag}"
            sh "docker tag ${serviceName}:${gitCommitHash} ${registryHost}/${serviceName}:latest"        
            sh "docker push ${registryHost}/${serviceName}:${buildTag}"        

            sh "docker rmi ${serviceName}:${gitCommitHash}"
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
