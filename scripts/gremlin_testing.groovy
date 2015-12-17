// define the default TraversalSource to bind queries to.

import groovy.json.JsonSlurper

limit = 10 - 1

conf = new BaseConfiguration()
conf.setProperty("storage.backend", "com.amazon.titan.diskstorage.dynamodb.DynamoDBStoreManager")
conf.setProperty("storage.dynamodb.client.endpoint", "http://localhost:9389")
conf.setProperty("schema.default", "none")
conf.setProperty("cluster.partition", "true")
conf.setProperty("cluster.max-partitions", "32")
conf.setProperty("ids.flush", "false")
conf.setProperty("storage.dynamodb.stores.edgestore.write-rate","20000")
conf.setProperty("storage.dynamodb.stores.graphindex.write-rate","20000")
conf.setProperty("storage.dynamodb.stores.edgestore.read-rate","20000")
conf.setProperty("storage.dynamodb.stores.graphindex.read-rate","20000")

graph = TitanFactory.open(conf)

g = graph.traversal()

sv = g.V().has('store_pretty_url','b_store').next()

tuples = new JsonSlurper().parseText(new File('scratch/gremlin_data.json').text)[0..limit]

println 'starting test'
result = null

ts = System.currentTimeMillis()
for (it in tuples) {
  result = g.V(sv).outE('tickets').has('date',it['date']).has('ticket_id',it['ticket_id']).inV().next()
}
te = System.currentTimeMillis()

println g.V(sv).outE('tickets').has('date',tuples[0]['date']).has('ticket_id',tuples[0]['ticket_id']).inV()

data_size = g.V(sv).outE('tickets').count().next()

println "Elapsed Time ${(te - ts)/1000.0}, tickets ${data_size}, samples ${tuples.size}"
//graph.close()
//println "Tada!"

