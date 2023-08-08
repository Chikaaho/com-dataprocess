package com.dataprocess.core.entity;

import lombok.*;

/**
 * @Author: hjx
 * @Date: 2023/7/24 15:06
 * @Version: 1.0
 * @Description:
 */
@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class ProblemEntity {

    private String id;
    private String problemCode;
    private String createByLogin;
    private String createByName;
    private String createTime;
    private String submitterRid;
    private String submitterDeptRaid;
    private String submitterTeamRaid;
    private String problemTitle;
    private String problemSource;
    private String problemDescribe;
    private String problemStandardAccept;
    private String systemName;
    private String problemPriority;
    private String endTime;
    private String problemHandler;
    private String problemType;
    private String problemCause;
    private String problemCauseType;
    private String problemSolution;
    private String relateChange;
    private String explanation;
    private String relateChangeCode;
    private String relateEventCode;
    private String problemFeedbackFrequency;
    private String relEndTime;
    private String changeId;
    private String changeCode;
    private String problemSubmitTime;
    private String devopsId;
    private String devopsStatus;
    private String presenterTel;
    private String devopsGid;
    private String systemCode;
    private String emergentId;
    private String problemStatus;
    private String problemReviewerRid;

}
