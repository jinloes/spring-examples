package com.jinloes.djl.service;

import ai.djl.Device;
import ai.djl.inference.Predictor;
import ai.djl.modality.Classifications;
import ai.djl.repository.zoo.Criteria;
import ai.djl.repository.zoo.ZooModel;
import ai.djl.training.util.ProgressBar;
import ai.djl.translate.DeferredTranslatorFactory;
import io.micrometer.common.util.StringUtils;
import java.nio.file.Path;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class DjlService {
  public Classifications classify(String code) {
    if (StringUtils.isEmpty(code)) {
      return new Classifications(List.of(), List.of());
    }

    Path modelPath = Path.of("/Users/jinloes/workspaces/model/CodeBERTa-language-id");
    Criteria<String, Classifications> criteria = Criteria.builder()
        .setTypes(String.class, Classifications.class)
        .optModelName("CodeBERTa-language-id")
        .optModelPath(modelPath)
        .optProgress(new ProgressBar())
        .optDevice(Device.cpu())
        .optTranslatorFactory(new DeferredTranslatorFactory())
        .build();

    try (ZooModel<String, Classifications> model = criteria.loadModel();
        Predictor<String, Classifications> predictor = model.newPredictor()) {

      return predictor.predict(code);
    } catch (Exception e) {
      throw new RuntimeException("Unable to classify code: %s".formatted(code), e);
    }
  }
}
