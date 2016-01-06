require 'json'
require 'csv'

unless ARGV.length == 4
  raise ArgumentError, "Usage: #{__FILE__} <dynamo_config_file> <script_file> <query_csv_file> <store_urls>"
end

# queries.csv
# query_by,order,limit
# total_spend,DESC,50
# total_spend,DESC,50

base_args = { 'config_file' => ARGV[0], 'script' => ARGV[1], 'stores' => ARGV[3] }
args = []
CSV.foreach(ARGV[2], headers: true) do |row|
  args << base_args.merge(row.to_hash)
end

results = Hash[args.map do |arguments|
  puts "Running query: #{arguments.inspect}"
  output = `bundle exec ruby #{arguments['script']} #{arguments['config_file']} #{arguments['query_by']} #{arguments['order']} #{arguments['limit']} #{arguments['stores']}`
  result = output.split("\n").find { |line| line.start_with?('RESULT~') }
  if result
    [arguments, result.split('~').last]
  else
    $stderr.puts "No RESULT found: #{output}"
    [arguments, '[]']
  end
end]

results.each do |key, value|
  results = JSON.parse(value)
  results.each do |stats|
    puts "#{key.inspect}, #{stats['store']}, #{stats['startup_time_ms']}, #{stats['query_duration_ms']}, #{stats['total_duration_ms']}"
  end
end
