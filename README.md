# graph_performance

## Setup

`$ bundle install --path vendor/support`

## execution

### Start DynamoDb local
`$ dynamodb-local --inMemory -port 9389`
Must restart each time you make data...


### make some test data
Make a store with 1000 tickets. Save 100 of them to sample in profiling.

`$ bundle exec ruby scripts/test_data.rb 1000 100`

Expected result:
```
starting script
Settting titan schema!
Making data
Tada - inserted 100 tickets for a store and wrote 100 properties to disk
```

## Running the filter test
Of the tickets in the grpah, profile getting 50 of the tickets using the saved sample properties.

`$ be ruby --profile.api scripts/pacer_testing.rb filter 50`

Expected output(profile results vary):
```
Profiling enabled; ^C shutdown will now dump profile info
starting script
Got store vertex in: 0.181 seconds
Getting vertex by edge properties
last ticket {"date"=>"2015-09-22", "ticket_id"=>"10586"}
Total time: 0.39

     total        self    children       calls  method
----------------------------------------------------------------
      0.39        0.00        0.39         929  Array#each
      0.30        0.00        0.30          51  Enumerable.first
      0.30        0.00        0.30          51  Pacer::Core::Route.each
      0.26        0.00        0.25          51  Java::ComTinkerpopPipes::AbstractPipe#next
      0.25        0.00        0.25          51  Pacer::Pipes::WrappingPipe#processNextStart
      0.25        0.25        0.00          51  Java::ComTinkerpopPipesTransform::InVertexPipe#next
      0.07        0.00        0.07          51  Pacer::Core::Graph::VerticesRoute.out_e
      0.06        0.00        0.06         153  Pacer::RouteBuilder#chain
      0.04        0.00        0.04         767  Class#new
      0.04        0.00        0.04          51  Pacer::Core::Graph::VerticesRoute.outE
      0.03        0.00        0.03          51  Pacer::Wrappers::ElementWrapper#chain_route
      0.03        0.00        0.03         102  Pacer::Core::Route.chain_route
      0.03        0.00        0.03         102  Pacer::Route.property_filter
      0.03        0.00        0.03         153  Pacer::RouteBuilder#type_def
      0.03        0.00        0.03          51  Pacer::Core::Route.iterator
      0.03        0.00        0.03        2703  Kernel.send
      0.02        0.00        0.02          51  Pacer::Core::Graph::EdgesRoute.in_v
      0.02        0.00        0.02         102  Pacer::Core::Route.build_pipeline
      0.02        0.00        0.02         306  ConcreteJavaProxy.new
      0.02        0.00        0.02         153  Pacer::Core::Route.pipe_source
      0.02        0.00        0.01        3978  Hash#[]
      0.02        0.00        0.02          51  Pacer::Filter::PropertyFilter.build_pipeline
      0.02        0.00        0.01         153  Pacer::RouteBuilder#extension_modules
      0.02        0.00        0.01         153  Pacer::Route.edge_filters
      0.01        0.00        0.01          51  Pacer::Core::Graph::ElementRoute.configure_iterator
      0.01        0.00        0.01          51  Pacer::Core::Graph::EdgesRoute.inV
      0.01        0.00        0.01         153  Pacer::RouteBuilder#configuration
      0.01        0.00        0.01         102  Pacer::Filter::PropertyFilter::EdgeFilters#initialize
      0.01        0.00        0.01        1938  Hash#fetch
      0.01        0.00        0.01          51  Pacer::Pipes::WrappingPipe#initialize
      0.01        0.00        0.01          51  Pacer::Filter::PropertyFilter::EdgeFilters#build_pipeline
      0.01        0.00        0.01          51  Pacer::Filter::PropertyFilter::Filters#build_pipeline
      0.01        0.00        0.01         153  Pacer::RouteBuilder#all_extensions
      0.01        0.00        0.01         918  Pacer::RouteBuilder#element_type
      0.01        0.00        0.01          51  Pacer::Wrappers::WrapperSelector.build
      0.01        0.00        0.01          51  Pacer::Core::Graph::VerticesRoute.extract_labels
      0.01        0.00        0.01          51  Pacer::Wrappers::VertexWrapper.wrapper_for
      0.01        0.00        0.01         102  Pacer::Filter::PropertyFilter::Filters#initialize
      0.01        0.00        0.01         612  Pacer::RouteBuilder#type_from_source?
      0.01        0.00        0.01         306  Pacer::RouteBuilder#extensions
      0.01        0.00        0.01         306  Pacer::RouteBuilder#wrapper
      0.01        0.00        0.00        2193  Pacer::RouteBuilder#source_value
      0.01        0.00        0.01         306  Pacer::RouteBuilder#function_modules
      0.01        0.00        0.01          51  Set#eql?
      0.01        0.00        0.01         306  ConcreteJavaProxy#initialize
      0.01        0.00        0.01        1071  Pacer::RouteBuilder#graph
      0.01        0.00        0.00         306  Pacer::FunctionResolver.function
      0.01        0.01        0.00          51  Hash#eql?
      0.01        0.00        0.01         255  Array#map
      0.01        0.00        0.01         153  Pacer::Core::Route.source_iterator
Tada - Got 51 of 100 edges using filter!
```

