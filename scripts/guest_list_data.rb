require 'json'
require 'pacer-titan'
require 'optparse'
require 'logger'
java_import 'java.lang.System'
java_import 'java.util.Random'
java_import 'com.thinkaurelius.titan.core.Multiplicity'
java_import 'com.tinkerpop.blueprints.Direction'
java_import 'com.thinkaurelius.titan.core.Order'
java_import 'com.tinkerpop.blueprints.Vertex'

$logger = Logger.new(STDOUT)

class KPI
  def self.value_of(*kpi_names)
    kpi_names.map { |kpi_name| kpis[kpi_name] }
  end

  attr_reader :name

  def initialize(name, type, sort_order)
    @name = name
    @type = type
    @sort_order = Order.value_of(sort_order)
  end

  def build_schema(mgmt, members_edge, not_indexed)
    property = mgmt.make_property_key(name).data_type(@type).make
    unless not_indexed
      mgmt.build_edge_index(members_edge, "members_by_#{name}", Direction::OUT, @sort_order, property)
    end
  end

  def generate_value(random)
    case name
    when 'total_spend'
      gaussian(random, 12500, 8700).to_i
    when 'customer_name'
      "#{fake_name(random)} #{fake_name(random)}"
    when 'last_visit'
      (Time.now - random.next_int((365 * 24 * 60 * 60) * 3)).iso8601
    when 'loyalty'
      (random.next_int(100) < 20) ? 1 : 0
    when 'visits'
      gaussian(random, 5, 3).to_i
    when 'average_spend'
      gaussian(random, 2900, 900).to_i
    end
  end

  private

  def gaussian(random, mean, std_dev, reject_negatives = true)
    loop do
      value = (random.next_gaussian * std_dev) + mean
      break value unless reject_negatives && value < 0
    end
  end

  def fake_name(random)
    length = random.next_int(9) + 3
    length.times.map { (random.next_int(26) + 97).chr }.join.capitalize
  end

  def self.kpis
    {
      'total_spend' => new('total_spend', java.lang.Integer.java_class, 'DESC'),
      'customer_name' => new('customer_name', java.lang.String.java_class, 'ASC'),
      'last_visit' => new('last_visit', java.lang.String.java_class, 'DESC'),
      'loyalty' => new('loyalty', java.lang.Integer.java_class, 'DESC'),
      'visits' => new('visits', java.lang.Integer.java_class, 'DESC'),
      'average_spend' => new('average_spend', java.lang.Integer.java_class, 'DESC')
    }
  end
end

def build_schema(graph, kpis, not_indexed, not_partitioned)
  $logger.debug 'Building the schema'
  mgmt = graph.blueprints_graph.get_management_system

  define_vertex(mgmt, 'store', not_partitioned)
  define_vertex(mgmt, 'store_guest', true)
  define_vertex(mgmt, 'store_guest_list', not_partitioned)

  members_edge = mgmt.make_edge_label('members').multiplicity(Multiplicity::ONE2MANY).make
  mgmt.make_edge_label('guest_lists').multiplicity(Multiplicity::ONE2MANY).make

  store_pretty_url_key = mgmt.make_property_key('store_pretty_url').data_type(java.lang.String.java_class).make
  mgmt.make_property_key('guest_id').data_type(java.lang.String.java_class).make
  mgmt.make_property_key('guest_list_id').data_type(java.lang.String.java_class).make
  mgmt.make_property_key('guest_list_name').data_type(java.lang.String.java_class).make

  kpis.each do |kpi|
    kpi.build_schema(mgmt, members_edge, not_indexed)
  end

  mgmt.build_index("by_store_pretty_url", Vertex.java_class).add_key(store_pretty_url_key).unique.buildCompositeIndex

  mgmt.commit
  $logger.debug 'Done building the schema'
end

def define_vertex(mgmt, label, not_partitioned)
  vertex = mgmt.make_vertex_label(label)
  vertex = vertex.partition unless not_partitioned

  vertex.make
end

def populate_graph(graph, num_guests, kpis)
  $logger.debug 'Populating the graph'
  random = Random.new(System.current_time_millis)
  log_slice = [(num_guests / 10), 1000].min

  graph.transaction do
    store_vertex = graph.create_vertex(label: 'store', store_pretty_url: 'blue-star-cafe-and-pub-seattle')
    all_guests_id = 'all'
    all_guests = graph.create_vertex(label: 'store_guest_list', guest_list_id: all_guests_id, guest_list_name: 'All Guests')
    store_vertex.add_edges_to(:guest_lists, all_guests, guest_list_id: all_guests_id)

    num_guests.times do |i|
      guest_vertex = graph.create_vertex(label: 'store_guest', guest_id: i.to_s)
      props = Hash[kpis.map { |kpi| [kpi.name, kpi.generate_value(random)] }]
      all_guests.add_edges_to(:members, guest_vertex, props)
      $logger.debug "#{i + 1} guests created" if ((i + 1) % log_slice == 0)

      if (i + 1) % 1000 == 0
        $logger.debug 'Committing implicit transaction'
        graph.commit_implicit_transaction
      end
    end
  end
  $logger.debug 'Done populating the graph'
end

options = { dynamo_config: 'config/dynamo_local.properties', not_indexed: false, not_partitioned: false }
OptionParser.new do |opts|
  opts.banner = "Usage: #{__FILE__} [options]"

  opts.on('-c', '--guests-count NUM', 'Number of guests in the list') do |count|
    options[:guests_count] = Integer(count)
  end

  opts.on('-k', '--kpis KPIs', 'List of KPIs to store on the members edges') do |kpi_names|
    options[:kpis] = kpi_names.split(',')
  end

  opts.on('-I', '--no-index', 'Do not build VCIs for list sorting properties') do |not_indexed|
    options[:not_indexed] = true
  end

  opts.on('-P', '--no-partition', 'Do not partition the store/guest_list vertices') do |not_partitioned|
    options[:not_partitioned] = true
  end

  opts.on('-f', '--config-file [DYNAMO_CONFIG]', 'Dynamo config file') do |file_path|
    options[:dynamo_config] = file_path
  end

  opts.on_tail('-h', '--help', 'Show this message') do
    puts opts
    exit
  end
end.parse!

fail OptionParser::MissingArgument if options[:guests_count].nil?
fail OptionParser::MissingArgument if options[:kpis].nil? || options[:kpis].empty?

$logger.debug "Generating guest list data, options = #{options.inspect}"

num_guests = options[:guests_count]
kpis = KPI.value_of(*options[:kpis])

graph = Pacer.titan(options[:dynamo_config])
build_schema(graph, kpis, options[:not_indexed], options[:not_partitioned])
populate_graph(graph, num_guests, kpis)

exit!