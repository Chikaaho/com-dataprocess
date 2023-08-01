package com.dataprocess.core.entity;

import lombok.Data;

@Data
public class EventReasonEntity {

  private String id;
  private String businessId;
  private String eventReasonCode;
  private String eventReasonType;
  private String eventReasonType1;
  private String eventReasonType2;
  private String eventReasonDeptRaid;
  private String eventReasonTeamRaid;
  private String eventReasonGroup;
  private String eventReasonArea;
  private String eventReasonSystemName;
  private String eventReasonSystemCode;
  private String eventReasonSystemLevel;
  private String eventReasonSystemDept;
  private String eventReasonSystemTeam;
  private String eventReasonOperator;
  private String eventReasonTechnician;

}
