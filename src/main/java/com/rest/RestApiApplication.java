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
public class RestApiApplication implements CommandLineRunner {

	public static void main(String[] args) {
		SpringApplication.run(RestApiApplication.class, args);
	}

	@Autowired
	private RoleRepository roleRepository;

	@Override
	public void run(String... args) {

		createRoleIfNotExists("ROLE_" + AppConstant.ADMIN_ROLE);
		createRoleIfNotExists("ROLE_" + AppConstant.GUEST_ROLE);
	}

	private void createRoleIfNotExists(String roleName) {

		roleRepository.findFirstByRoleName(roleName).ifPresentOrElse(
				role -> System.out.println(roleName + " already exists"),
				() -> {
					Role role = new Role();
					role.setRoleId(UUID.randomUUID().toString());
					role.setRoleName(roleName);
					roleRepository.save(role);
				}
		);
	}
}