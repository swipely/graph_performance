start = Time.now
require 'json'
require 'java'
require 'benchmark'
require 'json'
require 'lock_jar'

LockJar.load
require 'java'
java_import 'com.thinkaurelius.titan.core.TitanFactory'

def query(graph, list_vertex, order_by, direction, limit)
  java_import 'com.thinkaurelius.titan.graphdb.query.vertex.VertexCentricQueryBuilder'
  java_import 'org.apache.tinkerpop.gremlin.structure.Direction'
  java_import 'org.apache.tinkerpop.gremlin.process.traversal.Order'

  # uses Titan's query builder directly to gain access to the order_by functionality
  order = case direction
          when 'DESC'
            Order.decr
          when 'ASC'
            Order.incr
          else
            raise "Unknown direction: #{direction}"
          end
  query_builder = VertexCentricQueryBuilder.new(list_vertex)
  query_builder
    .labels('members')
    .direction(Direction.value_of('OUT'))
    .order_by(order_by, order)
    .limit(limit)
    .edges
end

raise ArgumentError, "Usage: #{__FILE__} <config_file> <query_by> <order> <limit>" unless ARGV.length == 4

config_file = ARGV[0]
query_by = ARGV[1]
order = ARGV[2]
limit = ARGV[3].to_i

graph = TitanFactory.open(config_file)

# TODO: figure out how to query for vertex by label
all_guests = graph.traversal.V.has('store_pretty_url', 'blue-star-cafe-and-pub-seattle').outE('guest_lists').has('guest_list_id', 'all').inV.next

startup_time = ((Time.now - start) * 1000).round(2)

duration = Benchmark.realtime do
  # Note: tried doing this the other way (querying for vertices then retrieving the members edge, but it was much slower)
  query(graph, all_guests, query_by, order, limit).each do |edge|
    # get the guest_id value from the vertex
    edge.get_vertex(1).property('guest_id').value

    # get all properties
    edge.properties.each do |property|
      property.property_key.name
      property.value
    end
  end
end

puts({ startup_time_ms: startup_time, query_duration_ms: (duration * 1000).round(2) }.to_json)

exit!
