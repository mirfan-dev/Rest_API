package com.rest;

import com.rest.entity.Role;
import com.rest.repository.RoleRepository;
import com.rest.util.AppConstant;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.UUID;

@SpringBootApplication
public class RestApiApplication{

	public static void main(String[] args) {
		SpringApplication.run(RestApiApplication.class, args);
	}


}