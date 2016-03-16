package org.mycore.backend.jpa.dnbtransfer;

import java.io.Serializable;
import java.util.Date;

import javax.persistence.Basic;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;

@Entity
public class MCRDNBTRANSFERRESULTS implements Serializable {

    /**
     * 
     */
    private static final long serialVersionUID = -5543608475323581768L;

    private int id;

    private boolean transferPackageArchived;

    private String protocolType, errorMessage, deliveryRole, objectId, errorModule, tpName;

    private Date date;

    /**
     * @return the id
     */
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public int getId() {
        return id;
    }

    /**
     * @param id
     *            the id to set
     */
    public void setId(int id) {
        this.id = id;
    }

    /**
     * @return the transferPackageArchived
     */
    @Basic
    @Column(name = "TRANSFERPACKAGEARCHIVED")
    public boolean getTransferPackageArchived() {
        return transferPackageArchived;
    }

    /**
     * @param transferPackageArchived
     *            the transferPackageArchived to set
     */
    public void setTransferPackageArchived(boolean transferPackageArchived) {
        this.transferPackageArchived = transferPackageArchived;
    }

    /**
     * @return the protocolType
     */
    @Basic
    @Column(name = "PROTOCOLTYPE", length = 9, nullable = false)
    public String getProtocolType() {
        return protocolType;
    }

    /**
     * @param protocolType
     *            the protocolType to set
     */
    public void setProtocolType(String protocolType) {
        this.protocolType = protocolType;
    }

    /**
     * @return the tpName
     */
    @Basic
    @Column(name = "TP_NAME", length = 9, nullable = false)
    public String getTpName() {
        return tpName;
    }

    /**
     * @param tpName the tpName to set
     */
    public void setTpName(String tpName) {
        this.tpName = tpName;
    }

    /**
     * @return the errorMessage
     */
    @Basic
    @Column(name = "ERRORMESSAGE", length = 1024)
    public String getErrorMessage() {
        return errorMessage;
    }

    /**
     * @param errorMessage
     *            the errorMessage to set
     */
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    /**
     * @return the deliveryRole
     */
    @Basic
    @Column(name = "DELIVERYROLE", length = 32, nullable = false)
    public String getDeliveryRole() {
        return deliveryRole;
    }

    /**
     * @param deliveryRole
     *            the deliveryRole to set
     */
    public void setDeliveryRole(String deliveryRole) {
        this.deliveryRole = deliveryRole;
    }

    /**
     * @return the objectId
     */
    @Basic
    @Column(name = "OBJECTID", length = 124)
    public String getObjectId() {
        return objectId;
    }

    /**
     * @param objectId
     *            the objectId to set
     */
    public void setObjectId(String objectId) {
        this.objectId = objectId;
    }

    /**
     * @return the errorModule
     */
    @Basic
    @Column(name = "ERRORMODULE", length = 64)
    public String getErrorModule() {
        return errorModule;
    }

    /**
     * @param errorModule
     *            the errorModule to set
     */
    public void setErrorModule(String errorModule) {
        this.errorModule = errorModule;
    }

    /**
     * @return the date
     */
    @Basic
    @Column(name = "DATE", nullable = false)
    public Date getDate() {
        return date;
    }

    /**
     * @param date
     *            the date to set
     */
    public void setDate(Date date) {
        this.date = date;
    }
}
