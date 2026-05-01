pipeline {
    agent any

    stages {

        stage('Checkout') {
            steps {
                git 'https://github.com/vlaszlo68/cms-app'
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
                docker exec cms-tomcat rm -rf /usr/local/tomcat/webapps/cms-app
                docker exec cms-tomcat rm -f /usr/local/tomcat/webapps/cms-app.war
                docker cp target/cms-app.war cms-tomcat:/usr/local/tomcat/webapps/
                docker restart cms-tomcat
                '''
            }
        }
    }
}