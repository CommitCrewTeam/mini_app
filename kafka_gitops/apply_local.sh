#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
COMPOSE_FILE="${COMPOSE_FILE:-"$SCRIPT_DIR/../docker-compose.yml"}"
TOPICS_FILE="${TOPICS_FILE:-"$SCRIPT_DIR/topics.local.yaml"}"
CLUSTER="${CLUSTER:-}"
BOOTSTRAP_SERVER="${BOOTSTRAP_SERVER:-}"
SKIP_START="${SKIP_START:-false}"
VALIDATE_ONLY="${VALIDATE_ONLY:-false}"
DELETE_UNMANAGED_TOPICS="${DELETE_UNMANAGED_TOPICS:-false}"

DEFAULT_PARTITIONS=""
DEFAULT_REPLICATION_FACTOR=""
declare -A DEFAULT_CONFIG=()

TOPIC_NAMES=()
declare -A TOPIC_PARTITIONS=()
declare -A TOPIC_REPLICATION_FACTORS=()
declare -A TOPIC_CONFIG=()

trim() {
  local value="$1"
  value="${value#"${value%%[![:space:]]*}"}"
  value="${value%"${value##*[![:space:]]}"}"
  value="${value%\"}"
  value="${value#\"}"
  value="${value%\'}"
  value="${value#\'}"
  printf '%s' "$value"
}

parse_topics_file() {
  local section=""
  local subsection=""
  local current_topic=""

  while IFS= read -r raw_line || [[ -n "$raw_line" ]]; do
    local line="${raw_line%%#*}"
    [[ -z "$(trim "$line")" ]] && continue

    local indent=0
    if [[ "$line" =~ ^( *) ]]; then
      indent="${#BASH_REMATCH[1]}"
    fi

    local stripped
    stripped="$(trim "$line")"

    if [[ "$indent" -eq 0 && "$stripped" =~ ^([^:]+):(.*)$ ]]; then
      section="$(trim "${BASH_REMATCH[1]}")"
      subsection=""
      local value
      value="$(trim "${BASH_REMATCH[2]}")"

      case "$section" in
        cluster) CLUSTER="${CLUSTER:-$value}" ;;
        bootstrapServer) BOOTSTRAP_SERVER="${BOOTSTRAP_SERVER:-$value}" ;;
      esac

      continue
    fi

    if [[ "$section" == "defaults" ]]; then
      if [[ "$indent" -eq 2 && "$stripped" =~ ^([^:]+):(.*)$ ]]; then
        local key value
        key="$(trim "${BASH_REMATCH[1]}")"
        value="$(trim "${BASH_REMATCH[2]}")"
        subsection=""

        case "$key" in
          partitions) DEFAULT_PARTITIONS="$value" ;;
          replicationFactor) DEFAULT_REPLICATION_FACTOR="$value" ;;
          config) subsection="defaults.config" ;;
        esac

        continue
      fi

      if [[ "$indent" -eq 4 && "$subsection" == "defaults.config" && "$stripped" =~ ^([^:]+):(.*)$ ]]; then
        local key value
        key="$(trim "${BASH_REMATCH[1]}")"
        value="$(trim "${BASH_REMATCH[2]}")"
        DEFAULT_CONFIG["$key"]="$value"
        continue
      fi
    fi

    if [[ "$section" == "topics" ]]; then
      if [[ "$indent" -eq 2 && "$stripped" =~ ^-\ +([^:]+):(.*)$ ]]; then
        local key value
        key="$(trim "${BASH_REMATCH[1]}")"
        value="$(trim "${BASH_REMATCH[2]}")"
        subsection=""

        if [[ "$key" == "name" ]]; then
          current_topic="$value"
          TOPIC_NAMES+=("$current_topic")
        fi

        continue
      fi

      if [[ "$indent" -eq 4 && -n "$current_topic" && "$stripped" =~ ^([^:]+):(.*)$ ]]; then
        local key value
        key="$(trim "${BASH_REMATCH[1]}")"
        value="$(trim "${BASH_REMATCH[2]}")"
        subsection=""

        case "$key" in
          partitions) TOPIC_PARTITIONS["$current_topic"]="$value" ;;
          replicationFactor) TOPIC_REPLICATION_FACTORS["$current_topic"]="$value" ;;
          config) subsection="topics.config" ;;
        esac

        continue
      fi

      if [[ "$indent" -eq 6 && -n "$current_topic" && "$subsection" == "topics.config" && "$stripped" =~ ^([^:]+):(.*)$ ]]; then
        local key value
        key="$(trim "${BASH_REMATCH[1]}")"
        value="$(trim "${BASH_REMATCH[2]}")"
        TOPIC_CONFIG["$current_topic|$key"]="$value"
        continue
      fi
    fi
  done < "$TOPICS_FILE"
}

