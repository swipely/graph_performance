puts 'starting script'
require 'json'
require 'pacer-titan'
require 'java'
java_import 'com.thinkaurelius.titan.core.Multiplicity'
java_import 'com.tinkerpop.blueprints.Direction'
java_import 'com.thinkaurelius.titan.core.Order'
java_import 'com.tinkerpop.blueprints.Vertex'

#
# Run as $bundle exec ruby scripts/test_data.rb {data_size} {sample_size}
# To create a store with data_size tickets and write out sample_size samples
# for testing

# get data size and set a reasonable default
data_size = ARGV[0].to_i
data_size = 100 if data_size == 0

# get sample size and set a reasonable default
sample_size = ARGV[1].to_i
sample_size = 100 if sample_size == 0

g = Pacer.titan('config/dynamo_testing.properties')

mgmt = g.blueprints_graph.get_management_system

puts 'Settting titan schema!'
storePrettyUrlKey = mgmt.makePropertyKey("store_pretty_url").dataType(java.lang.String.java_class).make()
dateKey = mgmt.makePropertyKey("date").dataType(java.lang.String.java_class).make()
ticketIdKey = mgmt.makePropertyKey("ticket_id").dataType(java.lang.String.java_class).make()

ticketsEdge = mgmt.makeEdgeLabel("tickets").multiplicity(Multiplicity::ONE2MANY).make()          # store tickets Ticket, etc

#store = mgmt.makeVertexLabel("store").partition.make()
store = mgmt.makeVertexLabel("store").make()
ticket = mgmt.makeVertexLabel("ticket").make()

mgmt.buildIndex("byStorePrettyUrl", Vertex.java_class).addKey(storePrettyUrlKey).unique().buildCompositeIndex()
mgmt.buildEdgeIndex(ticketsEdge,'byDateAndTicketId',Direction::OUT,Order::DESC,dateKey,ticketIdKey);

mgmt.commit()


puts 'Making data'

prng = Random.new(1234)
d = Date.new(2015,12,14)

tuples = []
g.transaction do

  store_vertex = g.create_vertex(label: 'store', store_pretty_url: "b_store")

  data_size.times do |i|

    props = {
      date: (d - prng.rand(1..1000)).to_s,
      ticket_id: (prng.rand(1..100000)).to_s
     }

    ticket_vertex = g.create_vertex(props.merge(label: 'ticket'))


    tuples << props if i % [1, data_size/sample_size].max == 0
    store_vertex.add_edges_to(:tickets, ticket_vertex, props)

    g.commit_implicit_transaction if i % 1000 == 0

  end
end

File.open("scratch/pacer_data.json","w") do |f|
  f.write(tuples.to_json)
end

puts "Tada - inserted #{data_size} tickets for a store and wrote #{tuples.size} properties to disk"
exit!