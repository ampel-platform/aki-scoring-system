services:
  kafka:
    image: 'apache/kafka:4.0.0@sha256:3f7b939115cd4872e9cee9369d80bd69712fde55f9902f46d793f64848dedc75'
    hostname: kafka
    container_name: kafka
    ports:
      - "19092:9092"
      - "19101:9101"
      - "29092:29092"
    environment:
      KAFKA_NODE_ID: 1
      KAFKA_LISTENER_SECURITY_PROTOCOL_MAP: "CONTROLLER:PLAINTEXT,PLAINTEXT:PLAINTEXT,PLAINTEXT_HOST:PLAINTEXT"
      KAFKA_ADVERTISED_LISTENERS: "PLAINTEXT://kafka:29092,PLAINTEXT_HOST://localhost:9092"
      KAFKA_JMX_PORT: 9101
      KAFKA_JMX_HOSTNAME: localhost
      KAFKA_PROCESS_ROLES: "broker,controller"
      KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR: 1
      KAFKA_CONTROLLER_QUORUM_VOTERS: "1@localhost:29093"
      KAFKA_LISTENERS: "PLAINTEXT://kafka:29092,CONTROLLER://localhost:29093,PLAINTEXT_HOST://0.0.0.0:9092"
      KAFKA_INTER_BROKER_LISTENER_NAME: "PLAINTEXT"
      KAFKA_CONTROLLER_LISTENER_NAMES: "CONTROLLER"
      CLUSTER_ID: "pW0o31_zTWqplLKY9UDTQg"
    healthcheck:
      test: nc -z kafka 29092 || exit -1
      start_period: 180s
      interval: 5s
      timeout: 10s
      retries: 5

  kafka-ui:
    image: provectuslabs/kafka-ui:v0.7.2@sha256:8f2ff02d64b0a7a2b71b6b3b3148b85f66d00ec20ad40c30bdcd415d46d31818
    ports:
      - "18080:8080"
    depends_on:
      kafka:
        condition: service_healthy
    environment:
      KAFKA_CLUSTERS_0_NAME: local
      KAFKA_CLUSTERS_0_BOOTSTRAPSERVERS: kafka:29092
      DYNAMIC_CONFIG_ENABLED: 'true'

  aki:
    image: ghcr.io/ampel-cdss-org/aki-service:{AKI_RELEASE_VERSION}
    depends_on:
      kafka:
        condition: service_healthy
      mongo:
        condition: service_healthy
    environment:
      MONGO_DB_HOST: "mongo"
      MONGO_DB_PORT: 27017
      MONGO_DB_NAME: "aki-validation-db"
      KAFKA_BOOTSTRAP_SERVERS: "kafka:29092"
      KAFKA_CONSUMER_GROUP_ID: "aki-service-validation"
      KAFKA_CONSUMER_COMMIT_TIMEOUT_MS: 5000

  mongo:
    image: mongo:8.0.11-noble@sha256:29f2124655aa6eab4e413cced7d1128d1b8da2022e3c92931281e95c792e3e4b
    ports:
      - "27017:27017"
    environment:
      MONGO_INITDB_DATABASE: "aki-validation-db"
      TZ: "Europe/Berlin"
    healthcheck:
      test: mongosh --eval 'db.runCommand("ping").ok' --quiet
      interval: 10s
      start_period: 30s
      timeout: 15s
      retries: 3

  aki-validation:
    image: ghcr.io/ampel-cdss-org/aki-service-validation:{AKI_RELEASE_VERSION}
    depends_on:
      kafka:
        condition: service_healthy
      aki:
        condition: service_started
    environment:
      KAFKA_BOOTSTRAP_SERVERS: "kafka:29092"
      # no other envVars to make use of bundled testdata
