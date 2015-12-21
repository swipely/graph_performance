start = Time.now
require 'pacer-titan'
require 'json'
require 'java'
require 'benchmark'
require 'json'

def query(graph, list_vertex, order_by, direction, limit)
  list_vtx = graph.blueprints_graph.get_vertex(list_vertex.element_id)

  java_import 'com.thinkaurelius.titan.graphdb.query.vertex.VertexCentricQueryBuilder'
  java_import 'com.tinkerpop.blueprints.Direction'
  java_import 'com.thinkaurelius.titan.core.Order'

  # uses Titan's query builder directly to gain access to the order_by functionality
  query_builder = VertexCentricQueryBuilder.new(list_vtx)
  query_builder
    .labels('members')
    .direction(Direction.value_of('OUT'))
    .order_by(order_by, Order.value_of(direction))
    .limit(limit)
    .edges
end

raise ArgumentError, "Usage: #{__FILE__} <config_file> <query_by> <order> <limit>" unless ARGV.length == 4

config_file = ARGV[0]
query_by = ARGV[1]
order = ARGV[2]
limit = ARGV[3].to_i

graph = Pacer.titan(config_file)

store_vertex = graph.v(label: 'store', store_pretty_url: 'blue-star-cafe-and-pub-seattle').first
all_guests = store_vertex.out_e(:guest_lists, guest_list_id: 'all').in_v(label: 'store_guest_list').first

startup_time = ((Time.now - start) * 1000).round(2)
duration = Benchmark.realtime { query(graph, all_guests, query_by, order, limit).to_a.last }

puts({ startup_time_ms: startup_time, query_duration_ms: (duration * 1000).round(2) }.to_json)

exit!
