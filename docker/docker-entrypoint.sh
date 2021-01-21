#!/bin/sh
mvn liquibase:update -Dliquibase.promptOnNonLocalDatabase=false
mvn jetty:run