package io.github.qwzhang01.shield.config;

import io.github.qwzhang01.shield.MaskAlgoContainer;
import io.github.qwzhang01.shield.advice.MaskAdvice;
import io.github.qwzhang01.shield.kit.SpringKit;
import io.github.qwzhang01.shield.shield.CoverAlgo;
import io.github.qwzhang01.shield.shield.DefaultCoverAlgo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Spring Boot auto-configuration for Seven-Shield data masking library.
 *
 * <p>This configuration class automatically registers the necessary beans
 * for data masking functionality when the library is included in a Spring
 * Boot application. All beans are conditional and can be overridden by
 * providing custom implementations.</p>
 *
 * <p><strong>Auto-configured Beans:</strong></p>
 * <ul>
 *   <li>{@link MaskAdvice} - Response interceptor for automatic masking</li>
 *   <li>{@link SpringKit} - Spring context utility for bean management</li>
 *   <li>{@link CoverAlgo} - Default masking algorithm implementation</li>
 * </ul>
 *
 * @author avinzhang
 * @since 1.0.0
 */
@Configuration
public class ShieldAutoConfig {

    /**
     * Provides the MaskAdvice bean for intercepting and masking HTTP
     * responses.
     * This bean is only created if no custom MaskAdvice is defined.
     *
     * @return a new MaskAdvice instance with MaskAlgoContainer
     */
    @Bean
    @ConditionalOnMissingBean(MaskAdvice.class)
    public MaskAdvice maskAlgoContainer() {
        return new MaskAdvice(new MaskAlgoContainer());
    }

    /**
     * Provides the SpringKit utility bean for accessing Spring application
     * context.
     * This bean is only created if no custom SpringKit is defined.
     *
     * @return a new SpringKit instance
     */
    @Bean
    @ConditionalOnMissingBean(SpringKit.class)
    public SpringKit shieldSpringKit() {
        return new SpringKit();
    }

    /**
     * Provides a default data masking algorithm bean.
     * This bean is only created if no other CoverAlgo implementation is
     * found in the context.
     *
     * <p>The DefaultCoverAlgo provides comprehensive masking for common
     * data types including phone numbers, ID cards, emails, and names.</p>
     *
     * @return a new instance of DefaultCoverAlgo with comprehensive masking
     * capabilities
     */
    @Bean
    @ConditionalOnMissingBean(CoverAlgo.class)
    public CoverAlgo defaultCoverAlgo() {
        return new DefaultCoverAlgo();
    }
}
