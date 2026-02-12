package vars

def call(Map pipelineParams) {
    stage('BUILD') {
        script{
            echo "BUILD SOME CODE"
        }
    }
}