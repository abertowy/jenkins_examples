pipeline {
agent any
	parameters {
        string(name: 'EVENT_TYPE', defaultValue: '', description: 'Webhook event type')
    }
    triggers {
        GenericTrigger(
            token: '12345678',
            genericVariables: [
                [key: 'status',  value: '$.status'],
                [key: 'build',   value: '$.build']
            ],
            printContributedVariables: true,
            printPostContent: true
        )
    }
	stages {
		stage ('Package') {
			steps{
				script{
                    sh("""
                        echo "HERE IS A WEBHOOK"
                    """)
				}
			}
		}
    }
}
