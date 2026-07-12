package za.co.fnb.dcre.platform.persistence;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jdbc.repository.config.EnableJdbcAuditing;
import org.springframework.data.relational.core.mapping.event.BeforeConvertCallback;

/** Wire into each service via @Import(JdbcConfig.class). */
@Configuration
@EnableJdbcAuditing
public class JdbcConfig {

    @Bean
    public BeforeConvertCallback<BaseEntity> idAssigningCallback() {
        return entity -> {
            entity.assignIdIfMissing();
            return entity;
        };
    }
}
