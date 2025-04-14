package com.jinloes.webflux.web;

import com.jinloes.webflux.service.AlertService;
import com.jinloes.webflux.model.Alert;
import lombok.AllArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

@AllArgsConstructor
@RestController
@RequestMapping("/alerts")
public class AlertController {
  private final AlertService alertService;

  @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
  public Mono<ResponseEntity<Alert>> create(@RequestBody Mono<Alert> request) {
    return request.map(alertService::createAlert)
        .map(alert -> ResponseEntity.status(HttpStatus.CREATED)
            .body(alert)
        );
  }
}
