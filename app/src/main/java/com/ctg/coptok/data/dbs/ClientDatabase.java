package com.ctg.coptok.data.dbs;

import androidx.room.Database;
import androidx.room.RoomDatabase;

import com.ctg.coptok.data.daos.DraftDao;
import com.ctg.coptok.data.entities.Draft;

@Database(entities = {Draft.class}, version = 4, exportSchema = false)
public abstract class ClientDatabase extends RoomDatabase {

    public abstract DraftDao drafts();
}
