# coding: utf-8
lib = File.expand_path('../lib', __FILE__)
$LOAD_PATH.unshift(lib) unless $LOAD_PATH.include?(lib)

Gem::Specification.new do |spec|
  spec.name          = 'graph_performance'
  spec.version       = '0.0.1'
  spec.authors       = ['David Stuebe']
  spec.email         = ['davidstuebe@swipely.com']
  spec.summary       = %(Graph Performance Testing Repo)
  spec.homepage      = 'https://github.com/swipely/graph_performance'

  spec.files         = `git ls-files -z`.split("\x0")
  spec.executables   = spec.files.grep(%r{^bin/}) { |f| File.basename(f) }
  spec.test_files    = spec.files.grep(%r{^(test|spec|features)/})
  spec.require_paths = ['lib']

  spec.required_ruby_version = '>= 1.9.3p551'

  spec.add_dependency 'activesupport'
  spec.add_dependency 'lock_jar', '~> 0.14.5'

  spec.add_development_dependency 'rake'
  spec.add_development_dependency 'rspec'
  spec.add_development_dependency 'pry'
  spec.add_development_dependency 'pry-rescue'
end
