pipeline{
    agent any
    triggers { pollSCM('H/2 * * * *') }
    tools {
        jdk 'javahome'
        maven 'mavanhome' 
    }
    stages {
        stage('checkout'){
            steps{
                // your checkout code
                checkout scmGit(branches: 
                [[name: '*/master']], 
                extensions: [], 
                userRemoteConfigs: 
                [[credentialsId: 'git-cred', url: 'https://github.com/saravanasm118/webapplication.git']])
            }
        }
        stage('quality'){
            steps{
                // your quality code
                sh """
                mvn clean install sonar:sonar \
                    -Dsonar.host.url=https://sonarcloud.io \
                    -Dsonar.login=0d867667f3464a482a0ab8cf794cc067b06a4dae \
                    -Dsonar.projectKey=air-india \
                     -Dsonar.organization=airindia \
                    -Dsonar.projectName=air-india \
                    -Dsonar.projectVersion=$BUILD_NUMBER
                    """
            }
        }
        stage('build'){
            steps{
                // your build code
                sh " mvn clean install "
            }
        }
        stage('imagecreation'){
            steps{
                sh " cp target/java-tomcat-maven-example.war . "
                echo 'Creating Docker image with Nginx'
 
                    // Create a Dockerfile in the workspace
                    writeFile file: 'Dockerfile', text: '''
                        # Use your custom base image from Docker Hub
                        FROM tomcat:latest
                        COPY *.war /opt/apache-tomcat-9.0.88/webapss/
                        WORKDIR /opt/apache-tomcat-9.0.88/webapss/
                        CMD ["/opt/apache-tomcat-9.0.88/bin/catalina.sh", "run"]
                      '''
                      
sh "docker build -t saravana118/airindia:${env.BUILD_NUMBER} ."
sh "docker images"
            }
        }
        stage('docker push'){
            steps {
                withCredentials([usernamePassword(credentialsId: 'docker-creds', usernameVariable: 'DOCKER_USERNAME', passwordVariable: 'DOCKER_PASSWORD')]) {
                    sh "docker login -u $DOCKER_USERNAME -p $DOCKER_PASSWORD && docker push saravana118/airindia:${env.BUILD_NUMBER}"
                    sh "docker rmi saravana118/airindia:${env.BUILD_NUMBER}"
                }
            }
        }
        stage('deployment'){
            steps{
                // your deployment code
                echo "deploymet is passed"
            }
        }
    }
    post{
        always{
            // send mail
            echo " this is always"
        }
        success{
            //trigger another job / print suucess mgs 
            echo " job is success"
        }
        failure{
            // disable the build or retrigger the job 
            echo "job is failed"
        }

    }
}
