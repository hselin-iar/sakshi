package com.kleos.sakshi.engine.tuning

object Tuning {
  // time
  const val STUDY_DAY_START_HOUR = 4                      // confirmed
  const val RAW_RETENTION_DAYS = 14                       // DOC 2 §2.3.4
  const val WINDOW_FINALISE_LAG_MIN = 15                  // DOC 2 §2.3.3
  const val ASSUMED_PLATFORM_RETENTION_DAYS = 3           // ESTIMATE; replaced by spike S-B result
  // glance, stay, return
  const val GLANCE_MAX_SEC = 30                           // DOC 1 §1.2.3 (ESTIMATE)
  const val STAY_MERGE_GAP_SEC = 20                       // ESTIMATE
  const val RETURN_BACK_IN_SET_SEC = 30
  const val RETURN_SCREEN_OFF_MIN = 5
  const val PUT_DOWN_MIN = 3                              // D1
  const val DEPENDS_JOIN_SEC = 90                         // ASSUMPTION
  const val STONE_LOOKBACK_SEC = 30
  const val NO_RIPPLE_SEC = 60
  // windows
  const val RUN_MIN_IN_SET_MIN = 5                        // D9
  const val RUN_MAX_GAP_MIN = 3                           // D9
  const val WINDOW_PAD_MIN = 10
  const val VALID_DAY_WINDOW_MIN = 45
  const val RAMP_UP_MIN_RUN_MIN = 2
  const val FLINCH_FIRST_MIN = 5
  // baseline and steadiness
  const val BASELINE_VALID_DAYS = 8
  const val STAYS_SMOOTH_STAYS = 1.0                      // add-one smoothing: (stays+1)/(hours+1)
  const val STAYS_SMOOTH_HOURS = 1.0
  const val RATIO_CLAMP_LO = 0.5; const val RATIO_CLAMP_HI = 1.5
  const val W_STRETCH = 0.35; const val W_STAYS = 0.30; const val W_RETURN = 0.20; const val W_QUIET = 0.15
  const val WORD_WAVERING_BELOW = 90; const val WORD_STEADIER_ABOVE = 110
  const val UNUSUAL_WEEK_WINDOW_MIN_RATIO = 0.5           // week window-minutes < 50% of normal ⇒ unusual
  const val REANCHOR_MIN_WEEK = 4
  // evidence gate (patterns)
  const val EVIDENCE_MIN_WINDOWS = 4; const val EVIDENCE_MIN_DAYS = 3
  const val EVIDENCE_RATIO_HI = 1.5;  const val EVIDENCE_RATIO_LO = 0.67
  const val RATE_EPS = 0.5                                // stays/hour added to numerator and denominator in ratios
  const val SHAPE_MIN_WINDOWS = 30; const val BREAK_MIN_STAYS = 30
  const val PINGED_STONE_SHARE = 0.60
  const val TREND_FLAT_BAND = 0.05
  const val CUSUM_BASE_WEEKS = 3; const val CUSUM_K = 0.5; const val CUSUM_H = 4.0; const val CUSUM_SCALE_FLOOR = 0.10
  const val CLUSTER_MIN_WINDOWS = 40; const val CLUSTER_MIN_WEEK = 8; const val CLUSTER_MAX_K = 3
  const val CLUSTER_MIN_SILHOUETTE = 0.50; const val CLUSTER_SEED = 20261006L
  const val CLUSTER_CELL_SHARE = 0.70; const val CLUSTER_MIN_SIZE = 5
  const val CROSS_DAY_LATE_FROM_MIN = 30                  // 00:30 local
  const val CROSS_DAY_MIN_NIGHTS = 4; const val CROSS_DAY_SHORTER = 0.25
  // suggestions
  const val SUGGEST_MIN_VALID_DAYS = 8; const val SUGGEST_MIN_WINDOWS = 10
  const val SUGGEST_DISMISS_WEEKS = 4
  const val SUGGEST_EARLY_KINDS = "S1,S2,S4,S7"           // eligible from week 2
  const val SUGGEST_LATER_KINDS = "S3,S5,S6,S8,S9"        // eligible from week 4
  const val SUGGEST_SLOWDOWN_WEEK = 8                     // D7: then one per two weeks
  const val SUGGEST_SLOWDOWN_GAP_WEEKS = 2
  const val S1_SLOT_HOURS = 2; const val S1_MIN_WINDOWS = 3; const val S1_STAY_RATIO = 0.5; const val S1_STRETCH_RATIO = 1.5
  const val S2_SHARE = 0.35; const val S2_MIN_WINDOWS = 8
  const val S3_SHARE = 0.60; const val S4_SHARE = 0.60
  const val S5_RATIO = 1.5; const val S5_MIN_MIN = 8.0; const val S5_WEEKS = 2
  const val S6_SHARE = 0.50
  const val S7_COVERAGE = 0.25; const val S7_MIN_WINDOWS = 10
  const val S9_BAND_MIN = 5; const val S9_SHARE = 0.50; const val S9_MIN_WEEK = 4
  const val S10_MIN_GLANCES = 8; const val S10_GLANCE_PER_STAY = 8.0
  const val S12_IMPROVE = 0.15
  // judging
  const val JUDGE_DAYS = 14; const val JUDGE_MIN_WINDOWS = 8; const val JUDGE_MOVED = 0.20
  const val FOOTPRINT_DROP = 0.50
  // meter, lapse, lake, note
  const val OWN_OPEN_MERGE_SEC = 30
  const val LAPSE_MIN_DAYS = 3
  const val LAKE_CHOPPY_RATIO = 1.5; const val LAKE_MIN_WINDOWS = 3
  const val NOTE_HOUR_LOCAL = 8
  const val WORKSET_CAP = 12
  const val SAYING_OFFER_EVERY_WEEKS = 3
  const val GOAL_TAP_LAST_WEEK = 8                       // fades by about day 60
  const val PING_COVERAGE_PARTIAL = 0.70                 // F17
}
