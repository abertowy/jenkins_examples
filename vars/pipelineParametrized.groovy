def call(Map pipelineParams) {
    pipeline {
    // agent any
        stageBuild(pipelineParams)
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
        // stageTest(pipelineParams)
        // try {
        //     stageBuild(pipelineParams)
        // } catch (Throwable ex) {
        //     throw ex
        // } finally {
        //     if (currentBuild.result == 'SUCCESS') {
        //     }
        //     if (currentBuild.result == 'UNSTABLE') {
        //     }
        //     stageTest(pipelineParams)
        // }
    }
}