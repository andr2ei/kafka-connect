FROM bitnami/kafka:3.4.0
LABEL authors="andr2ei"

USER root

WORKDIR /opt

RUN echo "installing utils"
RUN apt-get update
RUN #apt-get -y install vim
RUN apt-get -y install maven
RUN apt-get -y install git
RUN apt-get -y install netcat

RUN echo "installing kafka-connect"
RUN git clone https://github.com/andr2ei/kafka-connect.git
WORKDIR /opt/kafka-connect
RUN mvn package

WORKDIR /opt

RUN echo "adding actual connect props"
COPY ./config/connect-standalone.properties /opt/bitnami/kafka/config
COPY ./config/connector-source-csv.properties /opt/bitnami/kafka/config

RUN echo "adding connectors and input dir"
RUN mkdir /opt/connectors
RUN cp /opt/kafka-connect/target/kafka-connect-1.0-SNAPSHOT.jar /opt/connectors
RUN mkdir -p /opt/input/csv

RUN echo "copy scripts"
COPY scripts/create-topics.sh /opt
COPY scripts/run-connect.sh /opt
COPY scripts/get-users.sh /opt

RUN chmod -R 777 /opt

ENTRYPOINT [ "/opt/bitnami/scripts/kafka/entrypoint.sh" ]
CMD /opt/bitnami/scripts/kafka/run.sh & \
    while ! echo exit | nc localhost 9092; do sleep 2; done && \
    /opt/run-connect.sh