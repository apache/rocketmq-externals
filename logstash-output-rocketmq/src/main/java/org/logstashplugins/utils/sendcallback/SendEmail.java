package org.logstashplugins.utils.sendcallback;

import co.elastic.logstash.api.Configuration;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.logstashplugins.utils.PluginConfigParams;

import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.util.Arrays;
import java.util.Date;
import java.util.Properties;

public class SendEmail {

    public String myEmailAccount;
    public String myEmailPassword;
    public String myEmailSMTPHost;
    public String receiveMailAccount;
    public Configuration config;

    public SendEmail(Configuration config) {
        this.config = config;
        myEmailAccount = config.get(PluginConfigParams.MY_EMAIL_ACCOUNT_CONFIG);
        myEmailPassword = config.get(PluginConfigParams.MY_EMAIL_PASSWORD_CONFIG);
        myEmailSMTPHost = config.get(PluginConfigParams.MY_EMAIL_SMTPHOST_CONFIG);
        receiveMailAccount = config.get(PluginConfigParams.RECEIVE_MAIL_ACCOUNT_CONFIG);
        if (myEmailAccount==null||myEmailPassword==null||myEmailSMTPHost==null||receiveMailAccount==null){
            try {
                throw new Exception("email definition exception");
            } catch (Exception exception) {
                exception.printStackTrace();
            }
        }
    }

    public void sendAnEmail(SendResult sendResult, Message msg, Exception exception) throws Exception {

        Properties props = new Properties();                    // 参数配置
        props.setProperty("mail.transport.protocol", "smtp");   // 使用的协议（JavaMail规范要求）
        props.setProperty("mail.smtp.host", myEmailSMTPHost);   // 发件人的邮箱的 SMTP 服务器地址
        props.setProperty("mail.smtp.auth", "true");            // 需要请求认证
        Session session = Session.getInstance(props);
        session.setDebug(false);
        MimeMessage message = createMimeMessage(session, myEmailAccount, receiveMailAccount, sendResult, msg, exception);
        Transport transport = session.getTransport();
        transport.connect(myEmailAccount, myEmailPassword);
        transport.sendMessage(message, message.getAllRecipients());
        transport.close();
    }

    /**
     * @param session     和服务器交互的会话
     * @param sendMail    发件人邮箱
     * @param receiveMail 收件人邮箱
     */
    public static MimeMessage createMimeMessage(Session session, String sendMail, String receiveMail, SendResult sendResult, Message msg, Exception exception) throws Exception {

        MimeMessage message = new MimeMessage(session);
        message.setFrom(new InternetAddress(sendMail, "fromlogstash", "UTF-8"));
        message.setRecipient(MimeMessage.RecipientType.TO, new InternetAddress(receiveMail, "fromlogstah", "UTF-8"));
        if (sendResult != null) {
            message.setSubject(sendResult.getSendStatus() == null ? "null" : sendResult.getSendStatus().toString(), "UTF-8");
            message.setContent(sendResult.toString()+"message: "+(msg!=null?new String(msg.getBody()):"..."), "text/plain;charset=UTF-8");
        } else if (exception != null) {
            message.setSubject(exception.toString(), "UTF-8");
            message.setContent(Arrays.toString(exception.getStackTrace()) + "\n" + msg.toString()+"message: "+new String(msg.getBody()), "text/plain;charset=UTF-8");
        }
        message.setSentDate(new Date());
        message.saveChanges();
        return message;
    }
}
