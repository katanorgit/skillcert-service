package com.handson.skillcertsevice.controller;

import java.util.ArrayList;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.amazonaws.services.s3.model.S3ObjectSummary;
import com.amazonaws.services.sns.AmazonSNS;
import com.amazonaws.services.sns.AmazonSNSClient;
import com.amazonaws.services.sns.model.PublishRequest;
import com.amazonaws.services.sns.model.SubscribeRequest;
import com.handson.skillcertsevice.model.Employee;
import com.handson.skillcertsevice.producer.CertificateUpload;
import com.handson.skillcertsevice.producer.SkillCertMessageProducer;

@RestController
public class SkillCertController {

	@Autowired
	private AmazonSNS snsClient;

	@Autowired
	CertificateUpload certificateUpload;

	@Autowired
	SkillCertMessageProducer skillCertMessageProducer;

	String TOPIC_ARN = "arn:aws:sns:us-east-1:173245618849:SkillCert";

	public static Logger log = LoggerFactory.getLogger(SkillCertMessageProducer.class);

	@GetMapping("/{message}")
	public String produceMessage(@PathVariable String message) {
		skillCertMessageProducer.sendMessageToQueue(message);
		return "message successfuly produced to queue!";
	}

	@PostMapping("/sendEmployee")
	public void produceEmployeeInfo(@RequestBody Employee employee) {
		skillCertMessageProducer.sendEmployeeMessageToQueue(employee);
		log.info("object send to queue");
	}

	@GetMapping("/addSubscription/{email}")
	public String addSubscription(@PathVariable String email) {
		SubscribeRequest request = new SubscribeRequest(TOPIC_ARN, "email", email);
		snsClient.subscribe(request);
		return "Subscription request is pending. To confirm the subscription, check your email : " + email;
	}

	
	public String sendNotificationIfCertAdded(String certificateName) {
		PublishRequest publishRequest = new PublishRequest(TOPIC_ARN, buildEmailBodyForAddCertificate(certificateName), "Certification details");
		snsClient.publish(publishRequest);
		return "Notification send successfully !!";
	}
	public String sendNotificationIfCertDeleted(String certificateName) {
		PublishRequest publishRequest = new PublishRequest(TOPIC_ARN, buildEmailBodyForDeleteCertificate(certificateName), "Certification details");
		snsClient.publish(publishRequest);
		return "Notification send successfully !!";
	}
	private String buildEmailBodyForAddCertificate(String name) {
		return "New Certification "+ name + "has been added to skillcert portal.";
	}
	private String buildEmailBodyForDeleteCertificate(String name) {
		return "Certification "+ name + "has been deleted from skillcert portal.";
	}

	@PostMapping("/upload")
	public ResponseEntity<String> uploadFile(@RequestParam(value = "file") MultipartFile file) {
		ResponseEntity<String> responseEntity = new ResponseEntity<>(certificateUpload.uploadFile(file), HttpStatus.OK);
		if(responseEntity.getStatusCode()==HttpStatus.OK) {
			sendNotificationIfCertAdded(file.getOriginalFilename());
		}
		return responseEntity;
	}

	@GetMapping("/download/{fileName}")
	public ResponseEntity<ByteArrayResource> downloadFile(@PathVariable String fileName) {
		byte[] data = certificateUpload.downloadFile(fileName);

		ByteArrayResource resource = new ByteArrayResource(data);
		return ResponseEntity.ok().contentLength(data.length).header("Content-type", "application/octet-stream")
				.header("Content-disposition", "attachment; filename=\"" + fileName + "\"").body(resource);
	}

	@GetMapping("/delete/{fileName}")
	public ResponseEntity<String> deleteFile(@PathVariable String fileName) {
		 ResponseEntity<String> responseEntity = new ResponseEntity<>(certificateUpload.deleteFile(fileName), HttpStatus.OK);
		if(responseEntity.getStatusCode()==HttpStatus.OK) {
			sendNotificationIfCertDeleted(fileName);
		}
		return responseEntity;
	}

	@GetMapping("/getListOfS3Objects")
	public ResponseEntity<List<String>> getAllObjects() {
		List<S3ObjectSummary> allBucketFiles = certificateUpload.getAllBucketFiles();
		List<String> urlList = new ArrayList<>();
		for (S3ObjectSummary s3objects : allBucketFiles) {
			//System.out.println(allBucketFiles.get(0).getKey());
			String downloadurl = "http://localhost:8080/download/" + s3objects.getKey();
			String deleteurl = "http://localhost:8080/delete/" + s3objects.getKey();
			urlList.add(downloadurl);
			urlList.add(deleteurl);
		}
		return new ResponseEntity<>(urlList, HttpStatus.OK);
	}

}