## Running the vci test
Of the tickets in the grpah, profile getting 50 of the tickets using the saved sample properties.
`$ be ruby --profile.api scripts/pacer_testing.rb vci 50`

Expected output (profile results vary):
```
Profiling enabled; ^C shutdown will now dump profile info
starting script
Got store vertex in: 0.189 seconds
Getting vertex by edge properties
last ticket {"date"=>"2015-09-22", "ticket_id"=>"10586"}
Total time: 3.83

     total        self    children       calls  method
----------------------------------------------------------------
      3.83        0.00        3.82         516  Array#each
      3.73        0.00        3.73          51  Enumerable.first
      3.73        0.00        3.72          51  Pacer::Core::Route.each
      3.68        0.00        3.68         102  Java::ComTinkerpopPipes::AbstractPipe#next
      3.68        0.00        3.68          51  Pacer::Pipes::WrappingPipe#processNextStart
      3.68        0.00        3.67          51  Java::ComTinkerpopPipesTransform::InVertexPipe#next
      3.67        0.00        3.67          51  Pacer::Pipes::VertexQueryPipe#processNextStart
      3.62        3.62        0.00          51  Java::ComThinkaureliusTitanGraphdbQueryVertex::VertexCentricQueryBuilder#edges
      0.07        0.00        0.07         102  Pacer::RouteBuilder#chain
      0.05        0.00        0.05          51  Pacer::Core::Graph::VerticesRoute.vertex_query
      0.05        0.00        0.05         562  Class#new
      0.05        0.00        0.05          51  Pacer::Wrappers::ElementWrapper#chain_route
      0.04        0.00        0.04          51  Pacer::Core::Graph::EdgesRoute.in_v
      0.04        0.00        0.03         102  Pacer::RouteBuilder#type_def
      0.03        0.00        0.03         204  ConcreteJavaProxy.new
      0.03        0.00        0.03          51  Pacer::Core::Graph::EdgesRoute.inV
      0.03        0.00        0.02          51  Pacer::Core::Route.chain_route
      0.02        0.00        0.02        1479  Kernel.send
      0.02        0.01        0.01         102  Pacer::RouteBuilder#extension_modules
      0.02        0.00        0.02        2907  Hash#[]
      0.02        0.00        0.02          51  Pacer::Core::Graph::ElementRoute.configure_iterator
      0.02        0.00        0.02          51  Pacer::Core::Route.iterator
      0.02        0.00        0.02          51  Pacer::Pipes::WrappingPipe#initialize
      0.02        0.02        0.00          51  Java::ComThinkaureliusTitanGraphdbQuery::ResultSetIterator::1#iterator
      0.02        0.00        0.02          51  Pacer::Wrappers::WrapperSelector.build
      0.02        0.00        0.02          51  Pacer::Wrappers::VertexWrapper.wrapper_for
      0.02        0.00        0.02          51  Pacer::Route.property_filter
      0.01        0.00        0.01         102  Pacer::RouteBuilder#all_extensions
      0.01        0.00        0.01         612  Pacer::RouteBuilder#element_type
      0.01        0.00        0.01        1071  Hash#fetch
      0.01        0.00        0.01         102  Pacer::Core::Route.build_pipeline
      0.01        0.00        0.01         102  Pacer::RouteBuilder#configuration
      0.01        0.00        0.01          51  Set#eql?
      0.01        0.00        0.01          51  Pacer::Route.edge_filters
      0.01        0.00        0.01         204  Pacer::RouteBuilder#wrapper
      0.01        0.00        0.01         408  Pacer::RouteBuilder#type_from_source?
      0.01        0.00        0.01         714  Pacer::RouteBuilder#graph
      0.01        0.01        0.00          51  Java::ComThinkaureliusTitanGraphdbVertices::CacheVertex#query
      0.01        0.00        0.01         102  Pacer::Core::Route.attach_pipe
      0.01        0.00        0.01        1122  Pacer::RouteBuilder#source_value
      0.01        0.00        0.01          51  Pacer::Filter::PropertyFilter::EdgeFilters#initialize
      0.01        0.00        0.01         102  Pacer::Core::Route.pipe_source
      0.01        0.00        0.00         204  Pacer::RouteBuilder#extensions
      0.01        0.01        0.00          51  Hash#eql?
      0.01        0.00        0.01         204  Pacer::RouteBuilder#function_modules
      0.01        0.00        0.01         102  Pacer::Core::Route.source_iterator
      0.01        0.00        0.01          51  Pacer::Filter::PropertyFilter::Filters#initialize
      0.01        0.00        0.00          51  BasicObject#instance_exec
      0.01        0.00        0.00         204  Pacer::FunctionResolver.function
      0.01        0.00        0.00         102  Pacer::Route#initialize
Tada - Got 51 of 100 edges using vci!
```

