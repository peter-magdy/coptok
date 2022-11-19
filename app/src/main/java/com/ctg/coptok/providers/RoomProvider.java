package com.ctg.coptok.providers;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Room;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.vaibhavpandey.katora.contracts.MutableContainer;
import com.vaibhavpandey.katora.contracts.Provider;

import com.ctg.coptok.data.dbs.ClientDatabase;

public class RoomProvider implements Provider {

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {

        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE drafts ADD COLUMN allow_duet INTEGER DEFAULT 0 NOT NULL");
        }
    };

    static final Migration MIGRATION_2_3 = new Migration(2, 3) {

        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE drafts ADD COLUMN cta_label TEXT DEFAULT NULL");
            database.execSQL("ALTER TABLE drafts ADD COLUMN cta_link TEXT DEFAULT NULL");
        }
    };

    static final Migration MIGRATION_3_4 = new Migration(3, 4) {

        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            database.execSQL("ALTER TABLE drafts ADD COLUMN location TEXT DEFAULT NULL");
            database.execSQL("ALTER TABLE drafts ADD COLUMN latitude REAL DEFAULT NULL");
            database.execSQL("ALTER TABLE drafts ADD COLUMN longitude REAL DEFAULT NULL");
        }
    };

    private final Context mContext;

    public RoomProvider(Context context) {
        mContext = context;
    }

    @Override
    public void provide(MutableContainer container) {
        container.singleton(ClientDatabase.class, c ->
                Room.databaseBuilder(mContext, ClientDatabase.class, "client")
                        .allowMainThreadQueries()
                        .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                        .build()
        );
    }
}
