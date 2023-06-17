#!/bin/sh

/opt/bitnami/kafka/bin/kafka-console-consumer.sh --topic users --from-beginning --bootstrap-server localhost:9092