package com.ctg.coptok.data.daos;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import com.ctg.coptok.data.entities.Draft;

@Dao
public interface DraftDao {

    @Query("SELECT * FROM drafts ORDER BY id DESC LIMIT :length")
    List<Draft> findAll(int length);

    @Query("SELECT * FROM drafts WHERE id < :after ORDER BY id DESC LIMIT :length")
    List<Draft> findAll(int after, int length);

    @Insert
    void insert(Draft... drafts);

    @Query("DELETE FROM drafts")
    void delete();

    @Delete
    void delete(Draft draft);

    @Query("DELETE FROM drafts WHERE id = :id")
    void delete(int id);

    @Update
    void update(Draft draft);
}
