def JENKINS_REPO_BRANCH = params.JENKINS_REPO_BRANCH?.trim() ?: 'main'
library("purpdm-lib@${JENKINS_REPO_BRANCH}")

def gitVar
// can be defined as hidden parameter in Jenkins job, should only be defined for one P deploy job that is representative for all P deploy jobs
def sendDataToMetron = libraryHelpers.getOrDefaultString(params, 'SEND_DATA_TO_METRON', 'false')

pipeline {
agent { node { label 'LinuxSlave' } }
	environment {
        BUILD_PROPERTIES='build.properties'
        STATIC_CREDENTIALS_FOLDER = 'staticP'
	}
	options {
        timestamps () 
    }
	tools {
		jdk "jdk17"
	}
	stages {
        stage('Init') {
            steps {
                script {
                    libraryHelpers.printEnvironmentConfiguration(env)
                    libraryHelpers.printParamsConfiguration(params)
                }
            }
        }
		stage ('Checkout') {
			steps{
			    script {
                    gitVar = libraryHelpers.checkoutRepository(
                        'https://github.com/abertowy/jenkins_examples.git',
                        'main'
                    )
                    libraryHelpers.checkoutJenkinsPipelineRepo(JENKINS_REPO_BRANCH)
                    libraryHelpers.writeBuildProperties(
                            env.BUILD_PROPERTIES,
                            gitVar.GIT_BRANCH,
                            gitVar.GIT_COMMIT,
                            BUILD_NUMBER,
                            JOB_URL,
                            targetEnv)
                    currentBuild.description = libraryHelpers.createBuildDescription(gitVar.GIT_URL, gitVar.GIT_BRANCH, gitVar.GIT_COMMIT)
                }
			}
		}
		// stage ('ENV Setup'){
        //     steps {
		// 		script{
        //             libraryHelpers.copyCredentialsForMainProject(env.STATIC_CREDENTIALS_FOLDER)
		// 		}
		// 	}
		// }
		stage ('Package') {
			steps{
				script{
                    sh("""
                        echo "WE ARE HERE"
                    """)
				}
			}
		}
    }
	post{
		always{
            script {
                if (sendDataToMetron.equals('true')) {
                    println "Sending data to metron service for \n\tbuildUrl=${env.BUILD_URL} \n\trepo=${gitVar.GIT_URL} \n\tgitBranch=${gitVar.GIT_BRANCH} \n\tcommitId=${gitVar.GIT_COMMIT}"
                    sh '''
                        curl -X POST \
                            -H "Content-Type: application/json" \
                            -d '{"jobName":"${env.JOB_NAME}","buildId":"${env.BUILD_ID}", "buildUrl":"${env.BUILD_URL}","result": "${currentBuild.currentResult}", "startTime": "${currentBuild.startTimeInMillis}", "nodeName": "${env.NODE_NAME}", "gitBranch": "${gitVar.GIT_BRANCH}"}' \
                            http://localhost:8080/generic-webhook-trigger/invoke?token=12345678

                    '''
                    // libraryHelpers.callMetronService(
                    //         env.JOB_NAME,
                    //         env.BUILD_ID,
                    //         env.BUILD_URL,
                    //         currentBuild.currentResult,
                    //         currentBuild.startTimeInMillis,
                    //         currentBuild.startTimeInMillis + currentBuild.duration,
                    //         env.NODE_NAME,
                    //         gitVar.GIT_BRANCH,
                    //         gitVar.GIT_URL,
                    //         stageInfo(),
                    //         getFailureReason(),
                    //         getTimeInQueue(),
                    //         gitVar.GIT_COMMIT
                    // )
                } else {
                    println "parameter 'SEND_DATA_TO_METRON' was false or not defined so no data is sent to Metron service"
                }
            }
        }
	}
}
