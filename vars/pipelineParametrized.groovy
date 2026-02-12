def call(Map pipelineParams) {
    pipeline {
        node(pipelineParams.nodeLabel ?: '') {
        // stageTest(pipelineParams)
            try {
                stageBuild(pipelineParams)
                eccho "AAAA"
            } catch (Throwable ex) {
                throw ex
            } finally {
                if (currentBuild.result == 'SUCCESS') {
                }
                if (currentBuild.result == 'UNSTABLE') {
                }
                stageTest(pipelineParams)
                libraryHelpers.simpleEcho()
            }
        }
    }
}