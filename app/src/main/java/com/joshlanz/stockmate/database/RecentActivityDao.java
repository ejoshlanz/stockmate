package com.joshlanz.stockmate.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Delete;
import androidx.room.Update;
import androidx.room.OnConflictStrategy;

import com.joshlanz.stockmate.models.RecentActivity;

import java.util.List;

@Dao
public interface RecentActivityDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(RecentActivity activity);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<RecentActivity> activities);

    @Query("SELECT * FROM recent_activity ORDER BY id DESC")
    List<RecentActivity> getAllActivities();

    @Query("DELETE FROM recent_activity")
    void clearAll();

    @Delete
    void delete(RecentActivity activity);

    @Update
    void update(RecentActivity activity);


}
