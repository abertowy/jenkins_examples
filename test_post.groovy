library('purpdm-lib@main')

pipeline {
    agent any
    
    stages {
        stage('Initialize') {
            steps {
                script {
                    echo "BUILD"
                }
            }
        }
    }
    post{
        stageTest(branch: params.GIT_BRANCH)
        // success {
        //     script {
        //         echo "SUCCESS"
        //     }
		// }        
        // failure {
        //     script {
        //         echo "TFAILURE"
        //     }
        // }
        // always{
        //     script {
        //         echo "ALWAYS"
        //     }
        // }
    }
}