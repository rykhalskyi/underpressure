package com.otakeessen.underpressure.ui.util

import com.otakeessen.underpressure.R
import com.otakeessen.underpressure.domain.BloodPressureLevel
import com.otakeessen.underpressure.domain.BpGuidelines
import org.junit.Assert.assertEquals
import org.junit.Test

class BpLevelMapperTest {

    @Test
    fun `getRangeStringRes returns correct AHA resources`() {
        assertEquals(R.string.bp_range_aha_hypotension, BpLevelMapper.getRangeStringRes(BloodPressureLevel.HYPOTENSION, BpGuidelines.AHA_ACC))
        assertEquals(R.string.bp_range_aha_normal, BpLevelMapper.getRangeStringRes(BloodPressureLevel.NORMAL, BpGuidelines.AHA_ACC))
        assertEquals(R.string.bp_range_aha_elevated, BpLevelMapper.getRangeStringRes(BloodPressureLevel.ELEVATED, BpGuidelines.AHA_ACC))
        assertEquals(R.string.bp_range_aha_stage1, BpLevelMapper.getRangeStringRes(BloodPressureLevel.STAGE_1, BpGuidelines.AHA_ACC))
        assertEquals(R.string.bp_range_aha_stage2, BpLevelMapper.getRangeStringRes(BloodPressureLevel.STAGE_2, BpGuidelines.AHA_ACC))
        assertEquals(R.string.bp_range_aha_crisis, BpLevelMapper.getRangeStringRes(BloodPressureLevel.CRISIS, BpGuidelines.AHA_ACC))
    }

    @Test
    fun `getRangeStringRes returns correct ESC resources`() {
        assertEquals(R.string.bp_range_esc_hypotension, BpLevelMapper.getRangeStringRes(BloodPressureLevel.HYPOTENSION, BpGuidelines.ESC_ESH))
        assertEquals(R.string.bp_range_esc_normal, BpLevelMapper.getRangeStringRes(BloodPressureLevel.NORMAL, BpGuidelines.ESC_ESH))
        assertEquals(R.string.bp_range_esc_elevated, BpLevelMapper.getRangeStringRes(BloodPressureLevel.ELEVATED, BpGuidelines.ESC_ESH))
        assertEquals(R.string.bp_range_esc_stage1, BpLevelMapper.getRangeStringRes(BloodPressureLevel.STAGE_1, BpGuidelines.ESC_ESH))
        assertEquals(R.string.bp_range_esc_stage2, BpLevelMapper.getRangeStringRes(BloodPressureLevel.STAGE_2, BpGuidelines.ESC_ESH))
        assertEquals(R.string.bp_range_esc_crisis, BpLevelMapper.getRangeStringRes(BloodPressureLevel.CRISIS, BpGuidelines.ESC_ESH))
    }

    @Test
    fun `getSourceStringRes returns correct resources`() {
        assertEquals(R.string.label_source_aha, BpLevelMapper.getSourceStringRes(BpGuidelines.AHA_ACC))
        assertEquals(R.string.label_source_esc, BpLevelMapper.getSourceStringRes(BpGuidelines.ESC_ESH))
    }
}
