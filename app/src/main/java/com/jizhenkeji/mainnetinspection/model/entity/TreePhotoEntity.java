package com.jizhenkeji.mainnetinspection.model.entity;

import androidx.room.Entity;

@Entity
public class TreePhotoEntity extends PhotoEntity {

    private static final long serialVersionUID = 439079622216437181L;

    /* 水平离树障距离 */
    public float treeBarrierXDistance;

    /* 垂直离树障距离 */
    public float treeBarrierYDistance;

}
