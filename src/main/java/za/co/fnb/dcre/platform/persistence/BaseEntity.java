package za.co.fnb.dcre.platform.persistence;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.relational.core.mapping.Column;

import java.time.Instant;
import java.util.UUID;

/**
 * Common base for Spring Data JDBC aggregates (configuration.md point 20;
 * exemplar bank-withdrawal): UUID primary key assigned client-side by
 * {@link JdbcConfig}'s BeforeConvertCallback (Data JDBC includes the id in
 * INSERTs, bypassing DB defaults), optimistic-locking version and audit
 * timestamps matching the Liquibase-managed columns.
 */
public abstract class BaseEntity {

    @Id
    private UUID id;

    @Version
    private Long version;

    @CreatedDate
    @Column("created_at")
    private Instant createdAt;

    @LastModifiedDate
    @Column("updated_at")
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void assignIdIfMissing() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
