package vars

def call(Map pipelineParams) {
    // post{
    //     success {
    //         script {
    //             echo "SUCCESS"
    //         }
	// 	}        
	// 	failure {
    //         script {
    //             echo "TFAILURE"
    //         }
    //     }
	// 	always{
    //         script {
    //             echo "ALWAYS"
    //         }
    //     }
    // }
    script {
        echo "ALWAYS"
    }
}