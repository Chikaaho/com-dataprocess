package com.dataprocess.core.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * @Author: hjx
 * @Date: 2023/7/24 15:03
 * @Version: 1.0
 * @Description: problem
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class EventEntity {

    private String id;
    private String eventCode;
    private String createByLogin;
    private String createByName;
    private String createTime;
    private String applicantRid;
    private String applicantDeptRaid;
    private String applicantTeamRaid;
    private String eventTitle;
    private String eventHappenTime;
    private String eventFindTime;
    private String relateSystem;
    private String findWay;
    private String eventDescribe;
    private String eventDetailDescribe;
    private String eventEffectDescribe;
    private String effectUserType;
    private String effectUserNum;
    private String effectStartTime;
    private String effectEndTime;
    private String effectRequestNum;
    private String effectPerson;
    private String effectRequestSum;
    private String involveMoney;
    private String eventType;
    private String relateSystemName;
    private String relateSystemCode;
    private String reasonNonmtfound;
    private String eventPredictionLevel;
    private String triggerChange;
    private String createEmergent;
    private String offlineReviewTime;
    private String happenNum;
    private String reviewersRid;
    private String faultLocateTime;
    private String faultRestoreTime;
    private String eventLevelConfirm;
    private String teamLeaderReview;
    private String deptLeaderReview;
    private String reviewerRid;
    private String systemName;
    private String systemCode;
    private String systemLevel;
    private String systemGradeCategory;
    private String systemType;
    private String systemOperation1Rid;
    private String responsibleTeamRaid;
    private String responsibleTeamPersonRaid;
    private String deptOperationaReviewRid;
    private String systemItDeptRaid;
    private String systemItTeamRaid;
    private String emergentRespondLevel;
    private String emergentRespondStandard;
    private String emergentUnStandardReason;
    private String relateLink;
    private String eventStatus;
    private String relateChangeId;
    private String eventReview6PersonRid;
    private String informationLeakage;
    private String effectType;
    private String caseRoot;
    private String reasonDescribe;
    private String faultLocationDuration;
    private String emergencyResponseDuration;
    private String eventFindTimeDuration;
    private String effectTimeDuration;
    private String eventSituation;
    private String processDescribe;
    private String continuityLevel;
    private String objectionTeamLeader;
    private String objectionDeptLeader;
    private String eventHandlerDeptHideRaid;
    private String eventHandlerTeamHideRaid;
    private String eventReviewerDeptHideRaid;
    private String eventReviewerTeamHideRaid;
    private String relateAlarmId;
    private String relateEmergentId;
    private String effectTradingTimeDuration;
    private String effectNonTradingTimeDuration;
    private String relateRootCaseProblem;
    private String relateRootCaseProblemUrl;

}
