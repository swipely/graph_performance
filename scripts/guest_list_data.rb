puts 'starting script'
require 'json'
require 'pacer-titan'
require 'java'
java_import 'com.thinkaurelius.titan.core.Multiplicity'
java_import 'com.tinkerpop.blueprints.Direction'
java_import 'com.thinkaurelius.titan.core.Order'
java_import 'com.tinkerpop.blueprints.Vertex'

#
# Run as $bundle exec ruby scripts/guest_list_data.rb {data_size}
# To create a guest list vertex with data_size guests

# get data size and set a reasonable default
data_size = ARGV[0].to_i
data_size = 100 if data_size == 0


g = Pacer.titan('config/dynamo_local.properties')

mgmt = g.blueprints_graph.get_management_system

puts 'Settting titan schema!'
# mgmt.make_vertex_label('store_guest_list').partition.make # no partition by default
mgmt.make_vertex_label('store_guest_list').make
mgmt.make_vertex_label('guest').make

mgmt.make_edge_label('members').multiplicity(Multiplicity::ONE2MANY).make

guestListIdKey = mgmt.make_property_key('guest_list_id').data_type(java.lang.String.java_class).make
mgmt.make_property_key('guest_list_name').data_type(java.lang.String.java_class).make

totalSpendKey = mgmt.make_property_key('total_spend').data_type(java.lang.Float.java_class).make
avgSpendKey = mgmt.make_property_key('avg_spend').data_type(java.lang.Float.java_class).make
avgTipKey = mgmt.make_property_key('avg_tip').data_type(java.lang.Float.java_class).make
ticketsKey = mgmt.make_property_key('tickets').data_type(java.lang.Integer.java_class).make
coversKey = mgmt.make_property_key('covers').data_type(java.lang.Integer.java_class).make
elvKey = mgmt.make_property_key('elv').data_type(java.lang.Float.java_class).make
lastVisitKey = mgmt.make_property_key('last_visit').data_type(java.lang.String.java_class).make

mgmt.buildIndex("byGuestListId", Vertex.java_class).addKey(guestListIdKey).unique().buildCompositeIndex()
mgmt.buildEdgeIndex(ticketsEdge,'byTotalSpend',Direction::OUT,Order::DESC,totalSpendKey);
mgmt.buildEdgeIndex(ticketsEdge,'byAvgSpend',Direction::OUT,Order::DESC,avgSpendKey);
mgmt.buildEdgeIndex(ticketsEdge,'byAvgTip',Direction::OUT,Order::DESC,avgTipKey);
mgmt.buildEdgeIndex(ticketsEdge,'byTickets',Direction::OUT,Order::DESC,ticketsKey);
mgmt.buildEdgeIndex(ticketsEdge,'byCovers',Direction::OUT,Order::DESC,coversKey);
mgmt.buildEdgeIndex(ticketsEdge,'byElv',Direction::OUT,Order::DESC,elvKey);
mgmt.buildEdgeIndex(ticketsEdge,'byLastVisit',Direction::OUT,Order::DESC,lastVisitKey);

mgmt.commit()


puts 'Making data'

prng = Random.new(1234)
d = Date.new(2015,12,14)

tuples = []
g.transaction do

  store_vertex = g.create_vertex(
    label: 'store_guest_list',
    guest_list_name: 'a_list',
    guest_list_id: 'a'
    )

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