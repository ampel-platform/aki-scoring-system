#!/usr/bin/env bash

create_topic_with_config() {
  local -n config=$2 # creates a name reference!
  config_flags=""
  for key in ${!config[@]}; do
    config_flags+="--config ${key}=${config[$key]}"
  done

  /opt/kafka/bin/kafka-topics.sh \
    --bootstrap-server=${INIT_KAFKA_BOOTSTRAP_SERVER} \
    --create \
    --if-not-exists \
    --topic $1 \
    ${config_flags}

  if [ "$?" -ne 0 ]; then
    echo "Failed to create topic '${1}'"
    exit 1
  else
    echo "Topic '${1}' has been created"
  fi
}

declare -rA keep_forever_config=( ["retention.ms"]="-1" )

create_topic_with_config "AMPEL_RESULTS" keep_forever_config
create_topic_with_config "AMPEL_PATIENTS" keep_forever_config
create_topic_with_config "AMPEL_CASES" keep_forever_config
create_topic_with_config "AMPEL_LABORATORY" keep_forever_config
create_topic_with_config "AMPEL_PROCEDURES" keep_forever_config


# POPULATE TOPICS WITH DATA --------------------------------------------------------------------------------------------

echo "Populating topic with data..."

for file in /tmp/bootstrap_data/*.ndjson; do
  TOPIC_NAME=$(basename $file .ndjson)

  /opt/kafka/bin/kafka-topics.sh --bootstrap-server kafka:29092 --list | grep -q "${TOPIC_NAME}"
  if [ $? -ne 0 ]; then
    echo "Cannot populate topic '${TOPIC_NAME}' with data since it does not exist yet"
    continue
  fi

  echo -n "Populating topic '${TOPIC_NAME}' with data..."
  /opt/kafka/bin/kafka-console-producer.sh --bootstrap-server kafka:29092 --topic "$TOPIC_NAME" < "$file"

  if [ $? -ne 0 ]; then
    echo "FAILED"
    exit 1
  else
    echo "DONE"
  fi
done

