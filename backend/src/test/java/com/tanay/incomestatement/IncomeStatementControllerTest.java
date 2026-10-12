package com.tanay.incomestatement;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

/**
 * The HTTP endpoint with the real service and ledger behind it.
 */
@SpringBootTest
@AutoConfigureMockMvc
class IncomeStatementControllerTest {

	@Autowired
	private MockMvc mvc;

	// TODO (by hand)

}
