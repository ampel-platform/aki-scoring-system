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
