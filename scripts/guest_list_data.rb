require 'json'
require 'optparse'
require 'logger'
require 'lock_jar'

LockJar.load
require 'java'
java_import 'java.lang.System'
java_import 'java.util.Random'
java_import 'com.thinkaurelius.titan.core.TitanFactory'
java_import 'com.thinkaurelius.titan.core.Multiplicity'
java_import 'org.apache.tinkerpop.gremlin.process.traversal.Order'
java_import 'org.apache.tinkerpop.gremlin.structure.Direction'
java_import 'org.apache.tinkerpop.gremlin.structure.Vertex'

$logger = Logger.new(STDOUT)

# Specifies which directions an edge property is NOT indexed in
NonIndexedDirection = Struct.new(:asc, :desc) do
  def self.bidirectional
    new(true, true)
  end

  def negate
    self.class.new(!asc, !desc)
  end

  def both?
    asc && desc
  end
end

class KPI
  def self.value_of(*kpi_names)
    kpi_names.map { |kpi_name| kpis[kpi_name] }
  end

  attr_reader :name

  def initialize(name, type, _primary_sort_order)
    @name = name
    @type = type
  end

  def build_schema(mgmt, members_edge, not_indexed)
    property = mgmt.make_property_key(name).data_type(@type).make
    non_indexed_direction = not_indexed[name]

    if non_indexed_direction.both?
      $logger.debug "Building no indexes for members edge property: #{name}"
    end

    if !non_indexed_direction.asc
      $logger.debug "Building ASC indexed members edge property: #{name}"
      mgmt.build_edge_index(members_edge, "members_by_#{name}_ASC", Direction::OUT, Order.incr, property)
    end

    if !non_indexed_direction.desc
      $logger.debug "Building DESC indexed members edge property: #{name}"
      mgmt.build_edge_index(members_edge, "members_by_#{name}_DESC", Direction::OUT, Order.decr, property)
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
  mgmt = graph.open_management

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

def populate_graph(graph, num_guests, kpis, store)
  $logger.debug "Populating the graph for: #{store}"
  random = Random.new(System.current_time_millis)
  log_slice = [[(num_guests / 10), 1].max, 1000].min

  transaction(graph) do |graph|
    store_vertex = graph.add_vertex('store')
    store_vertex.property('store_pretty_url', store)

    all_guests_id = 'all'
    all_guests = graph.add_vertex('store_guest_list')
    all_guests.property('guest_list_id', all_guests_id)
    all_guests.property('guest_list_name', 'All Guests')
    store_vertex.add_edge('guest_lists', all_guests, 'guest_list_id', all_guests_id)

    num_guests.times do |i|
      guest_vertex = graph.add_vertex('store_guest')
      guest_vertex.property('guest_id', i.to_s)
      props = kpis.flat_map { |kpi| [kpi.name, kpi.generate_value(random)] }
      all_guests.add_edge('members', guest_vertex, *props)
      $logger.debug "#{i + 1} guests created" if ((i + 1) % log_slice == 0)

      if (i + 1) % 1000 == 0
        $logger.debug 'Committing implicit transaction'
        graph.tx.commit
      end
    end
  end
  $logger.debug "Done populating the graph for: #{store}"
end

def transaction(graph)
  yield graph
  $logger.debug 'Successfuly populated the graph'
ensure
  $logger.debug 'About to commit'
  graph.tx.commit
end

options = {
  dynamo_config: 'config/dynamo_local.properties',
  not_indexed: {}.tap { |h| h.default = false },
  not_partitioned: false
}

OptionParser.new do |opts|
  opts.banner = "Usage: #{__FILE__} [options]"

  opts.on('-c', '--guests-count NUM', 'Number of guests in the list') do |count|
    options[:guests_count] = Integer(count)
  end

  opts.on('-k', '--kpis KPIs', 'List of KPIs to store on the members edges') do |kpi_names|
    options[:kpis] = kpi_names.split(',')
  end

  opts.on('-I', '--non-indexed [KPIs]', 'Do not build VCIs for list sorting properties') do |non_indexed_props|
    if non_indexed_props.nil?
      options[:not_indexed].default = NonIndexedDirection.bidirectional
    else
      non_indexed_props.split(',').each do |prop|
        name, dir = prop.split(':')
        if dir
          options[:not_indexed][name] = NonIndexedDirection.new(dir == 'asc', dir == 'desc')
        else
          options[:not_indexed][name] = NonIndexedDirection.bidirectional
        end
      end
      options[:not_indexed].default = NonIndexedDirection.bidirectional.negate
    end
  end

  opts.on('-P', '--no-partition', 'Do not partition the store/guest_list vertices') do |not_partitioned|
    options[:not_partitioned] = true
  end

  opts.on('-f', '--config-file [DYNAMO_CONFIG]', 'Dynamo config file') do |file_path|
    options[:dynamo_config] = file_path
  end

  opts.on('-s' '--stores [PRETTY_URLS]', 'Store Pretty URL(s)') do |pretty_urls|
    options[:stores] = pretty_urls.split(',')
  end

  opts.on_tail('-h', '--help', 'Show this message') do
    puts opts
    exit
  end
end.parse!

fail OptionParser::MissingArgument if options[:guests_count].nil?
fail OptionParser::MissingArgument if options[:kpis].nil? || options[:kpis].empty?
options[:stores] ||= ['blue-star-cafe-and-pub-seattle']
fail OptionParser::MissingArgument if options[:stores].empty?

$logger.debug "Generating guest list data, options = #{options.inspect}"

graph = TitanFactory.open(options[:dynamo_config])

num_guests = options[:guests_count]
kpis = KPI.value_of(*options[:kpis])
build_schema(graph, kpis, options[:not_indexed], options[:not_partitioned])

options[:stores].each do |store|
  populate_graph(graph, num_guests, kpis, store)
end

exit!
