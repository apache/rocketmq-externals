
using RocketMQ.NetClient.Interop;
using System;
using System.Collections.Generic;
using System.Diagnostics;
using System.Runtime.InteropServices;
using System.Text;
using static RocketMQ.NetClient.Producer.ProducerWrap;

namespace RocketMQ.NetClient.Producer
{
    public class MQProducer : IProducer
    {
        #region Default Options
        private int SendMsgTimeout = 3000;
        private int MaxMessageSize = 4194304;
        private string LogPath = Environment.CurrentDirectory + "\\producer_log.txt";
        private LogLevel logLevel = LogLevel.Trace;
        private int autoRetryTimes = 2;
        #endregion

        #region producer handle

        private HandleRef _handleRef;
        private DiagnosticListener _diagnosticListener;

        #endregion

        #region Constructor
        private void handleInit(string groupName, DiagnosticListener diagnosticListener = null)
        {
            if (string.IsNullOrWhiteSpace(groupName))
            {
                throw new ArgumentNullException(nameof(groupName));
            }
            this._diagnosticListener = diagnosticListener;

            var handle = ProducerWrap.CreateProducer(groupName);
            if (handle == IntPtr.Zero)
            {
                throw new RocketMQProducerException($"create producer error, ptr is {handle}");
            }
            Console.WriteLine(this.LogPath);

            this._handleRef = new HandleRef(this, handle);
            this.SetProducerLogPath(this.LogPath);
            this.SetProducerLogLevel(this.logLevel);
            this.SetProducerSendMessageTimeout(this.SendMsgTimeout);
            this.SetProducerMaxMessageSize(this.MaxMessageSize);
            this.SetAutoRetryTimes(this.autoRetryTimes);
        }

        public MQProducer(string groupName, DiagnosticListener diagnosticListener = null)
        {
            this.handleInit(groupName,diagnosticListener);
        }
        //public MQProducer(string groupName, string nameServerDomain)
        //{
        //    this.handleInit(groupName, null);
        //    this.SetProducerNameServerDomain(nameServerDomain);
        //}
        public MQProducer(string groupName,string nameServerAddress, DiagnosticListener diagnosticListener = null)
        {
            this.handleInit(groupName,diagnosticListener);
            SetProducerNameServerAddress(nameServerAddress);
        }

        public MQProducer(string groupName, string nameServerAddress,string logPath, DiagnosticListener diagnosticListener = null) {
            this.handleInit(groupName, diagnosticListener);
            SetProducerNameServerAddress(nameServerAddress);
            SetProducerLogPath(logPath);
        }

        public MQProducer(string groupName, string nameServerAddress, string logPath,LogLevel logLevel, DiagnosticListener diagnosticListener = null)
        {
            this.handleInit(groupName, diagnosticListener);
            SetProducerNameServerAddress(nameServerAddress);
            SetProducerLogPath(logPath);
            SetProducerLogLevel(logLevel);
        }

        #endregion

        #region Set Options
        public void SetProducerNameServerAddress(string nameServerAddress)
        {
            if (string.IsNullOrWhiteSpace(nameServerAddress))
            {
                throw new ArgumentException(nameof(nameServerAddress));
            }
            var result = ProducerWrap.SetProducerNameServerAddress(this._handleRef, nameServerAddress);
            if (result != 0)
            {
                throw new RocketMQProducerException($"set producer nameServerAddress error. cpp sdk return code: {result}");
            }


        }

        public void SetProducerNameServerDomain(string nameServerDomain)
        {
            if (string.IsNullOrWhiteSpace(nameServerDomain))
            {
                throw new ArgumentException(nameof(nameServerDomain));
            }
            var result = ProducerWrap.SetProducerNameServerDomain(this._handleRef, nameServerDomain);
            if (result != 0)
            {
                throw new RocketMQProducerException($"set producer nameServerDomain error. cpp sdk return code: {result}");
            }


        }

        public void SetProducerGroupName(string groupName)
        {
            if (string.IsNullOrWhiteSpace(groupName))
            {
                throw new ArgumentException(nameof(groupName));
            }
            var result = ProducerWrap.SetProducerGroupName(this._handleRef, groupName);
            if (result != 0)
            {
                throw new RocketMQProducerException($"set producer groupName error. cpp sdk return code: {result}");
            }


        }

        public void SetProducerInstanceName(string instanceName)
        {
            if (string.IsNullOrWhiteSpace(instanceName))
            {
                throw new ArgumentException(nameof(instanceName));
            }
            var result = ProducerWrap.SetProducerInstanceName(this._handleRef, instanceName);
            if (result != 0)
            {
                throw new RocketMQProducerException($"set producer instanceName error. cpp sdk return code: {result}");
            }


        }

