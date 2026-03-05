library('purpdm-lib@main')
import groovy.json.JsonOutput

pipeline {
    agent any
    
    stages {
        stage('Initialize') {
            steps {
                script {
                    checkout scmGit(branches: [[name: "main"]],
                        userRemoteConfigs: [[
                            credentialsId: 'git',
                            url: "https://github.com/abertowy/jenkins_examples.git"
                        ]]
                    )
                }
            }
        }
        stage('Inspect Jenkins Nodes') {
            steps {
                script {
                    def json
                    dir("./ansible-playbooks"){
                        json = JsonOutput.toJson([
                            ansible_user: "username",
                            ansible_password: "password",
                            nodes: ["Built-In", "LinuxSlavePurpdm"]
                        ])
                        // Jenkins Ansible plugin ???
                        sh "ansible-playbook test_1.yaml --extra-vars '${json}'"
                    }
                }
            }
        }
    }
}