kafka() {
  docker compose -f "$COMPOSE_FILE" exec -T kafka "$@"
}

wait_for_kafka() {
  echo "Waiting for Kafka at $BOOTSTRAP_SERVER ..."

  for _ in {1..30}; do
    if kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server "$BOOTSTRAP_SERVER" --list >/dev/null 2>&1; then
      echo "Kafka is ready."
      return
    fi

    sleep 2
  done

  echo "Kafka did not become ready within 60 seconds." >&2
  exit 1
}

apply_topic() {
  local topic_name="$1"
  local partitions="${TOPIC_PARTITIONS[$topic_name]:-$DEFAULT_PARTITIONS}"
  local replication_factor="${TOPIC_REPLICATION_FACTORS[$topic_name]:-$DEFAULT_REPLICATION_FACTOR}"

  echo "Applying topic '$topic_name' ..."

  local create_args=(
    /opt/kafka/bin/kafka-topics.sh
    --bootstrap-server "$BOOTSTRAP_SERVER"
    --create
    --if-not-exists
    --topic "$topic_name"
    --partitions "$partitions"
    --replication-factor "$replication_factor"
  )

  for key in "${!DEFAULT_CONFIG[@]}"; do
    local value="${TOPIC_CONFIG[$topic_name|$key]:-${DEFAULT_CONFIG[$key]}}"
    create_args+=(--config "$key=$value")
  done

  for composite_key in "${!TOPIC_CONFIG[@]}"; do
    [[ "$composite_key" == "$topic_name|"* ]] || continue
    local key="${composite_key#"$topic_name|"}"
    [[ -n "${DEFAULT_CONFIG[$key]+x}" ]] && continue
    create_args+=(--config "$key=${TOPIC_CONFIG[$composite_key]}")
  done

  kafka "${create_args[@]}" >/dev/null

  local describe
  describe="$(kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server "$BOOTSTRAP_SERVER" --describe --topic "$topic_name")"
  local current_partitions
  current_partitions="$(sed -n 's/.*PartitionCount: \([0-9][0-9]*\).*/\1/p' <<< "$describe" | head -n 1)"

  if [[ -n "$current_partitions" && "$current_partitions" -lt "$partitions" ]]; then
    kafka /opt/kafka/bin/kafka-topics.sh \
      --bootstrap-server "$BOOTSTRAP_SERVER" \
      --alter \
      --topic "$topic_name" \
      --partitions "$partitions" >/dev/null
    echo "  partitions increased: $current_partitions -> $partitions"
  elif [[ -n "$current_partitions" && "$current_partitions" -gt "$partitions" ]]; then
    echo "  warning: existing partitions=$current_partitions is greater than desired=$partitions. Kafka cannot decrease partitions." >&2
  fi

  local config_pairs=()
  for key in "${!DEFAULT_CONFIG[@]}"; do
    local value="${TOPIC_CONFIG[$topic_name|$key]:-${DEFAULT_CONFIG[$key]}}"
    config_pairs+=("$key=$value")
  done
  for composite_key in "${!TOPIC_CONFIG[@]}"; do
    [[ "$composite_key" == "$topic_name|"* ]] || continue
    local key="${composite_key#"$topic_name|"}"
    [[ -n "${DEFAULT_CONFIG[$key]+x}" ]] && continue
    config_pairs+=("$key=${TOPIC_CONFIG[$composite_key]}")
  done

  if [[ "${#config_pairs[@]}" -gt 0 ]]; then
    local joined
    joined="$(IFS=,; echo "${config_pairs[*]}")"
    kafka /opt/kafka/bin/kafka-configs.sh \
      --bootstrap-server "$BOOTSTRAP_SERVER" \
      --alter \
      --entity-type topics \
      --entity-name "$topic_name" \
      --add-config "$joined" >/dev/null
  fi
}

