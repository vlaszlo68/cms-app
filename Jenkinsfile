pipeline {
    agent any

    options {
        buildDiscarder(logRotator(numToKeepStr: '3'))
        timestamps()
        timeout(time: 5, unit: 'MINUTES')
    }

    stages {

        stage('Checkout') {
            steps {
                //git 'https://github.com/vlaszlo68/cms-app'
                checkout scm
            }
        }

        stage('Build WAR') {
            steps {
                script {
                    docker.image('maven:3.9.9-eclipse-temurin-21').inside {
                        sh 'mvn clean package'
                    }
                }
            }
        }

        stage('Deploy') {
            steps {
                sh '''
                set -e

                echo "=== Deploy started ==="

                echo "Checking container..."
                docker ps | grep cms-tomcat

                echo "Cleaning old deployment..."
                docker exec cms-tomcat rm -rf /usr/local/tomcat/webapps/cms-app || true
                docker exec cms-tomcat rm -f /usr/local/tomcat/webapps/cms-app.war || true

                echo "Copying new WAR..."
                docker cp target/cms-app.war cms-tomcat:/usr/local/tomcat/webapps/

                echo "Restarting Tomcat..."
                docker restart cms-tomcat

                echo "=== Deploy finished ==="
                '''
            }
        }

        stage('Health Check') {
            steps {
                sh '''
                set -e

                echo "=== Health check started ==="

                for i in {1..5}; do
                    echo "Attempt $i..."
                    if curl -f http://cms-tomcat:8080/hello; then
                        echo "Health check OK"
                        exit 0
                    fi
                    sleep 3
                done

                echo "Health check FAILED"
                exit 1
                '''
            }
        }
    }
    post {
        always {
            cleanWs()
        }
    }
}