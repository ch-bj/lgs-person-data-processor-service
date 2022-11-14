package org.datarocks.lwgs.searchindex.client.configuration;

import static org.datarocks.lwgs.persondataprocessor.configuration.LWGSPersonDataProcessorParameters.*;

import java.io.IOException;
import java.io.InputStreamReader;
import lombok.extern.slf4j.Slf4j;
import org.datarocks.banzai.configuration.HandlerConfiguration;
import org.datarocks.banzai.pipeline.PipeLine;
import org.datarocks.banzai.transformer.PassTroughTransformer;
import org.datarocks.lwgs.persondataprocessor.configuration.model.SupportedAttributes;
import org.datarocks.lwgs.persondataprocessor.model.Attribute;
import org.datarocks.lwgs.persondataprocessor.model.GBPersonEvent;
import org.datarocks.lwgs.persondataprocessor.processor.AttributeSubPipeline;
import org.datarocks.lwgs.persondataprocessor.processor.attributeprocessor.AttributeGenerateSearchTerms;
import org.datarocks.lwgs.persondataprocessor.processor.attributeprocessor.AttributePhoneticallyNormalizeAttributeValue;
import org.datarocks.lwgs.persondataprocessor.processor.attributeprocessor.AttributeSearchTermsEncryptor;
import org.datarocks.lwgs.persondataprocessor.processor.attributeprocessor.AttributeSearchTermsHashing;
import org.datarocks.lwgs.persondataprocessor.processor.gbpersonprocessor.GBPersonEventAttributeValidator;
import org.datarocks.lwgs.persondataprocessor.transformer.GBPersonJsonSerializer;
import org.datarocks.lwgs.persondataprocessor.transformer.GBPersonRequestJsonDeserializer;
import org.datarocks.lwgs.searchindex.client.service.processor.BusinessValidationEventProcessor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

@Slf4j
@Configuration
public class LwgsPipelineConfiguration {
  @Value("${lwgs.searchindex.encryption.enabled}")
  private boolean encryptionEnabled;

  @Value("${lwgs.searchindex.encryption.public-key}")
  private String publicKey;

  @Value("${lwgs.searchindex.encryption.cypher-specification}")
  private String cipherSpecification;

  @Value("${lwgs.searchindex.supported-attributes-path}")
  private Resource supportedAttributesLocation;

  @Value("${lwgs.searchindex.supported-attributes-schema}")
  private Resource attributeSchemeLocation;

  @Value("${lwgs.searchindex.encryption.message-digest}")
  private String messageDigest;

  private String readSupportedAttributesJson() throws IOException {
    return FileCopyUtils.copyToString(
        new InputStreamReader(supportedAttributesLocation.getInputStream()));
  }

  private String readSupportedAttributesSchemaJson() throws IOException {
    return FileCopyUtils.copyToString(
        new InputStreamReader(attributeSchemeLocation.getInputStream()));
  }

  @Bean
  HandlerConfiguration handlerConfiguration() {
    try {
      String supportedAttributesJson = readSupportedAttributesJson();
      String supportedAttributesSchemaJson = readSupportedAttributesSchemaJson();

      return HandlerConfiguration.builder()
          .handlerConfigurationItem(
              PARAM_KEY_SUPPORTED_ATTRIBUTES,
              SupportedAttributes.fromJson(supportedAttributesSchemaJson, supportedAttributesJson))
          .handlerConfigurationItem(PARAM_KEY_PUBLIC_KEY, publicKey)
          .handlerConfigurationItem(PARAM_KEY_CIPHER, cipherSpecification)
          .handlerConfigurationItem(PARAM_KEY_MESSAGE_DIGEST, messageDigest)
          .build();
    } catch (IOException e) {
      log.error("Failed reading supported attributes file.");
      return HandlerConfiguration.builder()
          .handlerConfigurationItem(PARAM_KEY_PUBLIC_KEY, publicKey)
          .handlerConfigurationItem(PARAM_KEY_CIPHER, cipherSpecification)
          .build();
    }
  }

  @Bean
  PipeLine<Attribute, Attribute, Attribute> attributePipeline(
      HandlerConfiguration handlerConfiguration) {
    final PipeLine.PipeLineBuilder<Attribute, Attribute, Attribute> builder =
        PipeLine.builder(handlerConfiguration, Attribute.class, Attribute.class, Attribute.class)
            .addHeadTransformer(PassTroughTransformer.<Attribute>builder().build())
            .addStep(AttributePhoneticallyNormalizeAttributeValue.builder().build())
            .addStep(AttributeGenerateSearchTerms.builder().build())
            .addStep(AttributeSearchTermsHashing.builder().build());

    if (encryptionEnabled) {
      builder.addStep(AttributeSearchTermsEncryptor.builder().build());
    }

    return builder.addTailTransformer(PassTroughTransformer.<Attribute>builder().build()).build();
  }

  @Bean
  PipeLine<String, GBPersonEvent, String> gbPersonEventPipeline(
      BusinessValidationEventProcessor businessValidationEventProcessor,
      HandlerConfiguration handlerConfiguration,
      PipeLine<Attribute, Attribute, Attribute> attributePipeline) {
    return PipeLine.builder(handlerConfiguration, String.class, GBPersonEvent.class, String.class)
        .addHeadTransformer(GBPersonRequestJsonDeserializer.builder().build())
        .addStep(
            GBPersonEventAttributeValidator.builder()
                .processorEventListener(businessValidationEventProcessor)
                .build())
        .addStep(AttributeSubPipeline.builder().attributePipeLine(attributePipeline).build())
        .addTailTransformer(
            GBPersonJsonSerializer.builder().handlerConfiguration(handlerConfiguration).build())
        .build();
  }
}
