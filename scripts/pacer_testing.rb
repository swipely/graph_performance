puts 'starting script'
require 'active_support/core_ext/object/try'
require 'pacer-titan'
require 'json'
require 'java'
require 'jruby/profiler'

methods = %w(filter vci)
method_name = ARGV[0]
fail "Error - First argument must be one of #{methods}, you specified '#{method_name}'" unless methods.include? method_name


# read data for a few edges from disk
tuples = File.open("scratch/script_data.json","r") { |f| JSON.parse(f.read, symbolize_names:true) }

# allow limit on the number of samples to fetch
limit = ARGV[1].to_i
fail if limit > tuples.size
tuples = tuples[0..limit] if limit > 0 # ignore if not set or 0

# open the graph
g = Pacer.titan('config/dynamo_local.properties')

# get the store vertex
t1 = Time.now
store_vertex = g.v(store_pretty_url: "b_store").first
fail 'No store - b_store' if store_vertex.nil?
puts "Got store vertex in: #{Time.now - t1} seconds"


puts 'Getting vertex by edge properties'

def pacer_edge_filter(sv, tuples)
  ticket = nil
  profile_data = JRuby::Profiler.profile do
    tuples.each do |props|
      ticket = sv.out_e(:tickets, props).in_v.first
      fail 'No ticket found' if ticket.nil?
    end
  end
  puts "last ticket #{ticket.properties}"
  profile_data
end


def pacer_vci(sv, tuples)
  ticket = nil
  profile_data = JRuby::Profiler.profile do
    tuples.each do |props|
      ticket = sv.vertex_query('tickets', :out, element_type: :edge) { has('date', props[:date]).has('ticket_id', props[:ticket_id]) }.in_v.first
      fail 'No ticket found' if ticket.nil?
    end
  end
  puts "last ticket #{ticket.properties}"
  profile_data
end

profile_data = case method_name
when 'vci'
  profile_data = pacer_vci(store_vertex, tuples)
when 'filter'
  profile_data = pacer_edge_filter(store_vertex, tuples)
end


profile_printer = JRuby::Profiler::FlatProfilePrinter.new(profile_data)
profile_printer.printProfile(STDOUT)


data_size = store_vertex.out_e(:tickets).count

puts "Tada - Got #{tuples.size} of #{data_size} edges using #{method_name}!"
exit!