        public void SetProducerSessionCredentials(string accessKey, string secretKey, string onsChannel)
        {
            if (string.IsNullOrWhiteSpace(accessKey))
            {
                throw new ArgumentException(nameof(accessKey));
            }
            if (string.IsNullOrWhiteSpace(secretKey))
            {
                throw new ArgumentException(nameof(secretKey));
            }
            if (string.IsNullOrWhiteSpace(onsChannel))
            {
                throw new ArgumentException(nameof(onsChannel));
            }

            var result = ProducerWrap.SetProducerSessionCredentials(this._handleRef, accessKey, secretKey, onsChannel);
            if (result != 0)
            {
                throw new RocketMQProducerException($"set producer sessionCredentials error. cpp sdk return code: {result}");
            }
            return;
        }

        public void SetProducerLogPath(string logPath)
        {
            if (string.IsNullOrWhiteSpace(logPath))
            {
                throw new ArgumentException(nameof(logPath));
            }
            var result = ProducerWrap.SetProducerLogPath(this._handleRef, logPath);
            if (result != 0)
            {
                throw new RocketMQProducerException($"set producer logPath error. cpp sdk return code: {result}");
            }

            return;
        }

        public void SetProducerLogFileNumAndSize(int fileNum, long fileSize)
        {
            if (fileNum <= 0)
            {
                throw new ArgumentOutOfRangeException(nameof(fileNum));
            }

            if (fileSize <= 0)
            {
                throw new ArgumentOutOfRangeException(nameof(fileSize));
            }



            var result = ProducerWrap.SetProducerLogFileNumAndSize(this._handleRef, fileNum, fileSize);
            if (result != 0)
            {
                throw new RocketMQProducerException($"set producer logFileNumAndSize error. cpp sdk return code: {result}");
            }

            return;
        }

        public void SetProducerLogLevel(LogLevel logLevel)
        {
            if (logLevel == LogLevel.None)
            {
                throw new ArgumentException(nameof(logLevel));
            }
            var result = ProducerWrap.SetProducerLogLevel(this._handleRef, (CLogLevel)logLevel);
            if (result != 0)
            {
                throw new RocketMQProducerException($"set producer logLevel error. cpp sdk return code: {result}");
            }

            return;
        }

        public void SetProducerSendMessageTimeout(int timeout)
        {
            if (timeout < 0)
            {
                throw new ArgumentOutOfRangeException(nameof(timeout));
            }
            var result = ProducerWrap.SetProducerSendMsgTimeout(this._handleRef, timeout);
            if (result != 0)
            {
                throw new RocketMQProducerException($"set producer sendMessageTimeout error. cpp sdk return code: {result}");
            }

            return;
        }

        public void SetProducerCompressLevel(int level)
        {
            if ((level < 0||level >9)&&level!=-1)
            {
                throw new ArgumentOutOfRangeException(nameof(level));
            }
            var result = ProducerWrap.SetProducerCompressLevel(this._handleRef, level);
            if (result != 0)
            {
                throw new RocketMQProducerException($"set producer compressLevel error. cpp sdk return code: {result}");
            }

            return;
        }

        public void SetProducerMaxMessageSize(int size)
        {
            if (size < 0)
            {
                throw new ArgumentOutOfRangeException(nameof(size));
            }
            var result = ProducerWrap.SetProducerMaxMessageSize(this._handleRef, size);
            if (result != 0)
            {
                throw new RocketMQProducerException($"set producer maxMessageSize error. cpp sdk return code: {result}");
            }

            return;
        }

        public void SetAutoRetryTimes(int times)
        {
            this.autoRetryTimes = times;
            return;
        }
        #endregion

        #region start and shutdown
        /// <summary>
        /// 开启生产者
        /// </summary>
        /// <returns></returns>
        public bool StartProducer()
        {
            var startResult = ProducerWrap.StartProducer(this._handleRef);

            if (this._diagnosticListener?.IsEnabled(ConstValues.RocketMQProducerStart) ?? false)
            {
                this._diagnosticListener.Write(ConstValues.RocketMQProducerStart, new
                {
                    startResult
                });
            }

            return startResult == 0;
        }

        /// <summary>
        /// 关闭生成者
        /// </summary>
        /// <returns></returns>
        public bool ShutdownProducer()
        {
            var shutdownResult = ProducerWrap.ShutdownProducer(this._handleRef);

            if (this._diagnosticListener?.IsEnabled(ConstValues.RocketMQProducerStop) ?? false)
            {
                this._diagnosticListener.Write(ConstValues.RocketMQProducerStop, new
                {
                    shutdownResult
                });
            }

            return shutdownResult == 0;
        }

