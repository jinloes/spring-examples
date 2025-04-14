package com.jinloes.webflux.service;

import com.jinloes.webflux.model.Alert;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AlertService {
  private final List<Alert> alerts = new ArrayList<>();

  public Alert createAlert(Alert alert) {
    alert = alert.withId();
    alerts.add(alert);
    return alert;
  }
}
