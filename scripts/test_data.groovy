
import java.util.Random
import groovy.json.JsonBuilder

// Something is broken in the gremlin.sh script with arg parsing...
//data_size = (args.size() > 0) ? args[0].toInteger() : 100
//sample_size = (args.size() > 1) ? args[1].toInteger() : 100
data_size = 100
sample_size = 10


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


g = TitanFactory.open(conf)

mgmt = g.openManagement()

storePrettyUrlKey = mgmt.makePropertyKey("store_pretty_url").dataType(String.class).make()
dateKey = mgmt.makePropertyKey("date").dataType(String.class).make()
ticketIdKey = mgmt.makePropertyKey("ticket_id").dataType(String.class).make()

ticketsEdge = mgmt.makeEdgeLabel("tickets").multiplicity(Multiplicity.ONE2MANY).make()

store = mgmt.makeVertexLabel("store").partition().make()
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

