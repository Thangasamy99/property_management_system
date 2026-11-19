package com.pcc.identity.service;

import com.pcc.identity.dto.response.DashboardStatsResponse;

import com.pcc.identity.dto.response.DashboardStatsResponse;

import java.util.List;

public interface StatsService {

    DashboardStatsResponse dashboard();

    List<DashboardStatsResponse.ActivityPoint> activity(int days);
}
