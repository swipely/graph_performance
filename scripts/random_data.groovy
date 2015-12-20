
import java.util.Random
import groovy.json.JsonBuilder

// Something is broken in the gremlin.sh script with arg parsing...
//data_size = (args.size() > 0) ? args[0].toInteger() : 100
//sample_size = (args.size() > 1) ? args[1].toInteger() : 100
data_size = 100000
edge_size = 10


conf = new BaseConfiguration()
conf.setProperty("storage.dynamodb.prefix", "t_crm_titan")
conf.setProperty("schema.default", "none")
conf.setProperty("storage.batch-loading","true")

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
//conf.setProperty("storage.dynamodb.client.endpoint","http://localhost:9389")


conf.setProperty("storage.buffer-size","65400")

conf.setProperty("storage.setup-wait","300000")
conf.setProperty("ids.block-size","100000")
//conf.setProperty("storage.write-time","100 ms")
//conf.setProperty("storage.read-time","100 ms")
//conf.setProperty("cluster.partition", "true")
//conf.setProperty("cluster.max-partitions", "32")
conf.setProperty("ids.flush", "false")
g = TitanFactory.open(conf)

mgmt = g.openManagement()

//storePrettyUrlKey = mgmt.makePropertyKey("store_pretty_url").dataType(String.class).make()
//dateKey = mgmt.makePropertyKey("date").dataType(String.class).make()
//ticketIdKey = mgmt.makePropertyKey("ticket_id").dataType(String.class).make()

ticketsEdge = mgmt.makeEdgeLabel("tickets").multiplicity(Multiplicity.MULTI).make()

//store = mgmt.makeVertexLabel("store").partition().make()
//store = mgmt.makeVertexLabel("store").make()
ticket = mgmt.makeVertexLabel("ticket").make()

//mgmt.buildIndex("byStorePrettyUrl", Vertex.class).addKey(storePrettyUrlKey).unique().buildCompositeIndex()
//mgmt.buildEdgeIndex(ticketsEdge,"byDateAndTicketId",Direction.OUT,Order.decr,dateKey,ticketIdKey);

mgmt.commit()

println "Finished setting schema - sleeping 30 seconds"

sleep(30000)

println "Making data"

tickets = []

rand = new Random()

ts = System.currentTimeMillis()
data_size.times {
  tickets += g.addVertex(label, "ticket")
}
te = System.currentTimeMillis()
println "committing vertices"
g.tx().commit()
tf = System.currentTimeMillis()
println "Made vertices in ${(te - ts)/1000.0} s and committed in ${(tf - te)/1000.0} s"

ts = System.currentTimeMillis()
data_size.times {
  t1_i = it
  t1 = tickets[t1_i]

  edge_size.times {
    t2_i = (t1_i + 1 + rand.nextInt(data_size - 1) ) % data_size
    t2 = tickets[t2_i]

    t1.addEdge('tickets', t2)
  }
}
te = System.currentTimeMillis()
println "committing edges"
g.tx().commit()
tf = System.currentTimeMillis()
println "Made edges in ${(te - ts)/1000.0} s and committed in ${(tf - te)/1000.0} s"


// new File('scratch/gremlin_data.json').write(new JsonBuilder(tuples).toPrettyString())

println "Closing..."
g.close()

println  "Tada - inserted ${data_size} tickets and ${edge_size} edges"

