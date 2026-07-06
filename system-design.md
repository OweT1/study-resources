# The Complete System Design Interview Guide

## How to use this guide

Every system design interview follows roughly the same shape, no matter what you're asked to build:

1. **Clarify requirements** — functional (what should it do) and non-functional (how fast, how available, how consistent).
2. **Estimate scale** — back-of-envelope math on users, traffic, storage, bandwidth.
3. **Sketch a high-level design** — boxes and arrows: clients, load balancers, services, databases, caches.
4. **Deep-dive into components** — pick the 2-3 hardest parts and go deep (data model, algorithm, failure handling).
5. **Address bottlenecks and trade-offs** — nothing is free; show you know what you're giving up.

This guide is split into two halves that map to steps 2 and 3-4:

- **Part 1: Capacity Estimation** — how to go from "we have X users" to concrete numbers for QPS, storage, and bandwidth, at every scale from thousands to billions of users.
- **Part 2: Building Blocks** — the components you'll assemble into a design (load balancers, caches, CDNs, queues, databases, etc.), what each one is _for_, when to reach for it, and the key algorithms behind it.

Think of Part 1 as figuring out how big the swimming pool needs to be, and Part 2 as the catalog of pipes, pumps, and filters you can use to build it.

---

# PART 1: ESTIMATING REQUIREMENTS FROM USER COUNT

## The numbers you should just know

Before estimating anything, it helps to have a few reference numbers memorized. These are the "latency numbers every programmer should know," approximately:

| Operation                          | Approximate Time        |
| ---------------------------------- | ----------------------- |
| L1 cache reference                 | 1 ns                    |
| Branch mispredict                  | 5 ns                    |
| L2 cache reference                 | 7 ns                    |
| Mutex lock/unlock                  | 25 ns                   |
| Main memory reference              | 100 ns                  |
| Compress 1KB with Zippy            | 3,000 ns (3 μs)         |
| Send 1KB over 1 Gbps network       | 10,000 ns (10 μs)       |
| Read 1MB sequentially from memory  | 250,000 ns (250 μs)     |
| Round trip within same data center | 500,000 ns (500 μs)     |
| Read 1MB sequentially from SSD     | 1,000,000 ns (1 ms)     |
| Disk seek                          | 10,000,000 ns (10 ms)   |
| Read 1MB sequentially from disk    | 20,000,000 ns (20 ms)   |
| Send packet CA → Netherlands → CA  | 150,000,000 ns (150 ms) |

**The takeaway (analogy):** if a single CPU cycle were 1 second, then a round trip to another continent would take about 5 years. This is why every design decision about _where data lives relative to where it's needed_ (cache vs. DB, same region vs. cross-region) matters so much — network and disk are catastrophically slow compared to memory.

Also worth memorizing:

- **1 day ≈ 86,400 seconds** (round to 100,000 for quick math)
- **1 month ≈ 2.5 million seconds**
- A single modern server can typically handle **~1,000-10,000 requests/sec** for simple operations, far less for heavy computation.
- A single relational DB instance can typically handle a few thousand QPS for reads (more with replicas/caching), and far fewer writes.

## The general estimation framework

For any system, regardless of scale, you walk through the same chain of multiplications:

```
Total Users → Active Users (DAU/MAU) → Actions per user per day
    → Total actions/day → Requests per second (QPS)
    → Size per request/object → Storage per day → Storage over N years
    → Bandwidth (ingress + egress)
    → Number of servers needed (QPS ÷ capacity per server)
```

**Key ratios to ask about or assume:**

- **DAU/MAU ratio**: For a "sticky" app (social media, messaging) this might be 50-70%. For a less sticky app (e-commerce), maybe 10-20%.
- **Read/write ratio**: Most systems are read-heavy. Twitter-like feeds might be 100:1 read:write. A logging system might be write-heavy (1:1 or write > read).
- **Peak factor**: Average QPS isn't enough — always multiply by a peak factor (commonly **2x-3x** average) to size for traffic spikes (morning login rush, viral event, etc.)

**The 80/20 or "seconds in a day" shortcut:** Instead of dividing daily actions by 86,400, many candidates approximate a day as **100,000 seconds** — it makes mental math much faster and is only off by ~15%, which is fine for an estimate that's meant to be order-of-magnitude correct, not exact.

Now let's walk through this at each scale.

---

## Scale Tier 1: Thousands of users (1K – 100K)

**Analogy:** This is a corner store, not a supermarket chain. You can serve everyone with a single register (one server) and a small storage room (one database).

At this scale:

- **DAU**: If 100K total users with 30% DAU ratio → 30,000 DAU.
- **QPS**: Say each user makes 10 requests/day → 300,000 requests/day → ÷ 100,000 sec ≈ **3 QPS average**. Even with a 3x peak factor, that's ~10 QPS.
- **Servers needed**: **A single application server** (maybe two for redundancy) easily handles this. A single database instance (even unsharded, un-replicated) is often fine.
- **Storage**: Even generous per-user data (say 10 KB/user of metadata) is only ~1 GB total. Trivial — fits comfortably on one machine's disk, doesn't even need object storage tiering.
- **Caching**: Often unnecessary at this scale — a well-indexed single database can serve reads directly. If you add caching, it's usually for latency polish, not because the DB is falling over.
- **What actually matters at this scale**: correctness, maintainability, cost efficiency (don't over-engineer). Interviewers rarely ask you to design for this tier alone, but it's the baseline you scale up from.

**What NOT to do:** Don't reach for Kafka, sharded databases, or multi-region replication here. A monolith with a single Postgres/MySQL instance and maybe a load balancer for redundancy (not for load) is the right answer. Over-engineering at this stage is itself a red flag in interviews.

---

## Scale Tier 2: Tens of thousands to Millions (100K – 10M)

**Analogy:** Now you're a regional supermarket chain. One register isn't enough, and one warehouse might not be either — you need multiple checkout lanes (multiple app servers behind a load balancer) and maybe a bigger warehouse with an organized layout (a properly indexed, possibly replicated database).

Let's estimate for **10 million total users**, a common interview anchor number (think "Twitter at medium scale" or "Instagram in its early years").

- **DAU**: 10M users × 20% DAU ratio (moderate stickiness) = **2M DAU**.
- **Actions**: Say each DAU performs 5 read actions and 1 write action/day (typical for a feed-based app).
  - Reads: 2M × 5 = 10M reads/day → ÷100,000s ≈ **100 QPS average reads**, ~300 QPS peak.
  - Writes: 2M × 1 = 2M writes/day → ÷100,000s ≈ **20 QPS average writes**, ~60 QPS peak.
- **Servers**: If one app server handles ~1,000 QPS for simple logic, you need only a handful of app servers — but you now genuinely need a **load balancer** to distribute across them, and probably 2+ availability zones for redundancy.
- **Database**:
  - A single primary DB can often still handle 60 write QPS fine.
  - But 300 QPS of reads is where you start adding **read replicas** and/or a **cache layer** (like Redis) in front of the DB — especially if reads are skewed (some content is read far more than others, i.e. "hot" data), which is extremely common (see caching section).
- **Storage**: If each write creates a ~1 KB record (e.g., a tweet/post), 2M writes/day × 1 KB = **2 GB/day**, or under 1 TB/year. Still fits on a single well-provisioned disk, though you'd want replication for durability, not just capacity.
- **Media/blobs**: If users upload images (say 500 KB average, 10% of writes include an image): 2M × 10% × 500 KB = **100 GB/day** of media. This is where **object storage** (S3-like) and a **CDN** start to make sense, since media is large, immutable once written, and often re-read many times (viral posts).
- **Bandwidth**: 100 GB/day of media ÷ 100,000 sec ≈ 1 MB/s average egress just for media — manageable, but you'd want a CDN if content is geographically distributed or has hot spots (a viral post read by 1M people vs. written once).

**What changes here vs Tier 1:** You now genuinely justify a **load balancer**, a **cache**, possibly **read replicas**, and **object storage + CDN** for media. This is the tier where most "design Instagram/Twitter-lite" interview answers should land unless the interviewer says "assume global scale."

---

## Scale Tier 3: Tens of Millions to Hundreds of Millions (10M – 500M)

**Analogy:** You're now a national or multinational retail chain. A single warehouse can't serve the whole country fast enough — you need regional distribution centers (data center regions), and your checkout system needs to be modular enough that any single lane going down doesn't stop sales (redundancy, failover, horizontal scaling everywhere).

Let's anchor on **100 million total users** (think "Twitter/Reddit at large scale").

- **DAU**: 100M × 30% (assume decent stickiness) = **30M DAU**.
- **Reads**: Say 20 reads/user/day (feed refreshes, scrolling) → 30M × 20 = 600M reads/day → ÷100,000s = **6,000 QPS average**, ~15,000-20,000 QPS peak.
- **Writes**: Say 2 writes/user/day → 30M × 2 = 60M writes/day → ÷100,000s = **600 QPS average**, ~1,500-2,000 QPS peak.
- **Servers**: At 15-20K QPS peak read traffic, and assuming ~1,000-2,000 QPS per app server for moderately complex logic, you need **dozens of application servers**, definitely across multiple availability zones, likely multiple regions if users are global (for latency — remember that transcontinental round trip is ~150ms).
- **Database**: A single database instance, even with replicas, typically cannot handle 1,500-2,000 write QPS combined with the query complexity of a real app. This is the tier where **sharding/partitioning** the database becomes necessary (see Part 2), not optional. You'd typically shard by user ID or a similar key.
- **Caching becomes mandatory, not optional**: With 15-20K read QPS, hitting the database directly for every read is untenable. A distributed cache (Redis/Memcached cluster) in front of the DB, often absorbing 80-95% of read traffic (a very common real-world hit rate for feed/timeline-style data), is standard.
- **Storage**: 60M writes/day × ~1 KB = 60 GB/day of core data → **~22 TB/year**. This alone justifies moving old/cold data to cheaper storage tiers, and definitely justifies horizontal partitioning.
- **Media**: If 15% of writes have a 500 KB photo: 60M × 15% × 500KB = **4.5 TB/day** of media. Over a year, that's ~1.6 PB. This absolutely requires object storage (S3-class) with a CDN in front — serving this from application servers directly would be a design smell.
- **Message queues**: At this scale, synchronous request/response chains for every action (e.g., "send notification to all followers when someone posts") start to break down. This is where **async processing via message queues** (Kafka, SQS) becomes important — e.g., fan-out-on-write for a social feed is done via background workers consuming from a queue, not inline in the write request.

**What changes here:** Sharding, distributed caching as a first-class requirement (not an optimization), multi-region deployment for latency, message queues for async fan-out, and CDN + object storage for media are now core parts of the design, not "nice to haves."

---

## Scale Tier 4: Billions (1B+)

**Analogy:** You're now designing something like global logistics for a company like Amazon or the postal systems of every country combined — data and requests are flowing from everywhere to everywhere, constantly, and no single facility (or even single country's worth of facilities) can be a single point of failure or bottleneck. Every layer must be horizontally scalable and geographically distributed by default.

