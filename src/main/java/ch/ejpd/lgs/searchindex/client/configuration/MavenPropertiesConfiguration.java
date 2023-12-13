package ch.ejpd.lgs.searchindex.client.configuration;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

@Slf4j
@Configuration
public class MavenPropertiesConfiguration {
    private final Properties mavenProperties;
    public final String LGS_PROCESSOR_VERSION = "lgs.person.data.processor.version";

    public MavenPropertiesConfiguration() throws IOException {
        InputStream is = this.getClass().getClassLoader().getResourceAsStream("maven.properties");
        Properties properties = new Properties();
        properties.load(is);

        mavenProperties = properties;
    }

    public String getSafely(String property) {
        return mavenProperties != null
                ? mavenProperties.getProperty(property)
                : null;
    }

    public String getLgsPersonDataProcessorVersion() {
        return getSafely(LGS_PROCESSOR_VERSION);
    }
}
