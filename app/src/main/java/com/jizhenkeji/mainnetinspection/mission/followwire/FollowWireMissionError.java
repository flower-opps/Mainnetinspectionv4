package com.jizhenkeji.mainnetinspection.mission.followwire;

import com.jizhenkeji.mainnetinspection.common.JZIError;

public class FollowWireMissionError extends JZIError {

    public static final FollowWireMissionError INTERNAL_STATE_ERROR = new FollowWireMissionError(1, "Cannot perform this operation in the current state");

    public static final FollowWireMissionError NO_RESPONSE = new FollowWireMissionError(2, "No response");

    public static final FollowWireMissionError MISSING_PRECONDITION = new FollowWireMissionError(3, "Missing precondition");

    public FollowWireMissionError(int errorCode, String description) {
        super(errorCode, description);
    }

    @Override
    protected int getErrorMarkCode() { return 6 << 16; }

}
