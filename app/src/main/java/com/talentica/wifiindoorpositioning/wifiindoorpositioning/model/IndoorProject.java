package com.talentica.wifiindoorpositioning.wifiindoorpositioning.model;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Created by suyashg on 25/08/17.
 */
public class IndoorProject {

    private String id = UUID.randomUUID().toString();
    private Date createdAt = new Date();
    private String name;
    private String desc;
    private List<AccessPoint> aps = new ArrayList<>();
    private List<ReferencePoint> rps = new ArrayList<>();

    public IndoorProject() {
    }

    public IndoorProject(Date createdAt, String name, String desc) {
        this.createdAt = createdAt;
        this.name = name;
        this.desc = desc;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<AccessPoint> getAps() {
        return aps;
    }

    public void setAps(List<AccessPoint> aps) {
        this.aps = aps;
    }

    public List<ReferencePoint> getRps() {
        return rps;
    }

    public void setRps(List<ReferencePoint> rps) {
        this.rps = rps;
    }
}
