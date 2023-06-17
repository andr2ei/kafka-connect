FROM bitnami/kafka:3.4.0
LABEL authors="andr2ei"

USER root

RUN echo "adding actual connect props"
COPY ./config/connect-standalone.properties /opt/bitnami/kafka/config
COPY ./config/connector-source-csv.properties /opt/bitnami/kafka/config

RUN echo "adding connectors"
RUN mkdir /opt/connectors
COPY kafka-connect-1.0-SNAPSHOT.jar /opt/connectors
RUN mkdir -p /opt/input/csv

RUN echo "installing vim"
RUN apt-get update
RUN apt-get -y install vim

RUN echo "copy scripts"
COPY scripts/create-topics.sh /opt
COPY scripts/run-connect.sh /opt
COPY scripts/get-users.sh /opt

RUN chmod -R 777 /opt

WORKDIR /opt

ENTRYPOINT [ "/opt/bitnami/scripts/kafka/entrypoint.sh" ]
CMD [ "/opt/bitnami/scripts/kafka/run.sh" ]