package com.jinloes.djl.service;

import ai.djl.Device;
import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.DeferredTranslatorFactory;
import jakarta.annotation.PreDestroy;
import java.nio.file.Path;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Classifies the programming language of a code snippet using the CodeBERTa model via DJL.
 *
 * <p>The model is loaded once at startup from the path configured by {@code app.djl.model-path}. A
 * fresh {@link Predictor} is created per request because predictors are not thread-safe; the
 * underlying {@link ZooModel} is safe to share across threads.
 *
 * <p>See {@code application.yml} and the module CLAUDE.md for model download instructions.
 */
@Service
public class DjlService {

  private final ZooModel<String, Classifications> model;

  public DjlService(@Value("${app.djl.model-path}") String modelPath) throws Exception {
    Criteria<String, Classifications> criteria =
        Criteria.builder()
            .setTypes(String.class, Classifications.class)
            .optModelPath(Path.of(modelPath))
            .optTranslatorFactory(new DeferredTranslatorFactory())
            .optProgress(new ProgressBar())
            .optDevice(Device.cpu())
            .build();
    this.model = criteria.loadModel();
  }

  public Classifications classify(String code) {
    if (!StringUtils.hasText(code)) {
      return new Classifications(List.of(), List.of());
    }
    // Predictor is not thread-safe — create one per invocation and close it immediately
    try (Predictor<String, Classifications> predictor = model.newPredictor()) {
      return predictor.predict(code);
    } catch (Exception e) {
      throw new RuntimeException("Failed to classify code snippet", e);
    }
  }

  @PreDestroy
  public void close() {
    model.close();
  }
}
