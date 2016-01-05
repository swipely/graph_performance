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
  java_import 'org.apache.tinkerpop.gremlin.process.traversal.Order'

  order = case direction
          when 'DESC'
            Order.decr
          when 'ASC'
            Order.incr
          else
            raise "Unknown direction: #{direction}"
          end

  graph.traversal.V(list_vertex.id).out_e('members').order.by(order_by, order).take(limit)
end

raise ArgumentError, "Usage: #{__FILE__} <config_file> <query_by> <order> <limit>" unless ARGV.length == 4

config_file = ARGV[0]
query_by = ARGV[1]
order = ARGV[2]
limit = ARGV[3].to_i

graph = TitanFactory.open(config_file)

all_guests = graph.traversal.V.
               has_label('store').
               has('store_pretty_url', 'blue-star-cafe-and-pub-seattle').
               outE('guest_lists').
               has('guest_list_id', 'all').
               inV.
               next

puts "Total members of all guests list: #{graph.traversal.V(all_guests.id).out_e('members').to_a.count}"

startup_time = ((Time.now - start) * 1000).round(2)

duration = Benchmark.realtime do
  # Note: tried doing this the other way (querying for vertices then retrieving the members edge, but it was much slower)
  query(graph, all_guests, query_by, order, limit).each do |edge|
    # get the guest_id value from the vertex
    vertex = edge.get_vertex(1)
    prop = vertex.property('guest_id')
    if prop.present?
      prop.value
    else
      puts "No guest_id for vertex: #{vertex.id}"
    end

    # get all properties
    puts edge.properties.map { |property| "#{property.property_key.name} = #{property.value}" }.join(', ')
  end
end

puts({ startup_time_ms: startup_time, query_duration_ms: (duration * 1000).round(2) }.to_json)

exit!
