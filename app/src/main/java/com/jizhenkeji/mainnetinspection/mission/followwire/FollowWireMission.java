package com.jizhenkeji.mainnetinspection.mission.followwire;

import java.util.ArrayList;
import java.util.List;

public class FollowWireMission {

    private List<BasePublisher> mPublisherList = new ArrayList<>();

    private List<BaseHandler> mhandlerList = new ArrayList<>();

    public List<BasePublisher> getPublishers(){
        return mPublisherList;
    }

    public List<BaseHandler> getHandlers(){
        return mhandlerList;
    }

    public static class Builder{

        private FollowWireMission mFollowWireMission;

        public Builder(){
            mFollowWireMission = new FollowWireMission();
        }

        public Builder addPublisher(BasePublisher publisher){
            mFollowWireMission.mPublisherList.add(publisher);
            return this;
        }

        public Builder addHandler(BaseHandler handler){
            mFollowWireMission.mhandlerList.add(handler);
            return this;
        }

        public FollowWireMission build(){
            return mFollowWireMission;
        }

    }

}
