# Local Kafka GitOps

`topics.local.yaml` is the local source-of-truth for Kafka topics.

Run on Linux from the repository root:

```bash
./kafka_gitops/apply_local.sh
```

By default, the script only creates or updates topics. If a topic is removed or renamed in `topics.local.yaml`, the old topic is kept in Kafka.

To delete topics that are not defined in `topics.local.yaml`:

```bash
DELETE_UNMANAGED_TOPICS=true ./kafka_gitops/apply_local.sh
```
