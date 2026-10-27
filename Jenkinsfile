pipeline {
    agent none

    options {
        timestamps()
        disableConcurrentBuilds()
        buildDiscarder(logRotator(numToKeepStr: '20'))
    }

    parameters {
        string(name: 'TEST_DATABASE_URL',      defaultValue: 'jdbc:postgresql://host.docker.internal:5432/simplecrm_test', description: 'JDBC URL the integration tests connect to (must be reachable from the build agent container)')
        string(name: 'TEST_DATABASE_USERNAME', defaultValue: 'crm_app',     description: 'Database user for the test schema')
        string(name: 'TEST_DATABASE_PASSWORD', defaultValue: 'crm_dev_local', description: 'Database password for the test schema')
        string(name: 'DOCKER_NETWORK',         defaultValue: '',            description: 'Optional docker network for the build agent containers (e.g. when the test database runs in a container)')
        string(name: 'MAVEN_CACHE_VOLUME',     defaultValue: 'jenkins-m2',  description: 'Named volume used to cache the local Maven repository between builds')
        string(name: 'EXTRA_DOCKER_ARGS',      defaultValue: '',            description: 'Extra docker run flags for the agent containers (e.g. -v shared-ws:/var/jenkins_home/workspace when Jenkins itself runs in a container)')
    }

    environment {
        TEST_DATABASE_URL      = "${params.TEST_DATABASE_URL}"
        TEST_DATABASE_USERNAME = "${params.TEST_DATABASE_USERNAME}"
        TEST_DATABASE_PASSWORD = "${params.TEST_DATABASE_PASSWORD}"
    }

    stages {
        stage('Backend tests') {
            agent {
                docker {
                    image 'maven:3.9-eclipse-temurin-17'
                    reuseNode true
                    args "-v ${params.MAVEN_CACHE_VOLUME}:/root/.m2 ${params.DOCKER_NETWORK ? "--network ${params.DOCKER_NETWORK}" : ''} ${params.EXTRA_DOCKER_ARGS}"
                }
            }
            steps {
                dir('backend') {
                    sh 'mvn -B -ntp clean verify'
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
                    args "${params.DOCKER_NETWORK ? "--network ${params.DOCKER_NETWORK}" : ''} ${params.EXTRA_DOCKER_ARGS}"
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