## Benchmark Results
### Results
Times are from the profiled code block, best of 3 executions
Using Titan Dynamo backend with '--inmemeory'
#### VCI
| Ticket  | Samples | VCI    (s)| Filter (s) | Ext    (s) |
| ------: | ------: | ---------:| ----------:| ----------:|
| 100     | 10      | 0.81      | 0.23       | 0.24       |
| 1000    | 10      | 0.74      | 0.33       | 0.33       |
| 10000   | 10      | 0.87      | 0.46       | 0.43       |
| 100000  | 10      | 0.84      | 0.97       | N/A        |
| 10000   | 1       | 0.25      | 0.36       | 0.36       |
| 10000   | 50      | 3.55      | 0.54       | 0.60       |
| 10000   | 100     | 6.62      | 1.01       | 0.95       |
| 10000   | 1000    | 63.7      | 6.06       | 6.46       |
Did not test the ext method with the largest graph.

### Environment
#### System
```
  Model Name: MacBook Pro
  Model Identifier: MacBookPro11,3
  Processor Name: Intel Core i7
  Processor Speed:  2.3 GHz
  Number of Processors: 1
  Total Number of Cores:  4
  L2 Cache (per Core):  256 KB
  L3 Cache: 6 MB
  Memory: 16 GB
```
OSX El Capitan, version 10.11.2 (15C50)

#### Java
```
$ java -version
java version "1.8.0_40"
Java(TM) SE Runtime Environment (build 1.8.0_40-b25)
Java HotSpot(TM) 64-Bit Server VM (build 25.40-b25, mixed mode)
```

#### Ruby
```
$ ruby --version
jruby 1.7.22 (1.9.3p551) 2015-08-20 c28f492 on Java HotSpot(TM) 64-Bit Server VM 1.8.0_40-b25 +jit [darwin-x86_64]
```

#### DynamoDB Local
```
$ brew info dynamodb-local
dynamodb-local: stable 2015-07-16_1.0
Cient-side database and server imitating DynamoDB
```
See config directory for options used
Run with:
`$ dynamodb-local --inMemory -port 9389`

### Bundled Gems and Jars
See Jarfile.lock and Gemfile.lock for more details...
