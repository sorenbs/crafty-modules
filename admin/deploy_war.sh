#!/bin/bash
cp /var/lib/jenkins/jobs/crafty-modules/workspace/target/scala-2.9.1/crafty-modules_2.9.1-0.1-SNAPSHOT.war /opt/jetty/webapps/root.war; service jetty restart;
