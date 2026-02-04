pipeline {
agent { node { label 'LinuxSlave' } }
	parameters {
        string(name: 'EVENT_TYPE', defaultValue: '', description: 'Webhook event type')
    }
    triggers {
        // genericTrigger(...) / simpleTrigger(...) etc.
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
