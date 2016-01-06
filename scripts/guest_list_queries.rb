start = Time.now
require 'json'
require 'java'
require 'benchmark'
require 'json'
require 'lock_jar'

LockJar.load
require 'java'
java_import 'com.thinkaurelius.titan.core.TitanFactory'

def query(graph, list_vertex, order_by, direction, limit, proc)
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

  edges = query_builder
    .labels('members')
    .direction(Direction.value_of('OUT'))
    .order_by(order_by, order)
    .limit(limit)
    .edges

  edges.each_with_index(&proc)
end

def read_edge
  proc do |edge, index|
    edge.properties.each do |property|
      property.property_key.name
      property.value
    end
  end
end

def show_edge
  proc do |edge, index|
    puts edge.properties.map { |property| "#{property.property_key.name} = #{property.value}" }.join(', ')
  end
end

def read_vertex_and_edge
  proc do |edge, index|
    # get the guest_id value from the vertex
    vertex = edge.get_vertex(1)
    prop = vertex.property('guest_id')
    if prop.present?
      prop.value
      #else
      #puts "No guest_id for vertex: #{vertex.id}"
    end

    # get all properties
    edge.properties.each do |property|
      property.property_key.name
      property.value
    end
  end
end

raise ArgumentError, "Usage: #{__FILE__} <config_file> <query_by> <order> <limit> <store1>... <storeN>" unless ARGV.length == 5

java_import 'java.lang.System'
System.set_property('tinkerpop.profiling', 'true')

config_file = ARGV[0]
query_by = ARGV[1]
order = ARGV[2]
limit = ARGV[3].to_i
stores = (ARGV[4] || '').split(',')

graph = TitanFactory.open(config_file)

all_guests_by_store = Hash[stores.map do |store|
  all_guests = graph.traversal.V.
    has_label('store').
    has('store_pretty_url', store).
    outE('guest_lists').
    has('guest_list_id', 'all').
    inV.
    next

  # $stderr.puts(
  #   "Total members of all guests list at store #{store}: " +
  #   "#{graph.traversal.V(all_guests.id).out_e('members').to_a.count}"
  # )

  [store, all_guests]
end]

startup_time = ((Time.now - start) * 1000).round(2)

results = (stores * 2).map do |store|
  duration = Benchmark.realtime do
    # Note: tried doing this the other way (querying for vertices then retrieving the members edge, but it was much slower)
    query(graph, all_guests_by_store[store], query_by, order, limit, read_edge)
  end

  { startup_time_ms: startup_time, store: store, query_duration_ms: (duration * 1000).round(2) }
end

puts results.to_json

exit!
