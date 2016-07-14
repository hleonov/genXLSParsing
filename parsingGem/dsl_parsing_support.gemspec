Gem::Specification.new do |s|
  s.name	= 'dsl_parsing_support'
  s.version	= '0.0.0'
  s.date	= '2014-09-25'
  s.summary	= 'Ruby wrapper to support the HITS general parsing DSL framework'
  s.authors	= ['Meik Bittkowski']
  s.email	= 'meik.bittkowski@h-its.org'
  s.files	= ['lib/dsl_parsing_support.rb', 'bin/genXlsParsingWithProvenance.jar']
  s.homepage	= ''
  s.license	= ''
  s.add_runtime_dependency "open4"
  s.add_runtime_dependency "json"
end
