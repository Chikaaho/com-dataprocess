package com.dataprocess.core.entity;

import lombok.Data;

@Data
public class EventTaskEntity {

  private String id;
  private String businessId;
  private String eventTaskCode;
  private String eventTaskNo;
  private String eventTaskContent;
  private String eventTaskType;
  private String eventTaskLevel;
  private String eventTaskSystemName;
  private String eventTaskOwnerRid;
  private String eventTaskPlanTime;
  private String eventTaskSystemCode;
  private String eventTaskStandardAccept;
  private String createTime;

}
