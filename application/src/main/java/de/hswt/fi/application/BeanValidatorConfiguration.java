package de.hswt.fi.application;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import jakarta.validation.Validator;

@Configuration
public class BeanValidatorConfiguration {

	@Bean
	public Validator validator() {
		return new LocalValidatorFactoryBean();
	}

}