is_managed_topic() {
  local topic_name="$1"

  for managed_topic in "${TOPIC_NAMES[@]}"; do
    if [[ "$managed_topic" == "$topic_name" ]]; then
      return 0
    fi
  done

  return 1
}

delete_unmanaged_topics() {
  echo "Deleting unmanaged topics ..."

  local existing_topics
  existing_topics="$(kafka /opt/kafka/bin/kafka-topics.sh --bootstrap-server "$BOOTSTRAP_SERVER" --list)"

  while IFS= read -r topic_name; do
    [[ -n "$topic_name" ]] || continue
    [[ "$topic_name" == __* ]] && continue

    if ! is_managed_topic "$topic_name"; then
      echo "Deleting topic '$topic_name' ..."
      kafka /opt/kafka/bin/kafka-topics.sh \
        --bootstrap-server "$BOOTSTRAP_SERVER" \
        --delete \
        --topic "$topic_name" >/dev/null
    fi
  done <<< "$existing_topics"
}

[[ -f "$COMPOSE_FILE" ]] || { echo "Compose file not found: $COMPOSE_FILE" >&2; exit 1; }
[[ -f "$TOPICS_FILE" ]] || { echo "Topics file not found: $TOPICS_FILE" >&2; exit 1; }

parse_topics_file

[[ -n "$CLUSTER" ]] || { echo "Topics file is missing cluster." >&2; exit 1; }
[[ -n "$BOOTSTRAP_SERVER" ]] || BOOTSTRAP_SERVER="kafka:29092"
[[ -n "$DEFAULT_PARTITIONS" ]] || { echo "Topics file is missing defaults.partitions." >&2; exit 1; }
[[ -n "$DEFAULT_REPLICATION_FACTOR" ]] || { echo "Topics file is missing defaults.replicationFactor." >&2; exit 1; }
[[ "${#TOPIC_NAMES[@]}" -gt 0 ]] || { echo "Topics file must contain at least one topic." >&2; exit 1; }

if [[ "$VALIDATE_ONLY" == "true" ]]; then
  echo "Topics file is valid for cluster '$CLUSTER'."
  echo "Bootstrap server: $BOOTSTRAP_SERVER"
  echo "Topics: ${#TOPIC_NAMES[@]}"
  for topic_name in "${TOPIC_NAMES[@]}"; do
    echo "  - $topic_name partitions=${TOPIC_PARTITIONS[$topic_name]:-$DEFAULT_PARTITIONS} replicationFactor=${TOPIC_REPLICATION_FACTORS[$topic_name]:-$DEFAULT_REPLICATION_FACTOR}"
  done
  exit 0
fi

if [[ "$SKIP_START" != "true" ]]; then
  docker compose -f "$COMPOSE_FILE" up -d kafka kafka_ui
fi

wait_for_kafka

for topic_name in "${TOPIC_NAMES[@]}"; do
  apply_topic "$topic_name"
done

if [[ "$DELETE_UNMANAGED_TOPICS" == "true" ]]; then
  delete_unmanaged_topics
fi

echo "Local Kafka GitOps apply completed."
