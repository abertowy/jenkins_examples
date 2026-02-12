library('purpdm-lib@main')

pipeline {
    agent any
    
    stages {
        stageBuild(pipelineParams)
        // stage('Initialize') {
        //     steps {
        //         script {
        //             echo "BUILD"
        //         }
        //     }
        // }
    }
    stageTest(pipelineParams)
    // post{
    //     success {
    //         script {
    //             echo "SUCCESS"
    //         }
	// 	}        
    //     failure {
    //         script {
    //             echo "TFAILURE"
    //         }
    //     }
    //     always{
    //         script {
    //             echo "ALWAYS"
    //         }
    //     }
    // }
}