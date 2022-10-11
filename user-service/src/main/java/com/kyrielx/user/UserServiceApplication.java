package com.kyrielx.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.loadbalancer.LoadBalanced;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class UserServiceApplication {

	public static void main(String[] args) {
		SpringApplication.run(UserServiceApplication.class, args);
	}

	@Bean // 向Spring容器中注册restTemplate的bean
	@LoadBalanced // 根据应用名称进行负载均衡访问
	RestTemplate restTemplate(){
		return new RestTemplate();
	}
}
