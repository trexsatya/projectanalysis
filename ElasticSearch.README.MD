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


