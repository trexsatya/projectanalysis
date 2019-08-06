ElasticSearch:

Wrapper Around Lucene:
    RobinEngine
        Store wraps Lucene's Directory
            directory ->indexWriter -> indexReader, indexSearcher
        Wrap Lucene's Create, Index, Delete, DeleteByQuery commands, and add entry to Translog also under readLock
        Wrap Lucene's Searcher into an AcquirableResource
        Wrap Lucene's IndexWriter#commit call into flush method under writeLock
        Implement snapshot() and recover() which work with Translog.Snapshot

        Acquire and Release Resource algorithms:
            Non-Blocking using CAS: NonBlockingAcquirableResource using AtomicBoolean, AtomicStampedReference and CAS
            Blocking: BlockingAcquirableResource using flag, counter and synchronized methods.
ES Concepts:
    Fields, Documents,
    Document -> Field*
    Index -> Type*
    Mapping: Type -> Fields
        A mapping can be defined explicitly or generated automatically when a document is indexed using templates. (Templates include settings and mappings that can be applied automatically to a new index.)


Extra Functionalities that ES Provides:
    Clustering => Nodes, Shards, Clusters
        Shard: Each shard is in itself a fully-functional and independent "index" that can be hosted on any node in the cluster.
            (By default, each index in Elasticsearch is allocated 5 primary shards and 1 replica which means that if you have at least two nodes in your cluster, your index will have 5 primary shards and another 5 replica shards (1 complete replica) for a total of 10 shards per index.)

        Node: nodeId
        Cluster -> Node*

    High-Availability => Replication, Failover


Clustering models:
    IndexShard

    Node

    ClusterState
    Cluster related events and handlers; ClusterChangedEvent, ClusterStateListener


Relationships:
    shardId -> IndexShard mapping
    indexName -> [shardId -> IndexShard] mapping

    IndexService manages the first relationship.
    IndicesService manages the second relationship.

