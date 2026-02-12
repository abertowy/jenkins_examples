library('purpdm-lib@main')

pipeline {
    agent any
    // stageBuild(pipelineParams)
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
        success {
            script {
                echo "SUCCESS"
            }
		}        
        failure {
            script {
                echo "TFAILURE"
            }
        }
        always{
            script {
                echo "ALWAYS"
            }
        }
    }
}