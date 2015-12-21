require 'json'
require 'csv'

unless ARGV.length == 3
  raise ArgumentError, "Usage: #{__FILE__} <dynamo_config_file> <script_file> <query_csv_file>"
end

# queries.csv
# query_by,order,limit
# total_spend,DESC,50
# total_spend,DESC,50

base_args = { 'config_file' => ARGV[0], 'script' => ARGV[1] }
args = []
CSV.foreach(ARGV[2], headers: true) do |row|
  args << base_args.merge(row.to_hash)
end

results = Hash[args.map do |arguments|
  puts "Running query: #{arguments.inspect}"
  output = `bundle exec ruby #{arguments['script']} #{arguments['config_file']} #{arguments['query_by']} #{arguments['order']} #{arguments['limit']}`
  [arguments, output]
end]

results.each do |key, value|
  stats = JSON.parse(value)
  puts "#{key.inspect}, #{stats['query_duration_ms']}"
end