        public bool DestroyProducer()
        {
            var destroyResult = ProducerWrap.DestroyProducer(this._handleRef);

            if (this._diagnosticListener?.IsEnabled(ConstValues.RocketMQProducerDestroy) ?? false)
            {
                this._diagnosticListener.Write(ConstValues.RocketMQProducerDestroy, new
                {
                    destroyResult
                });
            }

            return destroyResult == 0;
        }
        #endregion

        #region Send Message API

        /// <summary>
        /// 设置监听 
        /// </summary>
        /// <param name="diagnosticListener"></param>
        public void SetDiagnosticListener(DiagnosticListener diagnosticListener)
        {
            //this._diagnosticListener = diagnosticListener;
        }

        /// <summary>
        /// 同步发送消息
        /// </summary>
        /// <param name="builder"></param>
        /// <returns></returns>
       
        public SendResult SendMessageSync(HandleRef message)
        {
          
            if (message.Handle == IntPtr.Zero)
            {
                throw new ArgumentException(nameof(message));
            }

            try
            {
                CSendResult sendResultStruct = new CSendResult();
                var result = ProducerWrap.SendMessageSync(this._handleRef, message,ref sendResultStruct);
                if (result != 0)
                {
                    throw new RocketMQProducerException($"set producer sendMessageTimeout error. cpp sdk return code: {result}");
                }
                return result == 0
                ? new SendResult
                {
                    SendStatus = sendResultStruct.sendStatus,
                    Offset = sendResultStruct.offset,
                    MessageId = sendResultStruct.msgId
                }
                : null;
            }
            catch (Exception e) {
                throw new Exception(e.Message);
            }
           

            
        }


        //public bool SendMessageASync(HandleRef message, QueueSelectorCallback callback, string args = "",CSendSuccessCallback sendSuccessCallback,CSendExceptionCallback sendExceptionCallback)
        //{

        //    if (message.Handle == IntPtr.Zero)
        //    {
        //        throw new ArgumentException(nameof(message));
        //    }

        //    var argsPtr = Marshal.StringToBSTR(args);
        //    var result = ProducerWrap.SendMessageAsync(this._handleRef, message,callback, argsPtr,sendSuccessCallback, sendExceptionCallback );

        //    return result == 0;
  
        //}



        /// <summary>
        /// 单向消息发送
        /// </summary>
        /// <param name="builder"></param>
        /// <returns></returns>
        public SendResult SendMessageOneway(HandleRef message)
        {
            
            if (message.Handle == IntPtr.Zero)
            {
                throw new ArgumentException(nameof(message));
            }

            var result = ProducerWrap.SendMessageOneway(this._handleRef, message);

            return result == 0
                ? new SendResult
                {
                    SendStatus = result,
                    Offset = 0,
                    MessageId = string.Empty
                }
                : null;
        }

        /// <summary>
        /// 顺序消息发送
        /// </summary>
        /// <param name="builder"></param>
        /// <param name="callback"></param>
        /// <param name="autoRetryTimes"></param>
        /// <param name="args"></param>
        /// <returns></returns>
        public SendResult SendMessageOrderly(HandleRef message, QueueSelectorCallback callback, string args = "")
        {
           
            if (message.Handle == IntPtr.Zero)
            {
                throw new ArgumentException(nameof(message));
            }

            var argsPtr = Marshal.StringToBSTR(args);
            var result = ProducerWrap.SendMessageOrderly(this._handleRef, message, callback, argsPtr, this.autoRetryTimes, out var sendResult);

            return result == 0
                ? new SendResult
                {
                    SendStatus = sendResult.sendStatus,
                    Offset = sendResult.offset,
                    MessageId = sendResult.msgId
                }
                : null;
        }

        #endregion


        #region Default Callback function
        private static ProducerWrap.QueueSelectorCallback _queueSelectorCallback = new ProducerWrap.QueueSelectorCallback(
            (size, message, args) =>
            {
                Console.WriteLine($"size: {size}, message: {message}, ptr: {args}");

                return 0;
            });

        #endregion
        public void Dispose()
        {
            if (this._handleRef.Handle != IntPtr.Zero)
            {
                ProducerWrap.DestroyProducer(this._handleRef);
                this._handleRef = new HandleRef(null, IntPtr.Zero);
                GC.SuppressFinalize(this);
            }
        }
       

    }

    
}
