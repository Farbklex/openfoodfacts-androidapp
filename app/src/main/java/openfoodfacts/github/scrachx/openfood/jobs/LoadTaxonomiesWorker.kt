/*
 * Copyright 2016-2020 Open Food Facts
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package openfoodfacts.github.scrachx.openfood.jobs

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import io.reactivex.Completable
import kotlinx.coroutines.rx2.await
import openfoodfacts.github.scrachx.openfood.repositories.ProductRepository
import openfoodfacts.github.scrachx.openfood.utils.Utils

/**
 * @param appContext The application [Context]
 * @param workerParams Parameters to setup the internal state of this worker
 */
@HiltWorker
class LoadTaxonomiesWorker @AssistedInject constructor(
    @Assisted private val appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val repo: ProductRepository
) : CoroutineWorker(appContext, workerParams) {
    override suspend fun doWork(): Result {
        val settings = appContext.getSharedPreferences("prefs", 0)

        // We use completable because we only care about state (error or completed), not returned value
        val syncObservables = listOf(
            repo.reloadLabelsFromServer().ignoreElement(),
            repo.reloadTagsFromServer().ignoreElement(),
            repo.reloadInvalidBarcodesFromServer().ignoreElement(),
            repo.reloadAllergensFromServer().ignoreElement(),
            repo.reloadIngredientsFromServer().ignoreElement(),
            repo.reloadAnalysisTagConfigsFromServer().ignoreElement(),
            repo.reloadAnalysisTagsFromServer().ignoreElement(),
            repo.reloadCountriesFromServer().ignoreElement(),
            repo.reloadAdditivesFromServer().ignoreElement(),
            repo.reloadCategoriesFromServer().ignoreElement(),
            repo.reloadStatesFromServer().ignoreElement(),
            repo.reloadStoresFromServer().ignoreElement(),
            repo.reloadBrandsFromServer().ignoreElement()
        )
        return try {
            Completable.merge(syncObservables).await()
            settings.edit { putBoolean(Utils.FORCE_REFRESH_TAXONOMIES, false) }
            Result.success()
        } catch (err: Throwable) {
            Log.e(LOG_TAG, "Cannot download taxonomies from server.", err)
            Result.failure()
        }
    }

    companion object {
        private val LOG_TAG = LoadTaxonomiesWorker::class.simpleName
    }
}