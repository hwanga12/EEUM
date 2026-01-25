package org.ssafy.eeum.domain.schedule.entity;

import jakarta.persistence.*;
import lombok.*;
import org.ssafy.eeum.domain.user.entity.User;
import org.ssafy.eeum.global.common.model.BaseEntity;

import java.time.LocalDate;

@Entity
@Table(name = "schedules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Schedule extends BaseEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "group_id", nullable = false)
    private Integer groupId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(name = "start_at", nullable = false)
    private LocalDate startAt;

    @Column(name = "end_at", nullable = false)
    private LocalDate endAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "category_type", nullable = false)
    private CategoryType categoryType;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String visitorName;
    private String visitPurpose;
    private Boolean isVisited;

    @Enumerated(EnumType.STRING)
    private RepeatType repeatType;
    private Boolean isLunar;
    private String targetPerson;

    public void update(String title, LocalDate startAt, LocalDate endAt, String description,
                       String visitorName, String visitPurpose, RepeatType repeatType,
                       Boolean isLunar, String targetPerson) {
        this.title = title;
        this.startAt = startAt;
        this.endAt = endAt;
        this.description = description;
        this.visitorName = visitorName;
        this.visitPurpose = visitPurpose;
        this.repeatType = repeatType;
        this.isLunar = isLunar;
        this.targetPerson = targetPerson;
    }
}