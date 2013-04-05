#!/bin/bash

mvn org.apache.maven.plugins:maven-dependency-plugin:2.4:get \
    -DrepoUrl=http://download.java.net/maven/2/ \
    -Dartifact=org.mortbay.jetty:jetty-runner:8.1.8.v20121106 \
    -Ddest=jetty-runner.jar

#mvn org.apache.maven.plugins:maven-dependency-plugin:2.4:get \
#    -DrepoUrl=http://download.java.net/maven/2/ \
#    -Dartifact=org.apache.solr:solr:3.5.0 \
#    -Dpackaging=war \
#    -Ddest=solr.war
