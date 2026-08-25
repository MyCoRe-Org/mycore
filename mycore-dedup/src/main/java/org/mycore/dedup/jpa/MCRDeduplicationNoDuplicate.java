package org.mycore.dedup.jpa;

import java.util.Date;

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
@Table(name = "MCRDEDUPLICATION_NO_DUPLICATE",
        indexes = {
                @Index(name = "DEDUPLICATION_NO_DUPLICATES_MCR_ID_1_IDX", columnList = "MCR_ID_1"),
                @Index(name = "DEDUPLICATION_NO_DUPLICATES_MCR_ID_2_IDX", columnList = "MCR_ID_2"),
                @Index(name = "DEDUPLICATION_NO_DUPLICATES_MCR_ID_1_MCR_ID_2_IDX", columnList = "MCR_ID_1, MCR_ID_2")
        })
@NamedQueries({
        @NamedQuery(name = MCRDeduplicationNoDuplicate.DEDUPLICATION_KEY_DELETE_BY_MCR_ID,
                query = "DELETE FROM MCRDeduplicationNoDuplicate d WHERE d.mcrId1 = :mcrId OR d.mcrId2 = :mcrId"),
})
public class MCRDeduplicationNoDuplicate {

    public static final String DEDUPLICATION_KEY_DELETE_BY_MCR_ID = "DeduplicationKeyNoDuplicate.deleteByMcrId";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "ID", nullable = false)
    private Integer id;

    @Size(max = 128)
    @NotNull
    @Column(name = "MCR_ID_1", nullable = false, length = 128)
    private String mcrId1;

    @Size(max = 128)
    @NotNull
    @Column(name = "MCR_ID_2", nullable = false, length = 128)
    private String mcrId2;
    @Size(max = 128)
    @NotNull
    @Column(name = "CREATOR", nullable = false, length = 128)
    private String creator;
    @NotNull
    @Column(name = "CREATION_DATE", nullable = false)
    private Date creationDate;

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getMcrId1() {
        return mcrId1;
    }

    public void setMcrId1(String mcrId1) {
        this.mcrId1 = mcrId1;
    }

    public String getMcrId2() {
        return mcrId2;
    }

    public void setMcrId2(String mcrId2) {
        this.mcrId2 = mcrId2;
    }

    public String getCreator() {
        return creator;
    }

    public void setCreator(String user) {
        this.creator = user;
    }

    public Date getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(Date date) {
        this.creationDate = date;
    }
}