Anchor on **2 billion total users**, ~1 billion DAU (think Facebook, WhatsApp, YouTube scale).

- **DAU**: ~1B.
- **Reads**: Say 50 reads/user/day for a heavy content app → 1B × 50 = 50B reads/day → ÷100,000s = **500,000 QPS average**, potentially **1-2 million QPS at peak**.
- **Writes**: Say 5 writes/user/day → 1B × 5 = 5B writes/day → ÷100,000s = **50,000 QPS average**, **~150,000 QPS peak**.
- **Servers**: You're now talking about **tens of thousands of servers** globally. No single load balancer or single region can handle this — you need:
  - **Global load balancing / DNS-based routing** (e.g., GeoDNS, Anycast) to route users to their nearest region.
  - **Regional data centers**, each independently capable of serving a large fraction of traffic.
- **Database**: Data is **sharded across thousands of database nodes**, often with multiple sharding strategies for different data types (user data sharded by user ID, message data sharded by conversation ID, etc.). Cross-shard queries become a real design challenge you must explicitly address (e.g., via denormalization, secondary indexes maintained by async jobs, or dedicated search infrastructure like Elasticsearch).
- **Caching**: Multi-layer caching: **CDN edge caches** (closest to user) → **regional caches** → **application-level in-memory caches** → **distributed cache clusters** (Redis Cluster, etc.) → database. Cache invalidation across this many layers becomes a genuinely hard problem (see cache invalidation strategies in Part 2).
- **Storage**: At 5B writes/day × ~1 KB, that's **5 TB/day** just for core records, ~1.8 PB/year, and that's before media. Media at this scale is measured in **exabytes** over the system's lifetime (YouTube stores exabytes of video). This requires:
  - **Erasure coding / replication** for durability across data centers.
  - **Tiered storage**: hot data on SSD, warm on HDD, cold/archival on tape or cold object storage (e.g., S3 Glacier-class) with lifecycle policies to auto-transition data.
- **Bandwidth**: With this much media, **CDN is not optional — it's the primary interface** most users interact with for media delivery, with origin servers only serving cache misses (ideally <5-10% of requests).
- **Consistency trade-offs become central**: At this scale, strict global consistency (e.g., a single global lock or global transaction) is often physically impossible to do with low latency (network round-trip physics). Systems lean heavily on the **CAP theorem** trade-offs — most large-scale systems choose **AP** (availability + partition tolerance) with **eventual consistency** for most data, reserving strong consistency for a small subset of operations that truly need it (e.g., financial transactions, unique username registration).
- **Failure is the default assumption, not the exception**: with tens of thousands of machines, hardware failures happen constantly (statistically, _something_ is always failing). Systems must be designed assuming continuous partial failure — redundancy, health checks, automatic failover, and graceful degradation are core requirements, not edge cases.

**What changes here:** Everything becomes distributed by necessity — global routing, sharding at massive scale, multi-tier caching, tiered storage, eventual consistency as the default, and "failure is normal" as a design principle.

---

## Quick-reference table: scaling summary

| Users      | DAU (approx) | Peak QPS (approx) | DB Strategy                     | Caching            | Media/CDN                     | Consistency                                 |
| ---------- | ------------ | ----------------- | ------------------------------- | ------------------ | ----------------------------- | ------------------------------------------- |
| 1K – 100K  | Hundreds–30K | <10-50            | Single instance                 | Optional           | Rarely needed                 | Strong, trivial                             |
| 100K – 10M | 20K–2M       | 100s–1,000s       | Primary + read replicas         | Recommended        | Starts to matter for media    | Mostly strong                               |
| 10M – 500M | 3M–150M      | 1,000s–20,000s    | Sharded, replicas mandatory     | Mandatory          | Mandatory for media           | Mixed (strong for core, eventual for feeds) |
| 1B+        | 300M–1B+     | 100,000s–millions | Massively sharded, multi-region | Multi-tier, global | Core UX layer, tiered storage | Mostly eventual, strong where required      |

**Interview tip:** Always state your assumptions out loud ("I'll assume 20% DAU/MAU ratio and a 3x peak multiplier — let me know if you'd like different numbers"). The exact numbers matter far less than showing you know _which levers_ move the estimate and _what breaks_ as you cross each order of magnitude.

---

# PART 2: SYSTEM DESIGN BUILDING BLOCKS

For each component below: **what it is, the analogy, when to use it, when NOT to use it, and the key algorithms/strategies** behind it.

---

## 1. Load Balancers

**What it does:** Distributes incoming traffic across multiple servers so no single server is overwhelmed, and so traffic can keep flowing if one server dies.

**Analogy:** A host at a restaurant entrance who looks at all the tables (servers) and decides who sits where — instead of everyone crowding around one table while others sit empty.

**When to use:** Almost always, once you have more than one server instance — even at modest scale, for redundancy alone (not just load).

**Layer 4 vs Layer 7:**

- **L4 (transport layer)**: Routes based on IP/TCP info only — fast, doesn't inspect content. Good for raw throughput, works for any protocol.
- **L7 (application layer)**: Understands HTTP — can route based on URL path, headers, cookies (e.g., `/api/video/*` → video service, `/api/users/*` → user service). Slightly more overhead, but much more flexible. Most modern API-based systems use L7.

**Key algorithms:**

- **Round robin**: Requests distributed sequentially, one after another to each server. Simple, works well if servers are equally powerful and requests are equally cheap.
- **Weighted round robin**: Like round robin, but more powerful servers get proportionally more requests.
- **Least connections**: Send the new request to whichever server currently has the fewest active connections. Better than round robin when request processing times vary a lot.
- **IP hash**: Hash the client's IP to consistently route the same client to the same server — useful for maintaining session affinity ("sticky sessions") without a shared session store.
- **Consistent hashing** (see dedicated section below): Used when you want requests for the _same key_ (not just same client) to consistently land on the same backend — critical for cache servers and sharded services.

**Failure handling:** Load balancers continuously run **health checks** (periodic pings/HTTP checks) against backend servers and stop routing to any that fail, automatically routing around outages.

**When NOT to use / limits:** The load balancer itself can become a single point of failure or bottleneck — in practice you run load balancers in redundant pairs (active-passive or active-active), and at the very top of the stack you often use **DNS-based load balancing** (like GeoDNS or Anycast) to distribute across entire regions, since one physical load balancer can't sit in front of the whole planet.

---

## 2. API Gateway

**What it does:** A single entry point that sits in front of your backend services and handles cross-cutting concerns — authentication, rate limiting, request routing, request/response transformation, logging — so individual services don't have to reimplement all of it.

**Analogy:** The reception desk of a large office building. Visitors (requests) check in once at reception (auth, badge/rate-limit check), and reception directs them to the right department (routing to the correct microservice), instead of every department having its own separate front desk doing the same checks.

**When to use:** Almost essential once you move to a **microservices architecture** — without it, every service needs to duplicate auth, rate limiting, logging, etc. Also very useful even with a single backend, to centralize things like SSL termination, request throttling, and API versioning.

**What it typically handles:**

