function nodeLabel(nId) {
    if (!nId) return ""
    var splits = nId.split(".")
    return splits[splits.length - 1]
}

function ClassData(it) {
    let nodes = []
    let counter = 0
    //Class nodes
    function createNodeIfRequired(label, type, typeSpecificData) {
        var found = nodes.find(it => it.fullName === label)
        if (found) {
            return found
        }
        let id = typeSpecificData.id
        var newNode = {
            id: id + "",
            label: nodeLabel(label),
            type: type,
            fullName: label,
            typeSpecificData: typeSpecificData,
            shape: 'box'
            //Add special configs like shape: 'box/star/triangle', color: 'red', size: 5
        };
        nodes.push(newNode)
        return newNode
    }

    Object.keys(it.nodes).forEach(k => {
        var n = it.nodes[k];
        if(n.name.startsWith("com.klarna")) {
            let node = createNodeIfRequired(n.name, n.type, n)
        }
    });
//         console.log(nodes);
    //edges [{key : { node: { name, type }, relationshipType }, value: }]
    var edges = Object.keys(it.edges)
        .map(k => {
            var e = it.edges[k].key;
            let fromNode = nodes.find(n => n.id+"" === e.node+"")
            let toNode = nodes.find(n => n.id+"" === e.target+"")
            if(fromNode == null || toNode == null) {
                return null
            }
            var edge = {from: fromNode.id, to:  toNode.id, relationshipType: e.relationshipType}
            if(e.relationshipType === 'IS_COMPOSED_OF') {
                edge.arrows = 'to'
            }
            if(e.relationshipType === 'IS_COMPOSED_OF') {
                edge.dashes = true
            }
            if(e.referenceName && e.referenceName.trim().length) {
                edge.label = e.referenceName
                edge.font = { size: 9, align: 'middle'}
            }
            return edge;
        })
        .filter(it => it != null)
        .filter(it => it.relationshipType === "IS_COMPOSED_OF")

        let findNode = (id) => nodes.find(n => n.id+'' === id + '')
        let findNodeByName = (name) => nodes.find(n => n.fullName+'' === name + '')

        //Only nodes which are linked?
        nodes = nodes.filter(n => edges.find(e => e.from === n.id || e.to === n.id))
        //console.log(edges);

        let CG = graph({nodes, edges});
        let CGG = new graphlib.Graph({directed: true, compound: true})

        Object.keys(CG.adjList).forEach(k => {
            CG.adjList[k].forEach(v => CGG.setEdge(findNode(k).fullName, findNode(v).fullName))
        })

        return  {
            nodes: nodes,
            edges: edges,
            graph: CGG,
            classesWithAnnotation: function(regex) {
                return nodes
                    .filter(it => it.type === "CLASS" &&
                        it.typeSpecificData.classData &&
                        it.typeSpecificData.classData.annotations.map(a => a.name).find(n => n.match(".*" + regex + ".*") != null)
                    )
            },
            reachableClasses: function(cls){
                let res = graphlib.alg.dijkstra(CGG, cls)
                let classes = []
                Object.keys(res).forEach(k => {
                    if(res[k].distance === Infinity) {
                        delete res[k]
                    } else {
                        classes.push({path: res[k], node: findNodeByName(cls)})
                    }
                })
                return classes
            }
        };
}

let counter = 0
let nodes = []
function createNodeIfRequired(label, type, typeSpecificData) {
    var found = nodes.find(it => it.fullName === label)
    if (found) {
        return found
    }
    var newNode = {
        id: (counter++) + "",
        label: nodeLabel(label),
        type: type,
        fullName: label,
        typeSpecificData: typeSpecificData,
        shape: 'box'
        //Add special configs like shape: 'box/star/triangle', color: 'red', size: 5
    };
    nodes.push(newNode)
    return newNode
}

function classNodesForNames(names) {
    return names.map(nm => loadedNetwork.data.classData.nodes.find(n => n.label+'' === nm + ''))
}

function classNodesForIds(ids) {
   return ids.map(id => loadedNetwork.data.classData.nodes.find(n => n.id+'' === id + ''))
}

