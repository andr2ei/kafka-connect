-- docker run detached

docker run --name kafka-connect -d -privileged -e ALLOW_PLAINTEXT_LISTENER=yes bitnami/kafka:3.4.0

in /home/andr2ei/IdeaProjects/kafka-connect/resources/input

docker run \
    -v ${PWD}:/opt/input/csv \
    -d \
    --name kafka-connect \
    -e ALLOW_PLAINTEXT_LISTENER=yes \
    -e KAFKA_CFG_AUTO_CREATE_TOPICS_ENABLE=true \
    andr2ei/kafka:3.4.0

-- attach to container

docker exec -it kafka-connect bash

--build image
in /home/andr2ei/IdeaProjects/kafka-connect/resources/docker/source/csv

docker build --no-cache -t andr2ei/kafka:3.4.0 .