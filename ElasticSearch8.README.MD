Model:
   
   Elasticsearch command for CLI:
        Elasticsearch (Is-A) EnvironmentAwareCommand (Is-A) Command
    
   Bootstrap#init()
        A singleton
        Create Node & validate required things; and Node#start()
         
   Node - represents a Node within a cluster
   
   Environment { dataFiles,repoFiles:Path[], configFile,pluginsFile,modulesFile,shardDataFiel,binFile,logsFile,libFile,pidFile,tmpFile }
   
   NodePath { path, indicesPath, FileStore } //Dir structure ${data.paths}/indices/{index.uuid}/{shard.id}

   ShardLock ///**
                * A shard lock guarantees exclusive access to a shards data
                * directory. Internal processes should acquire a lock on a shard
                * before executing any write operations on the shards data directory.
                *
                * @see NodeEnvironment
                */
   
   NodeEnvironment { NodePath[], sharedDataPath, locks:Lock[], ShardId->InternalShardLock } 
        InternalShardLock { mutex:Semaphore, waitCount, ShardId } //an inner class.
                     
    //A component that holds all data paths for a single node.
      ShardId->InternalShardLock is synchronized. InternalShardId's waitCount is guarded by this map. 
     /**
     * Tries to lock the given shards ID. A shard lock is required to perform any kind of
     * write operation on a shards data directory like deleting files, creating a new index writer
     * or recover from a different shard instance into it. If the shard lock can not be acquired
     * /
                    
   
       
       //A Node represent a node within a cluster ({@code cluster.name})//
       Node { Setting... , Environment, NodeEnvironment, NodeService, NamedWriteableRegistry }
            Node creates all the services required: NodeService, PluginsService, ResourceWatcherService, CircuitBreakerService, IngestService, UsageService, MetaStateService, IndicesService, MetaDataCreateIndexService, SearchService, SearchTransportService, TransportService, NetworkService, MetaDataIndexUpgradeService, ClusterInfoService, 
                PersistentTasksService, PersistentTasksClusterService, RepositoriesService, SnapshotsService, SnapshotShardsService, RestoreService, RerouteService
                Also creates Discovery
            
       DiscoveryNode { 
           String nodeName;
           String nodeId;
           String ephemeralId;
           String hostName;
           String hostAddress;
           TransportAddress address;
           Map<String, String> attributes;
           Version version;
           Set<DiscoveryNodeRole> roles;
       } //A discovery node represents a node that is part of the cluster.
   
   