function buildNetwork(eventGraphData, classData, container) {
    var data = eventGraphData.events

    //Construct graph data
    //Node id, label, type, typeSpecificData
    var edges = []

    var counter = 0;

    for (const k of Object.keys(data)) {
        const eventType = k;
        //TODO: Fix the logic, why non events are included in eventGraphData
        if(!eventType.startsWith("com.klarna.dcpipe.service.domain.event") &&
            !eventType.startsWith("com.klarna.dcpipe.service.domain.command")) {
            continue
        }
        data[k].producers.forEach(p => {
            var producerNode = createNodeIfRequired(p.invocationDetail.className, 'PRODUCER', p.invocationDetail)
            if(!data[k].consumers.length) {
                //No consumer => something external must be consuming it
                data[k].consumers.push({methodDetail: {fullyQualifiedClassName: eventType + ".X?"}})
            }
            data[k].consumers.forEach(c => {
                var consumerNode = createNodeIfRequired(c.methodDetail.fullyQualifiedClassName, 'CONSUMER', c.methodDetail)
                edges.push({
                    from: producerNode.id, to: consumerNode.id, arrows: 'to', label: eventType
                })
                // Consumer may not be producing directly,
                // find the references of the consumer,
                // and see if any of the references is a producer, this can be recursive

                classData.edges.filter(e =>
                    e.key.node.name === c.methodDetail.fullyQualifiedClassName &&
                    e.key.relationshipType === 'IS_COMPOSED_OF'
                ).filter(e => {
                    //Is a producer
                    return Object.keys(data).find(k => data[k].producers.find(p => p.invocationDetail.className === e.key.target) != null)
                }).forEach(e => {
                    var refNode = createNodeIfRequired(e.key.target, 'REFERENCE', e.key)
                    edges.push({
                        from: consumerNode.id, to: refNode.id, arrows: 'to', dashes:true, label: eventType
                    })
                })
            })
        })
    }

    //Only nodes which are linked?
    nodes = nodes.filter(n => edges.find(e => e.from === n.id || e.to === n.id))
    //console.log(edges);

    var graphDataJson = {
        nodes: nodes,
        edges: edges
    };
    const options = {
        layout: {
            improvedLayout: true,
            hierarchical: {
                enabled: true,
                edgeMinimization: true,
                sortMethod: 'directed' //or 'hubsize'
            }
        },
        physics: {
            enabled: false,
            solver: 'forceAtlas2Based', //'barnesHut', 'repulsion', 'hierarchicalRepulsion', 'forceAtlas2Based'
            stabilization: {
                iterations: 130, fit: false
            }
        }
    };

    container = document.getElementById(container);
    var network = null; //new vis.Network(container, graphDataJson, options);

    var hashSet = {}
    // Object.keys(eventGraphData.nodes).map(it => it.split(".").slice(0, -1).join(".")).forEach(it => hashSet[it] = true)

    var groups = Object.keys(hashSet)

    window.classData = ClassData(classData)
    window.loadedNetwork = {
        originalData: data,
        data: {nodes: nodes, edges: edges},
        options: options,
        container: container,
        graph: graph({nodes: nodes, edges: edges}),
        network: network
    }
    //Generate seq diagram data
    connectedComponents(loadedNetwork.graph).forEach(cc => {
        var output = {} //as set
        //Find the starting point; Pick anyone which does not have producer
        var withoutAnyProducer = cc.filter(it => !loadedNetwork.data.edges.find(e => e.to === it))
        withoutAnyProducer.sort((a, b) => {
            a = loadedNetwork.data.nodes[a]
            b = loadedNetwork.data.nodes[b]
            if(a.label.indexOf("EventHandler") >= 0) {
                return -1
            }
            return a.label.localeCompare(b.label)
        })
        let kafkaListeners = window.classData.classesWithAnnotation("KafkaListener")

        // withoutAnyProducer.forEach(wap => {
        //     let wapNode = loadedNetwork.data.nodes.find(n => n.id +"" === wap)
        //     //Outgoing from this
        //     loadedNetwork.data.edges.filter(it => it.from + "" === wap).forEach(e => {
        //         output[(loadedNetwork.data.nodes[e.from].label + " -> " + loadedNetwork.data.nodes[e.to].label + ": " + nodeLabel(e.label))] = 1;
        //     })
        //     //Incoming: See if a KafkaListener links to this
        // })
        var sourceNodes = {}
        cc.forEach(ccNode => {
            loadedNetwork.data.edges.filter(it => it.from + "" === ccNode ||  it.to +"" === ccNode).forEach(e => {
                output[(loadedNetwork.data.nodes[e.from].label + " -> " + loadedNetwork.data.nodes[e.to].label + ": " + nodeLabel(e.label))] = loadedNetwork.data.nodes[e.from];
                sourceNodes[loadedNetwork.data.nodes[e.from].id] = loadedNetwork.data.nodes[e.from]
            })
        })
        // Bring Kafka Listeners and APIs to front
        let seqDiagramEntities = Object.keys(output)

        const isKafkaListener = clsName => kafkaListeners.map(kl => kl.label).indexOf(clsName) >= 0
        const isApiImpl = clsName => clsName.indexOf("Api") >= 0
        const anythingElse = clsName => true

        //Predicates for sort order
        const sortOrder = [ isKafkaListener, isApiImpl, anythingElse]

        seqDiagramEntities = seqDiagramEntities.sort((a, b) => {
                a = output[a]
                b = output[b]
                return sortOrder.findIndex(predicate => predicate(a.label)) - sortOrder.findIndex(predicate => predicate(b.label))
        })

        console.log(seqDiagramEntities.join("\n"))
    })
    return network;
}

