# encoding: utf-8
require "logstash/outputs/base"
require "logstash/namespace"
require "java"

# Rocketmq 版 LogStash Output 插件，根据 Rocketmq v4.2 开发，实现方式较为简陋
# 将 Rocketmq 官方客户端依赖 jar 包放 LogStash 根目录下的 /vendor/jar/rocketmq 目录中即可
#
# 关于字段格式化：
# Rocketmq Message 相关的属性，topic、tag、key、body 都支持格式化
# 如 @body_format 为 true 时代表开启 @body 需要格式化，则 @body 最好为 "%{xxx}" 的格式
# 这样，@body 会被格式化成 event 对象中 xxx 对应的值，例如指定 @body="%{message}"
# 则发送的消息体就是采集到的内容，而不必包括 Logstash 添加的 @timestamp、host 等无用信息
class LogStash::Outputs::Rocketmq < LogStash::Outputs::Base

  # 设置插件可多线程并发执行
  concurrency :shared

  config_name "rocketmq"

  # 使用 codec，可使用 codec 配置进行相关格式化
  default :codec, "plain"

  # 本地 Logstash 的路径，必需，如 C:/ELK/logstash、/usr/local/logstash
  config :logstash_path, :validate => :string, :required => true

  # Rocketmq 的 NameServer 地址，必需，如 192.168.10.10:5678
  config :name_server_addr, :validate => :string, :required => true

  # Rocketmq 的 producer group
  config :producer_group, :validate => :string, :default => "defaultProducerGroup"

  # Rocketmq 是否使用 VIPChannel，默认值 false
  config :use_vip_channel, :validate => :boolean, :default => false

  # Message 的 topic，必需
  config :topic, :validate => :string, :required => true

  # topic 是否需要格式化，默认值 false
  config :topic_format, :validate => :boolean, :default => false

  # Message 的 tag
  config :tag, :validate => :string, :default => "defaultTag"

  # tag 是否需要格式化，默认值 false
  config :tag_format, :validate => :boolean, :default => false

  # Message 的 key
  config :key, :validate => :string, :default => "defaultKey"

  # key 是否需要格式化，默认值 false
  config :key_format, :validate => :boolean, :default => false

  # Message 的 body
  config :body, :validate => :string

  # body 是否需要格式化，默认值 false，发送 codec 插件格式化后的结果
  config :body_format, :validate => :boolean, :default => false

  # 发送异常后的重试次数，默认 2 次
  config :retry_times, :validate => :number, :default => 2

  def register
    validate_format

    load_jar_files

    @stopping = Concurrent::AtomicBoolean.new(false)

    # 创建生产者对象
    @producer = org.apache.rocketmq.client.producer.DefaultMQProducer.new(producer_group)
    @producer.setNamesrvAddr(name_server_addr)
    @producer.setVipChannelEnabled(use_vip_channel)
    @producer.start
  end

  # 如配置 codec ，则 data 为格式化后的数据
  # 不配置 codec 时 data 和 event 一致
  def multi_receive_encoded(events_and_data)
    events_and_data.each do |event, data|
      retrying_send(event, data)
    end
  end

  # 加载依赖的 jar 文件
  def load_jar_files
    jarpath = logstash_path + "/vendor/jar/rocketmq/*.jar"
    @logger.info("RocketMq plugin required jar files are loadding... Jar files path: ", path: jarpath)

    jars = Dir[jarpath]
    raise LogStash::ConfigurationError, 'RocketMq plugin init error, no jars found! Please check the jar files path!' if jars.empty?

    jars.each do |jar|
      @logger.trace('RocketMq plugin loaded a jar: ', jar: jar)
      require jar
    end
  end

  def retrying_send(event, data)
    sent_times = 0

    begin
      # 配置 message 对象
      mq_message = org.apache.rocketmq.common.message.Message.new

      # 根据配置，对 message 对象的各属性进行处理，格式化或取原值
      topic = @topic_format ? event.sprintf(@topic) : @topic
      mq_message.setTopic(topic)

      tag = @tag_format ? event.sprintf(@tag) : @tag
      mq_message.setTags(tag)

      key = @key_format ? event.sprintf(@key) : @key
      mq_message.setKeys(key)

      body = @body_format ? event.sprintf(@body) : data
      # 使用 Java 的 String.getBytes 方法代替 Ruby 的 bytes 方法，否则中文会报错
      java_body = java.lang.String.new(body)
      mq_message.setBody(java_body.getBytes(org.apache.rocketmq.remoting.common.RemotingHelper::DEFAULT_CHARSET))
      result = @producer.send(mq_message)

      if result.nil?
        raise "Send message error! Result is null."
      end

      if org.apache.rocketmq.client.producer.SendStatus::SEND_OK != result.getSendStatus
        status_name = result.getSendStatus.name
        raise "Send message error! Result code is #{status_name}"
      end
    rescue => e
      @logger.error('An Exception Occured!!',
                    :message => e.message,
                    :exception => e.class)
      if @stopping.false? and (sent_times < retry_times)
        # 重试
        sent_times += 1
        retry
      else
        # 根据实际需求处理没发送成功的消息
        @logger.info("Message send failed: #{data}")
      end
    end
  end

  def validate_format
    if @body_format and @body.nil?
      raise "body must be set if body_format is true"
    end
  end

  def close
    @stopping.make_true
    @producer.shutdown
  end

end
