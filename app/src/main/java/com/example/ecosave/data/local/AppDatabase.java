package com.example.ecosave.data.local;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import com.example.ecosave.model.BudgetEntry;
import com.example.ecosave.ui.chat.ChatMessage;

@Database(entities = {BudgetEntry.class, ChatMessage.class}, version = 2, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {
    public abstract BudgetDao budgetDao();
    public abstract ChatDao chatDao();

    private static volatile AppDatabase INSTANCE;

    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE IF NOT EXISTS `chat_messages` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `text` TEXT, `isUser` INTEGER NOT NULL)");
        }
    };

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                            AppDatabase.class, "ecosave_database")
                            .addMigrations(MIGRATION_1_2)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
