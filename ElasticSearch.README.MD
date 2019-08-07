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
    
    Index { name } 
        => ShardId { Index, shardId: int }
            => ShardRouting { ShardId, state: ShardRoutingState & additional infos related to state (e.g. relocatingNodeId, currentNodeId etc) }
                => IndexShardRoutingTable { ShardId, listOf(ShardRouting) }
                    => IndexRoutingTable { index:String, shardId -> IndexShardRoutingTable }
                        => RoutingTable { index -> IndexRoutingTable }
                        
    Node { name, nodeId:String, TransportAddress, isDataNode:boolean }
        => Nodes { nodeId -> Node, maserNodeId, localNodeId } The idea is to represent each Node as Nodes for convenience.
        
     

    ClusterState { version:long, Nodes, RoutingTable, MetaData, RoutingNodes }
    Cluster related events and handlers; ClusterChangedEvent, ClusterStateListener

    //What's happening to Shard?
    ShardRoutingState { UNASSIGNED, INITIALIZING, STARTED, RELOCATING; }
        A model for representing the activity of current state of Shard is
        ShardRouting { id, relocatingId }

    IndexShard { IndexShardState, ShardRouting }  //directly deals with Engine

    RoutingNode to represent Node as a sequence of such ShardRouting

    RoutingNodes represents RoutingNode as an iterator which has mappings of nodeId -> RoutingNode mapping and knows which ShardRouting ones are unassigned, etc..
    
    IndexShardRoutingTable { Index, shardId, List<ShardRouting> }
    
    IndexMetaData { Index, mappings, totalNumberOfShards }
        => MetaData { index -> IndexMetaData, maxNumberOfShardsPerNode, totalNumberOfShards }
    
Relationships:

    shardId -> IndexShard mapping : IndexService manages this.
    indexName -> [shardId -> IndexShard] mapping : IndicesService manages this.

 
DiscoveryService:
    starts, stops, manages Discovery.
    Discovery is implemented as JgroupsDiscovery which works using JGroups library's ability of forming a cluster.
           Nodes connect to the JGroups Channel. First Node acts as master, other nodes accept 
           JGroups allows us to hook to cluster changes using @override accept(View view) method.
            Handle scenarios:
                - I'm master, notify ClusterService, newNodes? remove dead members
                - Otherwise, check if I was disconnected (not listed in Nodes); resend a Message
                
           JGroupsDiscovery detects whether it is master node, and notifies ClusterService by submitting ProcessedClusterStateUpdateTask if it is the creator of cluster (i.e. master) else notifies by submitting ClusterStateUpdateTask. 
                
              If not master, it sends a Message (with node details) on Channel which is listened by all members in the cluster.
              
              Receiver of this Message handles:
                - message from the master, the cluster state has changed.
                - direct message from a member that indicate his state has changed.
              
           Discovery Situations/srouces for interaction with ClusterService:
               On startup: 
                - jgroups-disco-initialconnect(master)
                - jgroups-disco-initialconnect
               On Message: 
                - jgroups-disco-receive(from master)
                - jgroups-disco-receive(from node[x])
               On accept(View) 
                - jgroups-disco-view 
                 
                          
    ClusterService        
            Executes ClusterStateUpdateTask to transition the ClusterState. It also notifies InitialStateDiscoveryListener [who are interested in this?] if not already done.
            Increments the ClusterState's version the task is from master (only the master controls the version numbers)
              --> new cluster state, notify all listeners, notify TransportService  that nodes added if any; 
                    notify TimeoutClusterStateListener(s), ClusterStateListener(s) by giving a ClusterChangeEvent { source:String, previousState, newState, firstMaster:boolean, NodesDelta }
                  if task is from the master, publish the new state to all nodes
            
            
What happens when cluster is changed?

    IndicesClusterStateService (a ClusterStateListener)
        -->> creates the Index if it needs to be created; and notify listeners interested in this event (that node has been created).
        Handle situations:
            the master thinks we are started, but we don't have this shard at all, mark it as failed
            
            update the shard's ShardRouting if it does not match with event's ShardRouting
            the master thinks we are initializing, but we are already started (either master failover, or a cluster event before we managed to tell the master we started), mark us as started

            if there is no shard, create it
            
            we are already recovering (we can get to this state since the cluster event can happen several times while we recover)
            
            For each ShardRouting apply recovery process.
            
        Create/Update mappings for each index
        
        go over and delete either all indices or specific shards that need to get deleted
                