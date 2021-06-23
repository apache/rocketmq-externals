# encoding: utf-8
require "logstash/outputs/base"
require "logstash/namespace"
require "java"
require "logstash-output-rocketmq_jars"

# An rocketmq output .
class LogStash::Outputs::Example < LogStash::Outputs::Base

  concurrency :shared

  config_name "rocketmq"

  # use ''plain'' as a default codec
  # if you want your message field to be passed only from the whole event,you should make the 
  # configuration like this:
  # codec => plain{
  #    format => "%{message}"
  # }
  default :codec, "plain"

  # NameServer setting
  config :name_server_addr, :validate => :string, :required => true

  # producer group setting, the default value is "defaultProducerGroup"
  config :producer_group, :validate => :string, :default => "defaultProducerGroup"

  # topic setting
  config :topic, :validate => :string, :required => true

  # tag setting,the defalut value is "defaultTag"
  config :tag, :validate => :string, :default => "defaultTag"

  # key setting,the default value is defaultKey"
  config :key, :validate => :string, :default => "defaultKey"

  # config the times of resending a message when sending process goes wrong,the default value is 2
  config :retryTimes, :validate => :number, :default => 2

  public
  def register
  
   @stopping = Concurrent::AtomicBoolean.new(false)

    # initializing rocketmq producer
    @producer = org.apache.rocketmq.client.producer.DefaultMQProducer.new(producer_group)
    @producer.setNamesrvAddr(name_server_addr)
    @producer.setRetryTimesWhenSendFailed(retryTimes);
    @producer.start
  end 

  def multi_receive_encoded(events_and_data)
    events_and_data.each do |event, data|
      send(event, data)
    end
  end

 def send(event, data)
    begin
      msg = org.apache.rocketmq.common.message.Message.new
 
      msg.setTopic(@topic)
      msg.setTags(@tag)
      msg.setKeys(@key)
      # use a java string instead of a ruby string
      # use a rocketmq default charset
      body = java.lang.String.new(data)
      msg.setBody(body.getBytes(org.apache.rocketmq.remoting.common.RemotingHelper::DEFAULT_CHARSET))

      sendResult = @producer.send(msg)

      if org.apache.rocketmq.client.producer.SendStatus::SEND_OK != sendResult.getSendStatus
        sendStatus = sendResult.getSendStatus.name
        raise "Message Send failed! The send status is #{sendStatus}"
      end
    rescue => e
      @logger.error('An Exception Occured!', :message => e.message, :exception => e.class)
      @logger.info("Message send failed: #{data}")    
    end
  end

  def close
    @stopping.make_true
    @producer.shutdown
  end

end
