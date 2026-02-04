pipeline {
agent any
	parameters {
        string(name: 'EVENT_TYPE', defaultValue: '', description: 'Webhook event type')
    }
    triggers {
        GenericTrigger(
            token: '12345678',
            genericVariables: [
                [key: 'jobName',  value: '$.jobName'],
                [key: 'buildId',   value: '$.buildId'],
                [key: 'buildUrl',   value: '$.buildUrl'],
                [key: 'result',   value: '$.result'],
                [key: 'startTime',   value: '$.startTime'],
                [key: 'nodeName',   value: '$.nodeName'],
                [key: 'gitBranch',   value: '$.gitBranch']
                // [key: 'buildId',   value: '$.buildId'],
                // [key: 'buildId',   value: '$.buildId'],
                // [key: 'buildId',   value: '$.buildId'],
                // [key: 'buildId',   value: '$.buildId'],
            ],
            printContributedVariables: true,
            printPostContent: true
        )
    }
	stages {
		stage ('Package') {
			steps{
				script{
                    echo "HERE IS A WEBHOOK"
                    echo "jobName: ${env.jobName}"
                    echo "buildId: ${env.buildId}"
                    echo "buildUrl: ${env.buildUrl}"
                    echo "result: ${env.result}"
                    echo "startTime: ${env.startTime}"
                    echo "nodeName: ${env.nodeName}"
                    echo "gitBranch: ${env.gitBranch}"
				}
			}
		}
    }
}
