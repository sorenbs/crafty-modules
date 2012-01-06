#!/bin/bash
cp /var/lib/jenkins/jobs/crafty-modules/workspace/target/scala_2.9.1/crafty-modules_2.9.1-0.1-SNAPSHOT.war /opt/jetty/webapps/crafty.war; service jetty restart;
