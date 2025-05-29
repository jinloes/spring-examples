package com.jinloes.djl.web;

import ai.djl.modality.Classifications;
import com.jinloes.djl.service.DjlService;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.Builder;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
public class CodeClassificationController {
  private final DjlService djlService;

  @PostMapping("classify")
  public ResponseEntity<CodeClassifications> classify(@RequestBody ClassificationRequest request) {
    Classifications classifications = djlService.classify(request.code());
    return ResponseEntity.ok(CodeClassifications.builder()
        .setClassifications(classifications)
        .build());
  }

  public record ClassificationRequest(String code) {

  }


  @Builder
  public record CodeClassifications(Map<String, Double> classifications) {

    public static class CodeClassificationsBuilder {
      private Map<String, Double> classifications = new LinkedHashMap<>();

      public CodeClassificationsBuilder setClassifications(Classifications classifications) {
        this.classifications = classifications.items()
            .stream()
            .sorted(Comparator.comparing(Classifications.Classification::getProbability)
                .reversed())
            .collect(Collectors.toMap(Classifications.Classification::getClassName,
                Classifications.Classification::getProbability, (c1, c2) -> c1, LinkedHashMap::new));
        return this;
      }
    }
  }
}