function loadGraphData(container){
    fetch('/api/eventGraphData').then(res => res.json()).then(eventGraphData => {
        return fetch('/api/graphData').then(res => res.json()).then(classData => {
            return buildNetwork(eventGraphData, classData, container);
        })
    })
}

function connectedComponents(graph) {
    let visited = []
    let connected = []
    graph.data.nodes.forEach(n => {
        if(!visited.includes(n.id + "")) {
            let island = graph.dfs(n.id);
            island.forEach(it => visited.push(it))
            connected.push(island)
        }
    })
    return connected
}

function graph(data){
    var graph = new Graph(data)

    data.edges.forEach(e => graph.addEdge(e.from, e.to, true))

    return graph;
}

function Graph(data){
    this.data = data
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

Graph.prototype.path = function(source, target) {
    let visited = {}
    let stack = []
    stack.push(source)
    visited[source] = 1
    let parents = {}
    while(stack.length) {
        let node = stack.pop()
        let neighbors = (this.adjList[node] || [])

        for(let nbr in neighbors) {
            nbr = neighbors[nbr]
            parents[nbr] = node
            if(nbr+'' === target + '') {
                let parent = parents[target]
                let pathArr = []
                pathArr.push(target)
                while (parent) {
                    pathArr.push(parent)
                    parent = parents[parent]
                }
                return pathArr
            }
            if(!visited[nbr]) {
                stack.push(nbr)
                visited[nbr] = 1
            }
        }
    }

    return parents
}

Graph.prototype.dfs = function(source) {
    let visited = {}
    let stack = []
    stack.push(source)
    visited[source] = 1
    while(stack.length) {
        let node = stack.pop()
        let neighbors = (this.adjList[node] || [])

        neighbors.forEach(nbr => {
            if(!visited[nbr]) {
                stack.push(nbr)
                visited[nbr] = 1
            }
        });
    }

    return Object.keys(visited)
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

function testGraph() {
    var graph = new Graph({nodes: [], edges: []})
    graph.addEdge(1,2);
    graph.addEdge(1,3);
    graph.addEdge(2,4);
    graph.addEdge(3,4);
    graph.addEdge(4,5);

    var result = graph.dfs(1)
    var expected = [1,2,4,5, 3]
    console.log( JSON.stringify(expected) ==  JSON.stringify(result) ? "OK" : "TEST_FAILED "+ "Expected " + expected + " Found " + result)

}
testGraph()

function getHtmlForMethod(x){
    var $ = jQuery;
    var span = x => $('<span>').html(x)

    var annotations = x.annotations.map(x => span("@"+ nodeLabel(x)));

    var params = Object.keys(x.paramList).map(paramName => {
                            var paramDiv = $('<div>').addClass('param')
                                      .append(span(nodeLabel(x.paramList[paramName])).addClass('paramType'))
                                      .append(span(" "))
                                      .append(span(paramName).addClass('paramName'))

                            return paramDiv
                        });

    var div = $('<div>')
    annotations.forEach(a => div.append(a))
    div.append(span(nodeLabel(x.returnType)).addClass('returnType'))
    div.append(span(" "))
    div.append(span(x.name).addClass('methodName'))
    div.append(span("("))
    params.forEach(p => div.append(p))
    div.append(span(")"))
    return div.html();
}
