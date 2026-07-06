# Designing Data-Intensive Applications — Study Notes

---

## 1. Storage & Retrieval

### 1.1 Hash Indexes

- Simplest index: an in-memory hash map from key → byte offset in an append-only log file on disk.
- Writes are fast (append to log, update hash map). Reads are O(1) — look up offset, seek, read.
- **Compaction & segmentation**: log is broken into segments; a background process merges segments and discards overwritten/deleted keys (tombstones) to reclaim space.
- **Limitations**: hash map must fit in memory; range queries are inefficient (no ordering); crash recovery requires rebuilding the hash map or maintaining snapshots.
- Example: Bitcask (Riak's storage engine).

### 1.2 SSTables (Sorted String Tables) & LSM-Trees

- **SSTable**: a segment file where key-value pairs are sorted by key.
  - Merging segments is efficient (like mergesort) even if files are bigger than memory.
  - You don't need to keep an index of _all_ keys in memory — a sparse index (one key per few KB) suffices, since keys are sorted.
  - Records can be grouped into blocks and compressed before writing.
- **Building SSTables — LSM-Tree (Log-Structured Merge-Tree)**:
  1. Writes go to an in-memory balanced tree ("memtable").
  2. When memtable exceeds a threshold, write it to disk as a new SSTable segment (sequential write, sorted).
  3. To serve a read, check memtable, then most-recent-to-oldest SSTables.
  4. Periodically run merging/compaction in the background to combine segments and discard overwritten values.
  5. **Write-Ahead Log (WAL)**: an unsorted append-only log kept alongside the memtable, used to restore the memtable after a crash before it's flushed.
- **Bloom filters** are commonly used to avoid unnecessary disk lookups for keys that don't exist.
- **Compaction strategies**: size-tiered (newer/smaller SSTables merged into older/larger ones — used by Cassandra) vs leveled (LevelDB/RocksDB — smaller, more granular levels).
- Used by: LevelDB, RocksDB, Cassandra, HBase, Lucene (search indexes).
- LSM-trees favor **write throughput**; the log-structured append-only approach turns random writes into sequential ones.

### 1.3 B-Trees

- The most widely used indexing structure in relational DBs. Breaks the database into fixed-size **pages** (traditionally 4 KB), each page addressable by location, and pages reference each other.
- One page is the **root**; a lookup starts there and follows child pointers (fan-out is typically several hundred) until reaching a **leaf page** containing the actual values or pointers to rows.
- **Updates in place**: to change a value, find the leaf page, modify it, write it back to disk. Splitting a full page into two creates a new parent reference.
- **Write-Ahead Log (WAL) / redo log**: every B-tree modification is first appended to a WAL so the tree can be restored after a crash mid-write.
- **Concurrency control**: typically via latches (lightweight locks) protecting the tree's internal structure.
- **B-trees vs LSM-trees**:

|                     | B-Trees                                         | LSM-Trees                                                           |
| ------------------- | ----------------------------------------------- | ------------------------------------------------------------------- |
| Writes              | In-place, random I/O                            | Append-only, sequential I/O                                         |
| Write amplification | Lower generally, but WAL adds overhead          | Higher (compaction rewrites data multiple times)                    |
| Read performance    | Predictable, single lookup path                 | May need to check several structures + memtable                     |
| Space               | Can leave fragmented/unused space               | Better compression, less fragmentation                              |
| Concurrency         | Well-understood, mature                         | Compaction can contend with incoming writes, causing latency spikes |
| Transactions        | Locks attach naturally to tree structure (keys) | Harder to layer range locks over                                    |

### 1.4 Other Indexing Notes

- **Secondary indexes**: point to the location of the matching row(s); can be built from the same B-tree/LSM structures. Values in the index may be the row directly (clustered), just a reference (non-clustered), or a middle ground (covering index).
- **Multi-column and full-text indexes**: B-trees/LSM-trees only handle single-key lookups/ranges efficiently; multi-dimensional queries need structures like R-trees.
- **In-memory databases** (Redis, Memcached, VoltDB): durability achieved via logging, snapshots, or replication rather than disk-index structures; can be faster because they avoid encoding in-memory structures for disk.

---

## 2. Column-Oriented Storage

- **Row-oriented storage** (traditional OLTP): all values of one row stored together — efficient for reading/writing whole records.
- **Column-oriented storage** (analytics/OLAP, e.g. data warehouses): store all values from a single column together across rows.
  - Great when a query only touches a few of many columns (typical analytic queries) — avoids reading unneeded data from disk.
  - Enables strong **compression** (e.g. bitmap encoding, run-length encoding) because a column often has few distinct values.
  - Enables **vectorized processing**: operating on compressed column data directly using CPU-cache-friendly, SIMD-style operations.
- **Column-oriented sort order**: pick one or a few "sort key" columns to physically order data, which also improves compression on those columns and helps range queries.
- **Writes are harder**: in an LSM-style column store, writes go to an in-memory store first, then are merged into the columnar files on disk in bulk (similar idea to LSM-trees).
- **Materialized aggregates / data cubes**: precomputed summaries (e.g. sums, counts) cached like materialized views, common in warehouses.
- Examples: Vertica, Amazon Redshift, Apache Parquet, ORC.

---

## 3. Encoding & Data Formats

### 3.1 Why Encoding Matters

- In-memory representations (objects, pointers) differ from what's needed on disk or over the network → need to translate ("encode"/"serialize" and "decode"/"deserialize").
- Schema evolution matters: old and new code/schema versions may coexist during rolling upgrades — need **backward compatibility** (new code reads old data) and **forward compatibility** (old code reads new data).

### 3.2 Formats

- **JSON, XML, CSV**: human-readable, widely supported, but ambiguous about number types, no native binary support, and lack strict schemas.
- **Binary encodings** (MessagePack, BSON etc.): more compact but usually not schema-enforced.
- **Apache Thrift & Protocol Buffers**: require a schema (`.thrift` / `.proto` files); use field tags (numbers) instead of field names for compact binary encoding.
  - Backward/forward compatibility rules: you can add new optional fields with new tag numbers (old code ignores them); you must not change a field's tag or reuse an old tag; required fields make evolution hard, so fields are generally optional.
- **Apache Avro**:
  - Also schema-based (JSON-defined schema), but does **not** use field tags — it relies on the **exact schema used at write time** and the **schema the reader expects**, matching them up by field order/name at decode time.
  - Because there are no tag numbers, Avro's binary encoding is even more compact than Protobuf/Thrift.
  - **Writer's schema vs reader's schema**: the reader is given both; Avro resolves differences (missing fields get defaults, extra fields are ignored) — this is what allows schema evolution without regenerating code.
  - Well suited to **dynamically generated schemas** (e.g. dumping a database's tables, where columns can change often) since you don't need to recompile per-schema code — good fit for data lakes / large-scale ETL (e.g. used heavily in the Hadoop ecosystem).
  - Schema versions are typically tracked in a **schema registry**, and each record can carry a schema version/ID rather than the full schema.
- **Comparison**: Protobuf/Thrift favor RPC and strongly-typed generated code; Avro favors big-data pipelines with frequently evolving, dynamically-typed schemas.

---

## 4. Replication

### 4.1 Why Replicate

- Keep data geographically close to users (latency), allow the system to continue working if some nodes fail (availability), and scale out read throughput.

### 4.2 Replication Logs & Lag

- The leader records every write to a **replication log** (either a logical/row-based log, or a physical WAL-shipping mechanism as in Postgres); followers apply this log in order.
- **Replication lag**: followers apply writes asynchronously, so they can fall behind, causing **eventual consistency** issues:
  - **Read-your-writes consistency**: a user should see their own recent writes (e.g., route reads-after-write to the leader, or track a timestamp/version).
  - **Monotonic reads**: a user shouldn't see data go "back in time" across reads (e.g. always read from the same replica per user).
  - **Consistent prefix reads**: writes that happened in a certain order must be seen in that order (relevant with partitioning, since different partitions may replicate independently).

### 4.3 Single-Leader Replication

- All writes go to one leader; leader propagates changes to followers (sync, async, or semi-sync).
- Simple to reason about; but leader is a **single point of failure** for writes; failover (promoting a follower) is tricky — risk of split brain, lost writes, or requires manual intervention.

### 4.4 Multi-Leader Replication

- Multiple nodes accept writes (e.g., multi-datacenter setups, offline clients, collaborative editing).
- Better write availability and latency across regions, but introduces **write conflicts** that must be detected and resolved (last-write-wins, custom merge logic, CRDTs, or application-level resolution).
- Topologies: circular, star, all-to-all.

### 4.5 Leaderless Replication & Quorums

- Any replica can accept reads/writes (e.g., Dynamo-style systems — Cassandra, Riak, Voldemort).
- Client (or a coordinator) writes to **W** replicas and reads from **R** replicas out of **N** total.
- **Quorum condition**: if `W + R > N`, you're guaranteed to read at least one replica with the latest write (assuming no faulty nodes) — gives a tunable consistency/availability tradeoff.
- Read repair and anti-entropy background processes reconcile stale replicas.
- Conflicts detected via **version vectors** and resolved via causality tracking or LWW.
- Sloppy quorums + hinted handoff can increase availability during network partitions at the cost of the strict quorum guarantee.

---

## 5. Partitioning (Sharding)

- Splits large datasets across many nodes for scalability.
- **Key range partitioning**: good for range scans, but risk of hot spots if access patterns are skewed (e.g. sequential timestamp keys).
- **Hash partitioning**: better load distribution, but loses efficient range queries.
- **Consistent hashing**: minimizes data movement when nodes are added/removed.
- **Secondary indexes** across partitions: either "document-partitioned" (local index per partition, scatter-gather reads) or "term-partitioned" (global index, partitioned by term, faster reads but slower/more complex writes).
- **Rebalancing**: should avoid moving more data than necessary; avoid fixed hash mod N (breaks on resize) in favor of fixed number of partitions per node, or dynamic splitting.

---

## 6. Transactions & ACID

### 6.1 ACID

- **Atomicity**: a transaction either fully completes or fully aborts (not about concurrency — about handling partial failure mid-transaction).
- **Consistency**: application-defined invariants (e.g. account balances never negative) always hold — really an application property, not purely a database guarantee.
- **Isolation**: concurrently running transactions don't interfere with each other, as if executed serially (see below — actual isolation levels are usually weaker than true serializability).
- **Durability**: once committed, data survives crashes (write-ahead logs, replication, disk fsync).

### 6.2 Weak Isolation Levels

Real databases usually offer weaker-than-serializable isolation for performance, exposing different anomalies:

| Isolation Level                      | Prevents                                                                      | Still allows                    |
| ------------------------------------ | ----------------------------------------------------------------------------- | ------------------------------- |
| Read Uncommitted                     | Nothing much (dirty writes only)                                              | Dirty reads                     |
| Read Committed                       | Dirty reads, dirty writes                                                     | Non-repeatable reads, read skew |
| Snapshot Isolation (Repeatable Read) | Non-repeatable reads (via MVCC — each transaction sees a consistent snapshot) | Write skew, phantoms            |
| Serializable                         | All of the above                                                              | —                               |

- **Dirty read**: reading another transaction's uncommitted writes.
- **Dirty write**: overwriting another transaction's uncommitted write.
- **Read skew / non-repeatable read**: a transaction sees different values for the same query at different points in time (mitigated by snapshot isolation).
- **Lost update**: two transactions read-modify-write concurrently and one overwrites the other's update (fixed with atomic operations, explicit locking (`SELECT FOR UPDATE`), or automatic detection).
- **Write skew**: two transactions read overlapping data, then each writes to different objects, both individually valid but jointly violating an invariant — classic case: two doctors both go off-call because each independently sees "at least one other doctor is on call."
- **Phantoms**: a write in one transaction changes the result of another transaction's search query (e.g., inserting a new row that would have matched a previous `WHERE` clause).
- **MVCC (Multi-Version Concurrency Control)**: underlies snapshot isolation — keeps several versions of a row, tagged with transaction IDs, so readers never block writers.

### 6.3 Serializability

Two main real-world approaches to true serializable isolation:

1. **Two-Phase Locking (2PL)** _(concurrency control, NOT to be confused with 2-Phase Commit)_:
   - **Growing phase**: transaction acquires locks (shared for reads, exclusive for writes) as needed.
   - **Shrinking phase**: once a transaction releases any lock, it may not acquire new ones — locks are held until commit/abort.
   - Guarantees serializable execution but at a steep cost: reduced concurrency, and vulnerable to **deadlocks** (which the DB detects and resolves by aborting one transaction) and long tail latencies under contention.
   - **Predicate locks / index-range locks** are needed to prevent phantoms, since you must lock "all rows matching a condition," not just existing rows.

2. **Serializable Snapshot Isolation (SSI)**:
   - Optimistic concurrency control: transactions proceed on a snapshot without blocking, but the database tracks read/write dependencies.
   - At commit time, if it detects that a transaction _read_ data that was concurrently modified in a way that could break serializability (or that its writes affected another transaction's earlier reads), it **aborts** the offending transaction, which can be retried.
   - Much better performance under contention than 2PL for many workloads because reads never block writers and vice versa (this is what modern systems like PostgreSQL's `SERIALIZABLE` and FoundationDB use).

---

## 7. Time, Clocks & Ordering

### 7.1 Physical Clocks

- **Time-of-day clock** (e.g. `System.currentTimeMillis()`): returns wall-clock date/time synced against NTP servers. Can jump backward/forward due to NTP corrections or leap seconds — **not safe** for measuring elapsed time or ordering events across nodes.
- **Monotonic clock** (e.g. `System.nanoTime()`): guaranteed to always move forward, good for measuring durations/timeouts on a single machine, but the absolute value is meaningless across different machines (each machine has its own reference point).
- **Clock drift and skew**: physical clocks drift; even NTP-synced clocks can be tens of milliseconds off across machines, more if NTP sync fails silently.
- **Clock confidence intervals** (Google Spanner's **TrueTime**): rather than assuming a single precise timestamp, expose an interval `[earliest, latest]` with a guaranteed bound on uncertainty, and wait out the uncertainty window before committing to guarantee external consistency.

### 7.2 Logical Clocks & Ordering

- **Lamport timestamps**: a simple counter incremented on each event/message that provides a total order consistent with causality (if A happened-before B, A's timestamp < B's), but doesn't tell you about concurrency.
- **Vector clocks / version vectors**: track one counter per node, allowing you to detect true concurrency (neither event happened-before the other) — used to detect conflicting writes in leaderless/multi-leader replication.
- **Ordering guarantees**:
  - **Causality**: if event B depends on event A, all nodes must see A before B (a partial order).
  - **Total order broadcast**: all nodes deliver the same messages in the same order — equivalent to solving consensus, and is what's needed to implement things like a serializable single-leader replication log or a lock service.

---

## 8. Distributed System Faults

### 8.1 Partial Failures & Network Faults

- Distributed systems fail partially and unpredictably (unlike a single process, which either works or crashes): packets can be dropped, delayed, duplicated, or arrive out of order; you cannot distinguish a slow node from a dead one from a slow network.
- **Timeouts** are the main defense but force a tradeoff between detecting failure quickly (false positives under load) and avoiding premature failover (slow detection).

### 8.2 Byzantine Faults

- A node doesn't just crash or become unreachable — it can behave arbitrarily: sending corrupted or contradictory information, potentially maliciously ("Byzantine Generals Problem").
- Most mainstream databases assume **non-Byzantine** faults (crash-stop or crash-recovery) because it's expensive to protect against Byzantine faults, and in a single organization's datacenters, a compromised or arbitrarily malicious node is not the primary threat model.
- Byzantine fault tolerance matters in: aerospace/nuclear systems (radiation-induced bit flips), and in **blockchain/multi-party systems** where participants don't trust each other.
- Weak forms still worth defending against even in non-Byzantine settings: corrupted data on disk/network (checksums), buggy nodes sending garbage (validate inputs).

---

## 9. Linearizability

- The strongest single-object consistency model: once a write completes, **every** subsequent read (from any client, any replica) must return that value or a later one — the system behaves as if there's only a single copy of the data, and operations take effect atomically at some point between invocation and response.
- Different from **serializability** (which is about transactions/multi-object operations appearing to run in some serial order) — linearizability is about recency guarantees on individual reads/writes, and the two are independent concepts (a system can be one without the other; "strict serializability" combines both).
- **Why it's useful**: locking/leader election, uniqueness constraints, and cross-channel synchronization all typically need linearizability.
- **Cost**: linearizability requires coordination and typically increases latency, especially across a network partition (see **CAP theorem** below), because a replica must confirm it has the latest state before responding.

### CAP Theorem (brief)

- Under a network **P**artition, a system must choose between **C**onsistency (linearizability) and **A**vailability — you cannot have both.
- In practice, CAP is a narrow, often-misapplied result; more useful framing is a spectrum of tradeoffs (as in the PACELC extension: even without a Partition, you trade Latency vs Consistency).

---

## 10. Distributed Transactions & Consensus

### 10.1 Two-Phase Commit (2PC)

_(An atomic commitment protocol across multiple nodes/databases — not to be confused with Two-Phase Locking.)_

- Coordinates a transaction that spans multiple nodes/databases so that **all** participants commit or **all** abort (atomicity across nodes).
- **Phase 1 (prepare)**: coordinator asks every participant "can you commit?"; each participant persists (WAL) that it's ready and replies yes/no.
- **Phase 2 (commit/abort)**: if all said yes, coordinator tells everyone to commit; if any said no (or timed out), coordinator tells everyone to abort.
- **Blocking problem**: if the coordinator crashes after phase 1 but before sending the phase-2 decision, participants that voted "yes" must **block**, holding locks, until the coordinator recovers — 2PC is not fault-tolerant to coordinator failure without extra measures.
- Used for **XA transactions** across heterogeneous systems; also foundational to distributed databases' internal cross-partition transactions (with a more robust coordinator, e.g. using consensus for the coordinator itself).

### 10.2 Consensus (e.g., Paxos, Raft, Zab)

- Goal: get several nodes to **agree on one value/decision** despite failures and network issues, satisfying:
  - **Uniform agreement**: no two nodes decide differently.
  - **Integrity**: no node decides twice.
  - **Validity**: the decided value was actually proposed by some node.
  - **Termination**: every non-crashed node eventually decides (liveness — requires a majority of nodes to be up).
- Solves total order broadcast, leader election, and atomic commit in a fault-tolerant way (unlike naive 2PC, a consensus-based commit protocol can make progress even if the "coordinator"/leader crashes, since a new leader can be elected by majority vote).
- Practical systems (**Raft, Multi-Paxos, Zab**) elect a leader and replicate a log via majority quorum acknowledgment — this is what underlies systems like etcd, ZooKeeper, and consensus-based databases (e.g. CockroachDB, Spanner use Paxos/Raft-like protocols per partition).
- **FLP impossibility result**: in a fully asynchronous system with even one faulty node, consensus cannot be guaranteed to terminate — practical algorithms sidestep this using timeouts (partial synchrony assumptions).

---

## 11. Caching

### Cache Types / Patterns

- **Cache-aside (lazy loading)**: application checks cache first; on miss, reads from the DB and populates the cache. Simple, resilient to cache failures, but first request always misses and stale data possible.
- **Read-through cache**: the cache itself sits in front of the DB and loads data on a miss transparently to the application (application only ever talks to the cache).
- **Write-through cache**: writes go to the cache and the underlying store synchronously (in the same operation) — keeps cache consistent, but adds write latency.
- **Write-back (write-behind) cache**: writes go to the cache and are asynchronously flushed to the underlying store later — very low write latency, higher throughput, but risk of data loss if the cache node fails before flushing.
- **Write-around cache**: writes go directly to the store, bypassing the cache; the cache is only populated on reads — avoids filling the cache with data that may never be read again, at the cost of a miss on first read after a write.
- **Eviction policies**: LRU, LFU, FIFO, TTL-based expiry — govern what gets removed when the cache is full.
- **Distributed caching concerns**: cache invalidation strategy, thundering herd (many clients missing simultaneously and hammering the DB — mitigated with locks/request coalescing or early refresh), and consistency between cache and source of truth.

---

## 12. Materialized Views

- A **materialized view** is a precomputed, physically stored result of a query (as opposed to a normal "virtual" view which is just a shorthand for the underlying query, recomputed each time).
- Useful for expensive aggregations/joins that are read often but the underlying data changes less often than it's read.
- Must be **kept up to date** as underlying data changes — either via periodic full refresh, or **incremental maintenance** (updating just the affected rows), which is conceptually similar to derived data maintenance in stream processing / event sourcing pipelines.
- Tradeoff: faster reads, but write amplification (every write may need to update the view) and potential staleness between refreshes (a bounded staleness / eventual consistency concern, connecting back to replication lag concepts).

---

## 13. A Few More High-Value DDIA Concepts

### 13.1 Change Data Capture (CDC)

- CDC is the practice of capturing every row-level insert/update/delete made to a database — usually by tailing its internal **replication log** (the same log format used to feed replicas) — and publishing those changes as a stream of events to other systems.
- This effectively turns the system-of-record database into a **source of truth**, with every other representation of the data (search index, cache, data warehouse, materialized view, recommendation engine) treated as a **derived dataset** that can always be rebuilt by replaying the change stream from the start.
- **Log compaction**: since a derived system usually only needs the _latest_ value for each key (not the full history), the change stream can be compacted — obsolete updates to the same key are thrown away, keeping only the most recent value per key (Kafka's log compaction feature is built exactly for this).
- **Initial snapshot problem**: a newly added consumer can't just start tailing the log from "now" — it also needs a snapshot of the existing data as of some known position in the log, then apply changes from that position onward. This mirrors the same snapshot-plus-log approach used for setting up new replicas.
- **Contrast with dual writes**: without CDC, teams often try to keep a cache/index/warehouse in sync by having the application write to both the database and the other system in the same request path ("dual writes"). This is fragile — a crash between the two writes leaves them inconsistent, and there's no ordering guarantee if writes race. CDC avoids this by deriving everything from a single authoritative, ordered log.
- Common implementations: Debezium, Kafka Connect source connectors, Postgres logical replication slots, MySQL binlog readers, DynamoDB Streams.

### 13.2 Event Sourcing

- Event sourcing takes CDC's idea a step further as an **application design philosophy**: instead of storing only the current mutable state (e.g., "account balance = $80"), the application stores every state change as an immutable **event** ("deposited $100", "withdrew $20") in an append-only event log.
- Current state is never stored directly — it's **derived** by replaying all events for an entity from the beginning (or from a periodic snapshot plus the events since).
- **Benefits**:
  - Full audit trail / history "for free" — you can always answer "how did we get here?"
  - Easy to add new derived views later: since the raw events are preserved, a new read-model (e.g. a new report, a new index) can be built by replaying history, even if nobody anticipated that need when the events were first recorded.
  - Natural fit with CQRS (Command Query Responsibility Segregation) — writes append events, while one or more separate read-optimized views are built from those events.
- **Costs**:
  - Reconstructing current state by replaying a long event history can be slow, so systems typically use periodic **snapshots** and only replay events since the last snapshot.
  - Event schemas evolve over time, so old events must remain interpretable by new code (same forward/backward-compatibility concerns as in Section 3).
  - Deletion/GDPR-style "right to be forgotten" is awkward with an immutable log — usually solved via crypto-shredding (encrypt each record with its own key, then destroy the key) rather than literally deleting history.

### 13.3 Batch vs Stream Processing

- These are the two broad paradigms for processing data at scale, sitting on either side of a spectrum of "how bounded and how fresh does the input need to be."
- **Batch processing** (MapReduce, Apache Spark, Hadoop):
  - Operates over a large, **bounded** dataset (e.g. "all of yesterday's logs") known in full ahead of time.
  - Typically reads input files, applies a distributed computation, and writes a new output dataset — output is deterministic and reproducible by rerunning the same job on the same input.
  - Optimized for **throughput**, not latency: jobs might run for minutes to hours, but process huge volumes efficiently and can freely retry failed sub-tasks without visible side effects (since output isn't consumed until the whole job finishes).
  - Common use: nightly ETL jobs, building/refreshing large materialized views or search indexes from scratch.
- **Stream processing** (Kafka Streams, Apache Flink, Spark Structured Streaming):
  - Operates over an **unbounded**, continuously arriving dataset — there's no "end" to wait for.
  - Processes each event (or small micro-batches) as it arrives, enabling **low-latency** derived views (seconds or less, vs. batch's hours).
  - Must explicitly handle concerns batch mostly avoids: **out-of-order arrival** (events can arrive later than events that logically happened after them, due to network delays), and **windowing** (grouping events into tumbling, sliding, or session windows for aggregation, plus deciding how long to wait for late-arriving events before finalizing a window — the "watermark" concept).
  - Common use: real-time dashboards, fraud detection, live materialized-view maintenance (tying back to Section 12), CDC-consumer pipelines (Section 13.1).
- **Lambda architecture**: an older pattern that runs both a batch pipeline (for eventually-correct, comprehensive results) and a stream pipeline (for immediately-available approximate results) in parallel, merging them at read time — largely being replaced today by unified stream-processing engines that can handle both bounded and unbounded data with one codebase (e.g. Flink, Spark).

### 13.4 Exactly-Once vs At-Least-Once Processing Semantics

- When a message/event is processed and the processor crashes or a network call times out partway through, the sender generally can't tell whether the operation completed — so it must choose between two flawed defaults:
  - **At-most-once**: don't retry on uncertainty → risk of **lost** messages/updates, but never duplicates.
  - **At-least-once**: retry on any uncertainty → risk of **duplicate** processing, but never silently lost messages. This is the more common default for reliable systems, since losing data is usually worse than processing it twice.
- **True exactly-once delivery is impossible to guarantee in a fully general, fault-tolerant, asynchronous system** — you cannot simultaneously guarantee a message is delivered and processed exactly one time when the network and processes themselves are unreliable (closely related to the FLP impossibility flavor of reasoning in Section 10.2).
- What's actually achievable and used in practice is **"effectively-once" processing**: the _delivery_ may happen more than once (at-least-once), but the _effect_ on the system's state is made to look like it happened exactly once. Two main techniques:
  1. **Idempotent operations** (see 13.5) — reprocessing the same message twice has no additional effect.
  2. **Transactional / exactly-once processing frameworks** (e.g. Kafka's transactional producer/consumer API) — atomically commit "I consumed message X" together with "I produced output Y" as a single transaction, so a crash-and-retry either redoes both or neither, never just one.
- This connects directly back to **Atomicity** (Section 6.1) and **Distributed Transactions** (Section 10.1) — effectively-once processing is really just atomic commitment applied to a stream-processing pipeline instead of a classic database transaction.

### 13.5 Idempotence

- An operation is **idempotent** if applying it multiple times has exactly the same effect as applying it once — e.g. "set balance to $80" is idempotent (repeating it is harmless), while "increment balance by $20" is **not** (repeating it changes the outcome each time).
- Idempotence is the cheapest, most widely applicable defense against the uncertainty baked into at-least-once delivery (13.4) and network retries in general: if the client can't tell whether its request succeeded, it can simply **safely retry** an idempotent request without needing any special exactly-once machinery.
- **Common techniques to make naturally non-idempotent operations idempotent**:
  - **Client-generated unique request/idempotency keys**: the client attaches a unique ID to a request (e.g. "payment request #abc123"); the server keeps a record of processed IDs and, if it sees the same ID again, returns the original result instead of re-executing the operation.
  - **Conditional/compare-and-set writes**: e.g. "update this row only if its version is still 5," so a duplicate retry that arrives after the first succeeded simply becomes a no-op (version mismatch) instead of applying twice.
  - **Expressing operations as absolute values / set operations** rather than deltas where possible (as in the balance example above), since sets are naturally idempotent while increments aren't.
- Idempotence is a recurring, load-bearing assumption throughout distributed systems design — it's what makes retry-on-timeout, at-least-once delivery, and CDC/event replay all safe to reason about.

### 13.6 Fan-Out

- **Fan-out** describes the number of downstream nodes/consumers/replicas that a single write, message, or event must be propagated to.
- In **leaderless replication** (Section 4.5), fan-out is the number of replicas (`N`) a single write is sent to — higher fan-out increases durability and read availability but also increases the write's latency tail (you typically wait for the slowest of the `W` acknowledgments) and network/storage cost.
- In **pub-sub / stream processing** (Kafka topics, event buses), fan-out is the number of independent consumer groups reading the same topic — because each event is retained and can be read by many independent consumers without affecting each other (unlike a traditional queue where a message is removed once consumed), fan-out here is essentially "free" from the producer's perspective, which is a key reason log-based pub-sub systems are popular for feeding many derived views (search index, cache, warehouse, etc.) from one CDC stream simultaneously.
- **High fan-out systems** (e.g. a social network "celebrity" post needing to reach millions of followers' feeds) present a classic scaling challenge: **fan-out on write** (push the update to every follower's feed immediately — expensive for high-fan-out producers) vs **fan-out on read** (compute each follower's feed at read time by pulling from everyone they follow — expensive for high-fan-in readers). Real systems often use a hybrid: fan-out-on-write for typical users, fan-out-on-read for celebrities/high-fan-out accounts.
- This ties back to the caching (Section 11) and materialized view (Section 12) sections — a "feed" is itself a materialized view, and the fan-out strategy is really a decision about _when_ that view gets updated (write time vs. read time).
