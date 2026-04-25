#!/bin/bash
export SPRING_DATASOURCE_URL="jdbc:postgresql://<RDS-ENDPOINT>:5432/family_schedule"
export SPRING_DATASOURCE_USERNAME="family"
export SPRING_DATASOURCE_PASSWORD="<your-password>"
export PORT=8082

nohup java -jar /home/ec2-user/family-schedule.jar \
  --spring.profiles.active=prod \
  > /home/ec2-user/family-schedule.log 2>&1 &

echo "Started PID: $!"
