package com.jinloes.djl.web;

import ai.djl.modality.Classifications;
import com.jinloes.djl.service.DjlService;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/classify")
@RequiredArgsConstructor
@Slf4j
public class CodeClassificationController {
  private final DjlService djlService;

  @PostMapping
  public ResponseEntity<CodeClassifications> classify(@RequestBody ClassificationRequest request) {
    try {
      Classifications classifications = djlService.classify(request.code());
      return ResponseEntity.ok(CodeClassifications.from(classifications));
    } catch (RuntimeException e) {
      log.error("Classification failed", e);
      return ResponseEntity.internalServerError().build();
    }
  }

  public record ClassificationRequest(String code) {}

  public record CodeClassifications(Map<String, Double> classifications) {

    /** Builds a response sorted by probability descending so the top language appears first. */
    static CodeClassifications from(Classifications classifications) {
      Map<String, Double> sorted =
          classifications.items().stream()
              .sorted(
                  Comparator.comparing(Classifications.Classification::getProbability).reversed())
              .collect(
                  Collectors.toMap(
                      Classifications.Classification::getClassName,
                      Classifications.Classification::getProbability,
                      (c1, c2) -> c1,
                      LinkedHashMap::new));
      return new CodeClassifications(sorted);
    }
  }
}
