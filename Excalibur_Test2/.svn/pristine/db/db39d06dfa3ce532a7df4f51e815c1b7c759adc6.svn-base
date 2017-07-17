def call() {

    def devRegistry = "rhldcmesboot711.na.rccl.com:5000"
    def nexusUrl = "http://10.16.4.8:10118/repository/marathon-app-def"
    def localMarathon;

    def envMapping = [    
        'test': [
            'cloud': [marathonUrl: 'http://10.16.7.225:8080', registry: 'rhldcmesboot711.na.rccl.com:5000', jenkins: "http://10.135.11.108/service/jenkins"],
            'lab': [marathonUrl: 'http://10.135.10.153:8080', registry: 'tst-registry.nowlab.tstsh.tstrccl.com:10104', jenkins: "http://10.135.11.108/service/jenkins"]
        ],
        'stage': [
            'cloud': [marathonUrl: 'http://10.16.7.225:8080', registry: 'rhldcmesboot711.na.rccl.com:5000', jenkins: "http://10.135.11.108/service/jenkins"],
            'lab': [marathonUrl: 'http://10.135.10.153:8080', registry: 'tst-registry.nowlab.tstsh.tstrccl.com:10104', jenkins: "http://10.135.11.108/service/jenkins"]
        ]
    ]

    def userInput
    stage('Confirm Push') {
        /*def userInput = input(
        id: 'userInput', message: 'Confirm information', parameters: [
        [$class: 'ChoiceParameterDefinition', choices: repos.join("\n"), description: 'Repository', name: 'repo'],
        [$class: 'ChoiceParameterDefinition', choices: environments.join("\n"), description: 'Environment', name: 'env']
        ])*/
        userInput = input(id: 'userInput', message: 'Confirm Information', parameters: [
            string(defaultValue: imageUrl, description: 'Image Tag (name:version)', name: 'imageTag')
            , choice(choices: "test\nstage\nprod", description: 'test | stage | prod', name: 'environment', defaultValue: environment)
            , string(defaultValue: locations, description: 'all | cloud | {comma-separated predefined locations}', name: 'locations')]
            , submitterParameter: 'approver')
        
    }

    def confirmedImageTag = userInput['imageTag']
    def confirmedEnvironment = userInput['environment']
    def confirmedLocations = userInput['locations']
    def locationArrays

    echo "Starting pipeline for\nImage Tag: ${confirmedImageTag}\nEnvironment: ${confirmedEnvironment}\nLocations: ${confirmedLocations}"

    if (confirmedLocations == "all") {
        locationArrays = envMapping[confirmedEnvironment].keySet() as String[]
        print "Confirmed locations: ${locationArrays}"   
    } else {
        echo "**** only support ALL locations at this time ****"
        sh "exit 1"
        return
    }



    node {
        stage('Pull App Def') {
            def marathonPath = confirmedImageTag.replace(':', '/') + '/marathon.json' //change name:build-1 to name/build-1
            localMarathon = confirmedImageTag.replace(':', '_') + '_marathon.json'
            sh "curl -f -u excalibur:excalibur -o ${localMarathon} ${nexusUrl}/${marathonPath}"
        }
        stage('Push Image') {        
            
            sh "docker pull ${devRegistry}/${confirmedImageTag}"

            echo "pushing to ${locationArrays}"
            for(int i=0; i < locationArrays.size(); i++ ) {
                def loc = locationArrays[i]
                def locRegistry = envMapping[confirmedEnvironment][loc]['registry']
                def jenkins2ndUrl = envMapping[confirmedEnvironment][loc]['jenkins']
                echo "Pushing to ${loc}: ${locRegistry}"
                sh "docker tag ${devRegistry}/${confirmedImageTag} ${locRegistry}/${confirmedImageTag}"
                sh "docker push ${locRegistry}/${confirmedImageTag}" 
            }
        }
    }

    stage('Confirm Deploy') {
        input 'Proceed to Deploy?'
    }


    node {
        stage('Deploy') {
            echo "Deploying to ${locationArrays}"
            //read app ID
            def marathonAppDef = readJSON file: "./${localMarathon}"
            def marathonAppId = marathonAppDef.id;
            def slash = '/'
            if (marathonAppId.getAt(0) == '/') {
                slash = '' //if app Id begins with slash, do not add twice
            }
            for(int i=0; i < locationArrays.size(); i++ ) {
                def loc = locationArrays[i]
                def locRegistry = envMapping[confirmedEnvironment][loc]['registry']
                def marathonUrl = envMapping[confirmedEnvironment][loc]['marathonUrl']
                echo "Deploy to ${loc}: ${locRegistry}, Marathon URL ${marathonUrl}"
                 marathon appid: "${confirmedEnvironment}${slash}${marathonAppId}", credentialsId: '', docker: "${locRegistry}/${confirmedImageTag}", filename: localMarathon, url: marathonUrl           
            }
            echo "Deploy complete!"
        }

    }
}