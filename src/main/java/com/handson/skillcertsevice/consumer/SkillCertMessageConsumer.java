package com.handson.skillcertsevice.consumer;

import org.springframework.cloud.aws.messaging.config.annotation.EnableSqs;
import org.springframework.cloud.aws.messaging.listener.annotation.SqsListener;
import org.springframework.stereotype.Service;

import com.handson.skillcertsevice.model.Employee;

@Service
@EnableSqs
public class SkillCertMessageConsumer {

	@SqsListener(value ="${cloud.aws.end-point.uri}")
	public void loadMessageFromSQS(Employee emp) {
		System.out.println("message from SQS Queue {} " + emp);
	}
}
