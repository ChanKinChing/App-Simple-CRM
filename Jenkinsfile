pipeline {
    agent none

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    environment {
        TEST_DATABASE_URL      = 'jdbc:postgresql://host.docker.internal:5432/simplecrm_test'
        TEST_DATABASE_USERNAME = 'crm_app'
        TEST_DATABASE_PASSWORD = 'crm_dev_local'
    }

    stages {
        stage('Backend tests') {
            agent {
                docker {
                    image 'maven:3.9-eclipse-temurin-17'
                    reuseNode true
                    args '-v jenkins-m2:/root/.m2'
                }
            }
            steps {
                dir('backend') {
                    sh 'mvn -B -ntp verify'
                }
            }
            post {
                always {
                    junit allowEmptyResults: true, testResults: 'backend/target/surefire-reports/*.xml'
                }
            }
        }

        stage('Frontend lint and build') {
            agent {
                docker {
                    image 'node:22-alpine'
                    reuseNode true
                }
            }
            steps {
                dir('frontend') {
                    sh 'npm ci'
                    sh 'npm run lint'
                    sh 'npm run build'
                }
            }
            post {
                success {
                    archiveArtifacts artifacts: 'frontend/dist/**', allowEmptyArchive: true
                }
            }
        }
    }

    post {
        always {
            echo "Build ${currentBuild.currentResult}: ${env.BUILD_URL}"
        }
    }
}
