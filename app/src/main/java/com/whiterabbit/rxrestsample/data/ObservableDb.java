/*
 * Copyright (C) 2015 Federico Paolinelli
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *       http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package com.whiterabbit.rxrestsample.data;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

import rx.Observable;
import rx.subjects.PublishSubject;

/** Wraps our implementation of {@link RepoDbHelper} */
class ObservableDb {

	/** Will receive onNext calls on every update of DB */
    private PublishSubject<List<Repo>> mUpdatesSubject = PublishSubject.create();

	/** Wrapper around our implementation of {@link SQLiteOpenHelper} */
    private RepoDbHelper mDbHelper;

    public ObservableDb(Context c) {
        mDbHelper = new RepoDbHelper(c);
    }

	/** Returns Observable which emits current contents of DB contents after every update of DB */
    public Observable<List<Repo>> getObservable() {
        Observable<List<Repo>> firstTimeObservable = Observable.fromCallable(this::getAllReposFromDb);
        return firstTimeObservable.concatWith(mUpdatesSubject);
    }

    private List<Repo> getAllReposFromDb() {
        mDbHelper.openForRead();
        List<Repo> repos = new ArrayList<>();
        Cursor c = mDbHelper.getAllRepo();
        if (!c.moveToFirst()) { // empty
            return repos;
        }
        do {
            repos.add(new Repo(c.getString(RepoDbHelper.REPO_ID_COLUMN_POSITION),
                               c.getString(RepoDbHelper.REPO_NAME_COLUMN_POSITION),
                               c.getString(RepoDbHelper.REPO_FULLNAME_COLUMN_POSITION),
                               new Repo.Owner(c.getString(RepoDbHelper.REPO_OWNER_COLUMN_POSITION),
                                              "", "", "")));
        } while (c.moveToNext());
        c.close();
        mDbHelper.close();
        return repos;
    }

	void insertRepoList(List<Repo> repos) {
        // This could have been done inside a transaction + yieldIfContendedSafely
        mDbHelper.open();
        mDbHelper.removeAllRepo();
        for (Repo r : repos) {
            mDbHelper.addRepo(r.getId(),
                              r.getName(),
                              r.getFullName(),
                              r.getOwner().getLogin());
        }
        mDbHelper.close();
        mUpdatesSubject.onNext(repos);
    }

	void insertRepo(Repo r) {
        mDbHelper.open();
        mDbHelper.addRepo(r.getId(),
                r.getName(),
                r.getFullName(),
                r.getOwner().getLogin());

        mDbHelper.close();

        List<Repo> result = getAllReposFromDb();
        mUpdatesSubject.onNext(result);
    }

    /** Removes all repos from db */
    void clearDb() {
        mDbHelper.open();
        mDbHelper.removeAllRepo();
    }

}
