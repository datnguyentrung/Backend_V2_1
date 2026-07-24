package com.dat.ai_receptionist_web.util;

import com.dat.ai_receptionist_web.dto.Operation.StudentAttendanceDTO;

public class ScoreCalculator {
    // Đánh giá (Awareness)
    private static final double HIGH_SCORE_WEIGHT = 5.0;
    private static final double MEDIUM_SCORE_WEIGHT = 3.0;
    private static final double LOW_SCORE_WEIGHT = 0.0;

    // Điểm danh (Conduct)
    private static final double ABSENT_WEIGHT = -1.0; // vắng -1
    private static final double EXCUSED_WEIGHT = -0.5; // phép -0,5
    private static final double LATE_WEIGHT = -0.5; // muộn -0,5
    private static final double MAKEUP_WEIGHT = 0.5; // tập bù 0,5

    // Đóng góp (Contribute)
    private static final double TUITION_PAID_ONTIME_WEIGHT = 0.5;
    private static final double TUITION_PAID_LATE_WEIGHT = 0.5;
    private static final double VIDEO_WEIGHT = 1.0;

    // Thành tích (Achievement)
    private static final double SKILL_STRENGTH = 20.0;
    private static final double GOLD_MEDAL = 10.0;
    private static final double SILVER_MEDAL = 7.0;
    private static final double BRONZE_MEDAL = 5.0;

    // Điểm rèn luyện
    public static double calculateAttendanceScore(StudentAttendanceDTO.AttendanceStats score) {
        if (score == null || score.getTotalRecords() == 0) {
            return 0.0;
        }

        if (score.getAbsentCount() != 0 || score.getLateCount() != 0 || score.getExcusedCount() != 0) {
            return 4 + score.getAbsentCount() * ABSENT_WEIGHT
                    + score.getExcusedCount() * EXCUSED_WEIGHT
                    + score.getMakeupCount() * MAKEUP_WEIGHT
                    + score.getLateCount() * LATE_WEIGHT;
        } else return 5;
    }

    // Điểm ý thức
    public static double calculatePerformanceScore(StudentAttendanceDTO.AttendanceStats score) {
        if (score == null) {
            return 0.0;
        }

        return score.getEvalGoodCount() * HIGH_SCORE_WEIGHT
                + score.getEvalAverageCount() * MEDIUM_SCORE_WEIGHT
                + score.getEvalWeakCount() * LOW_SCORE_WEIGHT;
    }

//    // Điểm thành tích
//    public static double calculateAchievementScore(ScoreDTO.AchievementScore score) {
//        if (score == null) {
//            return 0.0;
//        }
//        int skillStrengthScore = score.getSkillStrengthScore() != null ? score.getSkillStrengthScore() : 0;
//
//        int goldMedalScore = score.getMedalScore().getGoldMedal() != null ? score.getMedalScore().getGoldMedal() : 0;
//        int silverMedalScore = score.getMedalScore().getSilverMedal() != null ? score.getMedalScore().getSilverMedal() : 0;
//        int bronzeMedalScore = score.getMedalScore().getBronzeMedal() != null ? score.getMedalScore().getBronzeMedal() : 0;
//
//        return skillStrengthScore * SKILL_STRENGTH + goldMedalScore * GOLD_MEDAL
//                + silverMedalScore * SILVER_MEDAL + bronzeMedalScore * BRONZE_MEDAL;
//    }
//
//    // Điểm đóng góp
//    public static double calculateContributeScore(ScoreDTO.ContributionScore response) {
//        if (response == null) {
//            return 0.0;
//        }
//        int memberCard = response.getMemberCard() != null ? response.getMemberCard() : 0;
//        int paidOnTime = response.getTuition().getOnTime() != null ? response.getTuition().getOnTime() : 0;
//        int paidLate = response.getTuition().getLate() != null ? response.getTuition().getLate() : 0;
//        int video = response.getVideo() != null ? response.getVideo() : 0;
//
//        return memberCard + paidOnTime * TUITION_PAID_ONTIME_WEIGHT + paidLate * TUITION_PAID_LATE_WEIGHT + video * VIDEO_WEIGHT;
//    }
//
//    public static double calculateBonusScore(ScoreDTO.BonusScore score) {
//        if (score == null) {
//            return 0.0;
//        }
//
//        int training = score.getTrainingScore() != null ? score.getTrainingScore() : 0;
//        double contribution = score.getContributionScore().getScore() != null ? score.getContributionScore().getScore() : 0;
//        double achievement = score.getAchievementScore().getScore() != null ? score.getAchievementScore().getScore() : 0;
//        return training + contribution + achievement;
//    }
//
//    public static double calculateSummaryScore(
//            ScoreDTO.ConductScore conductScore,
//            ScoreDTO.AwarenessScore awarenessScore,
//            ScoreDTO.BonusScore bonusScore) {
//        if (conductScore == null && awarenessScore == null && bonusScore == null) {
//            return 0.0;
//        }
//
//        Double conductScoreValue = conductScore != null ? conductScore.getScore() : 0.0;
//        Double awarenessScoreValue = awarenessScore != null ? awarenessScore.getScore() : 0.0;
//        Double bonusScoreValue = bonusScore != null ? bonusScore.getScore() : 0.0;
//
//        return conductScoreValue + awarenessScoreValue + bonusScoreValue;
//    }
}
