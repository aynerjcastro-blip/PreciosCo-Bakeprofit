package com.comprasco.bakeprofit;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

// Nunca procesa el staging aunque STAGING_ENABLED esté definida en el equipo
@SpringBootTest(properties = "preciosco.staging.enabled=false")
class BakeprofitApplicationTests {

	@Test
	void contextLoads() {
	}

}
