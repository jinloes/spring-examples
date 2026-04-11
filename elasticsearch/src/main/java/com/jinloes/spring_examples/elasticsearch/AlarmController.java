package com.jinloes.spring_examples.elasticsearch;

import com.jinloes.spring_examples.elasticsearch.data.Alarm;
import com.jinloes.spring_examples.elasticsearch.data.AlarmRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/alarms")
@RequiredArgsConstructor
public class AlarmController {

  private final AlarmRepository alarmRepository;

  @PostMapping
  public Alarm create(@RequestBody Alarm alarm) {
    return alarmRepository.save(new Alarm(UUID.randomUUID().toString(), alarm.getOrg()));
  }

  @GetMapping("/{id}")
  public Alarm get(@PathVariable String id) {
    return alarmRepository.findById(id).orElseThrow(() -> new AlarmNotFoundException(id));
  }
}
