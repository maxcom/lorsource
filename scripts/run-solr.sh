#!/bin/bash

if [ "x$1" = "x" -o "x$2" = "x" -o "x$3" = "x" ]; then
	echo "Usage: run-solr.sh <install_home> <solr-home> <jetty.xml>"
	exit 1
fi

INSTALL_HOME=$1

JETTY_RUNNER=$INSTALL_HOME/jetty-runner.jar
WAR_FILE=$INSTALL_HOME/solr.war

java -Dsolr.solr.home=$2 -jar $JETTY_RUNNER --config $3 $WAR_FILE
