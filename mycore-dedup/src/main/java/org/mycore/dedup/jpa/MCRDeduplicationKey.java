package org.mycore.dedup.jpa;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Entity
@Table(name = "MCRDEDUPLICATION_KEYS",
        indexes = {
                @Index(name = "DEDUPLICATION_KEYS_MCR_ID_IDX",
                        columnList = "MCR_ID"),
                @Index(name = "DEDUPLICATION_KEYS_KEY_TYPE_IDX",
                        columnList = "DEDUPLICATION_KEY, DEDUPLICATION_TYPE"),
                @Index(name = "DEDUPLICATION_KEYS_KEY_TYPE_MCR_ID_IDX",
                        columnList = "DEDUPLICATION_KEY, DEDUPLICATION_TYPE, MCR_ID")
        }
)
@NamedQueries({
        @NamedQuery(name = MCRDeduplicationKey.DEDUPLICATION_KEY_DELETE_BY_MCR_ID, query =
                "DELETE FROM MCRDeduplicationKey d WHERE d.mcrId = :mcrId"),
})
public class MCRDeduplicationKey {

    public static final String DEDUPLICATION_KEY_DELETE_BY_MCR_ID = "DeduplicationKey.deleteByMcrId";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Integer id;

    @Size(max = 128)
    @NotNull
    @Column(name = "MCR_ID", nullable = false, length = 128)
    private String mcrId;

    @Size(max = 32)
    @NotNull
    @Column(name = "DEDUPLICATION_TYPE", nullable = false, length = 32)
    private String deduplicationType;

    @Size(max = 32)
    @NotNull
    @Column(name = "DEDUPLICATION_KEY", nullable = false, length = 32)
    private String deduplicationKey;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMcrId() {
        return mcrId;
    }

    public void setMcrId(String mcrId) {
        this.mcrId = mcrId;
    }

    public String getDeduplicationType() {
        return deduplicationType;
    }

    public void setDeduplicationType(String deduplicationType) {
        this.deduplicationType = deduplicationType;
    }

    public String getDeduplicationKey() {
        return deduplicationKey;
    }

    public void setDeduplicationKey(String deduplicationKey) {
        this.deduplicationKey = deduplicationKey;
    }

}
