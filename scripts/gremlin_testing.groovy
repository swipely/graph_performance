// define the default TraversalSource to bind queries to.

import groovy.json.JsonSlurper

limit = 100 - 1

conf = new BaseConfiguration()
conf.setProperty("storage.dynamodb.prefix", "t_crm_titan")
conf.setProperty("schema.default", "none")

conf.setProperty("storage.dynamodb.force-consistent-read","false")
conf.setProperty("storage.dynamodb.max-self-throttled-retries","60")
conf.setProperty("storage.dynamodb.control-plane-rate","10")


conf.setProperty("storage.dynamodb.stores.edgestore.capacity-read","4000")
conf.setProperty("storage.dynamodb.stores.graphindex.capacity-read","4000")
conf.setProperty("storage.dynamodb.stores.titan_ids.capacity-read","20")
conf.setProperty("storage.dynamodb.stores.system_properties.capacity-read","20")
conf.setProperty("storage.dynamodb.stores.systemlog.capacity-read","20")
conf.setProperty("storage.dynamodb.stores.txlog.capacity-read","20")


conf.setProperty("storage.dynamodb.stores.edgestore.capacity-write","20000")
conf.setProperty("storage.dynamodb.stores.graphindex.capacity-write","4000")
conf.setProperty("storage.dynamodb.stores.titan_ids.capacity-write","20")
conf.setProperty("storage.dynamodb.stores.system_properties.capacity-write","20")
conf.setProperty("storage.dynamodb.stores.systemlog.capacity-write","20")
conf.setProperty("storage.dynamodb.stores.txlog.capacity-write","20")


conf.setProperty("storage.dynamodb.stores.edgestore.read-rate","4000")
conf.setProperty("storage.dynamodb.stores.graphindex.read-rate","4000")
conf.setProperty("storage.dynamodb.stores.titan_ids.read-rate","20")
conf.setProperty("storage.dynamodb.stores.system_properties.read-rate","20")
conf.setProperty("storage.dynamodb.stores.systemlog.read-rate","20")
conf.setProperty("storage.dynamodb.stores.txlog.read-rate","20")

conf.setProperty("storage.dynamodb.stores.edgestore.write-rate","20000")
conf.setProperty("storage.dynamodb.stores.graphindex.write-rate","4000")
conf.setProperty("storage.dynamodb.stores.titan_ids.write-rate","20")
conf.setProperty("storage.dynamodb.stores.system_properties.write-rate","20")
conf.setProperty("storage.dynamodb.stores.systemlog.write-rate","20")
conf.setProperty("storage.dynamodb.stores.txlog.write-rate","20")

conf.setProperty("storage.dynamodb.stores.edgestore.data-model","MULTI")
conf.setProperty("storage.dynamodb.stores.graphindex.data-model","MULTI")
conf.setProperty("storage.dynamodb.stores.titan_ids.data-model","MULTI")
conf.setProperty("storage.dynamodb.stores.system_properties.data-model","MULTI")
conf.setProperty("storage.dynamodb.stores.systemlog.data-model","MULTI")
conf.setProperty("storage.dynamodb.stores.txlog.data-model","MULTI")

conf.setProperty("storage.dynamodb.client.connection-max","250")
conf.setProperty("storage.dynamodb.client.retry-error-max","0")
conf.setProperty("storage.dynamodb.client.executor.max-pool-size","250")
conf.setProperty("storage.dynamodb.client.executor.max-queue-length","65400")

conf.setProperty("storage.backend", "com.amazon.titan.diskstorage.dynamodb.DynamoDBStoreManager")
conf.setProperty("storage.dynamodb.client.credentials.class-name","com.amazonaws.auth.DefaultAWSCredentialsProviderChain")
conf.setProperty("storage.dynamodb.client.credentials.constructor-args","")
conf.setProperty("storage.dynamodb.client.endpoint","https://dynamodb.us-east-1.amazonaws.com")
conf.setProperty("storage.buffer-size","65400")

conf.setProperty("storage.setup-wait","300000")
conf.setProperty("ids.block-size","100000")
conf.setProperty("storage.write-time","1 ms")
conf.setProperty("storage.read-time","1 ms")
//conf.setProperty("cluster.partition", "true")
//conf.setProperty("cluster.max-partitions", "32")
conf.setProperty("ids.flush", "false")


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

