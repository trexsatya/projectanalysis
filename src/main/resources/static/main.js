var sampleNetworkData = {
        nodes: [
                {id: '1', label: 'Node 1'},
                {id: '2', label: 'Node 2'},
                {id: 3, label: 'Node 3'},
                {id: 4, label: 'Node 4'},
                {id: 5, label: 'Node 5'},
                {id: 6, label: 'Node 6'},
                {id: 7, label: 'Node 7'},
                {id: 8, label: 'Node 8'}
        ],
        edges: [
               {from: 1, to: 8, arrows:'to', dashes:true},
               {from: 1, to: 3, arrows:'to'},
               {from: 1, to: 2, arrows:'to, from'},
               {from: 2, to: 4, arrows:'to, middle'},
               {from: 2, to: 5, arrows:'to, middle, from'},
               {from: 5, to: 6, arrows:{to:{scaleFactor:2}}},
               {from: 6, to: 7, arrows:{middle:{scaleFactor:0.5},from:true}}
       ]
}

function showSampleData(id) {
    var nodes = new vis.DataSet(sampleNetworkData.nodes);
    var edges = new vis.DataSet(sampleNetworkData.edges);

    var container = document.getElementById(id);

    var data = {
        nodes: nodes,
        edges: edges
    };
    var options = {};

    var network = new vis.Network(container, data, options);
    return network;
}

function nodeLabel(nId){
    var splits = nId.split(".")
    return splits[splits.length-1]
}

function loadGraphData(container){
    fetch('/api/graphData').then(res => res.json()).then(it => {
         var nodes = Object.keys(it.nodes).map(k => {
                 var n = it.nodes[k];

                 var nm = nodeLabel(n.name);

                 var node = {id: n.name, label: n.type == 'CLASS' ? nm : "<<" + nm + ">>", color: 'skyblue', title: n.name}

                 if(n.type == 'INTERFACE') {
                    node.color = 'yellow'
                 }
                 return node
         });
//         console.log(nodes);
        //edges [{key : { node: { name, type }, relationshipType }, value: }]
         var edges = Object.keys(it.edges)
            .map(k => {
                var e = it.edges[k].key;
                var edge = {from: e.node.name, to: it.edges[k].value }
                if(e.relationshipType == 'IS_LIKE' || e.relationshipType == 'IS_A' || e.relationshipType == 'IS_COMPOSED_OF') {
                    edge.arrows = 'to'
                }
                if(e.relationshipType == 'IS_COMPOSED_OF') {
                    edge.dashes = true
                }
                if(e.referenceName && e.referenceName.trim().length) {
                    edge.label = e.referenceName
                    edge.font = { size: 9, align: 'middle'}
                }
                return edge;
            })

//Only nodes which are linked?
        nodes = nodes.filter(n => edges.find(e => e.from == n.id || e.to == n.id))
//         console.log(edges);

         var data = {
                 nodes: nodes,
                 edges: edges
         };
         var options = {
            layout: {
                improvedLayout: false,
                hierarchical: {
                    enabled: false,
                    edgeMinimization: false,
                    sortMethod: 'directed' //or 'hubsize'
                }
            },
            physics: {
                enabled: false,
                solver: 'forceAtlas2Based', //'barnesHut', 'repulsion', 'hierarchicalRepulsion', 'forceAtlas2Based'
                stabilization: {
                      iterations: 10, fit: false
                }
            }
         };

         container = document.getElementById(container);
         var network = new vis.Network(container, data, options);

         var hashSet = {}
         Object.keys(it.nodes).map(it => it.split(".").slice(0,-1).join(".") ).forEach(it => hashSet[it] = true)

         var groups = Object.keys(hashSet)

//         for( var n in groups) {
//            var group = groups[n];
//            network.cluster({
//                joinCondition: item => {
//                     return item.id.split('.').slice(0,-1).join('.') == group
//                },
//                clusterNodeProperties: { label: group }
//            })
//         }
         window.loadedNetwork = {
            originalData: data,
            data : data,
            options: options,
            container: container,
            graph: graph(data),
            network: network
         }
         return network;
    })

}

function graph(data){
    var graph = new Graph()

    data.edges.forEach(e => graph.addEdge(e.from, e.to, true))

    return graph;
}

function Graph(){
    this.adjList = {}
}

Graph.prototype.addEdge = function(x,y, bidirectional) {
    var existingX = (this.adjList[x] || [])
    existingX.push(y);
    this.adjList[x] = existingX;

    if(bidirectional) {
        var existingY = (this.adjList[y] || [])
        existingY.push(x);
        this.adjList[y] = existingY;
    }
}

Graph.prototype.bfs = function(source){
    var visited = {}
    var queue = []
    visited[source] = true

    queue.push(source)
    while(queue.length > 0) {
        source = queue.shift()
        this.adjList[source].forEach(nbr => {
            if(!visited[nbr]) {
                visited[nbr] = true;
                queue.push(nbr);
            }
        })
    }
}

Graph.prototype.dfs = function(source, maxDepth, depth, visited, result) {
    maxDepth = Number.parseInt(maxDepth) || -1;
    depth = depth || 0;
    visited = visited || {}
    result = result || {}

    var shouldContinue = maxDepth < 0 || depth < maxDepth
    if(!shouldContinue) return Object.keys(result);

    visited[source] = true;
    result[source] = 1;

    var neighbors = (this.adjList[source] || [])

    if(depth >= 1 && neighbors.length > 10 ) {
        result[createSpecialNode(neighbors, source)] = 1
        neighbors = []
    }

    neighbors.forEach(nbr => {
        if(!visited[nbr]) {
            this.dfs(nbr, maxDepth, depth+1, visited, result);
        }
    });

    return Object.keys(result)
}

function createSpecialNode(neighbors, source){
    return 'SPL: ' + JSON.stringify({ connections: neighbors, source: source})
}

function parseSpecialNode(string){
    if(string.startsWith('SPL: ')) {
        var splits = string.split('SPL: ')
        return JSON.parse(splits[splits.length-1])
    }
    return null
}

function testGraph(){
    var graph = new Graph()
    graph.addEdge(1,2);
    graph.addEdge(1,3);
    graph.addEdge(2,4);
    graph.addEdge(3,4);
    graph.addEdge(4,5);

    var result = graph.dfs(1)
    var expected = [1,2,4,5, 3]
    console.log( JSON.stringify(expected) ==  JSON.stringify(result) ? "OK" : "TEST_FAILED "+ "Expected " + expected + " Found " + result)

};
testGraph()

