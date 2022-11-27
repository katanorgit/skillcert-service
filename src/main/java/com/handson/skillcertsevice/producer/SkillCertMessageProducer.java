package com.handson.skillcertsevice.producer;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.aws.messaging.core.QueueMessagingTemplate;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;

import com.handson.skillcertsevice.model.Employee;

@Service
public class SkillCertMessageProducer {
	@Value("${cloud.aws.end-point.uri}")
	private String endpoint;

	@Autowired
	private QueueMessagingTemplate queueMessagingTemplate;
	
	public void sendMessageToQueue(@PathVariable String message) {

		queueMessagingTemplate.send(endpoint, MessageBuilder.withPayload(message).build());
	}
	public void sendEmployeeMessageToQueue(Employee emp) {

		queueMessagingTemplate.send(endpoint, MessageBuilder.withPayload(emp).build());
	}
}
