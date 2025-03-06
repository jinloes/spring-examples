package com.jinloes.openapi.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AccountController.class)
public class AccountIntegTest {
  private static final String ACCOUNTS_PATH = "/accounts/";

  @Autowired
  private MockMvc mockMvc;

  @Test
  public void getAccount() throws Exception {
    this.mockMvc.perform(get(ACCOUNTS_PATH + "{id}", UUID.randomUUID().toString()))
        .andExpect(status().isOk());
  }
}
