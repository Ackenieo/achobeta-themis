package com.achobeta.themis.common.constants;

import lombok.Getter;

import java.util.Optional;
import java.util.stream.Stream;

@Getter
public enum PrimaryTagEnum {
    WORK_SALARY(1, "工资薪酬"),
    WORK_TIME(2, "工作时间"),
    CONTRACT(3, "劳动合同"),
    TERMINATION(4, "解除终止"),
    SOCIAL_SECURITY(5, "社会保险"),
    SPECIAL_CASE(6, "特殊情形"),
    LABOR_DISPUTE(7, "劳动争议处理"),

    // 个人用户一级标题
    // ---------------
    // 企业用户一级标题

    RECRUITMENT(8, "招聘录用"),
    CONTRACT_MANAGEMENT(9, "劳动合同管理"),
    SALARY_WELFARE_MANAGEMENT(10, "薪酬福利管理"),
    HOURS_VACATION_MANAGEMENT(11, "工时休假管理"),
    TERMINATION_MANAGEMENT(12, "解除终止管理"),
    SOCIAL_SECURITY_MANAGEMENT(13, "社会保险管理"),
    SPECIAL_CASE_MANAGEMENT(14, "特殊情形"),
    LABOR_DISPUTE_PREVENTION(15, "劳动争议预防");

    private final Integer id;
    private final String tag;

    PrimaryTagEnum(int id, String tag) {
        this.id = id;
        this.tag = tag;
    }

    public static Optional<PrimaryTagEnum> of(Integer id) {
        return Stream.of(values())
                .filter(t -> t.getId().intValue() == id.intValue())
                .findFirst();
    }
}
