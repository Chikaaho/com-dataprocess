package com.dataprocess.core.entity;

import lombok.Data;

@Data
public class EventProcessEntity {

  private String id;
  private String businessId;
  private String eventProcessTime;
  private String eventProcessPersonRid;
  private String eventProcessType;
  private String eventProcessDescribe;

}
