Gem::Specification.new do |s|
  s.name = 'logstash-output-rocketmq'
  s.version         = '0.1.0'
  s.licenses = ['Apache License (2.0)']
  s.summary = "This plugin pushes events to rocketmq"
  s.description     = "This gem is a Logstash plugin required to be installed on top of the Logstash core pipeline using $LS_HOME/bin/logstash-plugin install gemname. This gem is not a stand-alone program"
  s.authors = ["hehejiejie"]
  s.email = "905205173@qq.com"
  s.homepage = "https://github.com/hehejiejie/logstash-out-rocketmq"
  s.require_paths = ["lib"]
  s.platform = 'java'

  # Files
  s.files = Dir['lib/**/*','spec/**/*','vendor/**/*','*.gemspec','*.md','CONTRIBUTORS','Gemfile','LICENSE','NOTICE.TXT']
   # Tests
  s.test_files = s.files.grep(%r{^(test|spec|features)/})

  # Special flag to let us know this is actually a logstash plugin
  s.metadata = { "logstash_plugin" => "true", "logstash_group" => "output" }

  # Jar dependencies
  s.requirements << "jar 'org.apache.commons:commons-lang3', '3.11'"
  s.requirements << "jar 'com.alibaba:fastjson', '1.2.75'"
  s.requirements << "jar 'io.netty:netty-all', '4.1.63.Final'"
  s.requirements << "jar 'org.apache.rocketmq:rocketmq-client', '4.8.0'"
  s.requirements << "jar 'org.apache.rocketmq:rocketmq-common', '4.8.0'"
  s.requirements << "jar 'org.apache.rocketmq:rocketmq-remoting', '4.8.0'"
  s.requirements << "jar 'org.slf4j:slf4j-api', '1.7.25'"
  s.add_runtime_dependency 'jar-dependencies'

  # Gem dependencies
  #
  s.add_runtime_dependency "logstash-core-plugin-api", ">= 1.60", "<= 2.99"
  s.add_runtime_dependency "logstash-codec-plain"
  s.add_development_dependency 'logstash-devutils'
end
