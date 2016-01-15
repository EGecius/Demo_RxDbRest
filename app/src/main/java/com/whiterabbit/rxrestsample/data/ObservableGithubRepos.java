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

import android.app.Application;
import android.util.Log;

import com.whiterabbit.rxrestsample.rest.GitHubClient;

import java.util.List;

import javax.inject.Inject;

import rx.Observable;
import rx.schedulers.Schedulers;
import rx.subjects.BehaviorSubject;

/** Allows fetching repos from DB and from server */
public class ObservableGithubRepos {

    @Inject GitHubClient mClient;
    @Inject ObservableRepoDb mDatabase;
    @Inject Application mApplication;

    @Inject
    public ObservableGithubRepos() {
    }

	// TODO: 15/01/2016 rather than returning an underlying observable it should create one of its own, which in
	// OnSusbcribe will check whether it is time to update DB and wll do that
	/** Returns Observable which will emit contents of Database & any of its updates */
    public Observable<List<Repo>> getDbObservable() {
        Log.v("Eg:ObservableGithubRepos:48", "getDbObservable");
        Observable<List<Repo>> updateObservable = Observable.create(subscriber -> {
            scheduleUpdateIfNeeded();
            subscriber.onCompleted();
        });

        return updateObservable.concatWith(mDatabase.getObservable());
    }

    private void scheduleUpdateIfNeeded() {
        Log.i("Eg:ObservableGithubRepos:53", "scheduleUpdateIfNeeded");

        updateRepo("fedepaol");
    }

    /** Returns Observable informing about progress of an update.
	 * In case of success it 1) updates DB 2) Returns name of username, whose repo was updated */
    public Observable<String> updateRepo(String userName) {
        Log.d("Eg:ObservableGithubRepos:66", "updateRepo");
        BehaviorSubject<String> requestSubject = BehaviorSubject.create();

        Observable<List<Repo>> observable = mClient.getRepos(userName);
        observable.subscribeOn(Schedulers.io())
                  .observeOn(Schedulers.io())
                  .subscribe(l -> {


                          Log.w("Eg:ObservableGithubRepos:75", "updateRepo inserting into db size " + l.size());

                                    mDatabase.insertRepoList(l);
                                    requestSubject.onNext(userName);},
                             e -> requestSubject.onError(e),
                             () -> requestSubject.onCompleted());

        return requestSubject.asObservable();
    }

}
