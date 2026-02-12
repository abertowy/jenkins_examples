def call(Map pipelineParams) {
    pipeline {
        node(pipelineParams.nodeLabel ?: 'any') {
        // stageTest(pipelineParams)
            try {
                stageBuild(pipelineParams)
            } catch (Throwable ex) {
                throw ex
            } finally {
                if (currentBuild.result == 'SUCCESS') {
                }
                if (currentBuild.result == 'UNSTABLE') {
                }
                stageTest(pipelineParams)
            }
        }
    }
}