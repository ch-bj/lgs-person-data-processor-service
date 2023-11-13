package ch.ejpd.lgs.searchindex.client.configuration;

import static ch.ejpd.lgs.persondataprocessor.configuration.LWGSPersonDataProcessorParameters.*;

import ch.ejpd.lgs.persondataprocessor.configuration.model.SupportedAttributes;
import ch.ejpd.lgs.persondataprocessor.model.Attribute;
import ch.ejpd.lgs.persondataprocessor.model.GBPersonEvent;
import ch.ejpd.lgs.persondataprocessor.processor.AttributeSubPipeline;
import ch.ejpd.lgs.persondataprocessor.processor.attributeprocessor.AttributeGenerateSearchTerms;
import ch.ejpd.lgs.persondataprocessor.processor.attributeprocessor.AttributePhoneticallyNormalizeAttributeValue;
import ch.ejpd.lgs.persondataprocessor.processor.attributeprocessor.AttributeSearchTermsEncryptor;
import ch.ejpd.lgs.persondataprocessor.processor.attributeprocessor.AttributeSearchTermsHashing;
import ch.ejpd.lgs.persondataprocessor.processor.gbpersonprocessor.GBPersonEventAttributeValidator;
import ch.ejpd.lgs.persondataprocessor.transformer.GBPersonJsonSerializer;
import ch.ejpd.lgs.persondataprocessor.transformer.GBPersonRequestJsonDeserializer;
import ch.ejpd.lgs.searchindex.client.service.processor.BusinessValidationEventProcessor;
import java.io.IOException;
import java.io.InputStreamReader;
import lombok.extern.slf4j.Slf4j;
import org.datarocks.banzai.configuration.HandlerConfiguration;
import org.datarocks.banzai.pipeline.PipeLine;
import org.datarocks.banzai.transformer.PassTroughTransformer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.util.FileCopyUtils;

/**
 * Configuration class for the LWGS search index pipeline.
 */
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

  // Helper method to read supported attributes from JSON file
  private String readSupportedAttributesJson() throws IOException {
    return FileCopyUtils.copyToString(
        new InputStreamReader(supportedAttributesLocation.getInputStream()));
  }

  // Helper method to read supported attributes schema from JSON file
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
