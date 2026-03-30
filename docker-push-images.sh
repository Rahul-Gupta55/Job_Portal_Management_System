#!/bin/bash
set -e

mvn -q -f common-security/pom.xml clean install -DskipTests

modules=(eureka-server config-server user-service job-service application-service resume-service notification-service search-service api-gateway)
for module in "${modules[@]}"; do
  echo "Packaging $module..."
  mvn -q -f "$module/pom.xml" clean package -DskipTests
  echo "Building Docker image for $module..."
done

docker build -t jobportal-eureka-server ./eureka-server
docker build -t jobportal-config-server ./config-server
docker build -t jobportal-user-service ./user-service
docker build -t jobportal-job-service ./job-service
docker build -t jobportal-application-service ./application-service
docker build -t jobportal-resume-service ./resume-service
docker build -t jobportal-notification-service ./notification-service
docker build -t jobportal-search-service ./search-service
docker build -t jobportal-api-gateway ./api-gateway

docker push jobportal-eureka-server
docker push jobportal-config-server
docker push jobportal-user-service
docker push jobportal-job-service
docker push jobportal-application-service
docker push jobportal-resume-service
docker push jobportal-notification-service
docker push jobportal-search-service
docker push jobportal-api-gateway
