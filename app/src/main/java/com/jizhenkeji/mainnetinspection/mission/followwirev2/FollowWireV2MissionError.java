package com.jizhenkeji.mainnetinspection.mission.followwirev2;

import com.jizhenkeji.mainnetinspection.common.JZIError;

public class FollowWireV2MissionError extends JZIError {

    public static final FollowWireV2MissionError MISSION_PARAMETER_ERROR =
            new FollowWireV2MissionError(1, "Mission configuration parameter error");

    public static final FollowWireV2MissionError EXECUTOR_STATE_ERROR =
            new FollowWireV2MissionError(2, "Executor state error");

    public static final FollowWireV2MissionError NO_RESPONSE =
            new FollowWireV2MissionError(3, "Operation no response");

    public FollowWireV2MissionError(int errorCode, String description) {
        super(errorCode, description);
    }

    @Override
    protected int getErrorMarkCode() { return 7 << 16; }

}
