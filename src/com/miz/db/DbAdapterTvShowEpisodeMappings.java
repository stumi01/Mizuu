/*
 * Copyright (C) 2014 Michell Bak
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.miz.db;

import java.util.ArrayList;

import com.miz.functions.MizLib;
import com.miz.mizuu.MizuuApplication;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;

public class DbAdapterTvShowEpisodeMappings extends AbstractDbAdapter {

	public static final String KEY_FILEPATH = "filepath";
	public static final String KEY_SHOW_ID = "show_id";
	public static final String KEY_SEASON = "season";
	public static final String KEY_EPISODE = "episode";
	public static final String KEY_IGNORED = "ignored";

	public static final String DATABASE_TABLE = "episodes_map";

	public static final String[] ALL_COLUMNS = new String[]{KEY_FILEPATH, KEY_SHOW_ID, KEY_SEASON, KEY_EPISODE, KEY_IGNORED};

	public DbAdapterTvShowEpisodeMappings(Context context) {
		super(context);
	}

	public long createFilepathMapping(String filepath, String showId, String season, String episode) {
		if (!filepathExists(showId, season, episode, filepath)) {
			// Add values to a ContentValues object
			ContentValues values = new ContentValues();
			values.put(KEY_FILEPATH, filepath);
			values.put(KEY_SHOW_ID, showId);
			values.put(KEY_SEASON, season);
			values.put(KEY_EPISODE, episode);
			values.put(KEY_IGNORED, 0);

			// Insert into database
			return mDatabase.insert(DATABASE_TABLE, null, values);
		}
		return -1;
	}

	public boolean filepathExists(String showId, String season, String episode, String filepath) {
		String[] selectionArgs = new String[]{showId, filepath, season, episode};
		Cursor cursor = mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, KEY_SHOW_ID + " = ? AND " + KEY_FILEPATH + " = ? AND "
				+ KEY_SEASON + " = ? AND " + KEY_EPISODE + " = ?", selectionArgs, null, null, null);
		boolean result = false;

		if (cursor != null) {
			try {
				if (cursor.getCount() > 0)
					result = true;
			} catch (Exception e) {
			} finally {
				cursor.close();
			}
		}

		return result;
	}

	public String getFirstFilepath(String showId, String season, String episode) {
		Cursor cursor = mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, KEY_SHOW_ID + "='" + showId + "' AND " + KEY_SEASON + "='" +
				season + "' AND " + KEY_EPISODE + "='" + episode + "'", null, null, null, null);
		String filepath = "";

		if (cursor != null) {
			try {
				if (cursor.moveToFirst()) {
					filepath = cursor.getString(cursor.getColumnIndex(KEY_FILEPATH));
				}
			} catch (Exception e) {
			} finally {
				cursor.close();
			}
		}

		return filepath;
	}

	public ArrayList<String> getFilepathsForEpisode(String showId, String season, String episode) {
		ArrayList<String> paths = new ArrayList<String>();

		Cursor cursor = mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, KEY_SHOW_ID + "='" + showId + "' AND " + KEY_SEASON + "='" +
				season + "' AND " + KEY_EPISODE + "='" + episode + "'", null, null, null, null);

		if (cursor != null) {
			try {
				while (cursor.moveToNext()) {
					paths.add(cursor.getString(cursor.getColumnIndex(KEY_FILEPATH)));
				}
			} catch (Exception e) {
			} finally {
				cursor.close();
			}
		}

		return paths;
	}

	public Cursor getAllFilepaths() {
		return mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, null, null, null, null, null);
	}

	public Cursor getAllFilepathInfo(String filepath) {
		String[] selectionArgs = new String[]{filepath};
		return mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, KEY_FILEPATH + " = ?", selectionArgs, null, null, null);
	}

	public Cursor getAllFilepaths(String showId) {
		return mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, KEY_SHOW_ID + "='" + showId + "'", null, null, null, null);
	}

	public Cursor getAllIgnoredFilepaths() {
		return mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, KEY_IGNORED + "='1'", null, null, null, null);
	}

	public boolean deleteFilepath(String filepath) {
		// Get the show ID, season and episode, so we can check if there are any other filepaths mapped to the episode
		// if not - we'll delete the episode entry from the episodes database as well.
		String showId, season, episode;
		String[] selectionArgs = new String[]{filepath};
		Cursor cursor = mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, KEY_FILEPATH + " = ?", selectionArgs, null, null, null);

		if (cursor != null && cursor.moveToFirst()) {
			showId = cursor.getString(cursor.getColumnIndex(KEY_SHOW_ID));
			season = cursor.getString(cursor.getColumnIndex(KEY_SEASON));
			episode = cursor.getString(cursor.getColumnIndex(KEY_EPISODE));

			if (!hasMultipleFilepaths(showId, season, episode)) {
				MizuuApplication.getTvEpisodeDbAdapter().deleteEpisode(showId, Integer.parseInt(season), Integer.parseInt(episode));
			}
		}

		return mDatabase.delete(DATABASE_TABLE, KEY_FILEPATH + " = ?", selectionArgs) > 0;
	}

	public boolean ignoreFilepath(String filepath) {
		// Get the show ID, season and episode, so we can check if there are any other filepaths mapped to the episode
		// if not - we'll delete the episode entry from the episodes database as well.
		String showId, season, episode;
		String[] selectionArgs = new String[]{filepath};
		Cursor cursor = mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, KEY_FILEPATH + " = ?", selectionArgs, null, null, null);

		if (cursor != null && cursor.moveToFirst()) {
			showId = cursor.getString(cursor.getColumnIndex(KEY_SHOW_ID));
			season = cursor.getString(cursor.getColumnIndex(KEY_SEASON));
			episode = cursor.getString(cursor.getColumnIndex(KEY_EPISODE));

			if (!hasMultipleFilepaths(showId, season, episode)) {
				MizuuApplication.getTvEpisodeDbAdapter().deleteEpisode(showId, Integer.parseInt(season), Integer.parseInt(episode));
			}
		}

		ContentValues values = new ContentValues();
		values.put(KEY_IGNORED, 1); // Set the ignored value to 1 (true)

		return mDatabase.update(DATABASE_TABLE, values, KEY_FILEPATH + " = ?", selectionArgs) > 0;
	}

	public boolean deleteAllFilepaths(String showId) {
		boolean result = MizuuApplication.getTvEpisodeDbAdapter().deleteAllEpisodes(showId); // This also deletes the show
		return result && mDatabase.delete(DATABASE_TABLE, KEY_SHOW_ID + "='" + showId + "'", null) > 0;
	}

	public boolean deleteAllFilepaths() {
		return mDatabase.delete(DATABASE_TABLE, null, null) > 0;
	}

	public boolean hasMultipleFilepaths(String showId, String season, String episode) {
		Cursor cursor = mDatabase.query(DATABASE_TABLE, ALL_COLUMNS, KEY_SHOW_ID + "='" + showId + "' AND " + KEY_SEASON + "='" +
				season + "' AND " + KEY_EPISODE + "='" + episode + "'", null, null, null, null);
		boolean result = false;

		if (cursor != null) {
			try {
				if (cursor.getCount() > 1)
					result = true;
			} catch (Exception e) {
			} finally {
				cursor.close();
			}
		}

		return result;
	}

	public boolean removeSeason(String showId, int season) {
		return mDatabase.delete(DATABASE_TABLE, KEY_SHOW_ID + "='" + showId + "' AND " + KEY_SEASON + "='" + MizLib.addIndexZero(season) + "'", null) > 0;
	}

	public boolean ignoreSeason(String showId, int season) {
		ContentValues values = new ContentValues();
		values.put(KEY_IGNORED, 1); // Set the ignored value to 1 (true)

		return mDatabase.update(DATABASE_TABLE, values, KEY_SHOW_ID + "='" + showId + "' AND " + KEY_SEASON + "='" + MizLib.addIndexZero(season) + "'", null) > 0;
	}
}