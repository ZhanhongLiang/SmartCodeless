package com.robotlive.smartcodeless.quality;

import org.springframework.stereotype.Component;

@Component
public class QualityScoreCalculator {

    public int calculate(boolean dependencyPass, boolean scriptPass, boolean buildPass, boolean previewPass, boolean repairPass) {
        int score = 100;
        if (!dependencyPass) {
            score -= 30;
        }
        if (!scriptPass) {
            score -= 25;
        }
        if (!buildPass) {
            score -= 25;
        }
        if (!previewPass) {
            score -= 15;
        }
        if (!repairPass) {
            score -= 5;
        }
        return Math.max(score, 0);
    }
}
