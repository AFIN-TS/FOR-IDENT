package de.hswt.fi.application;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = TestApplicationContext.class)
public abstract class AbstractTest {

	@BeforeEach
	public abstract void setup();

	@AfterEach
	public abstract void cleanup();
}
