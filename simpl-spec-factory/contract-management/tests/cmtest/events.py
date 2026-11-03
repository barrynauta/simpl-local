"""Kafka observation per D51: subscribe before acting, filter by key or nid."""
import json
import time


class TopicWatcher:
    def __init__(self, config, topic):
        from kafka import KafkaConsumer, TopicPartition  # imported lazily
        self.config = config
        self.topic = topic
        self.messages = []
        self.consumer = KafkaConsumer(
            bootstrap_servers=config.kafka_bootstrap,
            enable_auto_commit=False,
            auto_offset_reset="latest",
            consumer_timeout_ms=500,
            request_timeout_ms=20000,
        )
        deadline = time.monotonic() + config.poll_timeout
        partitions = None
        while time.monotonic() < deadline:
            partitions = self.consumer.partitions_for_topic(topic)
            if partitions:
                break
            time.sleep(config.poll_interval)
        assert partitions, "Kafka topic %s does not exist" % topic
        tps = [TopicPartition(topic, p) for p in sorted(partitions)]
        self.consumer.assign(tps)
        self.consumer.seek_to_end(*tps)
        for tp in tps:
            self.consumer.position(tp)  # resolve the end offset now, before acting

    def _drain(self):
        batch = self.consumer.poll(timeout_ms=300)
        for records in batch.values():
            for rec in records:
                try:
                    value = json.loads(rec.value.decode("utf-8")) if rec.value else None
                except ValueError:
                    value = {"_raw": rec.value.decode("utf-8", "replace")}
                headers = {}
                for k, v in rec.headers or []:
                    headers[k] = v.decode("utf-8", "replace") if isinstance(v, bytes) else v
                self.messages.append({
                    "key": rec.key.decode("utf-8") if rec.key else None,
                    "value": value, "headers": headers})

    def wait_for(self, predicate, description, timeout=None):
        deadline = time.monotonic() + (timeout or self.config.poll_timeout)
        while True:
            self._drain()
            found = [m for m in self.messages if predicate(m)]
            if found:
                return found
            if time.monotonic() >= deadline:
                raise AssertionError("no message on %s matching %s; seen %d: %s" % (
                    self.topic, description, len(self.messages),
                    json.dumps([m["value"] for m in self.messages][-5:], default=str)[:1500]))
            time.sleep(self.config.poll_interval)

    def collect(self, seconds):
        deadline = time.monotonic() + seconds
        while time.monotonic() < deadline:
            self._drain()
            time.sleep(self.config.poll_interval)
        return list(self.messages)

    def close(self):
        try:
            self.consumer.close()
        except Exception:  # noqa: BLE001
            pass


def message_nid(msg):
    value = msg.get("value") or {}
    if isinstance(value, dict) and value.get("contractNegotiationId"):
        return value["contractNegotiationId"]
    if msg.get("headers", {}).get("contractNegotiationId"):
        return msg["headers"]["contractNegotiationId"]
    return msg.get("key")


def publish(config, topic, key, value, headers=None):
    """Publish one JSON message (used to inject lifecycle messages, D47, D48)."""
    from kafka import KafkaProducer
    producer = KafkaProducer(bootstrap_servers=config.kafka_bootstrap)
    try:
        producer.send(topic, key=key.encode("utf-8"),
                      value=json.dumps(value).encode("utf-8"),
                      headers=[(k, v.encode("utf-8")) for k, v in (headers or {}).items()])
        producer.flush(10)
    finally:
        producer.close()