- **Authentication/authorization** (validate tokens, API keys before forwarding).
- **Rate limiting / throttling** (see algorithms below).
- **Request routing** (path-based, similar to L7 load balancing, but with awareness of API versions/contracts).
- **Protocol translation** (e.g., REST from client → gRPC internally).
- **Response caching** for cacheable GET endpoints.
- **Request/response logging and metrics** for observability.

**When NOT to use:** For a simple monolith with a handful of endpoints, a full API Gateway product can be overkill — a load balancer + in-app middleware may suffice. It also adds an extra network hop (latency) and can become a bottleneck/single point of failure if not scaled and made redundant itself.

---

## 3. Content Delivery Network (CDN)

**What it does:** Caches static (and sometimes dynamic) content at servers physically distributed close to end users, so requests don't have to travel all the way back to the origin server/data center.

**Analogy:** Instead of every customer in every city ordering a book directly from one central warehouse (slow, especially if they're far away), you place copies of the popular books in local libraries in every city. Most people get the book from their local library (edge cache) instead of waiting for a shipment from the central warehouse (origin).

**When to use:** Any time you're serving static assets (images, videos, JS/CSS bundles, downloadable files) to a geographically distributed audience. Also increasingly used for caching semi-dynamic API responses at the edge.

**Two main strategies:**

- **Push CDN**: You proactively upload content to the CDN whenever it's created/updated. Good for content you know in advance and that doesn't change often (e.g., a video right after upload/transcode). You control exactly what's cached and when.
- **Pull CDN**: The CDN fetches content from your origin server the _first_ time it's requested from a given edge location, then caches it for subsequent requests (with a TTL). Good for large catalogs of content where you don't know in advance what will be popular — the cache naturally fills with whatever's actually being requested. Downside: the very first request from a new region is slow (**cache miss** to origin), and if content changes, you need cache invalidation (or versioned URLs).

**When NOT to use:** Highly personalized, rapidly-changing, per-user data (e.g., your private inbox) isn't a good CDN candidate — though even here, CDNs can still help by caching the _shell_ of the page/app while dynamic content is fetched separately via API calls.

**Key related concept — cache busting:** Since CDNs cache based on URL, a common pattern is to include a content hash or version number in the file name/URL (e.g., `app.a1b2c3.js`) so that updating the file changes the URL, guaranteeing clients get the new version without needing to explicitly invalidate the old cached copy.

---

## 4. Caching

**What it does:** Stores a copy of frequently-accessed data in fast storage (usually RAM) so future requests for the same data can be served without hitting a slower system (usually a database or an expensive computation).

**Analogy:** Keeping your most-used spices on the counter instead of walking to the pantry every time you cook. The pantry (DB) still holds everything, but the few things you use constantly live somewhere much faster to reach.

**Where caches live (in order from closest to user to farthest):**

1. **Client-side/browser cache**
2. **CDN edge cache**
3. **Application-level cache (in-process memory)**
4. **Distributed cache** (Redis, Memcached) — shared across app server instances
5. **Database query cache / buffer pool**

**Caching strategies (how the cache and DB stay in sync):**

- **Cache-aside (lazy loading)**: App checks cache first; on a miss, reads from DB, then writes the result into the cache for next time. Most common pattern. Simple, and only caches data that's actually requested. Downside: first request for any key is always a miss (slow), and there's a small window where cache and DB could be inconsistent if not handled carefully.
- **Write-through**: Every write goes to the cache _and_ the DB at the same time (synchronously), keeping them always in sync. Reads are always fast and fresh. Downside: writes are slower (two systems to update), and you cache data that might never be read.
- **Write-behind (write-back)**: Writes go to the cache immediately and are asynchronously flushed to the DB later (batched). Very fast writes. Downside: risk of data loss if the cache crashes before flushing, and added complexity.
- **Read-through**: Similar to cache-aside, but the cache itself (not the application) is responsible for loading from the DB on a miss — the app always just talks to the cache.

**Eviction policies (what to remove when the cache is full):**

- **LRU (Least Recently Used)**: Evict the item that hasn't been accessed for the longest time. Good general-purpose default — assumes recently used data is likely to be used again soon.
- **LFU (Least Frequently Used)**: Evict the item accessed the fewest number of times overall. Better than LRU when access frequency matters more than recency (e.g., a piece of content that's read constantly but wasn't touched in the last minute shouldn't be evicted).
- **FIFO**: Evict whatever was added first, regardless of usage. Simple but usually suboptimal — rarely the right choice unless data has a natural, meaningful order (e.g., you specifically want time-ordered expiry).
- **TTL (Time To Live)**: Independent of the above — items expire automatically after a fixed time regardless of usage, useful for data that's only valid for a certain period (e.g., a session token, a stock price).

**Cache invalidation — "there are only two hard things in computer science":**
This is famously one of the hardest problems in caching. Approaches:

- **TTL-based expiry**: Simplest — just let stale data expire naturally after a set time. Works when some staleness is acceptable.
- **Write-through invalidation**: When data changes, explicitly delete or update the corresponding cache key at write time.
- **Event-based invalidation**: Use a message queue/pub-sub so that whenever underlying data changes, an event fires that all relevant caches subscribe to and invalidate accordingly — necessary when many cache instances/layers need to stay in sync.

**When NOT to use caching:** Data that changes on every read anyway (no reuse benefit), or data where staleness is unacceptable even for milliseconds (e.g., an account balance right before a withdrawal — though even here, caching is often used carefully with strong invalidation).

---

## 5. Databases: SQL vs NoSQL, Replication, and Sharding

### SQL vs NoSQL

**Analogy:** A SQL database is like a strictly organized filing cabinet — every folder (row) in a drawer (table) must follow the same labeled format (schema), and you can cross-reference between drawers (joins) reliably. A NoSQL store is more like a set of labeled boxes where each box can hold whatever you want, organized for extremely fast retrieval by a specific key, at the cost of flexible cross-referencing.

- **Use SQL (relational)** when: data is structured and relationships between entities matter (orders ↔ customers ↔ products), you need strong consistency and transactions (ACID), and your scale doesn't yet require extreme horizontal partitioning (though modern systems like Vitess/CockroachDB do scale SQL horizontally).
- **Use NoSQL** when: you need massive horizontal scale with simple access patterns (fetch by key), your data model is naturally flexible/document-shaped (varying fields per record), or you're optimizing for a specific access pattern (e.g., wide-column stores for time-series, graph DBs for relationship-heavy data like social graphs).
  - **Key-value stores** (DynamoDB, Redis as persistent store): simplest, fastest, for direct lookups.
  - **Document stores** (MongoDB): semi-structured, nested data, flexible schema.
  - **Wide-column stores** (Cassandra, HBase): huge write throughput, time-series/event data.
  - **Graph databases** (Neo4j): relationship-heavy data (social networks, recommendation graphs).

### Replication

**What it does:** Keeps copies of the same data on multiple machines for durability (don't lose data if one machine dies) and for scaling reads (spread read traffic across replicas).

**Analogy:** Keeping duplicate copies of an important document in multiple filing cabinets in different buildings — if one building floods, you don't lose the document, and multiple people can read a copy at once without waiting in line for the single original.

- **Primary-replica (leader-follower)**: One node accepts writes (primary), and changes are streamed to one or more replicas which serve reads. Simple, and read-heavy systems scale well this way. Risk: replicas can lag behind the primary (**replication lag**), causing a read right after a write to potentially return stale data (a classic real-world consistency bug) unless you route "read your own write" traffic back to the primary.
- **Multi-leader replication**: Multiple nodes can accept writes, replicating to each other. Useful for multi-region setups where you want writes to be fast locally in each region, but introduces the need to resolve **write conflicts** (e.g., "last write wins," or more sophisticated conflict-free data types).

### Sharding / Partitioning

**What it does:** Splits a dataset across multiple database instances so no single machine has to hold or serve all the data — required once a dataset (or its write throughput) is too large for one machine.

**Analogy:** Instead of one giant library holding every book in the world (impossible), you split books across many branch libraries by, say, the first letter of the author's last name. Each branch only needs to hold and serve its slice.

**Common sharding strategies:**

- **Range-based sharding**: Partition by a range of the key (e.g., user IDs 1-1M on shard 1, 1M-2M on shard 2). Simple, and supports efficient range queries. Downside: can create "hot shards" if certain ranges get much more traffic (e.g., newly created users, if IDs are sequential and new users are most active).
- **Hash-based sharding**: Hash the key and use the hash to decide the shard. Distributes data much more evenly, avoiding hot spots — but loses the ability to do efficient range queries (adjacent keys end up on random shards).
- **Directory-based sharding**: A lookup service/table maps each key (or key range) to a specific shard explicitly. Most flexible — you can rebalance by just updating the mapping — but the directory itself must be highly available and fast, becoming a new critical component.
- **Geo-based sharding**: Partition by user location/region — great for latency (serve users from their nearby region) and for satisfying data residency/regulatory requirements, but can create imbalance if usage isn't evenly distributed globally.

**The rebalancing problem:** Whenever you add or remove shards, you ideally want to move as little data as possible. Naively hashing keys `mod N` (number of shards) means changing N reshuffles almost everything. This is exactly the problem **consistent hashing** solves.

### Read-heavy vs. write-heavy databases

The very first question to ask about any data store is: **what's the read/write ratio, and where's the actual bottleneck?** The right set of solutions is almost completely different depending on the answer.

**Analogy:** A read-heavy system is like a popular library where thousands of people want to _check the same few bestsellers_ — the fix is making more copies of those books available in more places. A write-heavy system is like a post office during tax season, where the bottleneck isn't handing out mail, it's the intake line processing a flood of incoming packages — the fix is about processing intake faster and smoothing out the flow, not making more copies of anything.

Most real systems (social feeds, e-commerce catalogs, content sites) are naturally **read-heavy** — often 100:1 or higher read:write ratios — because content is written once but viewed many times. Some systems are inherently **write-heavy** — logging/telemetry pipelines, IoT sensor ingestion, ad-click tracking, metrics systems — where every event must be durably recorded and reads are comparatively rare or batched.

#### Solutions for read-heavy workloads

- **Read replicas**: The single most common fix. Add one or more replicas of the primary database that only serve reads, and route read traffic to them via the load balancer/app layer, keeping writes on the primary. Scales linearly with the number of replicas for read throughput. Watch out for **replication lag** — a replica might be milliseconds to seconds behind the primary, so "read-your-own-write" flows (e.g., a user posts a comment and immediately refreshes) may need to be routed to the primary or a synchronously-updated replica.
- **Caching layer** (Redis/Memcached in front of the DB): As covered in the caching section — absorb the vast majority of reads before they ever hit the database. For a feed/timeline-style read pattern, cache hit rates of 90%+ are common and turn a database-bound problem into a cache-bound one.
- **CDN for read-mostly public content**: If reads are largely of public, cacheable data (e.g., product pages, articles), push them even further out to the CDN edge so they never even reach your application servers.
- **Denormalization / materialized views**: Precompute and store the _result_ of an expensive read query (e.g., a user's full news feed, a leaderboard) so a read becomes a cheap key lookup instead of an expensive join/aggregation done live every time. The cost is paid once at write time (or on a schedule) instead of on every read.
- **Search indexes** (Elasticsearch) for read patterns that need flexible querying/filtering rather than simple key lookups — keeps that load off the primary transactional database entirely.
- **Read-optimized database choices**: Column-oriented stores (e.g., for analytics) can dramatically speed up read-heavy aggregate queries by only scanning the columns actually needed, instead of full rows.

#### Solutions for write-heavy workloads

- **Sharding by write key**: Since a single primary can only accept so many writes/sec (limited by disk I/O and lock contention), split writes across many shards (e.g., by user ID, device ID, or time bucket) so each shard's write load stays manageable.
- **Write-optimized data stores**: Wide-column stores (Cassandra, HBase, ScyllaDB) and LSM-tree-based engines are specifically built for very high write throughput — they append writes sequentially to an in-memory structure (memtable) and flush to disk in batches (SSTables), which is far faster than the random-access disk seeks that traditional B-tree-based relational databases do for individual row updates.
- **Batching writes**: Instead of committing every single write individually, buffer writes for a short window (milliseconds) and flush them to the database in batches — dramatically reduces per-write overhead (fewer disk fsyncs, fewer round trips), at the cost of a small delay before data is durably persisted.
- **Write-behind (async) buffering via a queue**: Push writes into a message queue (Kafka) first, acknowledge the client immediately, and have a pool of consumers write to the database at a sustainable, controlled rate. This decouples "how fast writes arrive" from "how fast the database can absorb them," smoothing out spikes (see Message Queues section). The trade-off is the write isn't immediately durable in the final store — if the queue/consumer fails before flushing, there's a small window of risk, which is why the queue itself needs to be durable/replicated.
- **Relaxing consistency for writes**: If you don't need every write to be immediately visible everywhere (see CAP theorem/eventual consistency), you can accept writes locally in a region and replicate asynchronously, rather than waiting for a global quorum on every write — much higher write throughput, at the cost of temporary inconsistency across regions.
- **Log-structured storage / append-only patterns**: Rather than updating records in place, some write-heavy systems (event sourcing, time-series DBs) only ever _append_ new records (e.g., "balance changed by -$5" rather than "update balance to X") — sequential appends are much cheaper than in-place updates, and you can derive current state by replaying or periodically compacting the log.
- **Reducing secondary index count**: Every additional index on a table speeds up certain reads but slows down every write (since the index must also be updated). Write-heavy systems often deliberately minimize indexes on the hot write path, and rely on asynchronous denormalization or a separate search index for the read side instead.

**The general principle:** for read-heavy systems, you're mostly fighting to avoid re-doing the same read work over and over (solved via copies: replicas, caches, CDN, precomputed views). For write-heavy systems, you're mostly fighting to keep up with the _rate_ of incoming writes (solved via spreading load: sharding, batching, async buffering, and storage engines built for sequential writes). It's also common for a single system to be read-heavy on one type of data and write-heavy on another (e.g., a social app is read-heavy on the feed but write-heavy on impression/click logging) — in that case, it's normal and often correct to use two completely different storage solutions for the two halves, rather than forcing one database to do both jobs well.

---

## 6. Consistent Hashing

**What it does:** A hashing scheme that minimizes data movement when nodes (shards, cache servers) are added or removed — instead of almost all keys needing to move (as with naive `hash(key) % N`), only a small fraction do.

**Analogy:** Imagine a circular clock face with numbers 0-360. You place your servers at random points on the clock (based on hashing their name/ID). To find where a piece of data belongs, you hash the data's key to get a point on the clock, then walk clockwise until you hit the first server — that's the one responsible for it. If you add a new server, it only takes over the small arc of the clock between it and the next server clockwise — everyone else's data stays put. Compare that to `mod N` hashing, where adding one more "slot" reshuffles almost everyone's assignment.

**How it works, step by step:**

1. Hash each server to one or more points on a fixed circular hash space (e.g., 0 to 2^32 - 1).
2. To place a key, hash the key to a point on the same circle, then move clockwise to find the nearest server point — that server owns the key.
3. Adding a server: it takes ownership of a contiguous arc of keys from its clockwise neighbor — only those keys need to move.
4. Removing a server: its keys are simply reassigned to the next server clockwise.

**Virtual nodes:** In practice, each physical server is mapped to _many_ points on the ring (virtual nodes), not just one. This spreads a server's load more evenly (avoiding one server unluckily owning a huge arc) and makes load redistribution smoother when servers are added/removed.

**When to use:** Distributed caches (Memcached client-side sharding), distributed hash tables, sharded databases, and load balancers that need "same key → same backend" behavior (e.g., session affinity, or routing all requests for a given user consistently to the same cache node for the best hit rate).

---

## 7. Message Queues & Event Streaming

**What it does:** Decouples the producer of work from the consumer of work, letting them operate independently, at different speeds, without the producer having to wait for the consumer.

**Analogy:** A restaurant kitchen's order ticket rail. Waiters (producers) post orders on the rail and immediately go back to serving tables — they don't stand there waiting for the cook to finish. Cooks (consumers) pull tickets off the rail whenever they're free. If the kitchen gets slammed, tickets just queue up on the rail instead of waiters being blocked.

**When to use:**

- Any workflow that doesn't need to happen synchronously in the request path — e.g., sending a confirmation email, generating a thumbnail after upload, fanning out a new post to millions of followers' feeds.
- Smoothing out traffic spikes ("load leveling") — the queue absorbs a burst, and consumers process it at a steady, sustainable rate instead of the backend being overwhelmed instantly.
- Connecting multiple independent services/microservices without them needing to know about each other directly.

**Two broad patterns:**

- **Point-to-point / task queues** (e.g., SQS, RabbitMQ classic queues): each message is processed by exactly one consumer. Good for distributing discrete units of work among a worker pool (e.g., "resize this image").
- **Publish-subscribe / event streaming** (e.g., Kafka, SNS, Google Pub/Sub): a message (event) is broadcast to _all_ interested subscribers, each of which processes it independently. Good for "something happened, and multiple different systems care" (e.g., "order placed" event consumed by billing, inventory, and notification services simultaneously).

**Kafka specifics worth knowing:** Kafka organizes messages into **topics**, which are split into **partitions** for parallelism (each partition is an ordered, append-only log). Consumers in a **consumer group** each own a subset of partitions, allowing horizontal scaling of consumption while preserving order _within_ a partition (but not necessarily across the whole topic). This is why you often partition by a key like `user_id` — it guarantees all events for a given user are processed in order, while still parallelizing across users.

**When NOT to use:** If the caller genuinely needs an immediate response with the result (e.g., "is this password correct?"), a queue adds unnecessary latency and complexity — that's a job for a direct synchronous call.

---

## 8. Rate Limiting

**What it does:** Restricts how many requests a client (user, IP, API key) can make in a given time window, to protect backend systems from being overwhelmed (accidentally or maliciously) and to enforce fair usage / pricing tiers.

**Analogy:** A nightclub bouncer who only lets a certain number of people in per minute, regardless of how many are waiting outside — this keeps the inside of the club (your backend) from becoming dangerously overcrowded.

**Key algorithms:**

- **Token bucket**: A bucket holds up to N tokens, refilled at a steady rate (e.g., 10 tokens/second). Each request consumes one token; if the bucket's empty, the request is rejected/delayed. Naturally allows short bursts (as long as tokens have accumulated) while enforcing a steady average rate. This is the most commonly used algorithm in practice (used by AWS, Stripe, etc.) because it balances burst tolerance with simplicity.
- **Leaky bucket**: Requests enter a queue (the "bucket") and are processed ("leak out") at a fixed, constant rate, regardless of how bursty the incoming traffic is. Smooths traffic into a perfectly steady outflow — good when the downstream system truly can't handle any burstiness at all, but adds latency for bursty legitimate traffic.
- **Fixed window counter**: Count requests in fixed time windows (e.g., "0-60s", "60-120s"), reset the counter each window. Simple, but has an edge issue: a client can send double the intended rate right at the boundary between two windows (e.g., burst at the end of one window and the start of the next).
- **Sliding window log**: Keep a timestamp log of every request in the last window and count them precisely — accurate, but memory-heavy at scale (storing every timestamp).
- **Sliding window counter**: A hybrid — approximates the sliding window using weighted counts from the current and previous fixed windows, giving good accuracy without storing every individual timestamp. Common practical compromise.

**Where to enforce it:** Typically at the **API Gateway** or a dedicated middleware layer, so individual services don't each need to reimplement it, and so abusive traffic is stopped as early as possible (before it consumes downstream resources).

**What happens when limited:** Return HTTP `429 Too Many Requests`, often with a `Retry-After` header telling the client when to try again.

---

## 9. CAP Theorem & Consistency Models

**What it says:** In a distributed system, when a **network partition** occurs (some nodes can't talk to others), you must choose between:

- **Consistency (C)**: every read receives the most recent write, or an error.
- **Availability (A)**: every request receives a (non-error) response, even if it might not reflect the most recent write.

You cannot have both _during_ a partition (partition tolerance, P, is basically mandatory in any real distributed system, since networks do fail) — hence people describe systems as **CP** or **AP** (rarely a meaningful pure "CA" system exists at scale, since that would require assuming partitions never happen).

**Analogy:** Imagine two bank branches in different cities that occasionally lose their connection to each other. If a customer tries to withdraw money during an outage between branches: a **CP** system refuses the transaction until it can confirm the account balance with the other branch (consistent, but the customer is temporarily unable to act — unavailable). An **AP** system lets the withdrawal happen locally and reconciles the two branches' records later (available, but briefly, the two branches might disagree about the "true" balance).

**In practice:**

- **CP systems** (e.g., traditional relational DBs configured for strong consistency, HBase, MongoDB in certain configurations, ZooKeeper/etcd): good for data where correctness trumps availability — financial transactions, inventory counts, configuration/consensus data.
- **AP systems** (e.g., Cassandra, DynamoDB by default, most CDNs, most social media feeds): good for data where being highly available (and eventually correct) matters more than always being perfectly up to date — likes counts, follower feeds, view counts.

**Eventual consistency:** The common middle ground used by most AP systems — writes propagate to all replicas _eventually_, and if no new writes come in, all reads will _eventually_ converge to the same value. Most large-scale systems (social feeds, comment counts, etc.) use this, because users generally don't notice or mind if a like count is a second or two stale.

**Quorum-based consistency (tunable consistency):** Many distributed databases (Cassandra, DynamoDB) let you tune consistency per-operation using **W** (write quorum) and **R** (read quorum) out of **N** replicas. If **W + R > N**, you're guaranteed strong consistency (every read overlaps with the most recent write's set of replicas). If not, you get faster operations but weaker guarantees. This lets a single system flexibly support both "I need this read to be strongly consistent" and "I'm fine with eventual consistency" operations side by side.

---

## 10. Microservices vs. Monolith, and Service Discovery

**Monolith**: One large application, single codebase, single deployment unit. **Analogy:** A single all-in-one Swiss Army knife.

- **Pros:** simpler to develop, test, and deploy early on; no network overhead between "components" since they're all in-process; easier to reason about transactions across features.
- **Cons:** hard to scale parts independently (you must scale the whole app even if only one feature is hot); a bug in one module can take down the whole app; large codebases become slow to build/test/deploy as teams grow.

**Microservices**: The application is split into many independently deployable services, each owning its own data and communicating over the network (REST/gRPC/events). **Analogy:** A toolbox of separate, specialized tools — each can be improved, replaced, or scaled independently, but now you need to carry (and coordinate) all of them together.

- **Pros:** independent scaling (scale only the video-processing service if that's the bottleneck), independent deployment (teams ship without waiting on each other), fault isolation (one service crashing doesn't necessarily take down others), freedom to use different tech stacks per service.
- **Cons:** operational complexity (you now have distributed systems problems: network failures, partial failures, distributed tracing needed for debugging), data consistency across services is harder (no single database transaction spans services — often requires patterns like **Sagas** for multi-step transactions), and you need infrastructure for service discovery, inter-service auth, and monitoring that a monolith doesn't need.

**When to choose which:** Most systems should **start as a monolith** and split into microservices only once specific parts genuinely need independent scaling or independent team ownership — splitting too early is a very common and costly mistake. In an interview, if scale is small-to-medium, a well-structured monolith is often a perfectly reasonable (and slightly contrarian, in a good way) answer.

**Service Discovery**: In a microservices world, service instances come and go (scaling up/down, deployments, failures) — hardcoding IP addresses doesn't work. A **service registry** (e.g., Consul, etcd, Eureka, or built into orchestrators like Kubernetes) keeps track of which instances are currently healthy and where they are, so services can look each other up dynamically instead of relying on static configuration.

---

## 11. Blob / Object Storage

**What it does:** Stores large, unstructured binary data (images, videos, backups, logs) cheaply and durably, typically accessed via simple GET/PUT over HTTP rather than a filesystem or database interface.

**Analogy:** A massive self-storage warehouse: you don't get fine-grained control like a filing cabinet (no "edit byte 500 of this file"), but you get effectively unlimited space, extreme durability (multiple copies across facilities), and you only pay for what you use.

**When to use:** Any media or large file storage (S3, Google Cloud Storage, Azure Blob) — the standard pattern is: don't store binary blobs in your relational database (bloats backups, slows queries); store the blob in object storage and just keep a **reference/URL** to it in your database.

**Common pattern with CDN:** Upload → object storage (origin) → CDN caches and serves to users, so origin storage only serves cache misses.

**Durability techniques:** Object storage systems typically use **replication** (multiple full copies) and/or **erasure coding** (splitting data into fragments with added parity/redundancy, similar in spirit to RAID, so the original can be reconstructed even if some fragments are lost) — erasure coding gets similar durability to full replication with meaningfully less storage overhead, at the cost of more computation to reconstruct data.

---

## 12. Search & Indexing

**What it does:** Enables fast full-text or complex multi-attribute search over large datasets — something relational databases are notoriously bad at doing efficiently at scale (e.g., "find all documents containing these words, ranked by relevance").

**Analogy:** The index at the back of a textbook. Instead of reading every page to find mentions of "photosynthesis" (a full table scan), you jump straight to the index, which already lists every page it appears on.

**Core data structure — the inverted index:** Instead of mapping _documents → words_ (as you'd naturally store data), you map _words → the list of documents containing them_. Given a query, you look up each query word's list and combine them (e.g., intersect for AND queries), which is vastly faster than scanning every document.

**When to use:** Any time you need full-text search, faceted search/filtering across many fields, or relevance-ranked results — tools like **Elasticsearch** or **OpenSearch** (built on Apache Lucene) are the standard choice. These are typically kept as a **secondary, denormalized index** fed asynchronously from the primary database (via a message queue or change-data-capture) rather than being the system of record.

**When NOT to use:** For simple exact-match lookups by a known key (e.g., "get user by user ID"), a regular indexed database column is simpler, faster, and avoids the operational overhead of running a separate search cluster.

---

## 13. Real-Time Communication: WebSockets, Long Polling, SSE

**What it does:** Enables the server to push updates to clients (or maintain a persistent two-way channel), which plain HTTP request/response wasn't originally designed for.

**Analogy:** Regular HTTP is like mailing a letter and waiting for a reply each time you have something new to ask — slow if you need frequent updates. A WebSocket is like keeping an open phone line connected, so either side can speak whenever they have something to say, without redialing.

- **Polling**: Client repeatedly asks the server "anything new?" every few seconds. Simple, but wasteful (many empty responses) and adds latency (up to the polling interval).
- **Long polling**: Client asks "anything new?" and the server _holds the request open_ until there's new data (or a timeout), then responds — client immediately re-polls. Reduces wasted empty responses compared to plain polling, works over standard HTTP, but still has per-request overhead and doesn't scale as gracefully as a true persistent connection.
- **Server-Sent Events (SSE)**: A one-way persistent connection where the server can continuously push events to the client over plain HTTP. Simple to implement, works well for one-directional feeds (e.g., live scores, notifications) where the client doesn't need to send much back.
- **WebSockets**: A true full-duplex, persistent connection — either side can send messages at any time with very low overhead per message. Best for chat apps, multiplayer games, collaborative editing — anything needing frequent, low-latency, two-way communication.

**When to use which:** If you only need occasional server-to-client pushes and simplicity matters, SSE or long polling is often good enough. If you need frequent bidirectional communication with minimal latency (chat, live collaboration, gaming), WebSockets are the standard choice. At large scale, maintaining millions of persistent WebSocket connections requires dedicated connection-handling infrastructure (since each open connection holds server resources), which is itself a notable scaling challenge interviewers sometimes probe.

---

## 14. Monitoring, Logging, and Observability

**What it does:** Lets you know what your system is actually doing in production, detect problems before/as they happen, and debug issues after the fact.

**Analogy:** The dashboard, black box flight recorder, and air traffic control tower of an airplane, combined — gauges tell you the current state (metrics), the flight recorder tells you exactly what happened leading up to an incident (logs/traces), and the control tower watches for problems across the whole fleet and alerts when something looks wrong (alerting).

**Three pillars:**

- **Metrics**: numeric time-series data (QPS, latency percentiles, error rate, CPU/memory usage) — used for dashboards and alerting. Watch **p50/p95/p99 latency**, not just averages — a slow p99 can mean 1% of your users have a terrible experience even while the average looks fine.
- **Logs**: detailed, timestamped records of discrete events (a request came in, an error occurred, with full context). Centralized log aggregation (e.g., ELK stack — Elasticsearch/Logstash/Kibana) is standard once you have more than a couple of servers, since SSH-ing into individual boxes doesn't scale.
- **Distributed tracing**: tracks a single request as it flows across many microservices, showing exactly where time was spent and where it failed — essential once you have microservices, since a single user request might touch a dozen services and you need to pinpoint which one is slow or broken.

**When to design this in:** Always mention observability briefly in a system design interview, even if it's not the focus — it signals production-readiness thinking. You don't need to deep-dive unless asked.

---

## 15. Server-based vs. Serverless Architecture

**What the choice is about:** Where and how your compute actually runs — on machines/containers you provision and keep running continuously (traditional servers), versus short-lived functions that a cloud provider spins up on demand per request/event and tears down afterward (serverless, e.g., AWS Lambda, Google Cloud Functions, Azure Functions).

**Analogy:** A traditional server is like owning a restaurant with a kitchen staffed and running all day — even during slow hours, you're paying rent and keeping the lights and stoves on, but you can serve a walk-in customer instantly with zero setup delay. Serverless is like a pop-up kitchen that only assembles itself the moment an order comes in, and disassembles right after — you pay nothing while there are no orders, but the very first order of the day takes a bit longer because the kitchen has to be set up first (a "cold start").

### Traditional server architecture

**Pros:**

- **Predictable performance** — no cold-start delay; the process is already warm and can hold in-memory state (connection pools, caches) between requests.
- **Full control** over the runtime, OS, installed libraries, long-running background processes, and persistent in-memory state.
- **Cost-efficient at steady, high, predictable load** — you're not paying a per-invocation premium once utilization is consistently high.
- Naturally supports **long-lived connections** (WebSockets, persistent TCP connections, long-running batch jobs) without the constraints serverless platforms often place on execution duration.

**Cons:**

- **You pay for idle time** — a server provisioned for peak load sits underutilized (and still costs money) during low-traffic periods, unless you build your own autoscaling.
- **You own operational overhead**: patching, scaling policy, capacity planning, and provisioning ahead of expected demand — if you under-provision, you get outages; if you over-provision, you waste money.
- **Scaling has a floor and is slower** — spinning up a whole new server (or container) still takes real time (seconds to minutes), and you typically keep some minimum fleet running at all times.

### Serverless architecture

**Pros:**

- **Pay only for actual execution time/invocations** — genuinely cost-efficient for spiky, unpredictable, or low/intermittent traffic (a background job that runs a few times an hour, an endpoint hit occasionally).
- **No infrastructure management** — the provider handles provisioning, patching, and scaling entirely; you just deploy code.
- **Scales automatically and near-instantly to very high concurrency** — the platform can spin up huge numbers of parallel function instances in response to a burst, without you writing any autoscaling logic yourself.
- **Fine-grained, natural isolation** — each invocation typically runs in its own sandboxed instance, which can be a security/blast-radius advantage.

**Cons:**

- **Cold starts**: when a function hasn't been invoked recently, the platform must provision a fresh execution environment before running your code, adding latency (anywhere from tens of milliseconds to a few seconds depending on runtime/language and package size) to that particular request. This makes serverless a poor fit for strict, consistent low-latency requirements unless you pay extra for "provisioned concurrency" (keeping a minimum number of instances warm, which reduces — but partially reclaims — the cost savings).
- **No persistent in-memory state between invocations** — each invocation is (in general) a fresh, isolated environment; you can't rely on an in-process cache or a long-lived connection pool surviving across calls the way you can with a long-running server. This pushes you toward external state (a database, a distributed cache, or a managed connection-pooling proxy).
- **Execution time limits** — most serverless platforms cap how long a single invocation can run (e.g., minutes, not hours), making them a poor fit for long-running batch jobs or persistent connections (like raw WebSocket servers) without extra architecture (e.g., pairing serverless with a managed WebSocket/API Gateway service that handles the persistent connection separately from your business logic function).
- **Vendor lock-in and debugging complexity** — serverless functions are often tightly coupled to a specific cloud provider's event model and tooling, and distributed tracing/debugging across many small, ephemeral functions can be harder than debugging a single long-running process.
- **Cost can flip the other way at high, sustained volume** — per-invocation pricing can end up more expensive than a well-utilized always-on server once traffic is high and constant, rather than spiky.

### When to choose which

- **Choose serverless** for: event-driven/sporadic workloads (image processing on upload, scheduled/cron-style jobs, webhooks, glue logic between other services), unpredictable or spiky traffic where you don't want to pay for idle capacity, and situations where you want to move fast without managing infrastructure at all.
- **Choose traditional servers (or containers on an orchestrator like Kubernetes)** for: steady high-traffic services where cold starts and per-invocation cost would hurt, workloads needing persistent in-memory state or long-lived connections, and situations where you need very tight, consistent latency control.
- **In practice, most large real-world systems are hybrids**: core, high-traffic, latency-sensitive services run on traditional servers/containers, while auxiliary, event-driven, or bursty workloads (thumbnail generation, sending emails, processing uploaded files, periodic batch jobs) run as serverless functions.

### How do multiple "servers" in a serverless architecture communicate with one another?

This is a common point of confusion, since serverless functions are ephemeral and don't hold open direct connections to each other the way traditional servers might. In practice, serverless functions almost never talk to each other **directly** — instead, they communicate _indirectly_, through managed intermediary services, which is actually a deliberate and important architectural pattern:

- **Message queues / event streams** (SQS, Kafka, Google Pub/Sub, EventBridge): The most common pattern. Function A finishes its work and publishes an event/message; Function B is configured to be triggered whenever a new message arrives on that queue/topic. Neither function needs to know the other exists, where it's deployed, or whether it's currently running — the queue is the entire communication channel. This also naturally handles load leveling and retries (if Function B fails, the message queue can redeliver it).
- **API Gateway + HTTP calls**: One function can synchronously call another function's HTTP endpoint (exposed via an API Gateway) just like calling any external API. This is simpler to reason about but reintroduces tight coupling and synchronous waiting (and cold-start latency compounds if the callee also needs to cold-start).
- **Shared external state** (a database, a distributed cache like Redis, or object storage): Function A writes a result to a shared database/cache/S3 bucket; Function B, triggered separately (e.g., on a schedule, or triggered by a storage event like "new object uploaded to this bucket"), reads it. This is an extremely common pattern for pipelines — e.g., an upload triggers a function that processes a file and writes the result to storage, which in turn triggers the next function in the pipeline via a storage event notification.
- **Orchestration services** (AWS Step Functions, Azure Durable Functions, Google Workflows): For multi-step workflows with defined sequencing, branching, and retry logic (e.g., "process payment → then update inventory → then send confirmation, with rollback logic if any step fails"), a dedicated orchestrator coordinates calling each function in the right order and manages state _between_ function invocations (since the functions themselves can't hold that state). This is the serverless equivalent of an explicit workflow engine, and is often preferred over ad-hoc queue chains once the workflow logic gets non-trivial (e.g., needs conditional branches or compensating "undo" steps).

**The underlying principle:** since no serverless function can be assumed to be "always running" or reachable by a fixed address the way a traditional server is, all inter-function communication has to go through a durable, addressable middleman (a queue, a database, a gateway, or an orchestrator) rather than a direct connection — this is what makes serverless systems naturally event-driven and loosely coupled by default.

---

## 16. Backend Architecture Styles: REST, GraphQL, gRPC, and more

This is about the _contract/protocol_ your backend exposes to clients (or other services) — a separate decision from server vs. serverless, and one you'll often be asked to justify directly in interviews.

### REST (Representational State Transfer)

**What it is:** An architectural style built around resources (nouns) identified by URLs, manipulated via standard HTTP verbs (`GET`, `POST`, `PUT`, `PATCH`, `DELETE`). E.g., `GET /users/123`, `POST /users`, `DELETE /users/123/posts/456`.

**Analogy:** A well-labeled filing cabinet where every drawer (URL) has a clear name and you interact with it using a small, standard set of actions (verbs) that mean the same thing everywhere — "GET" always means "give me a copy," "DELETE" always means "remove this," regardless of which drawer you're using.

**Pros:**

- **Simple, widely understood, huge ecosystem/tooling support** (caching via HTTP semantics, browser-native, easy to test with basic tools like curl).
- **Leverages HTTP caching naturally** — `GET` requests can be cached by browsers, CDNs, and proxies using standard HTTP cache headers, essentially for free.
- **Stateless by convention**, which makes horizontal scaling straightforward (any server can handle any request, since no session state needs to live on a specific server).
- Good fit when your data maps cleanly to resources and standard CRUD operations.

**Cons:**

- **Over-fetching and under-fetching**: a `GET /users/123` endpoint returns a fixed shape of data — if the client only needs the user's name, it still gets the whole object (over-fetching); if the client needs the user _and_ their recent posts, it needs a second request (under-fetching), often leading to many round trips for complex UI screens.
- **Versioning can get awkward** as APIs evolve (`/v1/users`, `/v2/users`), since the whole endpoint contract is fixed server-side.
- Can lead to a proliferation of specialized endpoints over time to work around over/under-fetching (e.g., `/users/123/summary`, `/users/123/with-posts`), which becomes harder to maintain.

### GraphQL

**What it is:** A query language and runtime where the client specifies _exactly_ what data shape it wants in a single request, and the server resolves it from potentially many underlying data sources, returning precisely that shape — no more, no less.

**Analogy:** Instead of going to several different counters in a cafeteria (one for the read endpoint, another for the write endpoint, another for the "with extra details" endpoint), you hand one waiter a single detailed order describing exactly the combination of dishes you want, and they bring back exactly that plate — nothing extra, nothing missing.

**Pros:**

- **Solves over/under-fetching directly**: the client asks for exactly the fields it needs, including nested/related data, in a single request — great for mobile clients (bandwidth-sensitive) and complex UIs that pull from many related entities at once.
- **Single endpoint, strongly typed schema**: one URL (`/graphql`) serves everything, and the schema acts as self-documenting, machine-checkable contract between frontend and backend (tooling can autogenerate types, validate queries at build time, etc.).
- **Easier client-driven iteration**: frontend teams can often add new field requirements without needing a new backend endpoint or a backend deploy, as long as the underlying schema already exposes the data.

**Cons:**

- **Harder to cache** — since every query can request a different shape, you can't rely on simple HTTP-level URL caching the way REST GETs allow; caching has to happen at a finer grain (field-level or query-result level, e.g., via tools like Apollo's normalized cache) which is more complex to set up.
- **The N+1 query problem**: naively resolving nested fields (e.g., "for each of these 50 posts, fetch its author") can trigger many small backend/database calls unless you explicitly batch them (commonly solved via a **DataLoader** pattern that batches and deduplicates requests within a single tick).
- **Harder to rate-limit/cost-estimate**: since queries are arbitrarily shaped by the client, a single malicious or poorly-formed query can request deeply nested, expensive data (query complexity attacks), requiring extra work (query cost analysis, depth limiting) to protect the backend — something REST's fixed endpoints don't really suffer from.
- **More backend complexity to set up initially** — you need resolvers, a schema, and usually a dedicated library/framework, versus REST's simplicity of "just add a route."

### gRPC (and RPC-style APIs generally)

**What it is:** A high-performance RPC (Remote Procedure Call) framework where the client calls a method on a remote service as if it were a local function call, using Protocol Buffers (a compact binary serialization format) over HTTP/2.

**Analogy:** Instead of writing a letter in a common but somewhat verbose language (JSON over HTTP, like REST), you and the recipient agree in advance on a compact shorthand code (a strict, pre-compiled schema) that both sides can pack and unpack extremely quickly — faster to send, but both sides must have agreed on the code in advance.

**Pros:**

- **Very high performance**: binary serialization (Protocol Buffers) is much smaller and faster to (de)serialize than JSON, and HTTP/2 allows multiplexed streams over a single connection, reducing overhead — ideal for internal service-to-service communication at scale.
- **Strongly typed contracts** via `.proto` files, with code generation for many languages, catching mismatches at compile time rather than at runtime.
- **First-class support for streaming** (client streaming, server streaming, or bidirectional streaming), which is awkward to express in plain REST.

**Cons:**

- **Not natively browser-friendly** — browsers can't easily speak raw gRPC without a proxy layer (gRPC-Web), making it a poor fit for public-facing client APIs consumed directly by web frontends; it shines for internal, service-to-service (backend-to-backend) communication instead.
- **Less human-readable/debuggable** — binary payloads can't just be inspected with curl/browser dev tools the way JSON can, adding friction for debugging and quick manual testing.
- **Steeper initial setup** — requires defining `.proto` schemas and a compilation/codegen step in the build pipeline.

### Other worth knowing briefly

- **WebSockets** (as covered in Part 2, section 13): not really a competing "API style" for request/response, but the right choice when you need a persistent, low-latency, bidirectional channel (chat, live collaboration) rather than discrete request/response calls.
- **Webhooks**: instead of a client polling an API for updates, the server calls a URL the client registered, whenever an event happens (e.g., Stripe calling your server when a payment succeeds). This is a common complementary pattern to any of the above styles for event notification between systems.

### Quick comparison

|                         | REST                                      | GraphQL                                                             | gRPC                                                                    |
| ----------------------- | ----------------------------------------- | ------------------------------------------------------------------- | ----------------------------------------------------------------------- |
| **Best for**            | Public APIs, simple CRUD, browser clients | Complex/nested data needs, mobile clients, many client-driven views | Internal microservice-to-microservice calls, performance-critical paths |
| **Caching**             | Easy (HTTP-native)                        | Harder (needs custom/normalized caching)                            | Not HTTP-cache-friendly (custom needed)                                 |
| **Payload format**      | Usually JSON (text)                       | Usually JSON (text)                                                 | Protocol Buffers (binary)                                               |
| **Browser support**     | Native                                    | Native (over HTTP)                                                  | Needs a proxy (gRPC-Web)                                                |
| **Over/under-fetching** | Common issue                              | Solved by design                                                    | N/A (methods return fixed shapes, but you design the methods)           |
| **Learning curve**      | Low                                       | Medium                                                              | Medium-high                                                             |
| **Streaming support**   | Limited (needs SSE/WebSockets bolted on)  | Limited (subscriptions exist but less mature)                       | First-class (client/server/bidirectional streaming)                     |

**Interview tip:** A very common and reasonable answer is a **hybrid**: expose a public-facing REST or GraphQL API to external/frontend clients (whichever fits the client's data-fetching pattern better), while using gRPC for internal, high-throughput communication between your own microservices — you don't have to pick just one style for an entire system.

---

# WORKED EXAMPLE: Designing Twitter

This section walks through an entire interview end-to-end, applying every technique from Parts 1 and 2 in sequence, the way you'd actually talk through it in a room.

## Step 1: Clarify requirements

**Functional requirements (scope it down — don't try to design all of Twitter):**

- Users can post short text messages ("tweets"), optionally with an image.
- Users can follow other users.
- Users see a **home timeline/feed**: a reverse-chronological (or ranked) stream of tweets from people they follow.
- Users can like and reply to tweets.
- (Explicitly out of scope for this pass: search, trending topics, DMs, ads — mention them, but say you'll focus on posting + timeline, since that's the core hard problem.)

**Non-functional requirements:**

- **Read-heavy**: users check their timeline far more often than they post — expect a very high read:write ratio.
- **Low latency** on reading the timeline (this is the core product experience).
- **High availability** favored over strict consistency — it's fine if a tweet takes a few seconds to appear in every follower's feed (eventual consistency is acceptable); it's not fine if the app is down.
- Must handle **celebrity/viral accounts** with tens of millions of followers as a distinct edge case.

## Step 2: Estimate scale (applying Part 1's framework)

Let's assume **500 million total users, 200 million DAU** (Twitter-scale, per the Tier 3-4 boundary in Part 1).

- **Tweets/day**: Assume each DAU posts 0.5 tweets/day on average → 200M × 0.5 = **100M tweets/day** → ÷100,000 sec ≈ **1,000 writes/sec average**, ~3,000/sec peak.
- **Timeline reads/day**: Assume each DAU checks their timeline ~10 times/day (each "check" fetching, say, the latest page of tweets) → 200M × 10 = **2 billion reads/day** → ÷100,000 sec ≈ **20,000 reads/sec average**, ~50,000-60,000/sec peak.
- **Read:write ratio**: ~20:1 — solidly **read-heavy**, confirming the caching/read-replica strategies from Part 2's database section are the right lever, not sharding-for-write-throughput alone.
- **Storage**: Each tweet ≈ 300 bytes of text/metadata → 100M × 300B = **30 GB/day** of core tweet data → ~11 TB/year — very manageable for a sharded relational or wide-column store.
- **Media**: If 20% of tweets have an image (~200KB average) → 100M × 20% × 200KB = **4 TB/day** of media → clearly justifies **object storage + CDN**, exactly as flagged in Part 1's Tier 3 guidance.
- **The hard number**: the average user follows ~200 people. So generating one user's timeline potentially means merging tweets from 200 different sources — and on the write side, one tweet from a popular account must potentially reach millions of followers' timelines. This asymmetry is the crux of the whole design.

## Step 3: High-level architecture (applying Part 2's building blocks)

```
                                   ┌─────────────┐
                                   │     CDN      │  ← serves images/media, edge-cached
                                   └──────┬───────┘
                                          │ (cache miss only)
 ┌────────┐      ┌────────────────┐      │      ┌──────────────────┐
 │ Client │─────▶│ Load Balancer  │──────┴─────▶│   API Gateway     │
 │(mobile/│      │  (L7, routes   │             │ (auth, rate limit,│
 │ web)   │◀─────│  by path)      │◀────────────│  request routing) │
 └────────┘      └────────────────┘             └─────────┬─────────┘
                                                            │
                        ┌───────────────────────────────────┼───────────────────────────┐
                        ▼                                   ▼                           ▼
                ┌───────────────┐                  ┌────────────────┐         ┌──────────────────┐
                │ Tweet Service  │                  │ Timeline Service│         │  Social Graph     │
                │ (write path)   │                  │  (read path)    │         │  Service          │
                └───────┬────────┘                  └────────┬────────┘        │ (follows/followers)│
                        │                                     │                 └─────────┬─────────┘
                        ▼                                     ▼                           │
              ┌──────────────────┐                 ┌────────────────────┐                 │
              │  Message Queue   │───────consumed──▶│  Fan-out Workers    │◀────────────────┘
              │  (Kafka)         │                  │ (write to timeline  │
              └────────┬─────────┘                  │  cache per follower)│
                        │                            └──────────┬──────────┘
                        ▼                                       ▼
              ┌──────────────────┐                    ┌───────────────────┐
              │  Tweet DB        │                     │ Timeline Cache     │
              │ (sharded by      │                     │ (Redis, sharded    │
              │  tweet/user ID)  │                     │  by user ID —      │
              └──────────────────┘                     │  pre-computed feed)│
                                                        └───────────────────┘
              ┌──────────────────┐
              │  Object Storage  │  ← media, referenced by URL from Tweet DB
              │  (S3-class)      │
              └──────────────────┘
```

**Walking through the flow:**

1. Client hits the **load balancer**, which routes to the **API Gateway** (handles auth + rate limiting from Part 2, section 2/8).
2. **Posting a tweet**: API Gateway → Tweet Service writes the tweet to a sharded **Tweet DB** (and uploads any image to **object storage**, getting back a URL) → publishes a "new tweet" event onto a **Kafka** topic (Part 2, section 7).
3. **Fan-out workers** consume that event and push the new tweet into the **precomputed timeline cache** of each follower — this is the "fan-out-on-write" pattern, detailed below.
4. **Reading a timeline**: Timeline Service simply reads the requester's precomputed feed straight out of the **timeline cache** (Redis) — ideally never touching the Tweet DB directly for the common case, since that's what makes 50,000+ reads/sec feasible.

## Step 4: Deep dive — the fan-out problem (the classic hard part of this design)

This is almost always the part an interviewer wants to go deep on, because it's where the read-heavy vs. write-heavy trade-offs from the database section collide directly.

**Fan-out-on-write (push model):** When a tweet is posted, immediately push it into the precomputed timeline cache of _every one of the author's followers_ (via the fan-out workers described above). Reading a timeline is then just one fast cache lookup — extremely cheap on the read path, which matters enormously given our 20:1 read:write ratio.

- **Problem — the celebrity case**: if an account has 50 million followers, one tweet triggers 50 million cache writes. This is a massive, sudden burst of write amplification for a single event, exactly the kind of load-leveling problem message queues are meant to smooth out (Part 2, section 7) — but even then, at real scale, followers of mega-accounts might not get their feed updated within milliseconds, and that's usually an acceptable trade-off (eventual consistency, per Part 2 section 9, is fine here since it's a social feed, not a bank balance).

**Fan-out-on-read (pull model):** Instead, don't precompute anything at write time — when a user requests their timeline, fetch the latest tweets from each of the ~200 accounts they follow, in real time, and merge/sort them on the fly.

- **Problem**: this makes _every single read_ expensive (200 lookups + a merge, every single time, for every one of your 20,000+ reads/sec), which is the wrong trade-off for a read-heavy system — you'd be paying the expensive cost on the far more frequent operation.

**The hybrid solution used in practice (and the strongest interview answer):**

- Use **fan-out-on-write** for the vast majority of users (say, anyone with fewer than ~10,000 followers) — cheap to fan out, and it keeps reads fast for everyone's timeline.
- Use **fan-out-on-read** _just for celebrity/high-follower accounts_ — when generating a user's timeline, merge their precomputed cached feed (from normal accounts they follow) with a small number of live lookups against just the celebrity accounts they follow (since there are relatively few celebrities and a normal user follows only a handful of them, this stays cheap).
- This hybrid is a direct, practical application of "know your read/write ratio and use the right tool for each side" from the database section — treating the two access patterns (normal accounts vs. mega-accounts) as genuinely different problems, rather than forcing one uniform strategy to handle both.

## Step 5: Other notable design decisions worth stating out loud

- **Data store choice**: tweets themselves are simple, high-volume, append-mostly records with simple access patterns (fetch by tweet ID, fetch by user ID) — a **wide-column/NoSQL store** (or a heavily sharded relational DB) fits well, per the SQL vs. NoSQL guidance in Part 2. The **social graph** (follows/followers), on the other hand, benefits from a data model that handles relationships well — a graph database or a carefully indexed relational table, since "who do my followers also follow" style queries are relationship-heavy.
- **API style**: A **REST** API (or GraphQL, if the mobile app team wants to avoid over-fetching full tweet objects when they only need a few fields for a list view) exposed publicly to clients; internally, the Tweet Service, Timeline Service, and Social Graph Service likely talk to each other over **gRPC** for low-latency internal calls, per Part 2's backend architecture styles comparison.
- **Consistency choices**: Posting a tweet needs to be durable (once you get a success response, the tweet must not vanish) — but _propagation_ of that tweet into every follower's feed can be eventually consistent. This mirrors the CAP theorem discussion directly: we choose **AP-leaning eventual consistency** for feed propagation, while the core write itself is still durably persisted before acknowledging the client.
- **Caching**: beyond the timeline cache, individual hot tweets (viral content) benefit from their own cache entries (cache-aside, per Part 2 section 4) so a single viral tweet being read by millions doesn't repeatedly hit the Tweet DB.
- **Serverless vs. servers**: the core Tweet/Timeline/Social Graph services are steady, latency-sensitive, and high-throughput — a clear case for **traditional servers/containers** (per Part 2, section 15), not serverless. But auxiliary work — resizing an uploaded image, sending a push notification when someone gets a new follower — fits serverless well, since it's event-driven and bursty, triggered off the same Kafka events or storage upload notifications already in the design.

## Step 6: Trade-offs to name explicitly if asked

- Fan-out-on-write trades **storage/write amplification** for **read speed** — we're duplicating data across many followers' caches to make the far more frequent read operation fast.
- Eventual consistency on feed propagation trades **perfect real-time accuracy** for **availability and write throughput** — acceptable because a few seconds of delay on a social feed has essentially no real-world cost.
- The hybrid fan-out approach trades **added system complexity** (two different code paths) for **avoiding a genuinely catastrophic worst case** (a single celebrity tweet triggering 50 million synchronous cache writes).

This same five-step pattern — clarify, estimate, sketch, deep-dive, trade-offs — is exactly what you should run for any other prompt (design Instagram, design Uber, design a URL shortener, design WhatsApp); only the specific hard problem in Step 4 changes.

---

When given a prompt like "design X," run through:

1. **Clarify** functional scope and non-functional priorities (is this read-heavy or write-heavy? does it need strong consistency anywhere? what's the expected scale?).
2. **Estimate** using Part 1's framework — QPS, storage, bandwidth — and say out loud which scale tier you're in, since that determines which components are actually justified.
3. **Draw the high-level flow**: Client → CDN (if media) → Load Balancer → API Gateway → Services → Cache → Database (sharded/replicated as needed) → Message Queue for async work → Object storage for blobs.
4. **Deep dive** into 1-2 components the interviewer cares about most — usually the data model/sharding strategy, or one tricky algorithm (e.g., how exactly does the news feed fan-out work, or how does the rate limiter behave under bursts).
5. **Name the trade-offs explicitly** — every choice (SQL vs NoSQL, strong vs eventual consistency, push vs pull CDN, sharding key choice) has a cost. Interviewers are listening for whether you _know_ the cost, not just the benefit.
