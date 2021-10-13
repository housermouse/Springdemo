package com.apexsoft.springdemo;

import java.util.List;

public class RegistryCenterStatus {

    private boolean status;
    private String note;
    private String address;
    private List<RegistryCenterStatus> nodesStatus;


    public boolean isStatus() {
        return status;
    }

    public void setStatus(boolean status) {
        this.status = status;
    }

    public String getNote() {
        return note;
    }

    public void setNote(String note) {
        this.note = note;
    }

    public List<RegistryCenterStatus> getNodesStatus() {
        return nodesStatus;
    }

    public void setNodesStatus(List<RegistryCenterStatus> nodesStatus) {
        this.nodesStatus = nodesStatus;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public static Builder newBuilder(){
        return new Builder();
    }

    public static class Builder {
        private boolean status;
        private String note;
        private String address;
        private List<RegistryCenterStatus> nodesStatus;

        public RegistryCenterStatus build(){
            RegistryCenterStatus result = new RegistryCenterStatus();
            result.address = this.address;
            result.nodesStatus = this.nodesStatus;
            result.note = this.note;
            result.status = this.status;
            return result;
        }

        public Builder status(boolean status){
            this.status = status;
            return this;
        }

        public Builder note(String note){
            this.note = note;
            return this;
        }

        public Builder adderss(String address){
            this.address = address;
            return this;
        }

        public Builder nodeStatus(List<RegistryCenterStatus> nodesStatus){
            this.nodesStatus = nodesStatus;
            return this;
        }
    }
}
