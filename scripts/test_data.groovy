
import java.util.Random
import groovy.json.JsonBuilder

// Something is broken in the gremlin.sh script with arg parsing...
//data_size = (args.size() > 0) ? args[0].toInteger() : 100
//sample_size = (args.size() > 1) ? args[1].toInteger() : 100
data_size = 10000
sample_size = 1000


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
g = TitanFactory.open(conf)

mgmt = g.openManagement()

storePrettyUrlKey = mgmt.makePropertyKey("store_pretty_url").dataType(String.class).make()
dateKey = mgmt.makePropertyKey("date").dataType(String.class).make()
ticketIdKey = mgmt.makePropertyKey("ticket_id").dataType(String.class).make()

ticketsEdge = mgmt.makeEdgeLabel("tickets").multiplicity(Multiplicity.ONE2MANY).make()

//store = mgmt.makeVertexLabel("store").partition().make()
store = mgmt.makeVertexLabel("store").make()
ticket = mgmt.makeVertexLabel("ticket").make()

mgmt.buildIndex("byStorePrettyUrl", Vertex.class).addKey(storePrettyUrlKey).unique().buildCompositeIndex()
mgmt.buildEdgeIndex(ticketsEdge,"byDateAndTicketId",Direction.OUT,Order.decr,dateKey,ticketIdKey);

mgmt.commit()

println "Making data"

store_vertex = g.addVertex("store")
store_vertex.property("store_pretty_url", "b_store")

tuples = []

start_date = new Date(114,11,14) // That is 2015-12-14 - WTF?
rand = new Random()

data_size.times {

  date = (start_date - rand.nextInt(1000)).format("yyyy-MM-dd")
  ticket_id = "${rand.nextInt(100000)}"

  ticket_vertex = g.addVertex(label, "ticket")
  ticket_vertex.property("date", date)
  ticket_vertex.property("ticket_id", ticket_id)

  store_vertex.addEdge('tickets', ticket_vertex, 'date', date, "ticket_id", ticket_id)

  if (it % Math.max(1, (data_size/sample_size).toInteger()) == 0) {
    tuples += [date: date, ticket_id: ticket_id]
  }
}
//g.tx().commit()

new File('scratch/gremlin_data.json').write(new JsonBuilder(tuples).toPrettyString())

println "Closing..."
g.close()

println  "Tada - inserted ${data_size} tickets for a store and wrote ${tuples.size} properties to disk"

