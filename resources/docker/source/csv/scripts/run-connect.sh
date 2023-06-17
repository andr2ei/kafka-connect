#!/bin/sh

/opt/bitnami/kafka/bin/connect-standalone.sh \
  /opt/bitnami/kafka/config/connect-standalone.properties \
  /opt/bitnami/kafka/config/connector-source-csv.properties